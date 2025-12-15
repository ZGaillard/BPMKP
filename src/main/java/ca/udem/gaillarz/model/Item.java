package ca.udem.gaillarz.model;

/**
 * Represents an item in the Multiple Knapsack Problem.
 * Each item has an ID, weight (w_j), and profit (p_j).
 *
 * @param weight w_j
 * @param profit p_j
 */
public record Item(int id, int weight, int profit) {
    /**
     * Creates a new item.
     *
     * @param id     Unique identifier for this item (0-indexed)
     * @param weight Weight of the item (must be positive)
     * @param profit Profit of the item (must be positive)
     */
    public Item {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive, got: " + weight);
        }
        if (profit <= 0) {
            throw new IllegalArgumentException("Profit must be positive, got: " + profit);
        }
    }

    /**
     * @return The unique identifier of this item
     */
    @Override
    public int id() {
        return id;
    }

    /**
     * @return The weight of this item (w_j)
     */
    @Override
    public int weight() {
        return weight;
    }

    /**
     * @return The profit of this item (p_j)
     */
    @Override
    public int profit() {
        return profit;
    }

    /**
     * @return The profit-to-weight ratio (p_j / w_j)
     */
    public double getProfitWeightRatio() {
        return (double) profit / weight;
    }

    @Override
    public String toString() {
        return String.format("Item[id=%d, p=%d, w=%d, r=%.2f]", id, profit, weight, getProfitWeightRatio());
    }
}

