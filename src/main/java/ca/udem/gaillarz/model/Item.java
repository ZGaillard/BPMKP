package ca.udem.gaillarz.model;

import java.util.Objects;

/**
 * Represents an item in the Multiple Knapsack Problem.
 * Each item has an ID, weight (w_j), and profit (p_j).
 */
public class Item {
    private final int id;
    private final int weight;   // w_j
    private final int profit;   // p_j

    /**
     * Creates a new item.
     *
     * @param id     Unique identifier for this item (0-indexed)
     * @param weight Weight of the item (must be positive)
     * @param profit Profit of the item (must be positive)
     */
    public Item(int id, int weight, int profit) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive, got: " + weight);
        }
        if (profit <= 0) {
            throw new IllegalArgumentException("Profit must be positive, got: " + profit);
        }
        this.id = id;
        this.weight = weight;
        this.profit = profit;
    }

    /**
     * @return The unique identifier of this item
     */
    public int getId() {
        return id;
    }

    /**
     * @return The weight of this item (w_j)
     */
    public int getWeight() {
        return weight;
    }

    /**
     * @return The profit of this item (p_j)
     */
    public int getProfit() {
        return profit;
    }

    /**
     * @return The profit-to-weight ratio (p_j / w_j)
     */
    public double getProfitWeightRatio() {
        return (double) profit / weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id == item.id && weight == item.weight && profit == item.profit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, weight, profit);
    }

    @Override
    public String toString() {
        return String.format("Item[id=%d, p=%d, w=%d, r=%.2f]", id, profit, weight, getProfitWeightRatio());
    }
}

