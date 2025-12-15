package ca.udem.gaillarz.solver.bp;

import ca.udem.gaillarz.formulation.L2Solution;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses which item selection variable t_j to branch on.
 */
public class BranchingRule {

    private final int numItems;
    private final BranchingStrategy strategy;

    public BranchingRule(int numItems) {
        this(numItems, BranchingStrategy.MOST_FRACTIONAL);
    }

    public BranchingRule(int numItems, BranchingStrategy strategy) {
        this.numItems = numItems;
        this.strategy = strategy;
    }

    /**
     * Select a branching item based on current L2 solution and node fixing.
     *
     * @return item index or -1 if no fractional variable
     */
    public int selectBranchItem(L2Solution l2Solution, BranchNode node) {
        List<Integer> candidates = new ArrayList<>();

        // Preferred candidates: 0.1 <= t_j <= 0.9
        for (int j = 0; j < numItems; j++) {
            if (node.isItemFixed(j)) continue;
            double tj = l2Solution.getItemSelection(j);
            if (tj >= 0.1 && tj <= 0.9) {
                candidates.add(j);
            }
        }

        // Fallback: any fractional t_j
        if (candidates.isEmpty()) {
            for (int j = 0; j < numItems; j++) {
                if (node.isItemFixed(j)) continue;
                if (!l2Solution.isItemSelectionInteger(j)) {
                    candidates.add(j);
                }
            }
        }

        if (candidates.isEmpty()) {
            return -1;
        }

        return strategy.select(candidates, l2Solution, node);
    }
}
