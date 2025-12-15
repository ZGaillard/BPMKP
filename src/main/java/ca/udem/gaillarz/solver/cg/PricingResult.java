package ca.udem.gaillarz.solver.cg;

import ca.udem.gaillarz.formulation.Pattern;

/**
 * Result of a pricing subproblem.
 *
 * @param poolIndex 0-based knapsack index for P_i; 0 for P_0
 */
public record PricingResult(Pattern pattern, double reducedCost, int poolIndex, boolean p0) {

    public boolean isPI() {
        return !p0;
    }

    @Override
    public String toString() {
        String pool = p0 ? "P_0" : "P_" + (poolIndex + 1);
        return String.format("PricingResult(%s, items=%s, rc=%.4f)",
                pool, pattern.getItemIds(), reducedCost);
    }
}
