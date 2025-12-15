package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.model.MKPInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dantzig-Wolfe Master formulation (equations 28-33).
 *
 * <pre>
 * max Σ_{a∈P_0} (Σ_{j=1}^{n} p_j * a_j) * y_a - Σ_{j=1}^{n} p_j * s_j     (28)
 *
 * s.t.  Σ_{a∈P_0} a_j * y_a ≤ Σ_{i=1}^{m} Σ_{a∈P_i} a_j * y_a + s_j
 *                                              for j = 1,...,n      (29)
 *
 *       Σ_{a∈P_i} y_a = 1                      for i = 0,...,m      (30)
 *
 *       Σ_{j=1}^{n} p_j * (Σ_{a∈P_0} a_j * y_a) ≤ UB                (31)
 *
 *       y_a ∈ {0,1}                            for all a, all i     (32)
 *
 *       0 ≤ s_j ≤ 1                            for j = 1,...,n      (33)
 * </pre>
 * <p>
 * Key relationships to L2:
 * <pre>
 *   t_j = Σ_{a∈P_0} a_j * y_a    (derive item selection)
 *   x_ij = Σ_{a∈P_i} a_j * y_a   (derive item assignment)
 * </pre>
 */
public class DantzigWolfeMaster {
    private static final double TOLERANCE = 1e-5;

    private final MKPInstance instance;
    private final L2RelaxedFormulation l2Formulation;

    // Pattern pools
    private final List<Pattern> patternsP0;           // P_0 patterns (aggregated capacity)
    private final List<List<Pattern>> patternsPI;     // P_i patterns (one list per knapsack)

    // Upper bound for constraint (31)
    private double upperBound;

    /**
     * Creates a Dantzig-Wolfe master formulation from an L2 formulation.
     *
     * @param l2Formulation L2 relaxed formulation
     */
    public DantzigWolfeMaster(L2RelaxedFormulation l2Formulation) {
        if (l2Formulation == null) {
            throw new IllegalArgumentException("L2 formulation cannot be null");
        }

        this.l2Formulation = l2Formulation;
        this.instance = l2Formulation.getInstance();

        // Initialize pattern pools
        this.patternsP0 = new ArrayList<>();
        this.patternsPI = new ArrayList<>();
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            patternsPI.add(new ArrayList<>());
        }

        // Default upper bound: total profit
        this.upperBound = instance.getTotalProfit();
    }

    /**
     * @return The underlying MKP instance
     */
    public MKPInstance getInstance() {
        return instance;
    }

    /**
     * @return The L2 relaxed formulation
     */
    public L2RelaxedFormulation getL2Formulation() {
        return l2Formulation;
    }

    /**
     * Seed the master with a small feasible pattern pool:
     * - empty pattern in all pools
     * - all single-item patterns in all feasible pools
     * - one greedy fill pattern per pool (capacity-based)
     */
    public void seedInitialPatterns() {
        // Empty pattern
        Pattern empty = Pattern.empty(instance.getNumItems());
        safeAddP0(empty);
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            safeAddPI(i, empty);
        }

        // Single-item patterns
        for (int j = 0; j < instance.getNumItems(); j++) {
            Pattern single = Pattern.singleItem(j, instance);
            safeAddP0(single);
            for (int i = 0; i < instance.getNumKnapsacks(); i++) {
                safeAddPI(i, single);
            }
        }

        // Greedy patterns (one per pool)
        Pattern greedyP0 = buildGreedyPattern(instance.getTotalCapacity());
        safeAddP0(greedyP0);
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            Pattern greedy = buildGreedyPattern(instance.getKnapsack(i).capacity());
            safeAddPI(i, greedy);
        }
    }

    // ========== Pattern Management ==========

    /**
     * Add pattern to P_0 (aggregated capacity).
     * Only adds if pattern is feasible for total capacity.
     *
     * @param pattern Pattern to add
     * @throws FormulationException if pattern exceeds total capacity
     */
    public void addPatternP0(Pattern pattern) {
        if (!pattern.isFeasible(instance.getTotalCapacity())) {
            throw new FormulationException(
                    String.format("Pattern weight %d exceeds total capacity %d",
                            pattern.getTotalWeight(), instance.getTotalCapacity()));
        }

        for (Pattern existing : patternsP0) {
            if (existing.hasSameItems(pattern)) {
                return; // already present
            }
        }
        patternsP0.add(pattern);
    }

    /**
     * Add pattern to P_i (knapsack i).
     * Only adds if pattern is feasible for knapsack capacity.
     *
     * @param knapsackId Knapsack index
     * @param pattern    Pattern to add
     * @throws FormulationException if pattern exceeds knapsack capacity
     */
    public void addPatternPI(int knapsackId, Pattern pattern) {
        if (knapsackId < 0 || knapsackId >= instance.getNumKnapsacks()) {
            throw new IllegalArgumentException("Invalid knapsack ID: " + knapsackId);
        }

        int capacity = instance.getKnapsack(knapsackId).capacity();
        if (!pattern.isFeasible(capacity)) {
            throw new FormulationException(
                    String.format("Pattern weight %d exceeds knapsack %d capacity %d",
                            pattern.getTotalWeight(), knapsackId, capacity));
        }

        List<Pattern> patterns = patternsPI.get(knapsackId);

        for (Pattern existing : patterns) {
            if (existing.hasSameItems(pattern)) {
                return; // already present
            }
        }
        patterns.add(pattern);
    }

    private void safeAddP0(Pattern pattern) {
        try {
            addPatternP0(pattern);
        } catch (FormulationException ignored) {
            // skip infeasible/duplicate
        }
    }

    private void safeAddPI(int knapsackId, Pattern pattern) {
        try {
            addPatternPI(knapsackId, pattern);
        } catch (FormulationException ignored) {
            // skip infeasible/duplicate
        }
    }

    /**
     * Get all patterns in P_0.
     *
     * @return Unmodifiable list of P_0 patterns
     */
    public List<Pattern> getPatternsP0() {
        return Collections.unmodifiableList(patternsP0);
    }

    /**
     * Get all patterns for knapsack i.
     *
     * @param knapsackId Knapsack index
     * @return Unmodifiable list of P_i patterns
     */
    public List<Pattern> getPatternsPI(int knapsackId) {
        return Collections.unmodifiableList(patternsPI.get(knapsackId));
    }

    /**
     * Get total number of patterns across all sets.
     *
     * @return Total pattern count
     */
    public int getTotalPatternCount() {
        int count = patternsP0.size();
        for (List<Pattern> patterns : patternsPI) {
            count += patterns.size();
        }
        return count;
    }

    /**
     * Remove pattern from P_0.
     *
     * @param pattern Pattern to remove
     * @return true if pattern was removed
     */
    public boolean removePatternP0(Pattern pattern) {
        return patternsP0.remove(pattern);
    }

    /**
     * Remove pattern from P_i.
     *
     * @param knapsackId Knapsack index
     * @param pattern    Pattern to remove
     * @return true if pattern was removed
     */
    public boolean removePatternPI(int knapsackId, Pattern pattern) {
        return patternsPI.get(knapsackId).remove(pattern);
    }

    /**
     * Clear all patterns.
     */
    public void clearAllPatterns() {
        patternsP0.clear();
        for (List<Pattern> patterns : patternsPI) {
            patterns.clear();
        }
    }

    /**
     * @return Current upper bound
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * Set upper bound for constraint (31).
     *
     * @param ub Upper bound value
     */
    public void setUpperBound(double ub) {
        this.upperBound = ub;
    }

    // ========== Objective and Feasibility ==========

    /**
     * Compute objective value (equation 28).
     * Objective = Σ_{a∈P_0} (Σ_j p_j*a_j)*y_a - Σ_j p_j*s_j
     *
     * @param solution Solution to evaluate
     * @return Objective value
     */
    public double computeObjectiveValue(DWSolution solution) {
        double objective = 0.0;

        // Pattern contribution: Σ_{a∈P_0} profit(a)*y_a
        for (Pattern p : patternsP0) {
            objective += p.getTotalProfit() * solution.getPatternValueP0(p);
        }

        // Dual cut penalty: -Σ_j p_j*s_j
        for (int j = 0; j < instance.getNumItems(); j++) {
            objective -= instance.getItem(j).profit() * solution.getDualCutValue(j);
        }

        return objective;
    }

    /**
     * Check if solution satisfies all constraints (equations 29-33).
     *
     * @param solution Solution to check
     * @return true if feasible
     */
    public boolean isFeasible(DWSolution solution) {
        return checkItemConsistency(solution) &&
                checkPatternSelection(solution) &&
                checkUpperBound(solution);
    }

    /**
     * Check item-consistency constraints (equation 29).
     * Σ_{a∈P_0} a_j*y_a ≤ Σ_{i=1}^m Σ_{a∈P_i} a_j*y_a + s_j
     *
     * @param solution Solution to check
     * @return true if all item-consistency constraints are satisfied
     */
    public boolean checkItemConsistency(DWSolution solution) {
        for (int j = 0; j < instance.getNumItems(); j++) {
            // LHS: Σ_{a∈P_0} a_j * y_a
            double lhs = 0.0;
            for (Pattern p : patternsP0) {
                if (p.containsItem(j)) {
                    lhs += solution.getPatternValueP0(p);  // CHANGED: Content-based lookup
                }
            }

            // RHS: Σ_{i=1}^m Σ_{a∈P_i} a_j * y_a + s_j
            double rhs = solution.getDualCutValue(j);
            for (int i = 0; i < instance.getNumKnapsacks(); i++) {
                for (Pattern p : patternsPI.get(i)) {
                    if (p.containsItem(j)) {
                        rhs += solution.getPatternValuePI(p, i);  // CHANGED: Content-based lookup
                    }
                }
            }

            if (lhs > rhs + TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check pattern selection constraints (equation 30).
     * Σ_{a∈P_i} y_a = 1 for all i (including i=0)
     *
     * @param solution Solution to check
     * @return true if all pattern selection constraints satisfied
     */
    public boolean checkPatternSelection(DWSolution solution) {
        // Check P_0: Σ_{a∈P_0} y_a = 1
        double sumP0 = 0.0;
        for (Pattern p : patternsP0) {
            sumP0 += solution.getPatternValueP0(p);
        }
        if (Math.abs(sumP0 - 1.0) > TOLERANCE) {
            return false;
        }

        // Check each P_i: Σ_{a∈P_i} y_a = 1
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            double sumPi = 0.0;
            for (Pattern p : patternsPI.get(i)) {
                sumPi += solution.getPatternValuePI(p, i);
            }
            if (Math.abs(sumPi - 1.0) > TOLERANCE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check upper bound constraint (equation 31).
     * Σ_j p_j * (Σ_{a∈P_0} a_j*y_a) ≤ UB
     *
     * @param solution Solution to check
     * @return true if upper bound constraint is satisfied
     */
    public boolean checkUpperBound(DWSolution solution) {
        double profit = getTotalProfitP0(solution);
        return profit <= upperBound + TOLERANCE;
    }

    // ========== Conversion Methods ==========

    /**
     * Convert DW solution to L2 solution.
     * <p>
     * Derives:
     * t_j = Σ_{a∈P_0} a_j * y_a
     * x_ij = Σ_{a∈P_i} a_j * y_a
     *
     * @param dwSolution DW solution to convert
     * @return Corresponding L2 solution
     */
    public L2Solution toL2Solution(DWSolution dwSolution) {
        int n = instance.getNumItems();
        int m = instance.getNumKnapsacks();

        // Compute t_j = Σ_{a∈P_0} a_j * y_a
        double[] t = new double[n];
        for (int j = 0; j < n; j++) {
            t[j] = computeItemSelection(dwSolution, j);
        }

        // Compute x_ij = Σ_{a∈P_i} a_j * y_a
        double[][] x = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                x[i][j] = computeItemAssignment(dwSolution, i, j);
            }
        }

        return new L2Solution(t, x);
    }

    /**
     * Convert L2 solution to Classic solution.
     * Only works if L2 solution is integer.
     *
     * @param l2Solution L2 solution to convert
     * @return Classic solution
     * @throws IllegalArgumentException if L2 solution is fractional
     */
    public ClassicSolution toClassicSolution(L2Solution l2Solution) {
        return l2Solution.toClassicSolution();
    }

    /**
     * Direct conversion from DW to Classic (combines above two).
     * Only works if derived L2 solution would be integer.
     *
     * @param dwSolution DW solution to convert
     * @return Classic solution
     * @throws FormulationException if derived solution is fractional
     */
    public ClassicSolution toClassicSolution(DWSolution dwSolution) {
        L2Solution l2Solution = toL2Solution(dwSolution);
        if (!l2Solution.isInteger()) {
            throw new FormulationException("Cannot convert fractional DW solution to classic solution");
        }
        return l2Solution.toClassicSolution();
    }

    // ========== Helper Methods ==========

    private Pattern buildGreedyPattern(int capacity) {
        boolean[] selected = new boolean[instance.getNumItems()];
        int currentWeight = 0;

        List<Integer> order = new ArrayList<>();
        for (int j = 0; j < instance.getNumItems(); j++) {
            order.add(j);
        }
        order.sort((a, b) -> {
            double ra = instance.getItem(a).getProfitWeightRatio();
            double rb = instance.getItem(b).getProfitWeightRatio();
            int cmp = Double.compare(rb, ra); // descending ratio
            if (cmp != 0) return cmp;
            return Integer.compare(a, b);
        });

        for (int j : order) {
            int w = instance.getItem(j).weight();
            if (currentWeight + w <= capacity) {
                selected[j] = true;
                currentWeight += w;
            }
        }
        return new Pattern(selected, instance);
    }

    /**
     * Compute t_j value from DW solution.
     * t_j = Σ_{a∈P_0} a_j * y_a
     *
     * @param solution Solution to evaluate
     * @param itemId   Item index
     * @return t_j value
     */
    public double computeItemSelection(DWSolution solution, int itemId) {
        double tj = 0.0;
        for (Pattern p : patternsP0) {
            if (p.containsItem(itemId)) {
                tj += solution.getPatternValueP0(p);
            }
        }
        return tj;
    }

    /**
     * Compute x_ij value from DW solution.
     * x_ij = Σ_{a∈P_i} a_j * y_a
     *
     * @param solution   Solution to evaluate
     * @param knapsackId Knapsack index
     * @param itemId     Item index
     * @return x_ij value
     */
    public double computeItemAssignment(DWSolution solution, int knapsackId, int itemId) {
        double xij = 0.0;
        for (Pattern p : patternsPI.get(knapsackId)) {
            if (p.containsItem(itemId)) {
                // Use convenience method - content-based lookup!
                xij += solution.getPatternValuePI(p, knapsackId);
            }
        }
        return xij;
    }

    /**
     * Get total profit from P_0 patterns in solution.
     *
     * @param solution Solution to evaluate
     * @return Total profit
     */
    public double getTotalProfitP0(DWSolution solution) {
        double profit = 0.0;
        for (Pattern p : patternsP0) {
            profit += p.getTotalProfit() * solution.getPatternValueP0(p);
        }
        return profit;
    }

    @Override
    public String toString() {
        StringBuilder patternCounts = new StringBuilder("[");
        for (int i = 0; i < patternsPI.size(); i++) {
            if (i > 0) patternCounts.append(",");
            patternCounts.append(patternsPI.get(i).size());
        }
        patternCounts.append("]");

        return String.format("DWMKP(n=%d, m=%d, |P_0|=%d, |P_i|=%s, UB=%.1f)",
                instance.getNumItems(), instance.getNumKnapsacks(),
                patternsP0.size(), patternCounts, upperBound);
    }

    /**
     * Display formulation structure.
     *
     * @return Structure string representation
     */
    public String toStructureString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Dantzig-Wolfe Master Formulation\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append(String.format("Instance: n=%d, m=%d\n\n",
                instance.getNumItems(), instance.getNumKnapsacks()));

        sb.append("Pattern Pools:\n");
        sb.append(String.format("  P_0: %d patterns (total capacity = %d)\n",
                patternsP0.size(), instance.getTotalCapacity()));
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            sb.append(String.format("  P_%d: %d patterns (capacity = %d)\n",
                    i + 1, patternsPI.get(i).size(), instance.getKnapsack(i).capacity()));
        }
        sb.append(String.format("\nTotal patterns: %d\n", getTotalPatternCount()));
        sb.append(String.format("Upper bound: %.1f\n\n", upperBound));

        sb.append("Variables:\n");
        sb.append(String.format("  y_a: %d pattern variables\n", getTotalPatternCount()));
        sb.append(String.format("  s_j: %d dual cut variables\n", instance.getNumItems()));

        return sb.toString();
    }

    /**
     * Show patterns in each pool.
     *
     * @return Patterns table string
     */
    public String toPatternsString() {
        StringBuilder sb = new StringBuilder();

        // P_0 patterns
        sb.append(String.format("Pattern Pool P_0 (%d patterns):\n", patternsP0.size()));
        sb.append(patternListTable(patternsP0));
        sb.append("\n");

        // P_i patterns
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            List<Pattern> patterns = patternsPI.get(i);
            sb.append(String.format("Pattern Pool P_%d (%d patterns):\n", i + 1, patterns.size()));
            sb.append(patternListTable(patterns));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String patternListTable(List<Pattern> patterns) {
        StringBuilder sb = new StringBuilder();
        sb.append("┌────┬──────────────┬────────┬────────┬────────────┐\n");
        sb.append("│ ID │    Items     │ Weight │ Profit │ Efficiency │\n");
        sb.append("├────┼──────────────┼────────┼────────┼────────────┤\n");

        int count = 0;
        for (Pattern p : patterns) {
            if (count >= 10) {
                sb.append("│... │ ...          │    ... │    ... │        ... │\n");
                break;
            }
            String itemsStr = p.getItemIds().toString();
            if (itemsStr.length() > 12) {
                itemsStr = itemsStr.substring(0, 9) + "...";
            }
            sb.append(String.format("│%3d │ %-12s │ %6d │ %6.1f │     %6.2f │\n",
                    p.getId(), itemsStr, p.getTotalWeight(), p.getTotalProfit(), p.getEfficiency()));
            count++;
        }

        sb.append("└────┴──────────────┴────────┴────────┴────────────┘\n");
        return sb.toString();
    }

    /**
     * Visualize a DW solution.
     *
     * @param solution Solution to visualize
     * @return Visualization string
     */
    public String visualizeSolution(DWSolution solution) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Dantzig-Wolfe Solution ===\n\n");

        // Show objective value
        double objective = computeObjectiveValue(solution);
        sb.append(String.format("Objective value: %.2f\n", objective));
        sb.append(String.format("Feasible: %s\n", isFeasible(solution) ? "✓" : "✗"));
        sb.append(String.format("  - Item consistency: %s\n",
                checkItemConsistency(solution) ? "✓" : "✗"));
        sb.append(String.format("  - Pattern selection: %s\n",
                checkPatternSelection(solution) ? "✓" : "✗"));
        sb.append(String.format("  - Upper bound: %s\n\n",
                checkUpperBound(solution) ? "✓" : "✗"));

        // Show detailed solution
        sb.append(solution.toDetailedString(this));
        sb.append("\n");
        sb.append(solution.toPatternValuesString());

        return sb.toString();
    }
}
