package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.model.MKPInstance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a solution to the Classic MKP formulation.
 * Contains the x_ij assignment variables.
 */
public class ClassicSolution {

    private final boolean[][] assignment;  // x_ij values: assignment[i][j]
    private final int numKnapsacks;
    private final int numItems;

    /**
     * Create solution from assignment matrix.
     * assignment[i][j] = true if item j is assigned to knapsack i
     *
     * @param assignment Assignment matrix (will be copied)
     */
    public ClassicSolution(boolean[][] assignment) {
        if (assignment == null || assignment.length == 0) {
            throw new IllegalArgumentException("Assignment matrix cannot be null or empty");
        }
        this.numKnapsacks = assignment.length;
        this.numItems = assignment[0].length;

        // Defensive copy
        this.assignment = new boolean[numKnapsacks][numItems];
        for (int i = 0; i < numKnapsacks; i++) {
            if (assignment[i].length != numItems) {
                throw new IllegalArgumentException("All rows must have the same length");
            }
            System.arraycopy(assignment[i], 0, this.assignment[i], 0, numItems);
        }

        // Validate: each item assigned to at most one knapsack
        for (int j = 0; j < numItems; j++) {
            int count = 0;
            for (int i = 0; i < numKnapsacks; i++) {
                if (assignment[i][j]) count++;
            }
            if (count > 1) {
                throw new IllegalArgumentException("Item " + j + " is assigned to multiple knapsacks");
            }
        }
    }

    /**
     * Create empty solution (all items unassigned).
     *
     * @param numKnapsacks Number of knapsacks (m)
     * @param numItems     Number of items (n)
     */
    public ClassicSolution(int numKnapsacks, int numItems) {
        this.numKnapsacks = numKnapsacks;
        this.numItems = numItems;
        this.assignment = new boolean[numKnapsacks][numItems];
    }

    /**
     * Create a copy of another solution.
     *
     * @param other Solution to copy
     */
    public ClassicSolution(ClassicSolution other) {
        this.numKnapsacks = other.numKnapsacks;
        this.numItems = other.numItems;
        this.assignment = new boolean[numKnapsacks][numItems];
        for (int i = 0; i < numKnapsacks; i++) {
            System.arraycopy(other.assignment[i], 0, this.assignment[i], 0, numItems);
        }
    }

    /**
     * Check if an item is assigned to any knapsack.
     *
     * @param itemId Item index
     * @return true if item is assigned
     */
    public boolean isItemAssigned(int itemId) {
        for (int i = 0; i < numKnapsacks; i++) {
            if (assignment[i][itemId]) return true;
        }
        return false;
    }

    /**
     * Check if an item is in a specific knapsack.
     *
     * @param knapsackId Knapsack index
     * @param itemId     Item index
     * @return true if item is in knapsack
     */
    public boolean isItemInKnapsack(int knapsackId, int itemId) {
        return assignment[knapsackId][itemId];
    }

    /**
     * Get the knapsack to which an item is assigned.
     *
     * @param itemId Item index
     * @return Knapsack index, or -1 if unassigned
     */
    public int getKnapsackForItem(int itemId) {
        for (int i = 0; i < numKnapsacks; i++) {
            if (assignment[i][itemId]) return i;
        }
        return -1;
    }

    /**
     * Assign an item to a knapsack.
     *
     * @param knapsackId Knapsack index
     * @param itemId     Item index
     */
    public void assignItem(int knapsackId, int itemId) {
        // First unassign from any other knapsack
        unassignItem(itemId);
        assignment[knapsackId][itemId] = true;
    }

    /**
     * Unassign an item from its current knapsack.
     *
     * @param itemId Item index
     */
    public void unassignItem(int itemId) {
        for (int i = 0; i < numKnapsacks; i++) {
            assignment[i][itemId] = false;
        }
    }

    /**
     * Get a copy of the assignment matrix.
     *
     * @return Copy of assignment matrix
     */
    public boolean[][] getAssignment() {
        boolean[][] copy = new boolean[numKnapsacks][numItems];
        for (int i = 0; i < numKnapsacks; i++) {
            System.arraycopy(assignment[i], 0, copy[i], 0, numItems);
        }
        return copy;
    }

    /**
     * @return Number of knapsacks
     */
    public int getNumKnapsacks() {
        return numKnapsacks;
    }

    /**
     * @return Number of items
     */
    public int getNumItems() {
        return numItems;
    }

    /**
     * @return Number of assigned items
     */
    public int getNumAssigned() {
        int count = 0;
        for (int j = 0; j < numItems; j++) {
            if (isItemAssigned(j)) count++;
        }
        return count;
    }

    /**
     * Get items assigned to a specific knapsack.
     *
     * @param knapsackId Knapsack index
     * @return Set of item indices
     */
    public Set<Integer> getItemsInKnapsack(int knapsackId) {
        Set<Integer> items = new HashSet<>();
        for (int j = 0; j < numItems; j++) {
            if (assignment[knapsackId][j]) {
                items.add(j);
            }
        }
        return items;
    }

    @Override
    public String toString() {
        return String.format("ClassicSolution(assigned=%d/%d)", getNumAssigned(), numItems);
    }

    /**
     * Matrix representation showing assignment.
     *
     * @return Matrix string representation
     */
    public String toMatrixString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Assignment Matrix (x_ij):\n");

        // Header
        sb.append("       ");
        for (int j = 0; j < Math.min(numItems, 10); j++) {
            sb.append(String.format("Item %d  ", j));
        }
        if (numItems > 10) {
            sb.append("...");
        }
        sb.append("\n");

        // Rows
        for (int i = 0; i < numKnapsacks; i++) {
            sb.append(String.format("KS %d: ", i));
            for (int j = 0; j < Math.min(numItems, 10); j++) {
                sb.append(String.format("  %d     ", assignment[i][j] ? 1 : 0));
            }
            if (numItems > 10) {
                sb.append("...");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Visual representation with items in each knapsack.
     *
     * @param instance MKP instance for computing weights and profits
     * @return Detailed string representation
     */
    public String toDetailedString(MKPInstance instance) {
        StringBuilder sb = new StringBuilder();
        int width = 32;
        String border = "─".repeat(width - 2);

        int totalProfit = 0;
        boolean feasible = true;

        sb.append("┌").append(border).append("┐\n");

        for (int i = 0; i < numKnapsacks; i++) {
            Set<Integer> items = getItemsInKnapsack(i);
            int ksWeight = items.stream().mapToInt(j -> instance.getItem(j).weight()).sum();
            int ksProfit = items.stream().mapToInt(j -> instance.getItem(j).profit()).sum();
            totalProfit += ksProfit;

            int capacity = instance.getKnapsack(i).capacity();
            boolean overflow = ksWeight > capacity;
            if (overflow) feasible = false;

            String warning = overflow ? " ⚠️" : "";
            sb.append(String.format("│ Knapsack %d (used: %d/%d)%s", i, ksWeight, capacity, warning));
            sb.append(" ".repeat(Math.max(0, width - 26 - warning.length())));
            sb.append("│\n");

            sb.append(String.format("│   Items: %-" + (width - 13) + "s │\n", items));

            if (!items.isEmpty()) {
                String weights = items.stream()
                        .map(j -> String.valueOf(instance.getItem(j).weight()))
                        .reduce((a, b) -> a + ", " + b).orElse("");
                sb.append(String.format("│   Weights: %-" + (width - 15) + "s │\n",
                        weights.length() > width - 15 ? weights.substring(0, width - 18) + "..." : weights));

                String profits = items.stream()
                        .map(j -> String.valueOf(instance.getItem(j).profit()))
                        .reduce((a, b) -> a + ", " + b).orElse("");
                sb.append(String.format("│   Profits: %-" + (width - 15) + "s │\n",
                        profits.length() > width - 15 ? profits.substring(0, width - 18) + "..." : profits));

                sb.append(String.format("│   Total: w=%d, p=%-" + (width - 21) + "d │\n", ksWeight, ksProfit));
            }

            if (i < numKnapsacks - 1) {
                sb.append("├").append(border).append("┤\n");
            }
        }

        // Unassigned items
        Set<Integer> unassigned = new HashSet<>();
        for (int j = 0; j < numItems; j++) {
            if (!isItemAssigned(j)) unassigned.add(j);
        }

        sb.append("├").append(border).append("┤\n");
        String unassignedStr = unassigned.toString();
        if (unassignedStr.length() > width - 15) {
            unassignedStr = unassignedStr.substring(0, width - 18) + "...";
        }
        sb.append(String.format("│ Unassigned: %-" + (width - 15) + "s │\n", unassignedStr));
        if (!unassigned.isEmpty()) {
            sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Unassigned details:"));
            for (int j : unassigned) {
                int w = instance.getItem(j).weight();
                int p = instance.getItem(j).profit();
                sb.append(String.format("│   item %2d: w=%4d p=%4d %-" + (width - 23) + "s │\n", j, w, p, ""));
            }
        }
        sb.append("└").append(border).append("┘\n");

        sb.append(String.format("Total profit: %d\n", totalProfit));
        sb.append(String.format("Feasible: %s\n", feasible ? "✓" : "✗ (capacity violations)"));

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassicSolution that = (ClassicSolution) o;
        return Arrays.deepEquals(assignment, that.assignment);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(assignment);
    }
}
