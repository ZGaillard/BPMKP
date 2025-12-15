package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.Knapsack;
import ca.udem.gaillarz.model.MKPInstance;

/**
 * L2 Relaxed formulation (equations 13-18).
 *
 * <pre>
 * max Σ_{j=1}^{n} p_j * t_j                                       (13)
 *
 * s.t.  Σ_{j=1}^{n} w_j * x_ij ≤ c_i        for i = 1,...,m      (14)
 *
 *       t_j ≤ Σ_{i=1}^{m} x_ij               for j = 1,...,n      (15)
 *
 *       Σ_{j=1}^{n} w_j * t_j ≤ Σ_{i=1}^{m} c_i                  (16)
 *
 *       t_j ∈ {0,1}                          for j = 1,...,n      (17)
 *
 *       x_ij ∈ {0,1}                         for all i,j          (18)
 * </pre>
 */
public class L2RelaxedFormulation {
    private static final double TOLERANCE = 1e-5;

    private final MKPInstance instance;
    private final int totalCapacity;  // Σ_i c_i (cached)

    /**
     * Creates an L2 relaxed formulation for the given MKP instance.
     *
     * @param instance MKP instance
     */
    public L2RelaxedFormulation(MKPInstance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        this.instance = instance;
        this.totalCapacity = instance.getTotalCapacity();
    }

    /**
     * @return The underlying MKP instance
     */
    public MKPInstance getInstance() {
        return instance;
    }

    /**
     * @return Total capacity of all knapsacks
     */
    public int getTotalCapacity() {
        return totalCapacity;
    }

    /**
     * Compute objective value: Σ_j p_j * t_j (equation 13)
     *
     * @param solution Solution to evaluate
     * @return Objective value
     */
    public double computeObjectiveValue(L2Solution solution) {
        validateSolutionDimensions(solution);

        double objective = 0.0;
        for (int j = 0; j < instance.getNumItems(); j++) {
            objective += instance.getItem(j).profit() * solution.getItemSelection(j);
        }
        return objective;
    }

    /**
     * Compute Lagrangian objective L2(μ) (equation 19).
     * Objective = Σ_j (p_j - μ_j)*t_j + Σ_j μ_j*(Σ_i x_ij)
     *
     * @param solution Solution to evaluate
     * @param mu       Lagrangian multipliers (length n)
     * @return Lagrangian objective value
     */
    public double computeLagrangianObjective(L2Solution solution, double[] mu) {
        validateSolutionDimensions(solution);
        if (mu.length != instance.getNumItems()) {
            throw new IllegalArgumentException("mu length must equal number of items");
        }

        double objective = 0.0;

        // Σ_j (p_j - μ_j) * t_j
        for (int j = 0; j < instance.getNumItems(); j++) {
            objective += (instance.getItem(j).profit() - mu[j]) * solution.getItemSelection(j);
        }

        // Σ_j μ_j * (Σ_i x_ij)
        for (int j = 0; j < instance.getNumItems(); j++) {
            double sumX = 0.0;
            for (int i = 0; i < instance.getNumKnapsacks(); i++) {
                sumX += solution.getItemAssignment(i, j);
            }
            objective += mu[j] * sumX;
        }

        return objective;
    }

    /**
     * Check if solution satisfies all constraints (equations 14-18).
     *
     * @param solution Solution to check
     * @return true if feasible
     */
    public boolean isFeasible(L2Solution solution) {
        return checkKnapsackCapacities(solution) &&
                checkLinkingConstraints(solution) &&
                checkAggregatedCapacity(solution);
    }

    /**
     * Check knapsack capacity constraints (equation 14): Σ_j w_j * x_ij ≤ c_i
     *
     * @param solution Solution to check
     * @return true if all capacity constraints are satisfied
     */
    public boolean checkKnapsackCapacities(L2Solution solution) {
        validateSolutionDimensions(solution);

        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            double weight = 0.0;
            for (int j = 0; j < instance.getNumItems(); j++) {
                weight += instance.getItem(j).weight() * solution.getItemAssignment(i, j);
            }
            if (weight > instance.getKnapsack(i).capacity() + TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check linking constraints (equation 15): t_j ≤ Σ_i x_ij
     *
     * @param solution Solution to check
     * @return true if all linking constraints are satisfied
     */
    public boolean checkLinkingConstraints(L2Solution solution) {
        validateSolutionDimensions(solution);

        for (int j = 0; j < instance.getNumItems(); j++) {
            double sumX = 0.0;
            for (int i = 0; i < instance.getNumKnapsacks(); i++) {
                sumX += solution.getItemAssignment(i, j);
            }
            if (solution.getItemSelection(j) > sumX + TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check aggregated capacity constraint (equation 16): Σ_j w_j*t_j ≤ Σ_i c_i
     *
     * @param solution Solution to check
     * @return true if aggregated capacity constraint is satisfied
     */
    public boolean checkAggregatedCapacity(L2Solution solution) {
        validateSolutionDimensions(solution);

        double totalWeight = 0.0;
        for (int j = 0; j < instance.getNumItems(); j++) {
            totalWeight += instance.getItem(j).weight() * solution.getItemSelection(j);
        }
        return totalWeight <= totalCapacity + TOLERANCE;
    }

    /**
     * Convert to Dantzig-Wolfe master formulation.
     *
     * @return DantzigWolfeMaster for this formulation
     */
    public DantzigWolfeMaster toDantzigWolfeFormulation() {
        return new DantzigWolfeMaster(this);
    }

    /**
     * Convert back to classic formulation.
     *
     * @return ClassicFormulation for this instance
     */
    public ClassicFormulation toClassicFormulation() {
        return new ClassicFormulation(instance);
    }

    private void validateSolutionDimensions(L2Solution solution) {
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
        return String.format("L2RelaxedMKP(n=%d, m=%d, total_cap=%d)",
                instance.getNumItems(), instance.getNumKnapsacks(), totalCapacity);
    }

    /**
     * Display the mathematical formulation.
     *
     * @return Mathematical formulation as string
     */
    public String toMathematicalString() {
        StringBuilder sb = new StringBuilder();
        int n = instance.getNumItems();
        int m = instance.getNumKnapsacks();

        sb.append("L2 Relaxed Formulation\n");
        sb.append("═══════════════════════════════════════\n\n");

        // Objective function
        sb.append("max  ");
        boolean first = true;
        for (int j = 0; j < Math.min(n, 5); j++) {
            Item item = instance.getItem(j);
            if (!first) sb.append(" + ");
            sb.append(String.format("%d*t[%d]", item.profit(), j));
            first = false;
        }
        if (n > 5) sb.append(" + ...");
        sb.append("\n\n");

        // Knapsack capacity constraints
        sb.append("s.t. Knapsack capacity constraints:\n");
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

        // Linking constraints
        sb.append("     Linking constraints:\n");
        for (int j = 0; j < Math.min(n, 3); j++) {
            sb.append(String.format("     t[%d] ≤ ", j));
            first = true;
            for (int i = 0; i < m; i++) {
                if (!first) sb.append(" + ");
                sb.append(String.format("x[%d][%d]", i, j));
                first = false;
            }
            sb.append("\n");
        }
        if (n > 3) sb.append("     ...\n");
        sb.append("\n");

        // Aggregated capacity
        sb.append("     Aggregated capacity:\n");
        sb.append("     ");
        first = true;
        for (int j = 0; j < Math.min(n, 5); j++) {
            Item item = instance.getItem(j);
            if (!first) sb.append(" + ");
            sb.append(String.format("%d*t[%d]", item.weight(), j));
            first = false;
        }
        if (n > 5) sb.append(" + ...");
        sb.append(String.format(" ≤ %d\n\n", totalCapacity));

        sb.append("     t[j] ∈ {0,1}, x[i][j] ∈ {0,1}\n");

        return sb.toString();
    }

    /**
     * Display Lagrangian relaxation for given μ.
     *
     * @param mu Lagrangian multipliers
     * @return Lagrangian formulation as string
     */
    public String toLagrangianString(double[] mu) {
        StringBuilder sb = new StringBuilder();
        int n = instance.getNumItems();

        sb.append("L2(μ) Lagrangian Relaxation\n");
        sb.append("═══════════════════════════════════════\n");

        // Show μ values
        sb.append("μ = [");
        for (int j = 0; j < Math.min(n, 5); j++) {
            if (j > 0) sb.append(", ");
            sb.append(String.format("%.2f", mu[j]));
        }
        if (n > 5) sb.append(", ...");
        sb.append("]\n\n");

        // Objective
        sb.append("max  ");
        boolean first = true;
        for (int j = 0; j < Math.min(n, 3); j++) {
            Item item = instance.getItem(j);
            if (!first) sb.append(" + ");
            double coeff = item.profit() - mu[j];
            sb.append(String.format("%.1f*t[%d]", coeff, j));
            first = false;
        }
        sb.append(" + ...\n");
        sb.append("     + ");
        first = true;
        for (int j = 0; j < Math.min(n, 2); j++) {
            if (!first) sb.append(" + ");
            sb.append(String.format("%.1f*(Σ_i x[i][%d])", mu[j], j));
            first = false;
        }
        sb.append(" + ...\n\n");

        sb.append("s.t. [constraints 14, 16, 17, 18]\n\n");
        sb.append("Note: Constraint (15) has been dualized\n");

        return sb.toString();
    }

    /**
     * Visualize a solution.
     *
     * @param solution Solution to visualize
     * @return Visualization string
     */
    public String visualizeSolution(L2Solution solution) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== L2 Formulation Solution ===\n\n");

        // Show objective value
        double objective = computeObjectiveValue(solution);
        sb.append(String.format("Objective value: %.2f\n", objective));
        sb.append(String.format("Feasible: %s\n", isFeasible(solution) ? "✓" : "✗"));
        sb.append(String.format("  - Knapsack capacities: %s\n",
                checkKnapsackCapacities(solution) ? "✓" : "✗"));
        sb.append(String.format("  - Linking constraints: %s\n",
                checkLinkingConstraints(solution) ? "✓" : "✗"));
        sb.append(String.format("  - Aggregated capacity: %s\n\n",
                checkAggregatedCapacity(solution) ? "✓" : "✗"));

        // Show detailed solution
        sb.append(solution.toDetailedString());

        return sb.toString();
    }
}

