package ca.udem.gaillarz.model;

/**
 * Represents a knapsack in the Multiple Knapsack Problem.
 * Each knapsack has an ID and a capacity (c_i).
 *
 * @param capacity c_i
 */
public record Knapsack(int id, int capacity) {
    /**
     * Creates a new knapsack.
     *
     * @param id       Unique identifier for this knapsack (0-indexed)
     * @param capacity Capacity of the knapsack (must be positive)
     */
    public Knapsack {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive, got: " + capacity);
        }
    }

    /**
     * @return The unique identifier of this knapsack
     */
    @Override
    public int id() {
        return id;
    }

    /**
     * @return The capacity of this knapsack (c_i)
     */
    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return String.format("Knapsack[id=%d, capacity=%d]", id, capacity);
    }
}

