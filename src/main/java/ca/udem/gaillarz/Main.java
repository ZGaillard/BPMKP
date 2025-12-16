package ca.udem.gaillarz;

import ca.udem.gaillarz.formulation.ClassicSolution;
import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.bp.BPResult;
import ca.udem.gaillarz.solver.bp.BPStatus;
import ca.udem.gaillarz.solver.bp.BranchAndPrice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * CLI to solve MKP instances with Branch-and-Price.
 */
public class Main {
    static void main() {
        Scanner scanner = new Scanner(System.in);
        Path resourceRoot = Paths.get("src", "main", "resources");
        RunConfig config = new RunConfig();
        config.verbose = true;

        System.out.println("============================================================");
        System.out.println("BRANCH-AND-PRICE MKP SOLVER");
        System.out.println("============================================================\n");

        while (true) {
            System.out.println("Current config:");
            System.out.println(configSummary(config));
            System.out.println("------------------------------------------------------------");
            System.out.println("Choose an action:");
            System.out.println(" ");
            System.out.println("  1) Pick an instance file from resources");
            System.out.println("  2) Solve all instances in a resource directory");
            System.out.println("  c) Configure run parameters");
            System.out.println("  0) Exit");
            System.out.print("Selection [0-2 or c]: ");
            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) choice = "1";
            choice = choice.toLowerCase();

            try {
                switch (choice) {
                    case "1" -> pickAndSolveSingle(scanner, resourceRoot, config);
                    case "2" -> solveAllInDirectory(scanner, resourceRoot, config);
                    case "c" -> configureRun(scanner, config);
                    case "0" -> {
                        System.out.println("Bye.");
                        return;
                    }
                    default -> System.out.println("Unknown option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    // ========== Actions ==========

    private static void pickAndSolveSingle(Scanner scanner, Path root, RunConfig config) throws IOException {
        Path dir = promptForDirectory(scanner, root);
        List<Path> files = listInstanceFiles(dir);
        if (files.isEmpty()) {
            System.out.println("No instances found in " + dir);
            return;
        }
        System.out.println("Instances in " + dir + ":");
        for (int i = 0; i < files.size(); i++) {
            System.out.printf("  %2d) %s%n", i + 1, dir.relativize(files.get(i)));
        }
        System.out.print("Select file [1-" + files.size() + ", default 1]: ");
        String input = scanner.nextLine().trim();
        int idx = 0;
        try {
            if (!input.isEmpty()) {
                idx = Math.max(0, Math.min(files.size() - 1, Integer.parseInt(input) - 1));
            }
        } catch (NumberFormatException ignored) {
        }
        runFromFile(files.get(idx), config);
    }

    private static void solveAllInDirectory(Scanner scanner, Path root, RunConfig config) throws IOException {
        Path dir = promptForDirectory(scanner, root);
        List<Path> files = listInstanceFiles(dir);
        if (files.isEmpty()) {
            System.out.println("No instances found in " + dir);
            return;
        }
        List<RunSummary> runs = new ArrayList<>();
        int total = files.size();
        int optimal = 0;
        int gapLimit = 0;
        int infeasible = 0;
        for (int i = 0; i < total; i++) {
            Path p = files.get(i);
            System.out.printf("[%d/%d] Running %s...%n", i + 1, total, dir.relativize(p));
            RunSummary summary = runFromFile(p, config);
            runs.add(summary);
            if (summary.result() != null) {
                BPResult r = summary.result();
                if (r.status() == BPStatus.OPTIMAL) optimal++;
                if (r.status() == BPStatus.GAP_LIMIT) gapLimit++;
                if (r.status() == BPStatus.INFEASIBLE) infeasible++;
                System.out.printf("    -> %s obj=%.3f gap=%.2f%% time=%.2fs%n",
                        r.status(), r.objectiveValue(), r.gap() * 100.0, r.solveTimeMs() / 1000.0);
            } else {
                System.out.printf("    -> FAILED (%s)%n", summary.error());
            }
            System.out.printf("    Progress: optimal=%d, gap_limit=%d, infeasible=%d%n", optimal, gapLimit, infeasible);
        }
        printRunSummary(runs, dir);
    }

    // ========== Helpers ==========

    private static RunSummary runFromFile(Path file, RunConfig config) {
        try {
            MKPInstance instance = InstanceReader.readFromFile(file.toString());
            BPResult res = runBP(instance, file.toString(), config);
            return new RunSummary(file, res, null);
        } catch (Exception e) {
            System.out.println("Failed to solve " + file + ": " + e.getMessage());
            return new RunSummary(file, null, e.getMessage());
        }
    }

    private static BPResult runBP(MKPInstance instance, String label, RunConfig config) {
        System.out.printf("%nSolving %s...%n", label);
        BranchAndPrice solver = new BranchAndPrice(instance)
                .setVerbose(config.verbose)
                .setMaxNodes(config.maxNodes)
                .setGapTolerance(config.gapTolerance) // 1% gap tolerance default
                .setSatTimeLimitMs(config.satTimeLimitMs) // SAT repair: 2-10s is typical; 5s balances retries vs runtime
                .setLpTimeLimitSeconds(config.lpTimeLimitSeconds); // LP solves usually <<1s; 30s caps rare degeneracy
        if (config.timeLimitSeconds > 0) {
            solver.setTimeLimitMs(config.timeLimitSeconds * 1000);
        }

        BPResult result = solver.solve();

        System.out.println("Result: " + result);
        if (config.verbose && result.hasSolution()) {
            ClassicSolution sol = result.solution();
            System.out.println(sol.toDetailedString(instance));
        }
        return result;
    }

    private static void configureRun(Scanner scanner, RunConfig config) {
        while (true) {
            System.out.println("\n=== Run configuration ===");
            System.out.println("Current: " + configSummary(config));
            System.out.println("  1) Toggle verbose");
            System.out.println("  2) Set gap tolerance");
            System.out.println("  3) Set max nodes");
            System.out.println("  4) Set time limit seconds (0 = unlimited)");
            System.out.println("  5) Set SAT feasibility time limit ms (0 = unlimited)");
            System.out.println("  6) Set LP time limit seconds (0 = unlimited)");
            System.out.println("  0) Done");
            System.out.print("Select option [0-6]: ");
            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) choice = "0";

            switch (choice) {
                case "1" -> {
                    config.verbose = !config.verbose;
                    System.out.println("Verbose " + (config.verbose ? "ON" : "OFF"));
                }
                case "2" -> config.gapTolerance = promptDouble(scanner, "Gap tolerance (0 exact, 0.01 = 1%)", config.gapTolerance, 1.0);
                case "3" -> config.maxNodes = promptInt(scanner, config.maxNodes);
                case "4" -> config.timeLimitSeconds = promptLong(scanner, "Time limit seconds (0 = unlimited)", config.timeLimitSeconds);
                case "5" -> config.satTimeLimitMs = promptLong(scanner, "SAT feasibility time limit ms (0 = unlimited, e.g., 5000)", config.satTimeLimitMs);
                case "6" -> config.lpTimeLimitSeconds = promptDouble(scanner, "LP time limit seconds (0 = unlimited, e.g., 30)", config.lpTimeLimitSeconds, Double.MAX_VALUE);
                case "0" -> {
                    System.out.println("Config saved: " + configSummary(config));
                    return;
                }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private static String configSummary(RunConfig config) {
        return String.format("verbose=%s, gap=%.3f, nodes=%d, time=%ds, SAT=%dms, LP=%.1fs",
                config.verbose, config.gapTolerance, config.maxNodes, config.timeLimitSeconds, config.satTimeLimitMs, config.lpTimeLimitSeconds);
    }

    private static double promptDouble(Scanner scanner, String label, double current, double max) {
        System.out.printf("%s [current=%.4f]: ", label, current);
        String in = scanner.nextLine().trim();
        if (in.isEmpty()) return current;
        try {
            double v = Double.parseDouble(in);
            if (v < 0.0 || v > max) {
                System.out.println("Out of range; keeping current.");
                return current;
            }
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number; keeping current.");
            return current;
        }
    }

    private static int promptInt(Scanner scanner, int current) {
        System.out.printf("%s [current=%d]: ", "Max nodes (>=1)", current);
        String in = scanner.nextLine().trim();
        if (in.isEmpty()) return current;
        try {
            int v = Integer.parseInt(in);
            if (v < 1) {
                System.out.println("Out of range; keeping current.");
                return current;
            }
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Invalid integer; keeping current.");
            return current;
        }
    }

    private static long promptLong(Scanner scanner, String label, long current) {
        System.out.printf("%s [current=%d]: ", label, current);
        String in = scanner.nextLine().trim();
        if (in.isEmpty()) return current;
        try {
            long v = Long.parseLong(in);
            if (v < (long) 0) {
                System.out.println("Out of range; keeping current.");
                return current;
            }
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Invalid integer; keeping current.");
            return current;
        }
    }

    private static Path promptForDirectory(Scanner scanner, Path root) throws IOException {
        List<Path> dirs = listResourceDirectories(root);
        if (dirs.isEmpty()) throw new IOException("No resource directories under " + root);
        System.out.println("Available directories:");
        for (int i = 0; i < dirs.size(); i++) {
            System.out.printf("  %d) %s%n", i + 1, dirs.get(i).getFileName());
        }
        System.out.print("Select directory [1-" + dirs.size() + ", default 1]: ");
        String input = scanner.nextLine().trim();
        int idx = 0;
        try {
            if (!input.isEmpty()) {
                idx = Math.max(0, Math.min(dirs.size() - 1, Integer.parseInt(input) - 1));
            }
        } catch (NumberFormatException ignored) {
        }
        return dirs.get(idx);
    }

    private static List<Path> listResourceDirectories(Path root) throws IOException {
        List<Path> dirs = new ArrayList<>();
        if (!Files.exists(root)) return dirs;
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(dirs::add);
        }
        dirs.sort(Comparator.comparing(Path::getFileName));
        return dirs;
    }

    private static List<Path> listInstanceFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) return List.of();
        try (var stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .filter(p -> !p.getFileName().toString().equalsIgnoreCase("readme.txt"))
                    .sorted()
                    .toList();
        }
    }

    private static void printRunSummary(List<RunSummary> runs, Path baseDir) {
        System.out.println("\n=== Run summary ===");
        int total = runs.size();
        int failures = 0;
        int optimal = 0;
        int withSolution = 0;

        for (RunSummary r : runs) {
            if (r.result() == null) {
                failures++;
                continue;
            }
            if (r.result().isOptimal()) optimal++;
            if (r.result().hasSolution()) withSolution++;
        }

        System.out.printf("Total: %d | Succeeded: %d | Failures: %d | Optimal: %d | With solution: %d%n",
                total, total - failures, failures, optimal, withSolution);

        for (RunSummary r : runs) {
            String name = r.file().toString();
            if (baseDir != null && r.file().startsWith(baseDir)) {
                name = baseDir.relativize(r.file()).toString();
            }
            if (r.result() != null) {
                BPResult res = r.result();
                System.out.printf("  - %s: %s obj=%.3f gap=%.2f%% time=%.2fs%n",
                        name, res.status(), res.objectiveValue(), res.gap() * 100.0, res.solveTimeMs() / 1000.0);
            } else {
                System.out.printf("  - %s: FAILED (%s)%n", name, r.error());
            }
        }
    }

    private static class RunConfig {
        // Defaults chosen to balance speed and solution quality
        boolean verbose = false;
        double gapTolerance = 0.01;
        int maxNodes = 1000;
        long timeLimitSeconds = 0; // 0 = unlimited
        long satTimeLimitMs = 5000; // SAT repair: 2-10s typical; 5s avoids thrash without stalling too long
        double lpTimeLimitSeconds = 30.0; // LP solves are usually fast; 30s caps rare degeneracy
    }

    private record RunSummary(Path file, BPResult result, String error) {
    }
}
