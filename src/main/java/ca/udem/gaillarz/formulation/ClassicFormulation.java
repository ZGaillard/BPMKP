package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.Knapsack;
import ca.udem.gaillarz.model.MKPInstance;

import java.util.Set;

/**
 * Classic MKP formulation (equations 1-4).
 *
 * <pre>
 * (MKP) max Σ_{i=1}^{m} Σ_{j=1}^{n} p_j * x_ij                    (1)
 *
 * s.t.  Σ_{j=1}^{n} w_j * x_ij ≤ c_i        for i = 1,...,m      (2)
 *
 *       Σ_{i=1}^{m} x_ij ≤ 1                 for j = 1,...,n      (3)
 *
 *       x_ij ∈ {0,1}                         for all i,j          (4)
 * </pre>
 */
public record ClassicFormulation(MKPInstance instance) {

    /**
     * Creates a classic formulation for the given MKP instance.
     *
     * @param instance MKP instance
     */
    public ClassicFormulation {
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
    }

    /**
     * @return The underlying MKP instance
     */
    @Override
    public MKPInstance instance() {
        return instance;
    }

    /**
     * Compute objective value: Σ_i Σ_j p_j * x_ij (equation 1)
     *
     * @param solution Solution to evaluate
     * @return Objective value
     */
    public double computeObjectiveValue(ClassicSolution solution) {
        validateSolutionDimensions(solution);

        double objective = 0.0;
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            for (int j = 0; j < instance.getNumItems(); j++) {
                if (solution.isItemInKnapsack(i, j)) {
                    objective += instance.getItem(j).profit();
                }
            }
        }
        return objective;
    }

    /**
     * Check if solution satisfies all constraints.
     *
     * @param solution Solution to check
     * @return true if feasible
     */
    public boolean isFeasible(ClassicSolution solution) {
        return checkCapacityConstraints(solution) && checkAssignmentConstraints(solution);
    }

    /**
     * Check capacity constraints (equation 2): Σ_j w_j * x_ij ≤ c_i
     *
     * @param solution Solution to check
     * @return true if all capacity constraints are satisfied
     */
    public boolean checkCapacityConstraints(ClassicSolution solution) {
        validateSolutionDimensions(solution);

        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            int weight = getKnapsackWeight(solution, i);
            if (weight > instance.getKnapsack(i).capacity()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check assignment constraints (equation 3): Σ_i x_ij ≤ 1
     *
     * @param solution Solution to check
     * @return true if all assignment constraints are satisfied
     */
    public boolean checkAssignmentConstraints(ClassicSolution solution) {
        validateSolutionDimensions(solution);

        for (int j = 0; j < instance.getNumItems(); j++) {
            int count = 0;
            for (int i = 0; i < instance.getNumKnapsacks(); i++) {
                if (solution.isItemInKnapsack(i, j)) {
                    count++;
                }
            }
            if (count > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get total weight in knapsack i.
     *
     * @param solution   Solution to evaluate
     * @param knapsackId Knapsack index
     * @return Total weight in knapsack
     */
    public int getKnapsackWeight(ClassicSolution solution, int knapsackId) {
        int weight = 0;
        for (int j = 0; j < instance.getNumItems(); j++) {
            if (solution.isItemInKnapsack(knapsackId, j)) {
                weight += instance.getItem(j).weight();
            }
        }
        return weight;
    }

    /**
     * Get items assigned to knapsack i.
     *
     * @param solution   Solution to evaluate
     * @param knapsackId Knapsack index
     * @return Set of item indices in knapsack
     */
    public Set<Integer> getItemsInKnapsack(ClassicSolution solution, int knapsackId) {
        return solution.getItemsInKnapsack(knapsackId);
    }

    /**
     * Convert to L2 relaxed formulation.
     *
     * @return L2RelaxedFormulation for this instance
     */
    public L2RelaxedFormulation toL2Formulation() {
        return new L2RelaxedFormulation(instance);
    }

    private void validateSolutionDimensions(ClassicSolution solution) {
        if (solution.getNumKnapsacks() != instance.getNumKnapsacks() ||
                solution.getNumItems() != instance.getNumItems()) {
            throw new FormulationException(
                    String.format("Solution dimensions (%d knapsacks, %d items) don't match instance (%d, %d)",
                            solution.getNumKnapsacks(), solution.getNumItems(),
                            instance.getNumKnapsacks(), instance.getNumItems()));
        }
    }

    @Override
    public String toString() {
        return String.format("ClassicMKP(n=%d, m=%d)", instance.getNumItems(), instance.getNumKnapsacks());
    }

    /**
     * Display the mathematical formulation with actual values.
     *
     * @return Mathematical formulation as string
     */
    public String toMathematicalString() {
        StringBuilder sb = new StringBuilder();
        int n = instance.getNumItems();
        int m = instance.getNumKnapsacks();

        sb.append("Classic MKP Formulation\n");
        sb.append("═══════════════════════════════════════\n\n");

        // Objective function
        sb.append("max  ");
        boolean first = true;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < Math.min(n, 5); j++) {
                Item item = instance.getItem(j);
                if (!first) sb.append(" + ");
                sb.append(String.format("%d*x[%d][%d]", item.profit(), i, j));
                first = false;
            }
        }
        if (n > 5) sb.append(" + ...");
        sb.append("\n\n");

        // Capacity constraints
        sb.append("s.t. Capacity constraints:\n");
        for (int i = 0; i < Math.min(m, 3); i++) {
            Knapsack ks = instance.getKnapsack(i);
            sb.append("     ");
            first = true;
            for (int j = 0; j < Math.min(n, 5); j++) {
                Item item = instance.getItem(j);
                if (!first) sb.append(" + ");
                sb.append(String.format("%d*x[%d][%d]", item.weight(), i, j));
                first = false;
            }
            if (n > 5) sb.append(" + ...");
            sb.append(String.format(" ≤ %d\n", ks.capacity()));
        }
        if (m > 3) sb.append("     ...\n");
        sb.append("\n");

        // Assignment constraints
        sb.append("     Assignment constraints:\n");
        for (int j = 0; j < Math.min(n, 3); j++) {
            sb.append("     ");
            first = true;
            for (int i = 0; i < m; i++) {
                if (!first) sb.append(" + ");
                sb.append(String.format("x[%d][%d]", i, j));
                first = false;
            }
            sb.append(" ≤ 1\n");
        }
        if (n > 3) sb.append("     ...\n");
        sb.append("\n");

        sb.append("     x[i][j] ∈ {0,1}\n");

        return sb.toString();
    }

    /**
     * Visualize a solution in the context of this formulation.
     *
     * @param solution Solution to visualize
     * @return Visualization string
     */
    public String visualizeSolution(ClassicSolution solution) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Classic Formulation Solution ===\n\n");

        // Show objective value
        double objective = computeObjectiveValue(solution);
        sb.append(String.format("Objective value: %.2f\n", objective));
        sb.append(String.format("Feasible: %s\n\n", isFeasible(solution) ? "✓" : "✗"));

        // Show detailed solution
        sb.append(solution.toDetailedString(instance));

        return sb.toString();
    }
}

