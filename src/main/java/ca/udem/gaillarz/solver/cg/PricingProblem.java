package ca.udem.gaillarz.solver.cg;

import ca.udem.gaillarz.formulation.DualValues;
import ca.udem.gaillarz.formulation.Pattern;
import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.knapsack.KnapsackResult;
import ca.udem.gaillarz.solver.knapsack.KnapsackSolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pricing subproblems for column generation.
 * Finds patterns with positive reduced cost for P_0 and each P_i pool.
 */
public class PricingProblem {

    private static final double TOLERANCE = 1e-6;

    private final MKPInstance instance;
    private Set<Integer> forbiddenItems = Set.of();
    private Set<Integer> requiredItems = Set.of();

    public PricingProblem(MKPInstance instance) {
        this.instance = instance;
    }

    public void setBranchingConstraints(Set<Integer> forbidden, Set<Integer> required) {
        this.forbiddenItems = new HashSet<>(forbidden);
        this.requiredItems = new HashSet<>(required);
    }

    /**
     * Solve pricing for the aggregated pool P_0.
     * <p>
     * Reduced cost: ζ_0 = profit(a)*(1-τ) - Σ_j μ_j*a_j - π_0
     *
     * @param dualValues dual values from the current restricted master
     * @return improving pattern or null if none found
     */
    public PricingResult solveForP0(DualValues dualValues) {
        int n = instance.getNumItems();
        double[] mu = dualValues.mu();
        double pi0 = dualValues.pi()[0];
        double tau = dualValues.tau();

        double baseProfit = 0.0;
        int requiredWeight = 0;
        for (int req : requiredItems) {
            if (forbiddenItems.contains(req)) {
                return null; // inconsistent branching
            }
            requiredWeight += instance.getItem(req).weight();
            if (requiredWeight > instance.getTotalCapacity()) {
                return null; // infeasible given required items
            }
            baseProfit += instance.getItem(req).profit() * (1.0 - tau) - mu[req];
        }

        int remainingCapacity = instance.getTotalCapacity() - requiredWeight;
        if (remainingCapacity < 0) {
            return null;
        }

        List<Double> modifiedProfits = new ArrayList<>();
        List<Item> allowedItems = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            if (forbiddenItems.contains(j) || requiredItems.contains(j)) {
                continue;
            }
            allowedItems.add(instance.getItem(j));
            modifiedProfits.add(instance.getItem(j).profit() * (1.0 - tau) - mu[j]);
        }

        KnapsackSolver solver = new KnapsackSolver(allowedItems, remainingCapacity, toArray(modifiedProfits));
        KnapsackResult result = solver.solve();

        Set<Integer> chosen = new HashSet<>(result.getSelectedItemIds());
        chosen.addAll(requiredItems);

        double reducedCost = baseProfit + result.getOptimalProfit() - pi0;
        if (reducedCost > TOLERANCE) {
            Pattern pattern = Pattern.fromItemIds(chosen, instance);
            return new PricingResult(pattern, reducedCost, 0, true);
        }
        return null;
    }

    /**
     * Solve pricing for pool P_i (one knapsack).
     * <p>
     * Reduced cost: ζ_i = Σ_j μ_j*a_j - π_i
     *
     * @param knapsackId knapsack index
     * @param dualValues dual values from the current restricted master
     * @return improving pattern or null if none found
     */
    public PricingResult solveForPI(int knapsackId, DualValues dualValues) {
        int n = instance.getNumItems();
        double[] mu = dualValues.mu();
        double pi = dualValues.pi()[knapsackId + 1];

        List<Item> allowedItems = new ArrayList<>();
        List<Double> modifiedProfits = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            if (forbiddenItems.contains(j)) {
                continue;
            }
            allowedItems.add(instance.getItem(j));
            modifiedProfits.add(mu[j]);
        }

        KnapsackSolver solver = new KnapsackSolver(
                allowedItems,
                instance.getKnapsack(knapsackId).capacity(),
                toArray(modifiedProfits)
        );
        KnapsackResult result = solver.solve();

        double reducedCost = result.getOptimalProfit() - pi;
        if (reducedCost > TOLERANCE) {
            Pattern pattern = Pattern.fromItemIds(result.getSelectedItemIds(), instance);
            return new PricingResult(pattern, reducedCost, knapsackId, false);
        }
        return null;
    }

    /**
     * Run pricing for P_0 and all P_i pools.
     *
     * @param dualValues dual values from the current restricted master
     * @return list of improving patterns (may be empty)
     */
    public List<PricingResult> solveAll(DualValues dualValues) {
        List<PricingResult> results = new ArrayList<>();

        PricingResult p0 = solveForP0(dualValues);
        if (p0 != null) {
            results.add(p0);
        }

        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            PricingResult pi = solveForPI(i, dualValues);
            if (pi != null) {
                results.add(pi);
            }
        }

        return results;
    }

    private double[] toArray(List<Double> values) {
        double[] arr = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            arr[i] = values.get(i);
        }
        return arr;
    }
}
