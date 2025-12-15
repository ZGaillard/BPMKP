package solver;

import java.util.List;

/**
 * Strategies for picking next node to process.
 */
public enum NodeSelectionStrategy {
    /** Highest upper bound (best-first) */
    BEST_FIRST {
        @Override
        public BranchNode select(List<BranchNode> nodes) {
            BranchNode best = nodes.get(0);
            for (BranchNode node : nodes) {
                if (node.getUpperBound() > best.getUpperBound()) {
                    best = node;
                }
            }
            return best;
        }
    },
    /** Depth-first */
    DEPTH_FIRST {
        @Override
        public BranchNode select(List<BranchNode> nodes) {
            BranchNode deepest = nodes.get(0);
            for (BranchNode node : nodes) {
                if (node.getDepth() > deepest.getDepth()) deepest = node;
            }
            return deepest;
        }
    },
    /** Breadth-first */
    BREADTH_FIRST {
        @Override
        public BranchNode select(List<BranchNode> nodes) {
            BranchNode shallowest = nodes.get(0);
            for (BranchNode node : nodes) {
                if (node.getDepth() < shallowest.getDepth()) shallowest = node;
            }
            return shallowest;
        }
    },
    /** Lowest lower bound */
    WORST_FIRST {
        @Override
        public BranchNode select(List<BranchNode> nodes) {
            BranchNode worst = nodes.get(0);
            for (BranchNode node : nodes) {
                if (node.getLowerBound() < worst.getLowerBound()) worst = node;
            }
            return worst;
        }
    };

    public abstract BranchNode select(List<BranchNode> nodes);
}
