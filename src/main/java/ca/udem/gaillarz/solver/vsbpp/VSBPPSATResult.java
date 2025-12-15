package ca.udem.gaillarz.solver.vsbpp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Result of the VSBPP-SAT feasibility checker.
 *
 * @param itemToBin length = n, -1 if not selected/unassigned
 */
public record VSBPPSATResult(VSBPPSATStatus status, Set<Integer> selectedItems, int[] itemToBin, double solveTimeMs,
                             String message) {
    public VSBPPSATResult(VSBPPSATStatus status,
                          Set<Integer> selectedItems,
                          int[] itemToBin,
                          double solveTimeMs,
                          String message) {
        this.status = status;
        this.selectedItems = selectedItems == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(selectedItems));
        this.itemToBin = itemToBin == null ? null : itemToBin.clone();
        this.solveTimeMs = solveTimeMs;
        this.message = message;
    }

    public boolean isFeasible() {
        return status == VSBPPSATStatus.FEASIBLE;
    }

    public boolean isInfeasible() {
        return status == VSBPPSATStatus.INFEASIBLE;
    }

    @Override
    public int[] itemToBin() {
        return itemToBin == null ? null : itemToBin.clone();
    }

    @Override
    public String toString() {
        return String.format("VSBPPSATResult(status=%s, time=%.2fms, selected=%d)",
                status, solveTimeMs, selectedItems.size());
    }
}
