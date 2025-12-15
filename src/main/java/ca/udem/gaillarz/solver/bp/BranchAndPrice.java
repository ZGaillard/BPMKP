package ca.udem.gaillarz.solver.bp;

import ca.udem.gaillarz.formulation.*;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.preprocessing.PreprocessingResult;
import ca.udem.gaillarz.preprocessing.ReducedCostExtractor;
import ca.udem.gaillarz.preprocessing.ReducedCostFixer;
import ca.udem.gaillarz.solver.cg.CGParameters;
import ca.udem.gaillarz.solver.cg.CGResult;
import ca.udem.gaillarz.solver.cg.CGStatus;
import ca.udem.gaillarz.solver.cg.ColumnGeneration;
import ca.udem.gaillarz.solver.lp.LPSolver;
import ca.udem.gaillarz.solver.lp.ORToolsSolver;
import ca.udem.gaillarz.solver.vsbpp.OrToolsCpSatVSBPPSATChecker;
import ca.udem.gaillarz.solver.vsbpp.VSBPPSATChecker;
import ca.udem.gaillarz.solver.vsbpp.VSBPPSATResult;
import ca.udem.gaillarz.solver.vsbpp.VSBPPSATStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.List;

/**
 * Branch-and-Price driver combining column generation and branching.
 */
public class BranchAndPrice {

    private static final double TOLERANCE = 1e-5;
    private static final double DEFAULT_GAP_TOLERANCE = 0.01; // 1%
    private final MKPInstance instance;
    private final LPSolver lpSolver;
    private final VSBPPSATChecker feasibilityChecker = new OrToolsCpSatVSBPPSATChecker();
    private final NoGoodCutManager cutManager = new NoGoodCutManager();
    // Parameters
    private int maxNodes = Integer.MAX_VALUE;
    private long timeLimitMs = Long.MAX_VALUE;
    private double gapTolerance = DEFAULT_GAP_TOLERANCE;
    private boolean verbose = true;
    private boolean preprocessingEnabled = true;
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
    public BranchAndPrice setMaxNodes(int maxNodes) {
        this.maxNodes = maxNodes;
        return this;
    }

    public BranchAndPrice setTimeLimitMs(long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
        return this;
    }

    public BranchAndPrice setGapTolerance(double gapTolerance) {
        this.gapTolerance = gapTolerance;
        return this;
    }

    public BranchAndPrice setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public BranchAndPrice setPreprocessing(boolean enable) {
        this.preprocessingEnabled = enable;
        return this;
    }

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
        if (verbose) System.out.println("[BAP] Generating initial patterns...");
        PatternInitializer.initialize(rootMaster);
        if (verbose) System.out.printf("[BAP] Initial patterns: %d%n%n", rootMaster.getTotalPatternCount());

        // Solve root LP for bounds and preprocessing information
        if (verbose) System.out.println("[BAP] Solving root LP relaxation...");
        ColumnGeneration rootCG = new ColumnGeneration(rootMaster, lpSolver);
        CGParameters rootParams = new CGParameters().setVerbose(verbose);
        CGResult rootResult = rootCG.solve(rootParams);

        if (rootResult.status() != CGStatus.OPTIMAL) {
            if (verbose) {
                System.out.println("[BAP] Root LP not optimal: " + rootResult.status());
            }
            long totalTime = System.currentTimeMillis() - startTime;
            return new BPResult(BPStatus.ERROR, null, 0.0, Double.POSITIVE_INFINITY,
                    1.0, 0, 0, 0, totalTime);
        }

        double rootBound = rootResult.objectiveValue();
        globalUB = rootBound;
        if (verbose) {
            System.out.printf("[BAP] Root LP bound: %.2f%n", rootBound);
        }

        double heuristicLB = runGreedyHeuristic();
        if (verbose) {
            System.out.printf("[BAP] Heuristic LB: %.2f%n", heuristicLB);
        }

        Map<Integer, Integer> preprocessingFixings = new HashMap<>();
        if (preprocessingEnabled && heuristicLB > 0 && rootResult.l2Solution() != null && rootResult.dualValues() != null) {
            long prepStart = System.currentTimeMillis();
            ReducedCostExtractor extractor = new ReducedCostExtractor(instance);
            ReducedCostFixer fixer = new ReducedCostFixer(instance);

            double[] reducedCosts = extractor.extractReducedCosts(rootResult.l2Solution(), rootResult.dualValues());
            int fixed = fixer.fixByReducedCosts(rootBound, heuristicLB, reducedCosts);

            if (fixed > 0) {
                preprocessingFixings.putAll(fixer.getFixedItems());
            }

            double prepTime = (System.currentTimeMillis() - prepStart) / 1000.0;
            if (verbose) {
                PreprocessingResult prepResult = new PreprocessingResult(preprocessingFixings, instance.getNumItems(), prepTime);
                System.out.println("[BAP] Preprocessing complete: " + prepResult);
            }
        }

        // Root node
        BranchNode root = new BranchNode();
        root.setUpperBound(rootBound);
        root.addFixings(preprocessingFixings);
        PriorityQueue<BranchNode> queue = new PriorityQueue<>(
                Comparator.comparingDouble(BranchNode::getUpperBound).reversed()
        );
        queue.add(root);

        nodesProcessed = nodesPruned = nodesInfeasible = integralNodes = 0;

        while (!queue.isEmpty()) {
            if (nodesProcessed >= maxNodes) {
                if (verbose) System.out.println("[BAP] Reached node limit.");
                break;
            }
            if (System.currentTimeMillis() - startTime >= timeLimitMs) {
                if (verbose) System.out.println("[BAP] Reached time limit.");
                break;
            }
            if (computeGap() <= gapTolerance) {
                if (verbose) System.out.println("[BAP] Reached gap tolerance.");
                break;
            }

            BranchNode node = queue.poll();
            nodesProcessed++;

            if (verbose && (nodesProcessed == 1 || nodesProcessed % 5 == 0)) {
                assert node != null;
                System.out.printf("[BAP] Node %4d depth=%2d UB=%.2f LB=%.2f gap=%.2f%% queue=%d%n",
                        nodesProcessed, node.getDepth(), node.getUpperBound(), globalLB, computeGap() * 100, queue.size());
            }

            assert node != null;
            if (node.canPrune(globalLB)) {
                nodesPruned++;
                node.setStatus(NodeStatus.PRUNED);
                if (verbose) System.out.println("[BAP] Prune by bound (UB <= global LB).");
                updateGlobalUpperBound(queue, Double.NaN);
                continue;
            }

            // Build node master with branching filters
            DantzigWolfeMaster nodeMaster = createMasterForNode(rootMaster, node);

            if (verbose) {
                System.out.printf("[BAP] Solving node (depth %d, fixed=%d, patterns=%d)%n",
                        node.getDepth(), node.getFixedItems().size(), nodeMaster.getTotalPatternCount());
            }

            ColumnGeneration cg = new ColumnGeneration(nodeMaster, lpSolver);
            cg.setBranchingConstraints(node.getForbiddenItems(), node.getRequiredItems());
            CGParameters cgParams = new CGParameters().setVerbose(verbose);
            CGResult cgResult = cg.solve(cgParams);

            if (cgResult.status() != CGStatus.OPTIMAL) {
                nodesInfeasible++;
                node.setStatus(NodeStatus.INFEASIBLE);
                if (verbose) System.out.println("[BAP] Node not optimal by CG (" + cgResult.status() + ").");
                updateGlobalUpperBound(queue, Double.NaN);
                continue;
            }

            double nodeUB = cgResult.objectiveValue();
            node.setUpperBound(nodeUB);
            node.setSolution(cgResult.l2Solution(), cgResult.dwSolution(), cgResult.dualValues());
            node.setStatus(NodeStatus.SOLVED);
            updateGlobalUpperBound(queue, nodeUB);

            if (verbose) {
                System.out.printf("[BAP] Node UB=%.2f, global LB=%.2f, gap=%.2f%%%n",
                        nodeUB, globalLB, computeGap() * 100);
            }

            if (node.canPrune(globalLB)) {
                nodesPruned++;
                node.setStatus(NodeStatus.PRUNED);
                if (verbose) System.out.println("[BAP] Pruning after CG (UB <= global LB).");
                updateGlobalUpperBound(queue, Double.NaN);
                continue;
            }

            L2Solution l2Sol = cgResult.l2Solution();
            if (l2Sol == null) {
                nodesInfeasible++;
                node.setStatus(NodeStatus.INFEASIBLE);
                if (verbose) System.out.println("[BAP] No L2 solution returned; treating node as infeasible.");
                updateGlobalUpperBound(queue, Double.NaN);
                continue;
            }

            boolean integerT = l2Sol.areItemSelectionsInteger();
            boolean integerX = l2Sol.areAssignmentsInteger();

            if (integerT && integerX) {
                integralNodes++;
                try {
                    ClassicSolution classicSol = l2Sol.toClassicSolution();
                    ClassicFormulation classicForm = new ClassicFormulation(instance);
                    double obj = classicForm.computeObjectiveValue(classicSol);
                    node.setLowerBound(obj);

                    if (obj > globalLB) {
                        globalLB = obj;
                        bestSolution = classicSol;
                        if (verbose) {
                            System.out.printf("[BAP] New best integer solution: %.2f (gap=%.2f%%)%n",
                                    globalLB, computeGap() * 100);
                        }
                    }
                    node.setStatus(NodeStatus.INTEGER);
                    updateGlobalUpperBound(queue, Double.NaN);
                    continue;
                } catch (Exception ex) {
                    nodesInfeasible++;
                    node.setStatus(NodeStatus.INFEASIBLE);
                    if (verbose) {
                        System.out.println("[BAP] Integer-looking L2 solution failed conversion: " + ex.getMessage());
                    }
                    updateGlobalUpperBound(queue, Double.NaN);
                }
            }

            // t is integer but x is fractional/invalid: run VSBPP feasibility
            if (integerT && !integerX) {
                VSBPPSATResult feas = feasibilityChecker.checkFeasibility(instance, l2Sol, 2000);
                if (feas.status() == VSBPPSATStatus.FEASIBLE && feas.itemToBin() != null) {
                    integralNodes++;
                    ClassicSolution classicSol = new ClassicSolution(instance.getNumKnapsacks(), instance.getNumItems());
                    int[] map = feas.itemToBin();
                    for (int j = 0; j < (map != null ? map.length : 0); j++) {
                        if (map[j] >= 0) {
                            classicSol.assignItem(map[j], j);
                        }
                    }
                    ClassicFormulation classicForm = new ClassicFormulation(instance);
                    double obj = classicForm.computeObjectiveValue(classicSol);
                    node.setLowerBound(obj);
                    if (obj > globalLB) {
                        globalLB = obj;
                        bestSolution = classicSol;
                        if (verbose) {
                            System.out.printf("[BAP] VSBPP feasible -> new best: %.2f (gap=%.2f%%)%n",
                                    globalLB, computeGap() * 100);
                        }
                    }
                    node.setStatus(NodeStatus.INTEGER);
                    updateGlobalUpperBound(queue, Double.NaN);
                    continue;
                } else if (feas.status() == VSBPPSATStatus.INFEASIBLE) {
                    cutManager.addInfeasibleSet(feas.selectedItems());
                    if (verbose) {
                        System.out.println("[BAP] VSBPP infeasible; adding no-good cut for selection: " + feas.selectedItems());
                    }
                    queue.add(node); // reprocess with cuts
                    node.setStatus(NodeStatus.OPEN);
                    updateGlobalUpperBound(queue, node.getUpperBound());
                    continue;
                } else {
                    // Unknown/error/time-limit from SAT: add a no-good cut on the current selection to force a different assignment
                    cutManager.addInfeasibleSet(selectedItemsFrom(l2Sol));
                    if (verbose) {
                        System.out.println("[BAP] VSBPP returned " + feas.status() + "; adding cut on current selection and retrying node.");
                    }
                    queue.add(node);
                    node.setStatus(NodeStatus.OPEN);
                    updateGlobalUpperBound(queue, node.getUpperBound());
                    continue;
                }
            }

            // Branch
            BranchingRule branchingRule = new BranchingRule(instance.getNumItems());
            int branchItem = branchingRule.selectBranchItem(l2Sol, node);
            if (branchItem < 0) {
                if (verbose) {
                    System.out.println("[BAP] No fractional variable to branch on; skipping node.");
                    String fractional = l2Sol.firstFractionalVariable();
                    if (fractional != null) {
                        System.out.println("[BAP] First fractional variable: " + fractional);
                    }
                }
                updateGlobalUpperBound(queue, Double.NaN);
                continue;
            }

            BranchNode[] children = node.createChildren(branchItem);
            queue.add(children[0]); // t_j = 0
            queue.add(children[1]); // t_j = 1

            if (verbose) {
                System.out.printf("[BAP] Branching on t_%d (value=%.3f) -> children %d and %d%n",
                        branchItem, l2Sol.getItemSelection(branchItem),
                        children[0].getNodeId(), children[1].getNodeId());
            }

            updateGlobalUpperBound(queue, queue.peek() != null ? queue.peek().getUpperBound() : nodeUB);
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
        return new DantzigWolfeFormulationWithPatterns(node, rootMaster, l2, cutManager);
    }

    /**
     * Run a greedy heuristic to provide a quick feasible lower bound.
     */
    private double runGreedyHeuristic() {
        List<Integer> items = new ArrayList<>();
        for (int j = 0; j < instance.getNumItems(); j++) {
            items.add(j);
        }

        items.sort((a, b) -> {
            double ratioA = (double) instance.getItem(a).profit() / instance.getItem(a).weight();
            double ratioB = (double) instance.getItem(b).profit() / instance.getItem(b).weight();
            return Double.compare(ratioB, ratioA);
        });

        int[] remainingCapacity = new int[instance.getNumKnapsacks()];
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            remainingCapacity[i] = instance.getKnapsack(i).capacity();
        }

        double totalProfit = 0.0;
        for (int itemId : items) {
            int weight = instance.getItem(itemId).weight();
            int profit = instance.getItem(itemId).profit();
            for (int i = 0; i < remainingCapacity.length; i++) {
                if (remainingCapacity[i] >= weight) {
                    remainingCapacity[i] -= weight;
                    totalProfit += profit;
                    break;
                }
            }
        }

        return totalProfit;
    }

    private double computeGap() {
        if (!Double.isFinite(globalUB) || globalUB <= 0.0) return 1.0;
        if (globalLB <= 0.0) return 1.0;
        double gap = (globalUB - globalLB) / Math.abs(globalUB);
        return Math.max(0.0, gap);
    }

    private Set<Integer> selectedItemsFrom(L2Solution l2Sol) {
        Set<Integer> selected = new HashSet<>();
        for (int j = 0; j < l2Sol.getNumItems(); j++) {
            if (l2Sol.getItemSelection(j) > 0.5) {
                selected.add(j);
            }
        }
        return selected;
    }

    private void updateGlobalUpperBound(PriorityQueue<BranchNode> queue, double candidateUB) {
        double best = Double.NEGATIVE_INFINITY;
        if (Double.isFinite(candidateUB)) {
            best = candidateUB;
        }
        BranchNode peek = queue.peek();
        if (peek != null && Double.isFinite(peek.getUpperBound())) {
            best = Math.max(best, peek.getUpperBound());
        }
        if (best == Double.NEGATIVE_INFINITY) {
            // No open nodes; align UB with LB if we have a feasible solution
            globalUB = bestSolution != null ? globalLB : globalUB;
        } else {
            globalUB = best;
        }
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

    /**
     * Lightweight helper class to copy patterns with branch filters applied.
     */
    public static class DantzigWolfeFormulationWithPatterns extends DantzigWolfeMaster implements SupportsNoGoodCuts {
        private final NoGoodCutManager cutManager;
        private final Set<Integer> requiredItems;
        private final Set<Integer> forbiddenItems;

        DantzigWolfeFormulationWithPatterns(BranchNode node, DantzigWolfeMaster source, L2RelaxedFormulation l2, NoGoodCutManager cutManager) {
            super(l2);
            this.cutManager = cutManager;
            this.requiredItems = Set.copyOf(node.getRequiredItems());
            this.forbiddenItems = Set.copyOf(node.getForbiddenItems());

            // P0
            for (Pattern p : source.getPatternsP0()) {
                if (isCompatible(p, requiredItems, forbiddenItems, true)) {
                    addPatternP0(p);
                }
            }
            // PI
            for (int i = 0; i < getInstance().getNumKnapsacks(); i++) {
                for (Pattern p : source.getPatternsPI(i)) {
                    if (isCompatible(p, requiredItems, forbiddenItems, false)) {
                        addPatternPI(i, p);
                    }
                }
            }
        }

        private boolean isCompatible(Pattern p, Set<Integer> required, Set<Integer> forbidden, boolean checkRequired) {
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

        @Override
        public NoGoodCutManager getCutManager() {
            return cutManager;
        }

        public Set<Integer> getRequiredItems() {
            return requiredItems;
        }

        public Set<Integer> getForbiddenItems() {
            return forbiddenItems;
        }
    }
}
