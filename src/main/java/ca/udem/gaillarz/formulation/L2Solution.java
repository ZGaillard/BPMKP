package ca.udem.gaillarz.formulation;

import java.util.Arrays;

/**
 * Represents a solution to the L2 Relaxed formulation.
 * Contains both t_j (item selection) and x_ij (assignment) variables.
 */
public class L2Solution {
    private static final double SELECTION_TOLERANCE = 1e-5;
    private static final double INTEGER_TOLERANCE = 1e-6;

    private final double[] t;     // Item selection variables (t_j)
    private final double[][] x;   // Assignment variables (x_ij)
    private final int numItems;
    private final int numKnapsacks;

    /**
     * Create L2 solution from t_j and x_ij values.
     *
     * @param t Array of item selection values t_j (length n)
     * @param x Matrix of assignment values x_ij (m x n)
     */
    public L2Solution(double[] t, double[][] x) {
        if (t == null || t.length == 0) {
            throw new IllegalArgumentException("t array cannot be null or empty");
        }
        if (x == null || x.length == 0) {
            throw new IllegalArgumentException("x matrix cannot be null or empty");
        }

        this.numItems = t.length;
        this.numKnapsacks = x.length;

        // Validate dimensions
        for (int i = 0; i < numKnapsacks; i++) {
            if (x[i].length != numItems) {
                throw new IllegalArgumentException("All rows of x must have length " + numItems);
            }
        }

        // Defensive copy
        this.t = Arrays.copyOf(t, numItems);
        this.x = new double[numKnapsacks][numItems];
        for (int i = 0; i < numKnapsacks; i++) {
            System.arraycopy(x[i], 0, this.x[i], 0, numItems);
        }
    }

    /**
     * Create from classic solution (derives t_j from x_ij).
     *
     * @param classic  Classic solution
     * @param numItems Number of items
     * @return L2Solution with t_j = 1 if item j is assigned to any knapsack
     */
    public static L2Solution fromClassicSolution(ClassicSolution classic, int numItems) {
        int m = classic.getNumKnapsacks();

        double[] t = new double[numItems];
        double[][] x = new double[m][numItems];

        boolean[][] assignment = classic.getAssignment();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < numItems; j++) {
                x[i][j] = assignment[i][j] ? 1.0 : 0.0;
            }
        }

        // t_j = 1 if item j is assigned to any knapsack
        for (int j = 0; j < numItems; j++) {
            for (int i = 0; i < m; i++) {
                if (x[i][j] > SELECTION_TOLERANCE) {
                    t[j] = 1.0;
                    break;
                }
            }
        }

        return new L2Solution(t, x);
    }

    /**
     * Get t_j value for item.
     *
     * @param itemId Item index
     * @return t_j value
     */
    public double getItemSelection(int itemId) {
        return t[itemId];
    }

    /**
     * Check if item is selected (t_j ≈ 1).
     *
     * @param itemId Item index
     * @return true if t_j is approximately 1
     */
    public boolean isItemSelected(int itemId) {
        return Math.abs(t[itemId] - 1.0) < SELECTION_TOLERANCE;
    }

    /**
     * Check if t_j is integer (0 or 1).
     *
     * @param itemId Item index
     * @return true if t_j is approximately 0 or 1
     */
    public boolean isItemSelectionInteger(int itemId) {
        return isInteger(t[itemId]);
    }

    /**
     * Get x_ij value.
     *
     * @param knapsackId Knapsack index
     * @param itemId     Item index
     * @return x_ij value
     */
    public double getItemAssignment(int knapsackId, int itemId) {
        return x[knapsackId][itemId];
    }

    /**
     * Check if item is in knapsack (x_ij ≈ 1).
     *
     * @param knapsackId Knapsack index
     * @param itemId     Item index
     * @return true if x_ij is approximately 1
     */
    public boolean isItemInKnapsack(int knapsackId, int itemId) {
        return Math.abs(x[knapsackId][itemId] - 1.0) < SELECTION_TOLERANCE;
    }

    /**
     * Check if x_ij is integer.
     *
     * @param knapsackId Knapsack index
     * @param itemId     Item index
     * @return true if x_ij is approximately 0 or 1
     */
    public boolean isItemAssignmentInteger(int knapsackId, int itemId) {
        return isInteger(x[knapsackId][itemId]);
    }

    /**
     * Check if all t_j values are integer.
     *
     * @return true if all t_j are approximately 0 or 1
     */
    public boolean areItemSelectionsInteger() {
        for (int j = 0; j < numItems; j++) {
            if (!isItemSelectionInteger(j)) return false;
        }
        return true;
    }

    /**
     * Check if all x_ij values are integer.
     *
     * @return true if all x_ij are approximately 0 or 1
     */
    public boolean areAssignmentsInteger() {
        for (int i = 0; i < numKnapsacks; i++) {
            for (int j = 0; j < numItems; j++) {
                if (!isItemAssignmentInteger(i, j)) return false;
            }
        }
        return true;
    }

    /**
     * Check if entire solution is integer.
     *
     * @return true if both t_j and x_ij are all integer
     */
    public boolean isInteger() {
        return areItemSelectionsInteger() && areAssignmentsInteger();
    }

    /**
     * Get a copy of t_j values.
     *
     * @return Copy of item selection array
     */
    public double[] getItemSelections() {
        return Arrays.copyOf(t, numItems);
    }

    /**
     * Get a copy of x_ij values.
     *
     * @return Copy of assignment matrix
     */
    public double[][] getAssignments() {
        double[][] copy = new double[numKnapsacks][numItems];
        for (int i = 0; i < numKnapsacks; i++) {
            System.arraycopy(x[i], 0, copy[i], 0, numItems);
        }
        return copy;
    }

    /**
     * @return Number of items
     */
    public int getNumItems() {
        return numItems;
    }

    /**
     * @return Number of knapsacks
     */
    public int getNumKnapsacks() {
        return numKnapsacks;
    }

    /**
     * Convert to classic solution.
     * Only works if solution is integer.
     *
     * @return ClassicSolution
     * @throws IllegalStateException if solution is fractional
     */
    public ClassicSolution toClassicSolution() {
        if (!isInteger()) {
            throw new IllegalStateException("Cannot convert fractional L2 solution to classic solution");
        }

        boolean[][] assignment = new boolean[numKnapsacks][numItems];
        for (int i = 0; i < numKnapsacks; i++) {
            for (int j = 0; j < numItems; j++) {
                assignment[i][j] = false;
            }
        }

        // If an item appears in multiple knapsacks, keep the first one.
        for (int j = 0; j < numItems; j++) {
            int chosen = -1;
            for (int i = 0; i < numKnapsacks; i++) {
                if (isItemInKnapsack(i, j)) {
                    if (chosen < 0) {
                        chosen = i;
                        assignment[i][j] = true;
                    } // else ignore extra assignments
                }
            }
        }
        return new ClassicSolution(assignment);
    }

    private boolean isInteger(double value) {
        return Math.abs(value - Math.round(value)) < INTEGER_TOLERANCE;
    }

    /**
     * @return description of the first fractional variable, or null if all are integral.
     */
    public String firstFractionalVariable() {
        for (int j = 0; j < numItems; j++) {
            double tj = t[j];
            if (!isInteger(tj)) {
                return String.format("t_%d = %.6f", j, tj);
            }
        }
        for (int i = 0; i < numKnapsacks; i++) {
            for (int j = 0; j < numItems; j++) {
                double xij = x[i][j];
                if (!isInteger(xij)) {
                    return String.format("x_%d_%d = %.6f", i, j, xij);
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        double selected = Arrays.stream(t).sum();
        return String.format("L2Solution(selected=%.1f/%d, integer_t=%s, integer_x=%s)",
                selected, numItems,
                areItemSelectionsInteger() ? "yes" : "no",
                areAssignmentsInteger() ? "yes" : "no");
    }

    /**
     * Show t_j values in a table.
     *
     * @return Table string representation
     */
    public String toItemSelectionString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Item Selection (t_j):\n");
        sb.append("┌──────┬─────────┬─────────┐\n");
        sb.append("│ Item │   t_j   │ Integer │\n");
        sb.append("├──────┼─────────┼─────────┤\n");

        for (int j = 0; j < Math.min(numItems, 20); j++) {
            String intChar = isItemSelectionInteger(j) ? "✓" : "✗";
            sb.append(String.format("│ %4d │  %5.3f  │    %s    │\n", j, t[j], intChar));
        }
        if (numItems > 20) {
            sb.append("│  ... │   ...   │   ...   │\n");
        }

        sb.append("└──────┴─────────┴─────────┘\n");
        return sb.toString();
    }

    /**
     * Show x_ij values in a matrix.
     *
     * @return Matrix string representation
     */
    public String toAssignmentString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Assignment Matrix (x_ij):\n");

        // Header
        sb.append("       ");
        for (int j = 0; j < Math.min(numItems, 8); j++) {
            sb.append(String.format("Item %d  ", j));
        }
        if (numItems > 8) {
            sb.append("...");
        }
        sb.append("\n");

        // Rows
        for (int i = 0; i < numKnapsacks; i++) {
            sb.append(String.format("KS %d: ", i));
            for (int j = 0; j < Math.min(numItems, 8); j++) {
                sb.append(String.format("%5.3f   ", x[i][j]));
            }
            if (numItems > 8) {
                sb.append("...");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Combined detailed view.
     *
     * @return Detailed string representation
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        int width = 44;
        String border = "─".repeat(width - 2);

        sb.append("┌").append(border).append("┐\n");
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "L2 Solution"));
        sb.append("├").append(border).append("┤\n");

        // Summary
        double totalSelected = Arrays.stream(t).sum();
        sb.append(String.format("│ %-" + (width - 4) + "s │\n",
                String.format("Item Selection: %.2f items selected", totalSelected)));
        sb.append(String.format("│ %-" + (width - 4) + "s │\n",
                String.format("Integer t_j: %s", areItemSelectionsInteger() ? "YES" : "NO")));
        sb.append(String.format("│ %-" + (width - 4) + "s │\n",
                String.format("Integer x_ij: %s", areAssignmentsInteger() ? "YES" : "NO")));

        sb.append("├").append(border).append("┤\n");

        // t_j values
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Item Selection (t_j):"));
        for (int j = 0; j < Math.min(numItems, 5); j++) {
            String intChar = isItemSelectionInteger(j) ? "✓" : "✗";
            sb.append(String.format("│   t[%d] = %.3f %s%-" + (width - 21) + "s │\n",
                    j, t[j], intChar, ""));
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
        L2Solution that = (L2Solution) o;
        return Arrays.equals(t, that.t) && Arrays.deepEquals(x, that.x);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(t);
        result = 31 * result + Arrays.deepHashCode(x);
        return result;
    }
}
