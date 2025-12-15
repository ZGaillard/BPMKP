package solver;

import ca.udem.gaillarz.formulation.*;
import ca.udem.gaillarz.model.MKPInstance;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Branch-and-Price driver combining column generation and branching.
 */
public class BranchAndPrice {

    private static final double TOLERANCE = 1e-5;
    private static final double DEFAULT_GAP_TOLERANCE = 0.01; // 1%

    private final MKPInstance instance;
    private final LPSolver lpSolver;

    // Parameters
    private int maxNodes = Integer.MAX_VALUE;
    private long timeLimitMs = Long.MAX_VALUE;
    private double gapTolerance = DEFAULT_GAP_TOLERANCE;
    private boolean verbose = true;

    // Global state
    private double globalLB = 0.0;
    private double globalUB = Double.POSITIVE_INFINITY;
    private ClassicSolution bestSolution;

    // Stats
    private int nodesProcessed;
    private int nodesPruned;
    private int nodesInfeasible;
    private int integralNodes;
    private long startTime;

    public BranchAndPrice(MKPInstance instance) {
        this(instance, new ORToolsSolver());
    }

    public BranchAndPrice(MKPInstance instance, LPSolver solver) {
        this.instance = instance;
        this.lpSolver = solver;
    }

    // Configuration
    public BranchAndPrice setMaxNodes(int maxNodes) { this.maxNodes = maxNodes; return this; }
    public BranchAndPrice setTimeLimitMs(long timeLimitMs) { this.timeLimitMs = timeLimitMs; return this; }
    public BranchAndPrice setGapTolerance(double gapTolerance) { this.gapTolerance = gapTolerance; return this; }
    public BranchAndPrice setVerbose(boolean verbose) { this.verbose = verbose; return this; }

    /**
     * Solve MKP using Branch-and-Price.
     */
    public BPResult solve() {
        startTime = System.currentTimeMillis();
        if (verbose) {
            System.out.println("============================================================");
            System.out.println("BRANCH-AND-PRICE SOLVER");
            System.out.println("============================================================");
            System.out.printf("Instance: %d items, %d knapsacks%n%n",
                    instance.getNumItems(), instance.getNumKnapsacks());
        }

        // Root master + initial patterns
        L2RelaxedFormulation l2Root = new L2RelaxedFormulation(instance);
        DantzigWolfeMaster rootMaster = l2Root.toDantzigWolfeFormulation();
        if (verbose) System.out.println("Generating initial patterns...");
        PatternInitializer.initialize(rootMaster);
        if (verbose) System.out.printf("Initial patterns: %d%n%n", rootMaster.getTotalPatternCount());

        // Root node
        BranchNode root = new BranchNode();
        PriorityQueue<BranchNode> queue = new PriorityQueue<>(
                Comparator.comparingDouble(BranchNode::getUpperBound).reversed()
        );
        queue.add(root);

        nodesProcessed = nodesPruned = nodesInfeasible = integralNodes = 0;

        while (!queue.isEmpty()) {
            if (nodesProcessed >= maxNodes) {
                if (verbose) System.out.println("Reached node limit.");
                break;
            }
            if (System.currentTimeMillis() - startTime >= timeLimitMs) {
                if (verbose) System.out.println("Reached time limit.");
                break;
            }
            if (computeGap() <= gapTolerance) {
                if (verbose) System.out.println("Reached gap tolerance.");
                break;
            }

            BranchNode node = queue.poll();
            nodesProcessed++;

            if (verbose && (nodesProcessed == 1 || nodesProcessed % 10 == 0)) {
                assert node != null;
                System.out.printf("Node %4d depth=%2d UB=%.2f LB=%.2f gap=%.2f%% queue=%d%n",
                        nodesProcessed, node.getDepth(), node.getUpperBound(), globalLB, computeGap() * 100, queue.size());
            }

            assert node != null;
            if (node.canPrune(globalLB)) {
                nodesPruned++;
                node.setStatus(NodeStatus.PRUNED);
                continue;
            }

            // Build node master with branching filters
            DantzigWolfeMaster nodeMaster = createMasterForNode(rootMaster, node);

            ColumnGeneration cg = new ColumnGeneration(nodeMaster, lpSolver);
            CGResult cgResult = cg.solve();

            if (cgResult.getStatus() == CGStatus.INFEASIBLE) {
                nodesInfeasible++;
                node.setStatus(NodeStatus.INFEASIBLE);
                continue;
            }

            double nodeUB = cgResult.getObjectiveValue();
            node.setUpperBound(nodeUB);
            node.setSolution(cgResult.getL2Solution(), cgResult.getDwSolution(), cgResult.getDualValues());
            node.setStatus(NodeStatus.SOLVED);
            globalUB = Math.min(globalUB, nodeUB);

            if (node.canPrune(globalLB)) {
                nodesPruned++;
                node.setStatus(NodeStatus.PRUNED);
                continue;
            }

            L2Solution l2Sol = cgResult.getL2Solution();
            if (l2Sol != null && l2Sol.isInteger()) {
                integralNodes++;
                ClassicSolution classicSol = l2Sol.toClassicSolution();
                ClassicFormulation classicForm = new ClassicFormulation(instance);
                double obj = classicForm.computeObjectiveValue(classicSol);
                node.setLowerBound(obj);

                if (obj > globalLB) {
                    globalLB = obj;
                    bestSolution = classicSol;
                    if (verbose) {
                        System.out.printf("*** New best solution: %.2f (gap=%.2f%%)%n", globalLB, computeGap() * 100);
                    }
                }
                node.setStatus(NodeStatus.INTEGER);
                continue;
            }

            // Branch
            BranchingRule branchingRule = new BranchingRule(instance.getNumItems());
            int branchItem = branchingRule.selectBranchItem(l2Sol, node);
            if (branchItem < 0) {
                if (verbose) System.out.println("No fractional variable to branch on; skipping node.");
                continue;
            }

            BranchNode[] children = node.createChildren(branchItem);
            queue.add(children[0]); // t_j = 0
            queue.add(children[1]); // t_j = 1

            if (verbose) {
                assert l2Sol != null;
                PrintStream printf = System.out.printf("  Branching on t_%d (value=%.3f)%n", branchItem, l2Sol.getItemSelection(branchItem));
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        BPStatus finalStatus = determineStatus();

        if (verbose) {
            System.out.println();
            System.out.println("============================================================");
            System.out.println("SOLVER FINISHED");
            System.out.println("============================================================");
            System.out.println("Status: " + finalStatus);
            System.out.printf("Best objective: %.2f%n", globalLB);
            System.out.printf("Best bound: %.2f%n", globalUB);
            System.out.printf("Gap: %.2f%%%n", computeGap() * 100);
            System.out.printf("Nodes processed: %d (pruned %d, infeasible %d, integer %d)%n",
                    nodesProcessed, nodesPruned, nodesInfeasible, integralNodes);
            System.out.printf("Time: %.2f seconds%n", totalTime / 1000.0);
        }

        return new BPResult(finalStatus, bestSolution, globalLB, globalUB,
                computeGap(), nodesProcessed, nodesPruned, integralNodes, totalTime);
    }

    /**
     * Build a master for a node by copying root patterns and filtering based on fixings.
     * Patterns in P0 must exclude forbidden items and include all required items.
     * Patterns in P_i exclude forbidden items.
     */
    private DantzigWolfeMaster createMasterForNode(DantzigWolfeMaster rootMaster, BranchNode node) {
        L2RelaxedFormulation l2 = new L2RelaxedFormulation(instance);
        return new DantzigWolfeFormulationWithPatterns(node, rootMaster, l2);
    }

    /**
     * Lightweight helper class to copy patterns with branch filters applied.
     */
    private static class DantzigWolfeFormulationWithPatterns extends DantzigWolfeMaster {
        DantzigWolfeFormulationWithPatterns(BranchNode node, DantzigWolfeMaster source, L2RelaxedFormulation l2) {
            super(l2);
            var required = node.getRequiredItems();
            var forbidden = node.getForbiddenItems();

            // P0
            for (Pattern p : source.getPatternsP0()) {
                if (isCompatible(p, required, forbidden, true)) {
                    addPatternP0(p);
                }
            }
            // PI
            for (int i = 0; i < getInstance().getNumKnapsacks(); i++) {
                for (Pattern p : source.getPatternsPI(i)) {
                    if (isCompatible(p, required, forbidden, false)) {
                        addPatternPI(i, p);
                    }
                }
            }
        }

        private boolean isCompatible(Pattern p, java.util.Set<Integer> required, java.util.Set<Integer> forbidden, boolean checkRequired) {
            for (int item : forbidden) {
                if (p.containsItem(item)) return false;
            }
            if (checkRequired) {
                for (int item : required) {
                    if (!p.containsItem(item)) return false;
                }
            }
            return true;
        }
    }

    private double computeGap() {
        if (!Double.isFinite(globalUB) || globalUB == 0.0) return 1.0;
        return Math.max(0.0, (globalUB - globalLB) / Math.abs(globalUB));
    }

    private BPStatus determineStatus() {
        double gap = computeGap();
        if (gap <= TOLERANCE) return BPStatus.OPTIMAL;
        if (bestSolution != null) {
            if (System.currentTimeMillis() - startTime >= timeLimitMs) return BPStatus.TIME_LIMIT;
            if (nodesProcessed >= maxNodes) return BPStatus.NODE_LIMIT;
            if (gap <= gapTolerance) return BPStatus.GAP_LIMIT;
            return BPStatus.FEASIBLE;
        }
        return BPStatus.INFEASIBLE;
    }
}
