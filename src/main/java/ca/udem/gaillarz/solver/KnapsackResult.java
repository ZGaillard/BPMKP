package ca.udem.gaillarz.solver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set; /**
 * Package-private result holder for knapsack solutions.
 */
public class KnapsackResult {
    private final Set<Integer> selectedItemIds;
    private final double optimalProfit;

    KnapsackResult(Set<Integer> selectedItemIds, double optimalProfit) {
        this.selectedItemIds = new HashSet<>(selectedItemIds);
        this.optimalProfit = optimalProfit;
    }

    public Set<Integer> getSelectedItemIds() {
        return Collections.unmodifiableSet(selectedItemIds);
    }

    public double getOptimalProfit() {
        return optimalProfit;
    }

    public boolean isEmpty() {
        return selectedItemIds.isEmpty();
    }

    public int getNumItems() {
        return selectedItemIds.size();
    }

    @Override
    public String toString() {
        return String.format("KnapsackResult(items=%s, profit=%.2f)", selectedItemIds, optimalProfit);
    }
}
