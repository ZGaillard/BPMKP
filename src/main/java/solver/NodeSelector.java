package solver;

import java.util.List;

/**
 * Selects which node to process next.
 */
public class NodeSelector {
    private final NodeSelectionStrategy strategy;

    public NodeSelector() {
        this(NodeSelectionStrategy.BEST_FIRST);
    }

    public NodeSelector(NodeSelectionStrategy strategy) {
        this.strategy = strategy;
    }

    public BranchNode selectNode(List<BranchNode> openNodes) {
        if (openNodes == null || openNodes.isEmpty()) {
            return null;
        }
        return strategy.select(openNodes);
    }
}
