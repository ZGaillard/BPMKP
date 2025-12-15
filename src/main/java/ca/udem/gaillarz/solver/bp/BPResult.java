package ca.udem.gaillarz.solver.bp;

import ca.udem.gaillarz.formulation.ClassicSolution;

/**
 * Result container for Branch-and-Price.
 *
 * @param objectiveValue best integer (global LB)
 * @param bestBound      best LP bound (global UB)
 */
public record BPResult(BPStatus status, ClassicSolution solution, double objectiveValue, double bestBound, double gap,
                       int nodesProcessed, int nodesPruned, int integralNodes, long solveTimeMs) {
    public boolean isOptimal() {
        return status == BPStatus.OPTIMAL;
    }

    public boolean hasSolution() {
        return solution != null;
    }

    @Override
    public String toString() {
        return String.format("BPResult(status=%s, obj=%.3f, bound=%.3f, gap=%.2f%%, nodes=%d, time=%.2fs)",
                status, objectiveValue, bestBound, gap * 100.0, nodesProcessed, solveTimeMs / 1000.0);
    }
}
