package ca.udem.gaillarz.preprocessing;

import ca.udem.gaillarz.formulation.DualValues;
import ca.udem.gaillarz.formulation.L2Solution;
import ca.udem.gaillarz.model.MKPInstance;

/**
 * Extracts reduced costs for item selection variables using dual information.
 */
public class ReducedCostExtractor {

    private final MKPInstance instance;

    public ReducedCostExtractor(MKPInstance instance) {
        this.instance = instance;
    }

    /**
     * Extract reduced costs from the L2 solution and dual values.
     *
     * @param l2Solution L2 relaxation solution
     * @param dualValues Dual values from DW master
     * @return Reduced cost for each item
     */
    public double[] extractReducedCosts(L2Solution l2Solution, DualValues dualValues) {
        if (l2Solution == null) {
            throw new IllegalArgumentException("L2 solution cannot be null");
        }
        if (dualValues == null) {
            throw new IllegalArgumentException("Dual values cannot be null");
        }

        int n = instance.getNumItems();
        double[] reducedCosts = new double[n];
        double[] mu = dualValues.mu();
        double tau = dualValues.tau();

        for (int j = 0; j < n; j++) {
            double tj = l2Solution.getItemSelection(j);
            int profit = instance.getItem(j).profit();
            double base = profit * (1 - tau) - mu[j];
            reducedCosts[j] = (tj < 0.5) ? base : -base;
        }

        return reducedCosts;
    }

    /**
     * Estimate reduced costs when dual information is not available.
     *
     * @param l2Solution L2 relaxation solution
     * @return Heuristic reduced costs for each item
     */
    public double[] estimateReducedCosts(L2Solution l2Solution) {
        if (l2Solution == null) {
            throw new IllegalArgumentException("L2 solution cannot be null");
        }

        int n = instance.getNumItems();
        double[] reducedCosts = new double[n];

        for (int j = 0; j < n; j++) {
            double tj = l2Solution.getItemSelection(j);
            int profit = instance.getItem(j).profit();

            if (tj < 0.01) {
                reducedCosts[j] = profit * 0.5;
            } else if (tj > 0.99) {
                reducedCosts[j] = -profit * 0.5;
            } else {
                reducedCosts[j] = profit * (0.5 - tj);
            }
        }

        return reducedCosts;
    }
}
