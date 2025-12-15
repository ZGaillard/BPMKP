package ca.udem.gaillarz.solver.knapsack;

import ca.udem.gaillarz.model.Item;

import java.util.*;

/**
 * Solves a 0-1 knapsack problem via dynamic programming.
 * Used for generating core patterns during initialization and pricing.
 */
public class KnapsackSolver {

    private final List<Item> items;
    private final int capacity;
    private final double[] profits; // optional custom profits

    /**
     * Create a solver that uses item profits directly.
     */
    public KnapsackSolver(List<Item> items, int capacity) {
        this(items, capacity, null);
    }

    /**
     * Create a solver with custom profits (e.g., reduced costs in pricing).
     */
    public KnapsackSolver(List<Item> items, int capacity, double[] customProfits) {
        this.items = new ArrayList<>(Objects.requireNonNull(items, "items"));
        this.capacity = capacity;
        if (customProfits != null && customProfits.length != items.size()) {
            throw new IllegalArgumentException("Custom profit array length must equal number of items");
        }
        this.profits = customProfits != null ? Arrays.copyOf(customProfits, customProfits.length) : null;
    }

    /**
     * Solve with the standard O(n*C) DP table.
     *
     * @return optimal pattern and profit
     */
    public KnapsackResult solve() {
        int n = items.size();
        if (n == 0 || capacity <= 0) {
            return new KnapsackResult(Collections.emptySet(), 0.0);
        }

        double[][] dp = new double[n + 1][capacity + 1];

        for (int j = 1; j <= n; j++) {
            Item item = items.get(j - 1);
            int weight = item.weight();
            double profit = getProfit(j - 1);

            for (int c = 0; c <= capacity; c++) {
                dp[j][c] = dp[j - 1][c]; // skip item j
                if (weight <= c) {
                    double take = dp[j - 1][c - weight] + profit;
                    if (take > dp[j][c]) {
                        dp[j][c] = take;
                    }
                }
            }
        }

        // Backtrack
        Set<Integer> selected = new HashSet<>();
        int c = capacity;
        for (int j = n; j >= 1; j--) {
            if (dp[j][c] != dp[j - 1][c]) {
                Item item = items.get(j - 1);
                selected.add(item.id());
                c -= item.weight();
            }
        }

        return new KnapsackResult(selected, dp[n][capacity]);
    }

    /**
     * Solve using O(C) memory while still reconstructing selected items.
     */
    public KnapsackResult solveSpaceOptimized() {
        int n = items.size();
        if (n == 0 || capacity <= 0) {
            return new KnapsackResult(Collections.emptySet(), 0.0);
        }

        double[] prev = new double[capacity + 1];
        double[] curr = new double[capacity + 1];
        List<Set<Integer>> chosen = new ArrayList<>(capacity + 1);
        for (int c = 0; c <= capacity; c++) {
            chosen.add(new HashSet<>());
        }

        for (int j = 1; j <= n; j++) {
            Item item = items.get(j - 1);
            int weight = item.weight();
            double profit = getProfit(j - 1);

            List<Set<Integer>> newChosen = new ArrayList<>(capacity + 1);

            for (int c = 0; c <= capacity; c++) {
                curr[c] = prev[c];
                newChosen.add(new HashSet<>(chosen.get(c)));

                if (weight <= c) {
                    double take = prev[c - weight] + profit;
                    if (take > curr[c]) {
                        curr[c] = take;
                        newChosen.set(c, new HashSet<>(chosen.get(c - weight)));
                        newChosen.get(c).add(item.id());
                    }
                }
            }

            double[] tmp = prev;
            prev = curr;
            curr = tmp;
            chosen = newChosen;
        }

        return new KnapsackResult(chosen.get(capacity), prev[capacity]);
    }

    private double getProfit(int index) {
        return profits != null ? profits[index] : items.get(index).profit();
    }
}

