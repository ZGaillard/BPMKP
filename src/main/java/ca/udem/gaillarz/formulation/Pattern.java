package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.MKPInstance;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a pattern (subset of items) in the Dantzig-Wolfe formulation.
 * A pattern is a binary vector indicating which items are included.
 */
public class Pattern {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final boolean[] items;      // a_j indicators
    private final int totalWeight;      // Σ_j w_j * a_j (cached)
    private final double totalProfit;   // Σ_j p_j * a_j (cached)
    private final int id;               // Unique pattern ID
    private final int numItems;

    /**
     * Create pattern from item indicators.
     *
     * @param items    Array where items[j] = true if item j in pattern
     * @param instance MKP instance (to compute weight/profit)
     */
    public Pattern(boolean[] items, MKPInstance instance) {
        if (items == null) {
            throw new IllegalArgumentException("Items array cannot be null");
        }
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        if (items.length != instance.getNumItems()) {
            throw new IllegalArgumentException(
                String.format("Items array length (%d) must match instance items (%d)",
                    items.length, instance.getNumItems()));
        }

        this.numItems = items.length;
        this.items = Arrays.copyOf(items, numItems);
        this.id = ID_GENERATOR.incrementAndGet();

        // Compute cached values
        int weight = 0;
        double profit = 0.0;
        for (int j = 0; j < numItems; j++) {
            if (items[j]) {
                weight += instance.getItem(j).getWeight();
                profit += instance.getItem(j).getProfit();
            }
        }
        this.totalWeight = weight;
        this.totalProfit = profit;
    }

    /**
     * Private constructor for creating patterns with pre-computed values.
     */
    private Pattern(boolean[] items, int totalWeight, double totalProfit) {
        this.numItems = items.length;
        this.items = Arrays.copyOf(items, numItems);
        this.id = ID_GENERATOR.incrementAndGet();
        this.totalWeight = totalWeight;
        this.totalProfit = totalProfit;
    }

    /**
     * Create pattern from item IDs.
     *
     * @param itemIds  Set of item indices to include
     * @param instance MKP instance
     * @return New pattern
     */
    public static Pattern fromItemIds(Set<Integer> itemIds, MKPInstance instance) {
        boolean[] items = new boolean[instance.getNumItems()];
        for (int j : itemIds) {
            if (j < 0 || j >= instance.getNumItems()) {
                throw new IllegalArgumentException("Invalid item ID: " + j);
            }
            items[j] = true;
        }
        return new Pattern(items, instance);
    }

    /**
     * Create single-item pattern.
     *
     * @param itemId   Item index
     * @param instance MKP instance
     * @return Pattern containing only the specified item
     */
    public static Pattern singleItem(int itemId, MKPInstance instance) {
        boolean[] items = new boolean[instance.getNumItems()];
        items[itemId] = true;
        return new Pattern(items, instance);
    }

    /**
     * Create empty pattern.
     *
     * @param numItems Number of items in the instance
     * @return Empty pattern
     */
    public static Pattern empty(int numItems) {
        return new Pattern(new boolean[numItems], 0, 0.0);
    }

    /**
     * Check if pattern contains an item.
     *
     * @param itemId Item index
     * @return true if item is in pattern
     */
    public boolean containsItem(int itemId) {
        return items[itemId];
    }

    /**
     * @return Total weight of items in pattern
     */
    public int getTotalWeight() {
        return totalWeight;
    }

    /**
     * @return Total profit of items in pattern
     */
    public double getTotalProfit() {
        return totalProfit;
    }

    /**
     * @return Number of items in pattern
     */
    public int getNumItems() {
        int count = 0;
        for (boolean item : items) {
            if (item) count++;
        }
        return count;
    }

    /**
     * @return Set of item indices in pattern
     */
    public Set<Integer> getItemIds() {
        Set<Integer> ids = new HashSet<>();
        for (int j = 0; j < numItems; j++) {
            if (items[j]) {
                ids.add(j);
            }
        }
        return ids;
    }

    /**
     * @return Copy of item indicators array
     */
    public boolean[] getItems() {
        return Arrays.copyOf(items, numItems);
    }

    /**
     * @return Pattern ID
     */
    public int getId() {
        return id;
    }

    /**
     * Check if pattern respects capacity.
     *
     * @param capacity Maximum capacity
     * @return true if totalWeight ≤ capacity
     */
    public boolean isFeasible(int capacity) {
        return totalWeight <= capacity;
    }

    /**
     * Get profit/weight ratio (efficiency).
     *
     * @return Profit-to-weight ratio, or 0 if empty
     */
    public double getEfficiency() {
        return totalWeight > 0 ? totalProfit / totalWeight : 0.0;
    }

    /**
     * @return true if pattern is empty (no items)
     */
    public boolean isEmpty() {
        for (boolean item : items) {
            if (item) return false;
        }
        return true;
    }

    /**
     * Compare item membership with another pattern (ignores IDs).
     *
     * @param other Other pattern
     * @return true if both patterns contain exactly the same items
     */
    public boolean hasSameItems(Pattern other) {
        if (other == null || other.items.length != items.length) {
            return false;
        }
        return Arrays.equals(items, other.items);
    }

    @Override
    public String toString() {
        return String.format("Pattern(items=%s, w=%d, p=%.1f)", getItemIds(), totalWeight, totalProfit);
    }

    /**
     * Detailed representation.
     *
     * @param capacity Capacity to check feasibility against
     * @return Detailed string representation
     */
    public String toDetailedString(int capacity) {
        StringBuilder sb = new StringBuilder();
        int width = 27;
        String border = "─".repeat(width - 2);

        sb.append("┌").append(border).append("┐\n");
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Pattern #" + id));
        sb.append("├").append(border).append("┤\n");

        String itemsStr = getItemIds().toString();
        if (itemsStr.length() > width - 12) {
            itemsStr = itemsStr.substring(0, width - 15) + "...}";
        }
        sb.append(String.format("│ Items: %-" + (width - 11) + "s │\n", itemsStr));
        sb.append(String.format("│ Count: %-" + (width - 11) + "d │\n", getNumItems()));
        sb.append(String.format("│ Total Weight: %-" + (width - 18) + "d │\n", totalWeight));
        sb.append(String.format("│ Total Profit: %-" + (width - 18) + ".1f │\n", totalProfit));
        sb.append(String.format("│ Efficiency: %-" + (width - 16) + ".2f │\n", getEfficiency()));

        String feasible = isFeasible(capacity) ? "✓" : "✗";
        sb.append(String.format("│ Feasible (cap≤%d): %-" + (width - 20 - String.valueOf(capacity).length()) + "s │\n",
                capacity, feasible));

        sb.append("└").append(border).append("┘");

        return sb.toString();
    }

    /**
     * Show pattern with item details.
     *
     * @param instance MKP instance for item details
     * @return Table string representation
     */
    public String toTableString(MKPInstance instance) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Pattern #%d: %s\n", id, getItemIds()));
        sb.append("+------+--------+--------+\n");
        sb.append("| Item | Weight | Profit |\n");
        sb.append("+------+--------+--------+\n");

        for (int j = 0; j < numItems; j++) {
            if (items[j]) {
                Item item = instance.getItem(j);
                sb.append(String.format("| %4d | %6d | %6.1f |\n",
                        j, item.getWeight(), (double) item.getProfit()));
            }
        }

        sb.append("+------+--------+--------+\n");
        sb.append(String.format("Total: %8d   %6.1f\n", totalWeight, totalProfit));

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pattern pattern = (Pattern) o;
        return Arrays.equals(items, pattern.items);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items);
    }
}
