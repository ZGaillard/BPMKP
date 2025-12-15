package ca.udem.gaillarz;

import ca.udem.gaillarz.formulation.ClassicSolution;
import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * CLI to solve MKP instances with Branch-and-Price.
 */
public class Main {
    static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Path resourceRoot = Paths.get("src", "main", "resources");
        boolean verbose = false;

        System.out.println("============================================================");
        System.out.println("BRANCH-AND-PRICE MKP SOLVER");
        System.out.println("============================================================\n");

        while (true) {
            System.out.println("Choose an action:");
            System.out.println("  1) Solve hardcoded example");
            System.out.println("  2) Pick an instance file from resources");
            System.out.println("  3) Solve all instances in a resource directory");
            System.out.println("  4) Solve a random instance in a resource directory");
            System.out.println("  5) Solve all instances in resources");
            System.out.printf("  v) Toggle verbose (currently %s)%n", verbose ? "ON" : "OFF");
            System.out.println("  0) Exit");
            System.out.print("Selection [0-5 or v]: ");
            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) choice = "1";
            choice = choice.toLowerCase();

            try {
                switch (choice) {
                    case "1" -> solveExample(verbose);
                    case "2" -> pickAndSolveSingle(scanner, resourceRoot, verbose);
                    case "3" -> solveAllInDirectory(scanner, resourceRoot, verbose);
                    case "4" -> solveRandomInDirectory(scanner, resourceRoot, verbose);
                    case "5" -> solveAllInResources(resourceRoot, verbose);
                    case "v" -> {
                        verbose = !verbose;
                        System.out.println("Verbose " + (verbose ? "ON" : "OFF"));
                    }
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

    private static void solveExample(boolean verbose) throws InvalidInstanceException {
        MKPInstance instance = buildExampleInstance();
        runBP(instance, "Example", verbose, true);
    }

    private static void pickAndSolveSingle(Scanner scanner, Path root, boolean verbose) throws IOException {
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
        runFromFile(files.get(idx), verbose, true);
    }

    private static void solveAllInDirectory(Scanner scanner, Path root, boolean verbose) throws IOException {
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
            RunSummary summary = runFromFile(p, verbose, false);
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

    private static void solveRandomInDirectory(Scanner scanner, Path root, boolean verbose) throws IOException {
        Path dir = promptForDirectory(scanner, root);
        List<Path> files = listInstanceFiles(dir);
        if (files.isEmpty()) {
            System.out.println("No instances found in " + dir);
            return;
        }
        Path chosen = files.get(ThreadLocalRandom.current().nextInt(files.size()));
        System.out.println("Randomly selected: " + chosen);
        runFromFile(chosen, verbose, true);
    }

    private static void solveAllInResources(Path root, boolean verbose) throws IOException {
        List<Path> files = listInstanceFiles(root);
        if (files.isEmpty()) {
            System.out.println("No instances found under " + root);
            return;
        }
        List<RunSummary> runs = new ArrayList<>();
        int total = files.size();
        int optimal = 0;
        int gapLimit = 0;
        int infeasible = 0;
        for (int i = 0; i < total; i++) {
            Path p = files.get(i);
            System.out.printf("[%d/%d] Running %s...%n", i + 1, total, root.relativize(p));
            RunSummary summary = runFromFile(p, verbose, false);
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
        printRunSummary(runs, root);
    }

    // ========== Helpers ==========

    private static RunSummary runFromFile(Path file, boolean verbose, boolean printSolution) {
        try {
            MKPInstance instance = InstanceReader.readFromFile(file.toString());
            BPResult res = runBP(instance, file.toString(), verbose, printSolution);
            return new RunSummary(file, res, null);
        } catch (Exception e) {
            System.out.println("Failed to solve " + file + ": " + e.getMessage());
            return new RunSummary(file, null, e.getMessage());
        }
    }

    private static BPResult runBP(MKPInstance instance, String label, boolean verbose, boolean printSolution) {
        System.out.printf("%n--- Solving %s ---%n", label);
        BranchAndPrice solver = new BranchAndPrice(instance)
                .setVerbose(verbose)
                .setMaxNodes(1000)
                .setGapTolerance(0.01); // 1% gap tolerance

        BPResult result = solver.solve();

        System.out.println("Result: " + result);
        if (printSolution && result.hasSolution()) {
            ClassicSolution sol = result.solution();
            System.out.println(sol.toDetailedString(instance));
        }
        return result;
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

    private static MKPInstance buildExampleInstance() throws InvalidInstanceException {
        String content = """
                2
                5
                7
                6
                5\t10
                4\t8
                3\t6
                2\t5
                1\t4
                """;
        return InstanceReader.parseFromString(content, "example");
    }

    private record RunSummary(Path file, BPResult result, String error) {
    }
}
