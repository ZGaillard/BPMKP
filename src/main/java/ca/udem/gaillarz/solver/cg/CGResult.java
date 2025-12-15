package ca.udem.gaillarz.solver.cg;

import ca.udem.gaillarz.formulation.DWSolution;
import ca.udem.gaillarz.formulation.DualValues;
import ca.udem.gaillarz.formulation.L2Solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result wrapper for column generation runs.
 */
public record CGResult(CGStatus status, DWSolution dwSolution, L2Solution l2Solution, DualValues dualValues,
                       double objectiveValue, int iterations, int patternsAdded, List<Double> objectiveHistory,
                       long solveTimeMs, long lpBuildTimeMs, long lpSolveTimeMs, long pricingTimeMs) {
    public CGResult(CGStatus status,
                    DWSolution dwSolution,
                    L2Solution l2Solution,
                    DualValues dualValues,
                    double objectiveValue,
                    int iterations,
                    int patternsAdded,
                    List<Double> objectiveHistory,
                    long solveTimeMs,
                    long lpBuildTimeMs,
                    long lpSolveTimeMs,
                    long pricingTimeMs) {
        this.status = status;
        this.dwSolution = dwSolution;
        this.l2Solution = l2Solution;
        this.dualValues = dualValues;
        this.objectiveValue = objectiveValue;
        this.iterations = iterations;
        this.patternsAdded = patternsAdded;
        this.objectiveHistory = new ArrayList<>(objectiveHistory);
        this.solveTimeMs = solveTimeMs;
        this.lpBuildTimeMs = lpBuildTimeMs;
        this.lpSolveTimeMs = lpSolveTimeMs;
        this.pricingTimeMs = pricingTimeMs;
    }

    public boolean isOptimal() {
        return status == CGStatus.OPTIMAL;
    }

    @Override
    public List<Double> objectiveHistory() {
        return Collections.unmodifiableList(objectiveHistory);
    }

    @Override
    public String toString() {
        return String.format("CGResult(status=%s, obj=%.3f, iter=%d, added=%d, time=%.2fs, build=%.2fs, lp=%.2fs, pricing=%.2fs)",
                status, objectiveValue, iterations, patternsAdded, solveTimeMs / 1000.0,
                lpBuildTimeMs / 1000.0, lpSolveTimeMs / 1000.0, pricingTimeMs / 1000.0);
    }
}
