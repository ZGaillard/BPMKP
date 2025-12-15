package ca.udem.gaillarz.solver.cg;

import ca.udem.gaillarz.formulation.*;
import ca.udem.gaillarz.solver.bp.BranchAndPrice;
import ca.udem.gaillarz.solver.lp.LPSolution;
import ca.udem.gaillarz.solver.lp.LPSolver;
import ca.udem.gaillarz.solver.lp.LinearProgram;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Column generation driver for the Dantzig-Wolfe master LP relaxation.
 */
public class ColumnGeneration {

    private final DantzigWolfeMaster master;
    private final LPSolver lpSolver;
    private final PricingProblem pricingProblem;

    // Statistics
    private int iterations;
    private int patternsAdded;
    private double bestObjective;
    private long totalTimeMs;

    public ColumnGeneration(DantzigWolfeMaster master, LPSolver lpSolver) {
        this.master = master;
        this.lpSolver = lpSolver;
        this.pricingProblem = new PricingProblem(master.getInstance());
        this.iterations = 0;
        this.patternsAdded = 0;
        this.bestObjective = Double.NEGATIVE_INFINITY;
        this.totalTimeMs = 0L;
    }

    public ColumnGeneration setBranchingConstraints(Set<Integer> forbiddenItems, Set<Integer> requiredItems) {
        this.pricingProblem.setBranchingConstraints(forbiddenItems, requiredItems);
        return this;
    }

    public CGResult solve() {
        return solve(new CGParameters());
    }

    /**
     * Solve using column generation with the provided parameters.
     */
    public CGResult solve(CGParameters params) {
        long start = System.currentTimeMillis();

        List<Double> objectiveHistory = new ArrayList<>();
        iterations = 0;
        patternsAdded = 0;
        bestObjective = Double.NEGATIVE_INFINITY;

        while (iterations < params.getMaxIterations()) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > params.getTimeLimitMs()) {
                totalTimeMs = elapsed;
                return new CGResult(CGStatus.TIME_LIMIT, null, null, null,
                        bestObjective, iterations, patternsAdded, objectiveHistory, totalTimeMs);
            }

            iterations++;

            // 1) Build and solve RMP
            DWMasterLPBuilder builder = new DWMasterLPBuilder(master);
            LinearProgram lp = builder.buildLP();
            // Apply no-good cuts if available
            if (master instanceof ca.udem.gaillarz.solver.bp.SupportsNoGoodCuts ng) {
                ng.getCutManager().addCutsToLP(lp, master.getPatternsP0());
            }
            LPSolution lpSolution = lpSolver.solve(lp);

            if (lpSolution.isUnbounded()) {
                totalTimeMs = System.currentTimeMillis() - start;
                return new CGResult(CGStatus.UNBOUNDED, null, null, null,
                        Double.POSITIVE_INFINITY, iterations, patternsAdded, objectiveHistory, totalTimeMs);
            }
            if (!lpSolution.isOptimal() && !lpSolution.isFeasible()) {
                totalTimeMs = System.currentTimeMillis() - start;
                return new CGResult(CGStatus.INFEASIBLE, null, null, null,
                        Double.NaN, iterations, patternsAdded, objectiveHistory, totalTimeMs);
            }

            double currentObj = lpSolution.objectiveValue();
            bestObjective = currentObj;
            objectiveHistory.add(currentObj);

            // 2) Extract dual values
            DualValues dualValues = builder.extractDualValues(lpSolution, lp);

            if (master instanceof BranchAndPrice.DantzigWolfeFormulationWithPatterns bm) {
                pricingProblem.setBranchingConstraints(bm.getForbiddenItems(), bm.getRequiredItems());
            }

            // 3) Pricing
            List<PricingResult> improving = pricingProblem.solveAll(dualValues);
            if (params.isVerbose()) {
                logPricing(improving);
            }

            // 4) Optimality check
            if (improving.isEmpty()) {
                DWSolution dwSolution = builder.extractDWSolution(lpSolution, lp);
                L2Solution l2Solution = master.toL2Solution(dwSolution);
                totalTimeMs = System.currentTimeMillis() - start;
                return new CGResult(CGStatus.OPTIMAL, dwSolution, l2Solution, dualValues,
                        currentObj, iterations, patternsAdded, objectiveHistory, totalTimeMs);
            }

            // 5) Add new columns
            int added = 0;
            int rejectedForBranching = 0;
            int duplicates = 0;
            BranchAndPrice.DantzigWolfeFormulationWithPatterns branchMaster =
                    (master instanceof BranchAndPrice.DantzigWolfeFormulationWithPatterns bm) ? bm : null;
            Set<Integer> forbidden = branchMaster != null ? branchMaster.getForbiddenItems() : Set.of();
            Set<Integer> required = branchMaster != null ? branchMaster.getRequiredItems() : Set.of();

            for (PricingResult result : improving) {
                boolean incompatible = branchMaster != null && violatesBranching(result, forbidden, required);
                boolean duplicate = isDuplicate(result);

                if (incompatible) {
                    rejectedForBranching++;
                }
                if (duplicate) {
                    duplicates++;
                }

                if (!incompatible && !duplicate) {
                    if (result.p0()) {
                        master.addPatternP0(result.pattern());
                    } else {
                        master.addPatternPI(result.poolIndex(), result.pattern());
                    }
                    added++;
                }

                if (params.isVerbose() && (incompatible || duplicate)) {
                    System.out.printf("[CG] Pricing %s %s%s%n",
                            result.p0() ? "P0" : "P" + (result.poolIndex() + 1),
                            incompatible ? "violates branching; skipping column" : "duplicate column; skipping",
                            incompatible && duplicate ? " (also duplicate)" : "");
                }
            }
            patternsAdded += added;

            if (params.isVerbose()) {
                System.out.printf("CG iter %3d: obj=%.4f, pricing=%d, added=%d, rejected=%d, duplicates=%d, total patterns=%d%n",
                        iterations, currentObj, improving.size(), added, rejectedForBranching, duplicates, master.getTotalPatternCount());
            }

            // Guard against infinite loop: pricing returned only duplicates/incompatible columns.
            if (added == 0) {
                if (params.isVerbose()) {
                    System.out.println("[CG] No new columns added (all duplicates/incompatible); treating as optimal for current pool.");
                }
                DWSolution dwSolution = builder.extractDWSolution(lpSolution, lp);
                L2Solution l2Solution = master.toL2Solution(dwSolution);
                totalTimeMs = System.currentTimeMillis() - start;
                return new CGResult(CGStatus.OPTIMAL, dwSolution, l2Solution, dualValues,
                        currentObj, iterations, patternsAdded, objectiveHistory, totalTimeMs);
            }
        }

        totalTimeMs = System.currentTimeMillis() - start;
        return new CGResult(CGStatus.ITERATION_LIMIT, null, null, null,
                bestObjective, iterations, patternsAdded, objectiveHistory, totalTimeMs);
    }

    // Accessors
    public int getIterations() {
        return iterations;
    }

    public int getPatternsAdded() {
        return patternsAdded;
    }

    public double getBestObjective() {
        return bestObjective;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    private boolean violatesBranching(PricingResult result, Set<Integer> forbidden, Set<Integer> required) {
        Pattern pattern = result.pattern();
        for (int f : forbidden) {
            if (pattern.containsItem(f)) {
                return true;
            }
        }
        if (result.p0()) {
            for (int r : required) {
                if (!pattern.containsItem(r)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDuplicate(PricingResult result) {
        Pattern pattern = result.pattern();
        if (result.p0()) {
            for (Pattern p : master.getPatternsP0()) {
                if (p.hasSameItems(pattern)) {
                    return true;
                }
            }
        } else {
            for (Pattern p : master.getPatternsPI(result.poolIndex())) {
                if (p.hasSameItems(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void logPricing(List<PricingResult> improving) {
        if (improving.isEmpty()) {
            System.out.println("[CG] Pricing found no positive reduced-cost columns.");
        }
    }
}
