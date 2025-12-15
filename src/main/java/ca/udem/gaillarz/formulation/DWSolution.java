package ca.udem.gaillarz.formulation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Solution to the Dantzig-Wolfe master formulation.
 * Contains y_a (pattern values) and s_j (dual cut values).
 * <p>
 * REDESIGNED: Uses PatternVariable for content-based pattern identification
 * with explicit pool tracking.
 */
public class DWSolution {
    private static final double TOLERANCE = 1e-5;

    private final Map<PatternVariable, Double> patternValues;  // y_a values
    private final Map<Integer, Double> dualCutValues;          // s_j values
    private final int numItems;

    /**
     * Create DW solution from pattern variables and dual cut values.
     *
     * @param patternValues Map from pattern variables to their y_a values
     * @param dualCutValues Map from item indices to their s_j values
     */
    public DWSolution(Map<PatternVariable, Double> patternValues,
                      Map<Integer, Double> dualCutValues) {
        if (patternValues == null) {
            throw new IllegalArgumentException("Pattern values cannot be null");
        }
        if (dualCutValues == null) {
            throw new IllegalArgumentException("Dual cut values cannot be null");
        }

        this.patternValues = new HashMap<>(patternValues);  // Regular HashMap!
        this.dualCutValues = new HashMap<>(dualCutValues);
        this.numItems = dualCutValues.size();
    }

    /**
     * Create solution with zero dual cuts.
     *
     * @param patternValues Map from pattern variables to their y_a values
     * @param numItems      Number of items
     */
    public DWSolution(Map<PatternVariable, Double> patternValues, int numItems) {
        this.patternValues = new HashMap<>(patternValues);
        this.dualCutValues = new HashMap<>();
        for (int j = 0; j < numItems; j++) {
            dualCutValues.put(j, 0.0);
        }
        this.numItems = numItems;
    }

    // ========== Query Methods ==========

    /**
     * Get y_a value for a pattern variable.
     *
     * @param variable Pattern variable to query
     * @return y_a value, or 0 if not in solution
     */
    public double getPatternValue(PatternVariable variable) {
        return patternValues.getOrDefault(variable, 0.0);
    }

    /**
     * Get y_a value for a pattern in P_0 pool.
     * Convenience method - creates PatternVariable internally.
     *
     * @param pattern Pattern to query
     * @return y_a value for this pattern in P_0
     */
    public double getPatternValueP0(Pattern pattern) {
        return getPatternValue(PatternVariable.forP0(pattern));
    }

    /**
     * Get y_a value for a pattern in P_i pool.
     * Convenience method - creates PatternVariable internally.
     *
     * @param pattern    Pattern to query
     * @param knapsackId Knapsack index
     * @return y_a value for this pattern in P_i
     */
    public double getPatternValuePI(Pattern pattern, int knapsackId) {
        return getPatternValue(PatternVariable.forPI(pattern, knapsackId));
    }

    /**
     * Get s_j value for an item.
     *
     * @param itemId Item index
     * @return s_j value
     */
    public double getDualCutValue(int itemId) {
        return dualCutValues.getOrDefault(itemId, 0.0);
    }

    /**
     * Get all pattern variables with non-zero values.
     *
     * @return Set of active pattern variables
     */
    public Set<PatternVariable> getActivePatternVariables() {
        return patternValues.entrySet().stream()
                .filter(e -> Math.abs(e.getValue()) > TOLERANCE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Get all pattern variables and their values.
     *
     * @return Unmodifiable map of pattern variable values
     */
    public Map<PatternVariable, Double> getPatternValues() {
        return Collections.unmodifiableMap(patternValues);
    }

    /**
     * Get all dual cut values.
     *
     * @return Unmodifiable map of dual cut values
     */
    public Map<Integer, Double> getDualCutValues() {
        return Collections.unmodifiableMap(dualCutValues);
    }

    /**
     * @return Number of items
     */
    public int getNumItems() {
        return numItems;
    }

    // ========== Integrality Checks ==========

    /**
     * Check if pattern values are integer (within tolerance).
     *
     * @return true if all pattern values are approximately 0 or 1
     */
    public boolean arePatternValuesInteger() {
        for (double value : patternValues.values()) {
            if (!isInteger(value)) return false;
        }
        return true;
    }

    /**
     * Check if dual cuts are integer.
     *
     * @return true if all dual cuts are approximately integer
     */
    public boolean areDualCutsInteger() {
        for (double value : dualCutValues.values()) {
            if (!isInteger(value)) return false;
        }
        return true;
    }

    /**
     * Check if entire solution is integer.
     *
     * @return true if both pattern values and dual cuts are integer
     */
    public boolean isInteger() {
        return arePatternValuesInteger() && areDualCutsInteger();
    }

    private boolean isInteger(double value) {
        return Math.abs(value - Math.round(value)) < TOLERANCE;
    }

    // ========== Visualization ==========

    @Override
    public String toString() {
        int totalVars = patternValues.size();
        int activeVars = getActivePatternVariables().size();
        return String.format("DWSolution(active=%d/%d, integer=%s)",
                activeVars, totalVars, isInteger() ? "yes" : "no");
    }

    /**
     * Show pattern values in a table.
     *
     * @return Table string representation
     */
    public String toPatternValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Active Pattern Variables (y_a > 0):\n");
        sb.append("┌──────────────┬──────────────┬────────┬──────────┬──────────┐\n");
        sb.append("│     Pool     │    Items     │ Profit │   y_a    │ Integer? │\n");
        sb.append("├──────────────┼──────────────┼────────┼──────────┼──────────┤\n");

        List<Map.Entry<PatternVariable, Double>> activeList = patternValues.entrySet().stream()
                .filter(e -> Math.abs(e.getValue()) > TOLERANCE)
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .collect(Collectors.toList());

        int count = 0;
        for (Map.Entry<PatternVariable, Double> entry : activeList) {
            if (count >= 20) {
                sb.append("│     ...      │     ...      │   ...  │    ...   │    ...   │\n");
                break;
            }

            PatternVariable var = entry.getKey();
            double value = entry.getValue();
            Pattern p = var.getPattern();
            boolean isInt = isInteger(value);

            String pool = var.isP0() ? "P_0" : String.format("P_%d", var.getPoolIndex() + 1);
            String itemsStr = p.getItemIds().toString();
            if (itemsStr.length() > 12) {
                itemsStr = itemsStr.substring(0, 9) + "...";
            }

            sb.append(String.format("│ %-12s │ %-12s │ %6.1f │  %6.3f  │    %s     │\n",
                    pool, itemsStr, p.getTotalProfit(), value, isInt ? "✓" : "✗"));
            count++;
        }

        sb.append("└──────────────┴──────────────┴────────┴──────────┴──────────┘\n");
        return sb.toString();
    }

    /**
     * Show dual cut values.
     *
     * @return Table string representation
     */
    public String toDualCutsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Dual Cuts (s_j):\n");
        sb.append("┌──────┬──────────┬──────────┐\n");
        sb.append("│ Item │   s_j    │ Integer? │\n");
        sb.append("├──────┼──────────┼──────────┤\n");

        List<Integer> items = new ArrayList<>(dualCutValues.keySet());
        Collections.sort(items);

        for (int j = 0; j < Math.min(items.size(), 20); j++) {
            int itemId = items.get(j);
            double value = dualCutValues.get(itemId);
            boolean isInt = isInteger(value);
            sb.append(String.format("│ %4d │  %6.3f  │    %s     │\n",
                    itemId, value, isInt ? "✓" : "✗"));
        }
        if (items.size() > 20) {
            sb.append("│  ... │    ...   │    ...   │\n");
        }

        sb.append("└──────┴──────────┴──────────┘\n");
        return sb.toString();
    }

    /**
     * Combined detailed view.
     *
     * @param master DantzigWolfeMaster for computing derived values
     * @return Detailed string representation
     */
    public String toDetailedString(DantzigWolfeMaster master) {
        StringBuilder sb = new StringBuilder();
        int width = 44;
        String border = "─".repeat(width - 2);

        sb.append("┌").append(border).append("┐\n");
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Dantzig-Wolfe Solution"));
        sb.append("├").append(border).append("┤\n");

        // Summary
        int totalVars = patternValues.size();
        int activeVars = getActivePatternVariables().size();
        sb.append(String.format("│ %-" + (width - 4) + "s │\n",
                String.format("Pattern variables: %d active / %d total", activeVars, totalVars)));
        sb.append(String.format("│ %-" + (width - 4) + "s │\n",
                String.format("Pattern values integer: %s", arePatternValuesInteger() ? "YES" : "NO")));
        sb.append(String.format("│ %-" + (width - 4) + "s │\n",
                String.format("Dual cuts integer: %s", areDualCutsInteger() ? "YES" : "NO")));
        sb.append(String.format("│ %-" + (width - 4) + "s │\n",
                String.format("Overall integer: %s", isInteger() ? "YES" : "NO")));

        sb.append("├").append(border).append("┤\n");

        // Derived item selections
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Derived item selections (t_j):"));
        for (int j = 0; j < Math.min(numItems, 5); j++) {
            double tj = master.computeItemSelection(this, j);
            String intChar = Math.abs(tj - Math.round(tj)) < TOLERANCE ? "✓" : "✗";
            sb.append(String.format("│   t[%d] = %.3f %s%-" + (width - 21) + "s │\n",
                    j, tj, intChar, ""));
        }
        if (numItems > 5) {
            sb.append(String.format("│   %-" + (width - 7) + "s │\n", "..."));
        }

        sb.append("└").append(border).append("┘\n");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DWSolution that = (DWSolution) o;
        return Objects.equals(patternValues, that.patternValues) &&
                Objects.equals(dualCutValues, that.dualCutValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patternValues, dualCutValues);
    }
}