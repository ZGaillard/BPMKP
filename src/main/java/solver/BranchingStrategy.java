package solver;

import ca.udem.gaillarz.formulation.L2Solution;

import java.util.List;
import java.util.Random;

/**
 * Strategy for selecting branching variable.
 */
public enum BranchingStrategy {

    /** Branch on item with t_j closest to 0.5 */
    MOST_FRACTIONAL {
        @Override
        public int select(List<Integer> candidates, L2Solution l2Sol, BranchNode node) {
            int best = candidates.get(0);
            double bestDistance = Double.MAX_VALUE;
            for (int j : candidates) {
                double distance = Math.abs(l2Sol.getItemSelection(j) - 0.5);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = j;
                }
            }
            return best;
        }
    },

    /** Branch on first fractional candidate */
    FIRST_FRACTIONAL {
        @Override
        public int select(List<Integer> candidates, L2Solution l2Sol, BranchNode node) {
            return candidates.get(0);
        }
    },

    /** Branch randomly among candidates */
    RANDOM {
        private final Random random = new Random();

        @Override
        public int select(List<Integer> candidates, L2Solution l2Sol, BranchNode node) {
            return candidates.get(random.nextInt(candidates.size()));
        }
    };

    public abstract int select(List<Integer> candidates, L2Solution l2Sol, BranchNode node);
}
