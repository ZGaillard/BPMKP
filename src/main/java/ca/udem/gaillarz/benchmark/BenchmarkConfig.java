package ca.udem.gaillarz.benchmark;

import java.util.Arrays;
import java.util.List;

/**
 * Minimal configuration for benchmarking runs.
 */
public class BenchmarkConfig {
    private double gapTolerance = 0.01;       // 1%
    private long timeLimitSeconds = 600;      // 10 minutes per instance
    private int maxNodes = 10000;
    private boolean verbose = false;
    private int maxInstancesPerSet = Integer.MAX_VALUE;
    private final List<String> instanceFilter = List.of(); // empty = all
    private final List<String> instanceSets = Arrays.asList("SMALL", "FK_1", "FK_2", "FK_3", "FK_4");
    private String outputDirectory = "benchmark_results";

    public double getGapTolerance() {
        return gapTolerance;
    }

    public BenchmarkConfig setGapTolerance(double gapTolerance) {
        this.gapTolerance = gapTolerance;
        return this;
    }

    public long getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public BenchmarkConfig setTimeLimitSeconds(long timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
        return this;
    }

    public int getMaxNodes() {
        return maxNodes;
    }

    public BenchmarkConfig setMaxNodes(int maxNodes) {
        this.maxNodes = maxNodes;
        return this;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public BenchmarkConfig setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public int getMaxInstancesPerSet() {
        return maxInstancesPerSet;
    }

    public BenchmarkConfig setMaxInstancesPerSet(int maxInstancesPerSet) {
        this.maxInstancesPerSet = maxInstancesPerSet;
        return this;
    }

    public List<String> getInstanceFilter() {
        return instanceFilter;
    }

    public List<String> getInstanceSets() {
        return instanceSets;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public BenchmarkConfig setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    @Override
    public String toString() {
        return String.format("Config[gaptol=%.2f%%, time=%ds, nodes=%d, verbose=%s]",
                gapTolerance * 100, timeLimitSeconds, maxNodes, verbose);
    }
}
