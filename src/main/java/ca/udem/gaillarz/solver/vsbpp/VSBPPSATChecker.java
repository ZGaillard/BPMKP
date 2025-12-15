package ca.udem.gaillarz.solver.vsbpp;

import ca.udem.gaillarz.formulation.L2Solution;
import ca.udem.gaillarz.model.MKPInstance;

import java.util.HashSet;
import java.util.Set;

/**
 * Interface for VSBPP feasibility checking (Phase 4).
 */
public interface VSBPPSATChecker {

    /**
     * Check feasibility of assigning selected items to knapsacks.
     *
     * @param instance    MKP instance
     * @param selected    set of item IDs with t_j = 1
     * @param timeLimitMs time limit in milliseconds (<=0 for no limit)
     * @return result with status and mapping if feasible
     */
    VSBPPSATResult checkFeasibility(MKPInstance instance, Set<Integer> selected, long timeLimitMs);

    /**
     * Convenience overload: derive selected items from an L2 solution (t_j = 1 within tolerance).
     */
    default VSBPPSATResult checkFeasibility(MKPInstance instance, L2Solution l2Solution, long timeLimitMs) {
        Set<Integer> selected = new HashSet<>();
        double tol = 1e-5;
        for (int j = 0; j < instance.getNumItems(); j++) {
            double tj = l2Solution.getItemSelection(j);
            if (Math.abs(tj - 1.0) < tol) {
                selected.add(j);
            }
        }
        return checkFeasibility(instance, selected, timeLimitMs);
    }
}
