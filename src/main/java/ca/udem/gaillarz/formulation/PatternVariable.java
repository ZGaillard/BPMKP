package ca.udem.gaillarz.formulation;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a pattern variable y_a in the Dantzig-Wolfe master formulation.
 * Combines pattern content with pool membership to create a unique variable identifier.
 *
 * <p>This solves the problem of the same pattern appearing in different pools
 * (e.g., pattern {0,2} in both P_0 and P_1) representing different variables.
 *
 * <p>Example:
 * <pre>
 * Pattern p = Pattern.singleItem(0, instance);
 *
 * PatternVariable y_p0 = PatternVariable.forP0(p);        // y_a for pattern in P_0
 * PatternVariable y_p1 = PatternVariable.forPI(p, 0);     // y_a for pattern in P_1
 *
 * // These are DIFFERENT variables even though pattern content is same
 * assert !y_p0.equals(y_p1);
 * </pre>
 */
public class PatternVariable {

    private final Pattern pattern;
    private final PoolType poolType;
    private final int poolIndex;  // For PI: knapsack index (0 to m-1)

    /**
     * Private constructor - use factory methods.
     */
    private PatternVariable(Pattern pattern, PoolType poolType, int poolIndex) {
        this.pattern = pattern;
        this.poolType = poolType;
        this.poolIndex = poolIndex;
    }
    // For P0: always -1

    /**
     * Create a pattern variable for the P_0 pool (aggregated capacity).
     *
     * @param pattern Pattern in P_0
     * @return PatternVariable for y_a where a ∈ P_0
     */
    public static PatternVariable forP0(Pattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        return new PatternVariable(pattern, PoolType.P0, -1);
    }

    /**
     * Create a pattern variable for a P_i pool (individual knapsack).
     *
     * @param pattern    Pattern in P_i
     * @param knapsackId Knapsack index (0-based)
     * @return PatternVariable for y_a where a ∈ P_i
     */
    public static PatternVariable forPI(Pattern pattern, int knapsackId) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        if (knapsackId < 0) {
            throw new IllegalArgumentException("Knapsack ID must be non-negative");
        }
        return new PatternVariable(pattern, PoolType.PI, knapsackId);
    }

    /**
     * @return The pattern associated with this variable
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @return The pool type (P0 or PI)
     */
    public PoolType getPoolType() {
        return poolType;
    }

    /**
     * @return The pool index (knapsack ID for PI, -1 for P0)
     */
    public int getPoolIndex() {
        return poolIndex;
    }

    /**
     * @return true if this variable is in the P_0 pool
     */
    public boolean isP0() {
        return poolType == PoolType.P0;
    }

    /**
     * @return true if this variable is in a P_i pool
     */
    public boolean isPI() {
        return poolType == PoolType.PI;
    }

    /**
     * Get the knapsack ID for PI variables.
     *
     * @return Knapsack index (0-based)
     * @throws IllegalStateException if this is a P0 variable
     */
    public int getKnapsackId() {
        if (!isPI()) {
            throw new IllegalStateException("Cannot get knapsack ID for P0 variable");
        }
        return poolIndex;
    }

    /**
     * Two pattern variables are equal if they have:
     * - Same pool type (P0 or PI)
     * - Same pool index (for PI)
     * - Same pattern CONTENT (not object identity!)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatternVariable that = (PatternVariable) o;
        return poolType == that.poolType &&
                poolIndex == that.poolIndex &&
                pattern.hasSameItems(that.pattern);
    }

    /**
     * Hash based on pool membership and pattern content.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(pattern.getItems()),
                poolType,
                poolIndex
        );
    }

    /**
     * @return String representation: "y_P0[{0,2,4}]" or "y_P1[{0,2,4}]"
     */
    @Override
    public String toString() {
        if (isP0()) {
            return String.format("y_P0[%s]", pattern.getItemIds());
        } else {
            return String.format("y_P%d[%s]", poolIndex + 1, pattern.getItemIds());
        }
    }

    /**
     * Pool type in the Dantzig-Wolfe master formulation.
     */
    public enum PoolType {
        /**
         * Aggregated capacity pool (P_0)
         */
        P0,
        /**
         * Individual knapsack pool (P_i for i = 1, ..., m)
         */
        PI
    }
}