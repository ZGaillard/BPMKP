package solver;

import ca.udem.gaillarz.formulation.DualValues;
import ca.udem.gaillarz.formulation.Pattern;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.KnapsackResult;
import ca.udem.gaillarz.solver.KnapsackSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pricing subproblems for column generation.
 * Finds patterns with positive reduced cost for P_0 and each P_i pool.
 */
public class PricingProblem {

    private static final double TOLERANCE = 1e-6;

    private final MKPInstance instance;

    public PricingProblem(MKPInstance instance) {
        this.instance = instance;
    }

    /**
     * Solve pricing for the aggregated pool P_0.
     *
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

        double[] modifiedProfits = new double[n];
        for (int j = 0; j < n; j++) {
            modifiedProfits[j] = instance.getItem(j).getProfit() * (1.0 - tau) - mu[j];
        }

        KnapsackSolver solver = new KnapsackSolver(instance.getItems(), instance.getTotalCapacity(), modifiedProfits);
        KnapsackResult result = solver.solve();

        double reducedCost = result.getOptimalProfit() - pi0;
        if (reducedCost > TOLERANCE) {
            Pattern pattern = Pattern.fromItemIds(result.getSelectedItemIds(), instance);
            return new PricingResult(pattern, reducedCost, 0, true);
        }
        return null;
    }

    /**
     * Solve pricing for pool P_i (one knapsack).
     *
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

        double[] modifiedProfits = Arrays.copyOf(mu, n);

        KnapsackSolver solver = new KnapsackSolver(
                instance.getItems(),
                instance.getKnapsack(knapsackId).getCapacity(),
                modifiedProfits
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
}
