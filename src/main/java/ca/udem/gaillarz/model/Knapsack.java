package ca.udem.gaillarz.model;

import java.util.Objects;

/**
 * Represents a knapsack in the Multiple Knapsack Problem.
 * Each knapsack has an ID and a capacity (c_i).
 */
public class Knapsack {
    private final int id;
    private final int capacity;  // c_i

    /**
     * Creates a new knapsack.
     *
     * @param id       Unique identifier for this knapsack (0-indexed)
     * @param capacity Capacity of the knapsack (must be positive)
     */
    public Knapsack(int id, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive, got: " + capacity);
        }
        this.id = id;
        this.capacity = capacity;
    }

    /**
     * @return The unique identifier of this knapsack
     */
    public int getId() {
        return id;
    }

    /**
     * @return The capacity of this knapsack (c_i)
     */
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Knapsack knapsack = (Knapsack) o;
        return id == knapsack.id && capacity == knapsack.capacity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, capacity);
    }

    @Override
    public String toString() {
        return String.format("Knapsack[id=%d, capacity=%d]", id, capacity);
    }
}

