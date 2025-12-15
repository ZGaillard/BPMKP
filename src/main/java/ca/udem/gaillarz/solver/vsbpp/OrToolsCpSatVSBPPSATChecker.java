package ca.udem.gaillarz.solver.vsbpp;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.MKPInstance;
import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;

import java.util.*;

/**
 * Exact feasibility checker using OR-Tools CP-SAT.
 */
public class OrToolsCpSatVSBPPSATChecker implements VSBPPSATChecker {

    static {
        try {
            Loader.loadNativeLibraries();
        } catch (UnsatisfiedLinkError | SecurityException ignored) {
            // OR-Tools may have been loaded elsewhere; proceed best-effort.
        }
    }

    @Override
    public VSBPPSATResult checkFeasibility(MKPInstance instance, Set<Integer> selected, long timeLimitMs) {
        long start = System.currentTimeMillis();
        Set<Integer> selectedCopy = selected == null ? Set.of() : new HashSet<>(selected);

        if (selectedCopy.isEmpty()) {
            int[] mapping = new int[instance.getNumItems()];
            Arrays.fill(mapping, -1);
            return new VSBPPSATResult(VSBPPSATStatus.FEASIBLE, selectedCopy,
                    mapping, System.currentTimeMillis() - start, null);
        }

        try {
            int m = instance.getNumKnapsacks();
            List<Integer> items = new ArrayList<>(selectedCopy);

            CpModel model = new CpModel();
            BoolVar[][] x = new BoolVar[m][items.size()];

            // Variables
            for (int i = 0; i < m; i++) {
                for (int k = 0; k < items.size(); k++) {
                    x[i][k] = model.newBoolVar("x_" + i + "_" + items.get(k));
                }
            }

            // Assignment: each selected item exactly once
            for (int k = 0; k < items.size(); k++) {
                BoolVar[] vars = new BoolVar[m];
                for (int i = 0; i < m; i++) vars[i] = x[i][k];
                model.addEquality(linearSum(vars), 1);
            }

            // Capacity constraints
            for (int i = 0; i < m; i++) {
                int capacity = instance.getKnapsack(i).capacity();
                com.google.ortools.sat.LinearExprBuilder builder = com.google.ortools.sat.LinearExpr.newBuilder();
                for (int k = 0; k < items.size(); k++) {
                    Item item = instance.getItem(items.get(k));
                    builder.addTerm(x[i][k], item.weight());
                }
                model.addLinearConstraint(builder, Long.MIN_VALUE, capacity);
            }

            CpSolver solver = new CpSolver();
            if (timeLimitMs > 0) {
                solver.getParameters().setMaxTimeInSeconds(timeLimitMs / 1000.0);
            }

            CpSolverStatus status = solver.solve(model);
            double elapsed = System.currentTimeMillis() - start;

            if (status == CpSolverStatus.FEASIBLE || status == CpSolverStatus.OPTIMAL) {
                int[] mapping = new int[instance.getNumItems()];
                Arrays.fill(mapping, -1);

                for (int k = 0; k < items.size(); k++) {
                    int itemId = items.get(k);
                    for (int i = 0; i < m; i++) {
                        if (solver.booleanValue(x[i][k])) {
                            mapping[itemId] = i;
                            break;
                        }
                    }
                }

                return new VSBPPSATResult(VSBPPSATStatus.FEASIBLE, selectedCopy, mapping, elapsed, null);
            }

            if (status == CpSolverStatus.INFEASIBLE) {
                return new VSBPPSATResult(VSBPPSATStatus.INFEASIBLE, selectedCopy, null,
                        elapsed, "Infeasible selection");
            }

            if (status == CpSolverStatus.UNKNOWN || status == CpSolverStatus.MODEL_INVALID) {
                VSBPPSATStatus s = status == CpSolverStatus.UNKNOWN ? VSBPPSATStatus.TIME_LIMIT : VSBPPSATStatus.ERROR;
                return new VSBPPSATResult(s, selectedCopy, null, elapsed, status.toString());
            }

            return new VSBPPSATResult(VSBPPSATStatus.ERROR, selectedCopy, null,
                    elapsed, "Unexpected status: " + status);
        } catch (Exception e) {
            return new VSBPPSATResult(VSBPPSATStatus.ERROR, selectedCopy, null,
                    System.currentTimeMillis() - start, e.getMessage());
        }
    }

    /**
     * Helper to sum BoolVars (for assignment constraints).
     */
    private com.google.ortools.sat.LinearExpr linearSum(BoolVar[] vars) {
        return com.google.ortools.sat.LinearExpr.sum(vars);
    }
}
