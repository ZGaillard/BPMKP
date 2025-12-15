package ca.udem.gaillarz.solver.cg;

import ca.udem.gaillarz.formulation.*;
import ca.udem.gaillarz.solver.lp.LPSolution;
import ca.udem.gaillarz.solver.lp.LPSolver;
import ca.udem.gaillarz.solver.lp.LinearProgram;

import java.util.ArrayList;
import java.util.List;

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

            // 3) Pricing
            List<PricingResult> improving = pricingProblem.solveAll(dualValues);

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
            for (PricingResult result : improving) {
                if (result.p0()) {
                    master.addPatternP0(result.pattern());
                } else {
                    master.addPatternPI(result.poolIndex(), result.pattern());
                }
                added++;
            }
            patternsAdded += added;

            if (params.isVerbose()) {
                System.out.printf("CG iter %3d: obj=%.4f, added=%d, total patterns=%d%n",
                        iterations, currentObj, added, master.getTotalPatternCount());
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
}
