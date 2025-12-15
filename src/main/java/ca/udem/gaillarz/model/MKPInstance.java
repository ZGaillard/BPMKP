package ca.udem.gaillarz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Multiple Knapsack Problem (MKP) instance.
 * Contains items with profits and weights, and knapsacks with capacities.
 */
public class MKPInstance {
    private final List<Item> items;
    private final List<Knapsack> knapsacks;
    private final String name;

    // Cached computed values
    private final int totalCapacity;
    private final int totalWeight;
    private final int totalProfit;

    /**
     * Creates a new MKP instance.
     *
     * @param items     List of items in the problem
     * @param knapsacks List of knapsacks in the problem
     */
    public MKPInstance(List<Item> items, List<Knapsack> knapsacks) {
        this(items, knapsacks, "unnamed");
    }

    /**
     * Creates a new MKP instance with a name.
     *
     * @param items     List of items in the problem
     * @param knapsacks List of knapsacks in the problem
     * @param name      Name of the instance
     */
    public MKPInstance(List<Item> items, List<Knapsack> knapsacks, String name) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be null or empty");
        }
        if (knapsacks == null || knapsacks.isEmpty()) {
            throw new IllegalArgumentException("Knapsacks list cannot be null or empty");
        }

        this.items = new ArrayList<>(items);
        this.knapsacks = new ArrayList<>(knapsacks);
        this.name = name != null ? name : "unnamed";

        // Compute cached values
        this.totalCapacity = knapsacks.stream().mapToInt(Knapsack::capacity).sum();
        this.totalWeight = items.stream().mapToInt(Item::weight).sum();
        this.totalProfit = items.stream().mapToInt(Item::profit).sum();
    }

    /**
     * @return Number of items (n)
     */
    public int getNumItems() {
        return items.size();
    }

    /**
     * @return Number of knapsacks (m)
     */
    public int getNumKnapsacks() {
        return knapsacks.size();
    }

    /**
     * Gets an item by its index.
     *
     * @param j Item index (0-indexed)
     * @return The item at index j
     */
    public Item getItem(int j) {
        return items.get(j);
    }

    /**
     * Gets a knapsack by its index.
     *
     * @param i Knapsack index (0-indexed)
     * @return The knapsack at index i
     */
    public Knapsack getKnapsack(int i) {
        return knapsacks.get(i);
    }

    /**
     * @return Unmodifiable list of all items
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * @return Unmodifiable list of all knapsacks
     */
    public List<Knapsack> getKnapsacks() {
        return Collections.unmodifiableList(knapsacks);
    }

    /**
     * @return Instance name
     */
    public String getName() {
        return name;
    }

    /**
     * @return Total capacity of all knapsacks: Σ_{i=1}^{m} c_i
     */
    public int getTotalCapacity() {
        return totalCapacity;
    }

    /**
     * @return Total weight of all items: Σ_{j=1}^{n} w_j
     */
    public int getTotalWeight() {
        return totalWeight;
    }

    /**
     * @return Total profit of all items: Σ_{j=1}^{n} p_j
     */
    public int getTotalProfit() {
        return totalProfit;
    }

    /**
     * @return Average profit-to-weight ratio of all items
     */
    public double getAverageProfitWeightRatio() {
        return items.stream().mapToDouble(Item::getProfitWeightRatio).average().orElse(0.0);
    }

    /**
     * Validates the instance.
     *
     * @return true if the instance is valid
     */
    public boolean isValid() {
        // Check at least one item fits in at least one knapsack
        for (Item item : items) {
            for (Knapsack knapsack : knapsacks) {
                if (item.weight() <= knapsack.capacity()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("MKP(n=%d, m=%d, total_cap=%d)", getNumItems(), getNumKnapsacks(), totalCapacity);
    }

    /**
     * Returns a detailed multi-line representation of the instance.
     *
     * @return Detailed string representation
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        int width = 40;
        String border = "─".repeat(width - 2);

        sb.append("┌").append(border).append("┐\n");
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "MKP Instance: " + name));
        sb.append(String.format("│ %-" + (width - 4) + "s │\n",
                String.format("Items: %d, Knapsacks: %d", getNumItems(), getNumKnapsacks())));
        sb.append("├").append(border).append("┤\n");

        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Items:"));
        for (int j = 0; j < Math.min(items.size(), 10); j++) {
            Item item = items.get(j);
            String itemStr = String.format("  [%d] profit=%d, weight=%d (r=%.2f)",
                    j, item.profit(), item.weight(), item.getProfitWeightRatio());
            sb.append(String.format("│ %-" + (width - 4) + "s │\n", itemStr));
        }
        if (items.size() > 10) {
            sb.append(String.format("│ %-" + (width - 4) + "s │\n", "  ... (" + (items.size() - 10) + " more)"));
        }

        sb.append("├").append(border).append("┤\n");

        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Knapsacks:"));
        for (int i = 0; i < Math.min(knapsacks.size(), 10); i++) {
            Knapsack ks = knapsacks.get(i);
            String ksStr = String.format("  [%d] capacity=%d", i, ks.capacity());
            sb.append(String.format("│ %-" + (width - 4) + "s │\n", ksStr));
        }
        if (knapsacks.size() > 10) {
            sb.append(String.format("│ %-" + (width - 4) + "s │\n", "  ... (" + (knapsacks.size() - 10) + " more)"));
        }

        sb.append("├").append(border).append("┤\n");
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Total capacity: " + totalCapacity));
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Total weight:   " + totalWeight));
        sb.append(String.format("│ %-" + (width - 4) + "s │\n", "Total profit:   " + totalProfit));
        sb.append("└").append(border).append("┘");

        return sb.toString();
    }

    /**
     * Returns an ASCII table representation of items and knapsacks.
     *
     * @return Table string representation
     */
    public String toTable() {
        StringBuilder sb = new StringBuilder();

        // Item summary table
        sb.append("Item Summary:\n");
        sb.append("+----+--------+--------+-------+\n");
        sb.append("| ID | Profit | Weight | Ratio |\n");
        sb.append("+----+--------+--------+-------+\n");
        for (int j = 0; j < Math.min(items.size(), 20); j++) {
            Item item = items.get(j);
            sb.append(String.format("| %2d | %6d | %6d | %5.2f |\n",
                    j, item.profit(), item.weight(), item.getProfitWeightRatio()));
        }
        if (items.size() > 20) {
            sb.append(String.format("| .. |    ... |    ... |   ... | (%d more)\n", items.size() - 20));
        }
        sb.append("+----+--------+--------+-------+\n");

        sb.append("\nKnapsack Summary:\n");
        sb.append("+----+----------+\n");
        sb.append("| ID | Capacity |\n");
        sb.append("+----+----------+\n");
        for (int i = 0; i < Math.min(knapsacks.size(), 20); i++) {
            Knapsack ks = knapsacks.get(i);
            sb.append(String.format("| %2d | %8d |\n", i, ks.capacity()));
        }
        if (knapsacks.size() > 20) {
            sb.append(String.format("| .. |      ... | (%d more)\n", knapsacks.size() - 20));
        }
        sb.append("+----+----------+\n");

        return sb.toString();
    }
}

