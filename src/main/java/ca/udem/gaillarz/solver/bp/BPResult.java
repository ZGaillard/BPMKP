package ca.udem.gaillarz.solver.bp;

import ca.udem.gaillarz.formulation.ClassicSolution;

/**
 * Result container for Branch-and-Price.
 *
 * @param objectiveValue best integer (global LB)
 * @param bestBound      best LP bound (global UB)
 */
public record BPResult(BPStatus status, ClassicSolution solution, double objectiveValue, double bestBound, double gap,
                       int nodesProcessed, int nodesPruned, int integralNodes, long solveTimeMs,
                       long cgTimeMs, long lpBuildTimeMs, long lpSolveTimeMs, long pricingTimeMs, long satTimeMs) {
    public boolean isOptimal() {
        return status == BPStatus.OPTIMAL;
    }

    public boolean hasSolution() {
        return solution != null;
    }

    @Override
    public String toString() {
        return String.format("BPResult(status=%s, obj=%.3f, bound=%.3f, gap=%.2f%%, nodes=%d, time=%.2fs, cg=%.2fs, lpBuild=%.2fs, lpSolve=%.2fs, pricing=%.2fs, sat=%.2fs)",
                status, objectiveValue, bestBound, gap * 100.0, nodesProcessed, solveTimeMs / 1000.0,
                cgTimeMs / 1000.0, lpBuildTimeMs / 1000.0, lpSolveTimeMs / 1000.0, pricingTimeMs / 1000.0, satTimeMs / 1000.0);
    }
}
