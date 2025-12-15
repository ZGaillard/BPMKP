package ca.udem.gaillarz.solver.cg;

/**
 * Tunable parameters for column generation.
 */
public class CGParameters {
    private int maxIterations = 1000;
    private double tolerance = 1e-6;
    private long timeLimitMs = Long.MAX_VALUE;
    private boolean verbose = false;

    public int getMaxIterations() {
        return maxIterations;
    }

    public CGParameters setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    public double getTolerance() {
        return tolerance;
    }

    public CGParameters setTolerance(double tolerance) {
        this.tolerance = tolerance;
        return this;
    }

    public long getTimeLimitMs() {
        return timeLimitMs;
    }

    public CGParameters setTimeLimitMs(long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
        return this;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public CGParameters setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
}
