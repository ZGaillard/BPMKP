package ca.udem.gaillarz.benchmark;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.bp.BPResult;
import ca.udem.gaillarz.solver.bp.BranchAndPrice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Orchestrates benchmarking across instance sets.
 */
public class BenchmarkRunner {
    private final BenchmarkConfig config;
    private final ResultsWriter writer = new ResultsWriter();

    public BenchmarkRunner(BenchmarkConfig config) {
        this.config = config;
    }

    public void runAll() throws IOException {
        writer.ensureOutputDir(config.getOutputDirectory());
        Map<String, BenchmarkSummary> summaries = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        for (String set : config.getInstanceSets()) {
            if (shouldSkipSet(set)) {
                System.out.println("Skipping set " + set + " (per config)");
                continue;
            }
            BenchmarkSummary summary = runSet(set);
            summaries.put(set, summary);
        }
        double totalMins = (System.currentTimeMillis() - start) / 60000.0;
        System.out.printf("%nAll sets completed in %.2f minutes%n", totalMins);
    }

    public BenchmarkSummary runSet(String setName) throws IOException {
        InstanceSet set = loadSet(setName);
        ProgressTracker tracker = new ProgressTracker();
        tracker.start(setName, set.size());
        List<BenchmarkResult> results = new ArrayList<>();

        int limit = Math.min(set.size(), config.getMaxInstancesPerSet());
        for (int i = 0; i < limit; i++) {
            String name = set.getInstanceName(i);
            tracker.step(name);
            BenchmarkResult res = solveInstance(set.getInstance(i), name, setName);
            results.add(res);
            System.out.println("    -> " + res.shortLine());
        }

        tracker.finish(setName);

        BenchmarkSummary summary = new BenchmarkSummary(setName, results);
        System.out.println(summary.format());

        String base = config.getOutputDirectory() + "/" + setName;
        writer.writeCsv(results, base + "_results.csv");
        writer.writeJsonSummary(summary, base + "_summary.json");
        writer.writeSolutions(results, base + "_solutions.txt");
        return summary;
    }

    private BenchmarkResult solveInstance(MKPInstance instance, String name, String setName) {
        try {
            BranchAndPrice solver = new BranchAndPrice(instance)
                    .setVerbose(config.isVerbose())
                    .setGapTolerance(config.getGapTolerance())
                    .setMaxNodes(config.getMaxNodes())
                    .setSatTimeLimitMs(config.getSatTimeLimitMs())
                    .setLpTimeLimitSeconds(config.getLpTimeLimitSeconds())
                    .setTimeLimitMs(config.getTimeLimitSeconds() * 1000);
            BPResult result = solver.solve();
            return new BenchmarkResult(name, setName, instance, result);
        } catch (Exception e) {
            return BenchmarkResult.error(name, setName, e.getMessage());
        }
    }

    private InstanceSet loadSet(String setName) throws IOException {
        Path dir = Path.of("src", "main", "resources", setName);
        if (!Files.isDirectory(dir)) {
            throw new IOException("Instance set directory not found: " + dir);
        }
        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .filter(p -> !p.getFileName().toString().equalsIgnoreCase("readme.txt"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
        }
        List<String> names = new ArrayList<>();
        List<MKPInstance> instances = new ArrayList<>();
        Set<String> filters = new HashSet<>();
        for (String f : config.getInstanceFilter()) {
            filters.add(f.toLowerCase());
        }
        int limit = config.getMaxInstancesPerSet();
        for (Path f : files) {
            if (!filters.isEmpty() && !filters.contains(f.getFileName().toString().toLowerCase())) {
                continue;
            }
            try {
                MKPInstance inst = InstanceReader.readFromFile(f.toString());
                names.add(f.getFileName().toString());
                instances.add(inst);
                if (names.size() >= limit) {
                    break;
                }
            } catch (Exception e) {
                System.err.println("Failed to load " + f + ": " + e.getMessage());
            }
        }
        return new InstanceSet(setName, names, instances);
    }

    private boolean shouldSkipSet(String setName) {
        for (String skip : config.getSkipSets()) {
            if (setName.equalsIgnoreCase(skip)) {
                return true;
            }
        }
        return false;
    }
}
