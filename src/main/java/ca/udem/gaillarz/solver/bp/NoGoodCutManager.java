package ca.udem.gaillarz.solver.bp;

import ca.udem.gaillarz.formulation.Pattern;
import ca.udem.gaillarz.formulation.PatternVariable;
import ca.udem.gaillarz.solver.lp.Constraint;
import ca.udem.gaillarz.solver.lp.ConstraintSense;
import ca.udem.gaillarz.solver.lp.LinearProgram;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Maintains no-good cuts forbidding specific item-selection sets S.
 * Each cut: sum_{a in P0} (sum_{j in S} a_j) * y_a <= |S| - 1
 */
public class NoGoodCutManager {
    private final List<Set<Integer>> infeasibleSets = new ArrayList<>();

    public void addInfeasibleSet(Set<Integer> s) {
        infeasibleSets.add(Set.copyOf(s));
    }

    public int getNumCuts() {
        return infeasibleSets.size();
    }

    /**
     * Apply cuts to the given LP using current P0 patterns.
     * Assumes y variables already exist for all P0 patterns.
     */
    public void addCutsToLP(LinearProgram lp, List<Pattern> patternsP0) {
        for (int idx = 0; idx < infeasibleSets.size(); idx++) {
            Set<Integer> s = infeasibleSets.get(idx);
            Constraint cut = lp.addConstraint("nogood_" + idx, ConstraintSense.LE, s.size() - 1);
            for (Pattern p : patternsP0) {
                int overlap = 0;
                for (int item : s) {
                    if (p.containsItem(item)) overlap++;
                }
                if (overlap > 0) {
                    PatternVariable pv = PatternVariable.forP0(p);
                    cut.addTerm(lp.getVariable("y_P0_" + pv.getPattern().getId()), overlap);
                }
            }
        }
    }
}
