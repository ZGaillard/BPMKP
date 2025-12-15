package ca.udem.gaillarz.benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes benchmark outputs.
 */
public class ResultsWriter {

    public void ensureOutputDir(String dir) {
        try {
            Files.createDirectories(Path.of(dir));
        } catch (IOException e) {
            System.err.println("Could not create output directory " + dir + ": " + e.getMessage());
        }
    }

    public void writeCsv(List<BenchmarkResult> results, String filepath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.println("instance,set,items,knapsacks,status,objective,bound,gap,nodes,time,optimal,error");
            for (BenchmarkResult r : results) {
                writer.println(r.toCsvRow());
            }
            System.out.println("[CSV] Wrote " + filepath);
        } catch (IOException e) {
            System.err.println("[CSV] Failed to write " + filepath + ": " + e.getMessage());
        }
    }

    public void writeJsonSummary(BenchmarkSummary summary, String filepath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.printf("{\"set\":\"%s\",\"total\":%d,\"solved\":%d,\"optimal\":%d,\"gap_limit\":%d,\"infeasible\":%d,\"errors\":%d,\"avg_gap\":%.6f,\"avg_time\":%.3f,\"total_time\":%.3f}%n",
                    summary.getSetName(), summary.getTotal(), summary.getSolved(), summary.getOptimal(),
                    summary.getGapLimited(), summary.getInfeasible(), summary.getErrors(),
                    summary.getAvgGap(), summary.getAvgTime(), summary.getTotalTime());
            System.out.println("[JSON] Wrote " + filepath);
        } catch (IOException e) {
            System.err.println("[JSON] Failed to write " + filepath + ": " + e.getMessage());
        }
    }
}
