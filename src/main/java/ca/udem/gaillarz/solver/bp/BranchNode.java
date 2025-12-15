package ca.udem.gaillarz.solver.bp;

import ca.udem.gaillarz.formulation.DWSolution;
import ca.udem.gaillarz.formulation.DualValues;
import ca.udem.gaillarz.formulation.L2Solution;
import ca.udem.gaillarz.formulation.Pattern;

import java.util.*;

/**
 * Represents a node in the branch-and-bound tree.
 */
public class BranchNode {

    private static int nodeIdCounter = 0;

    private final int nodeId;
    private final BranchNode parent;
    private final int depth;

    // itemId -> {0,1}
    private final Map<Integer, Integer> fixedItems;

    private double upperBound;
    private double lowerBound;

    private L2Solution l2Solution;
    private DWSolution dwSolution;
    private DualValues dualValues;

    private NodeStatus status;

    /**
     * Create root node.
     */
    public BranchNode() {
        this.nodeId = nodeIdCounter++;
        this.parent = null;
        this.depth = 0;
        this.fixedItems = new HashMap<>();
        this.upperBound = Double.POSITIVE_INFINITY;
        this.lowerBound = Double.NEGATIVE_INFINITY;
        this.status = NodeStatus.OPEN;
    }

    /**
     * Create child node by branching on item.
     *
     * @param parent parent node
     * @param itemId item to fix
     * @param value  0 or 1
     */
    public BranchNode(BranchNode parent, int itemId, int value) {
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException("Branch value must be 0 or 1");
        }
        this.nodeId = nodeIdCounter++;
        this.parent = parent;
        this.depth = parent.depth + 1;
        this.fixedItems = new HashMap<>(parent.fixedItems);
        if (this.fixedItems.containsKey(itemId)) {
            int existing = this.fixedItems.get(itemId);
            if (existing != value) {
                throw new IllegalStateException("Item " + itemId + " already fixed to " + existing + "; cannot fix to " + value);
            }
        }
        this.fixedItems.put(itemId, value);
        this.upperBound = parent.upperBound;
        this.lowerBound = parent.lowerBound;
        this.status = NodeStatus.OPEN;
    }

    // Getters
    public int getNodeId() {
        return nodeId;
    }

    public BranchNode getParent() {
        return parent;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public Map<Integer, Integer> getFixedItems() {
        return Collections.unmodifiableMap(fixedItems);
    }

    public boolean isItemFixed(int itemId) {
        return fixedItems.containsKey(itemId);
    }

    public int getItemFixing(int itemId) {
        return fixedItems.getOrDefault(itemId, -1);
    }

    public Set<Integer> getForbiddenItems() {
        Set<Integer> set = new HashSet<>();
        for (Map.Entry<Integer, Integer> e : fixedItems.entrySet()) {
            if (e.getValue() == 0) {
                set.add(e.getKey());
            }
        }
        return set;
    }

    public Set<Integer> getRequiredItems() {
        Set<Integer> set = new HashSet<>();
        for (Map.Entry<Integer, Integer> e : fixedItems.entrySet()) {
            if (e.getValue() == 1) {
                set.add(e.getKey());
            }
        }
        return set;
    }

    public double getUpperBound() {
        return upperBound;
    }

    // Setters
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public L2Solution getL2Solution() {
        return l2Solution;
    }

    public DWSolution getDWSolution() {
        return dwSolution;
    }

    public DualValues getDualValues() {
        return dualValues;
    }

    public void setSolution(L2Solution l2Solution, DWSolution dwSolution, DualValues dualValues) {
        this.l2Solution = l2Solution;
        this.dwSolution = dwSolution;
        this.dualValues = dualValues;
    }

    /**
     * Create left/right children fixing t_j = 0 and t_j = 1.
     */
    public BranchNode[] createChildren(int itemId) {
        BranchNode left = new BranchNode(this, itemId, 0);
        BranchNode right = new BranchNode(this, itemId, 1);
        return new BranchNode[]{left, right};
    }

    /**
     * Check pattern compatibility against forbidden items.
     */
    public boolean isPatternCompatible(Pattern pattern) {
        for (int item : getForbiddenItems()) {
            if (pattern.containsItem(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Bound-based pruning.
     */
    public boolean canPrune(double globalLowerBound) {
        return upperBound <= globalLowerBound + 1e-6;
    }

    @Override
    public String toString() {
        return String.format("Node[id=%d, depth=%d, UB=%.2f, LB=%.2f, fixed=%d, status=%s]",
                nodeId, depth, upperBound, lowerBound, fixedItems.size(), status);
    }

    public String toDetailedString() {
        return String.format(Locale.US,
                "Node %d (depth %d)%n status=%s%n bounds=[%.2f, %.2f]%n fixed=%s%n forbidden=%s%n required=%s%n",
                nodeId, depth, status, lowerBound, upperBound, fixedItems, getForbiddenItems(), getRequiredItems());
    }
}
