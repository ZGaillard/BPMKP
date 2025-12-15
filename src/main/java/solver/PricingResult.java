package solver;

import ca.udem.gaillarz.formulation.Pattern;

/**
 * Result of a pricing subproblem.
 */
public class PricingResult {
    private final Pattern pattern;
    private final double reducedCost;
    private final int poolIndex; // 0-based knapsack index for P_i; 0 for P_0
    private final boolean p0;

    public PricingResult(Pattern pattern, double reducedCost, int poolIndex, boolean isP0) {
        this.pattern = pattern;
        this.reducedCost = reducedCost;
        this.poolIndex = poolIndex;
        this.p0 = isP0;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public double getReducedCost() {
        return reducedCost;
    }

    public int getPoolIndex() {
        return poolIndex;
    }

    public boolean isP0() {
        return p0;
    }

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
