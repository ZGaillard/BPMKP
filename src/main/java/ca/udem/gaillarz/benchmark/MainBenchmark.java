package ca.udem.gaillarz.benchmark;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Quick entry point to run benchmarks on all datasets with default settings.
 */
public class MainBenchmark {
    static void main() throws Exception {
        BenchmarkConfig config = new BenchmarkConfig()
                .setVerbose(false)
                .setTimeLimitSeconds(600)
                .setGapTolerance(0)
                .setMaxNodes(1000)
                .setMaxInstancesPerSet(999)
	    .setSkipSets(java.util.List.of("")); // e.g., List.of("FK_3", "FK_4")

        config.setOutputDirectory(buildOutputDirectory(config));

        BenchmarkRunner runner = new BenchmarkRunner(config);
        runner.runAll();
    }

    private static String buildOutputDirectory(BenchmarkConfig config) {
        DecimalFormat df = new DecimalFormat("#.########", DecimalFormatSymbols.getInstance(Locale.US));
        String gap = df.format(config.getGapTolerance());
        return String.format("benchmark_results/time-limit%d_gap%s_max-nodes%d_max-instances%d",
                config.getTimeLimitSeconds(),
                gap,
                config.getMaxNodes(),
                config.getMaxInstancesPerSet());
    }
}
