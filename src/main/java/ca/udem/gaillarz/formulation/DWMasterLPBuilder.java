package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.model.MKPInstance;
import solver.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds an LP for the DW master formulation and extracts solutions/duals.
 *
 * UPDATED: Works with PatternVariable for content-based pattern identification.
 */
public class DWMasterLPBuilder {

    private final DantzigWolfeMaster master;

    public DWMasterLPBuilder(DantzigWolfeMaster master) {
        this.master = master;
    }

    /**
     * Build LP relaxation of DW master.
     *
     * Variables:
     * - y_a for each pattern in each pool (continuous [0,1])
     * - s_j for each item (continuous [0,1])
     *
     * Constraints:
     * - Item consistency (29): Σ_{a∈P_0} a_j*y_a ≤ Σ_i Σ_{a∈P_i} a_j*y_a + s_j
     * - Pattern selection (30): Σ_{a∈P_i} y_a = 1 for each pool
     * - Upper bound (31): Σ_j p_j*(Σ_{a∈P_0} a_j*y_a) ≤ UB
     */
    public LinearProgram buildLP() {
        MKPInstance instance = master.getInstance();
        LinearProgram lp = new LinearProgram("DW_Master", true);

        // Maps to track variables by PatternVariable
        Map<PatternVariable, Variable> yVars = new HashMap<>();
        Map<Integer, Variable> sVars = new HashMap<>();

        // ========== Create Variables ==========

        // Pattern variables for P_0
        for (Pattern p : master.getPatternsP0()) {
            PatternVariable pv = PatternVariable.forP0(p);
            Variable y = lp.addVariable(varName(pv), 0.0, 1.0);
            yVars.put(pv, y);
        }

        // Pattern variables for each P_i
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            for (Pattern p : master.getPatternsPI(i)) {
                PatternVariable pv = PatternVariable.forPI(p, i);
                Variable y = lp.addVariable(varName(pv), 0.0, 1.0);
                yVars.put(pv, y);
            }
        }

        // Dual cut variables s_j
        for (int j = 0; j < instance.getNumItems(); j++) {
            Variable s = lp.addVariable("s_" + j, 0.0, 1.0);
            sVars.put(j, s);
        }

        // ========== Objective Function (equation 28) ==========
        // max Σ_{a∈P_0} profit(a)*y_a - Σ_j p_j*s_j

        // Pattern contribution from P_0
        for (Pattern p : master.getPatternsP0()) {
            PatternVariable pv = PatternVariable.forP0(p);
            lp.setObjectiveCoefficient(yVars.get(pv), p.getTotalProfit());
        }

        // Dual cut penalty
        for (int j = 0; j < instance.getNumItems(); j++) {
            lp.setObjectiveCoefficient(sVars.get(j), -instance.getItem(j).getProfit());
        }

        // ========== Constraints ==========

        // Item consistency constraints (equation 29) - DUALS: μ_j
        // Σ_{a∈P_0} a_j*y_a ≤ Σ_i Σ_{a∈P_i} a_j*y_a + s_j
        // Rewritten: Σ_{a∈P_0} a_j*y_a - Σ_i Σ_{a∈P_i} a_j*y_a - s_j ≤ 0
        for (int j = 0; j < instance.getNumItems(); j++) {
            Constraint con = lp.addConstraint("item_consistency_" + j, ConstraintSense.LE, 0.0);

            // LHS: Σ_{a∈P_0} a_j*y_a (positive coefficient)
            for (Pattern p : master.getPatternsP0()) {
                if (p.containsItem(j)) {
                    PatternVariable pv = PatternVariable.forP0(p);
                    con.addTerm(yVars.get(pv), 1.0);
                }
            }

            // RHS: Σ_i Σ_{a∈P_i} a_j*y_a (negative coefficient - moved to LHS)
            for (int i = 0; i < instance.getNumKnapsacks(); i++) {
                for (Pattern p : master.getPatternsPI(i)) {
                    if (p.containsItem(j)) {
                        PatternVariable pv = PatternVariable.forPI(p, i);
                        con.addTerm(yVars.get(pv), -1.0);
                    }
                }
            }

            // RHS: s_j (negative coefficient - moved to LHS)
            con.addTerm(sVars.get(j), -1.0);
        }

        // Pattern selection constraints (equation 30) - DUALS: π_i

        // P_0: Σ_{a∈P_0} y_a = 1
        Constraint conP0 = lp.addConstraint("pattern_selection_P0", ConstraintSense.EQ, 1.0);
        for (Pattern p : master.getPatternsP0()) {
            PatternVariable pv = PatternVariable.forP0(p);
            conP0.addTerm(yVars.get(pv), 1.0);
        }

        // Each P_i: Σ_{a∈P_i} y_a = 1
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            Constraint conPi = lp.addConstraint("pattern_selection_P" + (i + 1), ConstraintSense.EQ, 1.0);
            for (Pattern p : master.getPatternsPI(i)) {
                PatternVariable pv = PatternVariable.forPI(p, i);
                conPi.addTerm(yVars.get(pv), 1.0);
            }
        }

        // Upper bound constraint (equation 31) - DUAL: τ
        // Σ_j p_j*(Σ_{a∈P_0} a_j*y_a) ≤ UB
        if (Double.isFinite(master.getUpperBound())) {
            Constraint conUB = lp.addConstraint("upper_bound", ConstraintSense.LE, master.getUpperBound());
            for (int j = 0; j < instance.getNumItems(); j++) {
                double profit = instance.getItem(j).getProfit();
                for (Pattern p : master.getPatternsP0()) {
                    if (p.containsItem(j)) {
                        PatternVariable pv = PatternVariable.forP0(p);
                        conUB.addTerm(yVars.get(pv), profit);
                    }
                }
            }
        }

        return lp;
    }

    /**
     * Extract DW solution from LP solution.
     *
     * @param lpSol LP solution
     * @param lp    Linear program
     * @return DW solution with pattern values and dual cuts
     */
    public DWSolution extractDWSolution(LPSolution lpSol, LinearProgram lp) {
        MKPInstance instance = master.getInstance();
        Map<PatternVariable, Double> patternValues = new HashMap<>();

        // Extract pattern values from P_0
        for (Pattern p : master.getPatternsP0()) {
            PatternVariable pv = PatternVariable.forP0(p);
            Variable y = lp.getVariable(varName(pv));
            patternValues.put(pv, lpSol.getPrimalValue(y));
        }

        // Extract pattern values from each P_i
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            for (Pattern p : master.getPatternsPI(i)) {
                PatternVariable pv = PatternVariable.forPI(p, i);
                Variable y = lp.getVariable(varName(pv));
                patternValues.put(pv, lpSol.getPrimalValue(y));
            }
        }

        // Extract dual cut values
        Map<Integer, Double> dualCuts = new HashMap<>();
        for (int j = 0; j < instance.getNumItems(); j++) {
            Variable s = lp.getVariable("s_" + j);
            dualCuts.put(j, lpSol.getPrimalValue(s));
        }

        return new DWSolution(patternValues, dualCuts);
    }

    /**
     * Extract dual values (μ, π, τ) used for pricing.
     *
     * @param lpSol LP solution
     * @param lp    Linear program
     * @return Dual values for pricing subproblems
     */
    public DualValues extractDualValues(LPSolution lpSol, LinearProgram lp) {
        MKPInstance instance = master.getInstance();

        // μ_j: duals for item consistency constraints (equation 29)
        double[] mu = new double[instance.getNumItems()];
        for (int j = 0; j < instance.getNumItems(); j++) {
            Constraint con = lp.getConstraint("item_consistency_" + j);
            mu[j] = lpSol.getDualValue(con);
        }

        // π_i: duals for pattern selection constraints (equation 30)
        double[] pi = new double[instance.getNumKnapsacks() + 1];

        // π_0 for P_0
        Constraint conP0 = lp.getConstraint("pattern_selection_P0");
        pi[0] = lpSol.getDualValue(conP0);

        // π_i for each P_i
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            Constraint conPi = lp.getConstraint("pattern_selection_P" + (i + 1));
            pi[i + 1] = lpSol.getDualValue(conPi);
        }

        // τ: dual for upper bound constraint (equation 31)
        double tau = 0.0;
        if (Double.isFinite(master.getUpperBound())) {
            Constraint conUB = lp.getConstraint("upper_bound");
            tau = lpSol.getDualValue(conUB);
        }

        return new DualValues(mu, pi, tau);
    }

    /**
     * Generate variable name for a pattern variable.
     * Format: "y_P0_id" or "y_P1_id", "y_P2_id", etc.
     *
     * @param pv Pattern variable
     * @return Variable name
     */
    private String varName(PatternVariable pv) {
        if (pv.isP0()) {
            return "y_P0_" + pv.getPattern().getId();
        } else {
            return "y_P" + (pv.getKnapsackId() + 1) + "_" + pv.getPattern().getId();
        }
    }
}