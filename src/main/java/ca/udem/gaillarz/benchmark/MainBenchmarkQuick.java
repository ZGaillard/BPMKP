package ca.udem.gaillarz.benchmark;

/**
 * Quick entry point to run benchmarks on all datasets with default settings.
 */
public class MainBenchmarkQuick {
    static void main() throws Exception {
        BenchmarkConfig config = new BenchmarkConfig()
                .setOutputDirectory("benchmark_results")
                .setVerbose(false);

        BenchmarkRunner runner = new BenchmarkRunner(config);
        runner.runAll();
    }
}
