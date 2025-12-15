package ca.udem.gaillarz;

import ca.udem.gaillarz.formulation.ClassicSolution;
import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.bp.BPResult;
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
            System.out.println("  0) Exit");
            System.out.print("Selection [0-5]: ");
            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) choice = "1";

            try {
                switch (choice) {
                    case "1" -> solveExample();
                    case "2" -> pickAndSolveSingle(scanner, resourceRoot);
                    case "3" -> solveAllInDirectory(scanner, resourceRoot);
                    case "4" -> solveRandomInDirectory(scanner, resourceRoot);
                    case "5" -> solveAllInResources(resourceRoot);
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

    private static void solveExample() throws InvalidInstanceException {
        MKPInstance instance = buildExampleInstance();
        boolean verbose = askVerbose(new Scanner(System.in));
        runBP(instance, "Example", verbose);
    }

    private static void pickAndSolveSingle(Scanner scanner, Path root) throws IOException {
        Path dir = promptForDirectory(scanner, root);
        List<Path> files = listInstanceFiles(dir);
        if (files.isEmpty()) {
            System.out.println("No instances found in " + dir);
            return;
        }
        boolean verbose = askVerbose(scanner);
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
        runFromFile(files.get(idx), verbose);
    }

    private static void solveAllInDirectory(Scanner scanner, Path root) throws IOException {
        Path dir = promptForDirectory(scanner, root);
        List<Path> files = listInstanceFiles(dir);
        if (files.isEmpty()) {
            System.out.println("No instances found in " + dir);
            return;
        }
        boolean verbose = askVerbose(scanner);
        for (Path p : files) {
            runFromFile(p, verbose);
        }
    }

    private static void solveRandomInDirectory(Scanner scanner, Path root) throws IOException {
        Path dir = promptForDirectory(scanner, root);
        List<Path> files = listInstanceFiles(dir);
        if (files.isEmpty()) {
            System.out.println("No instances found in " + dir);
            return;
        }
        boolean verbose = askVerbose(scanner);
        Path chosen = files.get(ThreadLocalRandom.current().nextInt(files.size()));
        System.out.println("Randomly selected: " + chosen);
        runFromFile(chosen, verbose);
    }

    private static void solveAllInResources(Path root) throws IOException {
        List<Path> files = listInstanceFiles(root);
        if (files.isEmpty()) {
            System.out.println("No instances found under " + root);
            return;
        }
        boolean verbose = askVerbose(new Scanner(System.in));
        for (Path p : files) {
            runFromFile(p, verbose);
        }
    }

    // ========== Helpers ==========

    private static void runFromFile(Path file, boolean verbose) {
        try {
            MKPInstance instance = InstanceReader.readFromFile(file.toString());
            runBP(instance, file.toString(), verbose);
        } catch (Exception e) {
            System.out.println("Failed to solve " + file + ": " + e.getMessage());
        }
    }

    private static void runBP(MKPInstance instance, String label, boolean verbose) {
        System.out.printf("%n--- Solving %s ---%n", label);
        BranchAndPrice solver = new BranchAndPrice(instance)
                .setVerbose(verbose)
                .setMaxNodes(1000)
                .setGapTolerance(0.01); // 1% gap tolerance

        BPResult result = solver.solve();

        System.out.println("Result: " + result);
        if (result.hasSolution()) {
            ClassicSolution sol = result.solution();
            System.out.println(sol.toDetailedString(instance));
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

    private static boolean askVerbose(Scanner scanner) {
        System.out.print("Enable verbose output? [y/N]: ");
        String in = scanner.nextLine().trim().toLowerCase();
        return in.startsWith("y");
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
}
