package ca.udem.gaillarz.benchmark;

import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.bp.BPResult;
import ca.udem.gaillarz.solver.bp.BPStatus;
import ca.udem.gaillarz.formulation.ClassicSolution;

/**
 * Result wrapper for a single benchmarked instance.
 */
public class BenchmarkResult {
    private final String instanceName;
    private final String setName;
    private final int numItems;
    private final int numKnapsacks;
    private final BPStatus status;
    private final double objective;
    private final double bound;
    private final double gap;
    private final int nodesProcessed;
    private final double timeSeconds;
    private final boolean hasSolution;
    private final boolean optimal;
    private final String error;
    private final ClassicSolution solution;

    public BenchmarkResult(String instanceName, String setName, MKPInstance instance, BPResult result) {
        this.instanceName = instanceName;
        this.setName = setName;
        this.numItems = instance.getNumItems();
        this.numKnapsacks = instance.getNumKnapsacks();
        this.status = result.status();
        this.objective = result.objectiveValue();
        this.bound = result.bestBound();
        this.gap = result.gap();
        this.nodesProcessed = result.nodesProcessed();
        this.timeSeconds = result.solveTimeMs() / 1000.0;
        this.hasSolution = result.solution() != null;
        this.optimal = status == BPStatus.OPTIMAL || gap <= 1e-4;
        this.error = null;
        this.solution = result.solution();
    }

    private BenchmarkResult(String instanceName, String setName, String error) {
        this.instanceName = instanceName;
        this.setName = setName;
        this.numItems = 0;
        this.numKnapsacks = 0;
        this.status = BPStatus.ERROR;
        this.objective = 0.0;
        this.bound = 0.0;
        this.gap = Double.POSITIVE_INFINITY;
        this.nodesProcessed = 0;
        this.timeSeconds = 0.0;
        this.hasSolution = false;
        this.optimal = false;
        this.error = error;
        this.solution = null;
    }

    public static BenchmarkResult error(String instanceName, String setName, String message) {
        return new BenchmarkResult(instanceName, setName, message);
    }

    public String shortLine() {
        if (isError()) {
            return String.format("[ERROR] %s (%s): %s", instanceName, setName, error);
        }
        return String.format("[%s] obj=%.0f gap=%.2f%% time=%.2fs nodes=%d :: %s",
                status, objective, gap * 100, timeSeconds, nodesProcessed, instanceName);
    }

    public String toCsvRow() {
        return String.format("%s,%s,%d,%d,%s,%.3f,%.3f,%.6f,%d,%.3f,%s,%s",
                instanceName, setName, numItems, numKnapsacks, status,
                objective, bound, gap, nodesProcessed, timeSeconds,
                optimal, error == null ? "" : error.replace(",", ";"));
    }

    public String instanceName() {
        return instanceName;
    }

    public String setName() {
        return setName;
    }

    public int numItems() {
        return numItems;
    }

    public int numKnapsacks() {
        return numKnapsacks;
    }

    public BPStatus status() {
        return status;
    }

    public double objective() {
        return objective;
    }

    public double bound() {
        return bound;
    }

    public double gap() {
        return gap;
    }

    public int nodesProcessed() {
        return nodesProcessed;
    }

    public double timeSeconds() {
        return timeSeconds;
    }

    public boolean hasSolution() {
        return hasSolution;
    }

    public boolean isOptimal() {
        return optimal;
    }

    public boolean isError() {
        return error != null;
    }

    public String error() {
        return error;
    }

    public ClassicSolution solution() {
        return solution;
    }

    /**
     * Compact string of the assignment per knapsack (e.g., KS0:[1,3];KS1:[0]).
     */
    public String assignmentSummary() {
        if (solution == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numKnapsacks; i++) {
            if (i > 0) sb.append("; ");
            sb.append("KS").append(i).append(":").append(solution.getItemsInKnapsack(i));
        }
        return sb.toString();
    }
}
