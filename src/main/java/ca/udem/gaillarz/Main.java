package ca.udem.gaillarz;

import ca.udem.gaillarz.formulation.*;
import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import solver.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demonstration entry point for the MKP formulations and column generation.
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Path resourceRoot = Paths.get("src", "main", "resources");

        System.out.println("============================================================");
        System.out.println("MKP FORMULATION HIERARCHY DEMONSTRATION");
        System.out.println("============================================================\n");
        System.out.println("Choose what to run:");
        System.out.println("  1) Hardcoded example");
        System.out.println("  2) Random instance from a resource directory");
        System.out.println("  3) All instances in a specific resource directory");
        System.out.println("  4) All instances in resources");
        System.out.print("Selection [1-4, default 1]: ");
        String choice = scanner.nextLine().trim();
        if (choice.isEmpty()) choice = "1";

        try {
            switch (choice) {
                case "2" -> runRandomInDirectory(scanner, resourceRoot);
                case "3" -> runAllInDirectory(scanner, resourceRoot);
                case "4" -> runAllInResources(resourceRoot);
                case "1" -> {
                    MKPInstance instance = buildExampleInstance();
                    runDemo(instance, "Example");
                }
                default -> {
                    System.out.println("Unknown choice, running example.\n");
                    MKPInstance instance = buildExampleInstance();
                    runDemo(instance, "Example");
                }
            }
        } catch (IOException e) {
            System.out.println("I/O error while reading resources: " + e.getMessage());
        }
    }

    // ========== Demo Runner ==========

    private static void runDemo(MKPInstance instance, String label) {
        System.out.println("============================================================");
        System.out.printf("Running demo for: %s%n", label);
        System.out.println("============================================================\n");

        // Display instance
        System.out.println(instance.toDetailedString());
        System.out.println();
        System.out.println(instance.toTable());
        System.out.println();

        // Classic formulation
        System.out.println("============================================================");
        System.out.println("CLASSIC FORMULATION");
        System.out.println("============================================================\n");

        ClassicFormulation classic = new ClassicFormulation(instance);
        System.out.println(classic.toMathematicalString());

        ClassicSolution classicSol = new ClassicSolution(instance.getNumKnapsacks(), instance.getNumItems());
        if (instance.getNumItems() >= 2 && instance.getNumKnapsacks() >= 2) {
            classicSol.assignItem(0, 0);
            classicSol.assignItem(0, instance.getNumItems() - 1);
            classicSol.assignItem(1, 1);
        }

        System.out.println("Sample Solution:");
        System.out.println(classicSol.toDetailedString(instance));
        System.out.println("Objective: " + classic.computeObjectiveValue(classicSol));
        System.out.println("Feasible: " + classic.isFeasible(classicSol));
        System.out.println();

        // L2 formulation
        System.out.println("============================================================");
        System.out.println("L2 RELAXED FORMULATION");
        System.out.println("============================================================\n");

        L2RelaxedFormulation l2 = classic.toL2Formulation();
        System.out.println(l2.toMathematicalString());

        L2Solution l2Sol = L2Solution.fromClassicSolution(classicSol, instance.getNumItems());
        System.out.println("Converted L2 Solution:");
        System.out.println(l2Sol.toItemSelectionString());
        System.out.println(l2Sol.toAssignmentString());
        System.out.println("L2 Objective: " + l2.computeObjectiveValue(l2Sol));
        System.out.println("L2 Feasible: " + l2.isFeasible(l2Sol));
        System.out.println();

        // DW + column generation
        System.out.println("============================================================");
        System.out.println("DANTZIG-WOLFE MASTER FORMULATION");
        System.out.println("============================================================\n");

        DantzigWolfeMaster master = l2.toDantzigWolfeFormulation();

        System.out.println("Seeding initial patterns (Phase 1)...");
        PatternInitializer.initialize(master);

        System.out.println(master.toStructureString());
        System.out.println();

        System.out.println("Running column generation...");
        ColumnGeneration cg = new ColumnGeneration(master, new ORToolsSolver());
        CGParameters params = new CGParameters().setMaxIterations(200).setVerbose(true);
        CGResult cgResult = cg.solve(params);

        System.out.println("\nColumn Generation Result:");
        System.out.println("  Status: " + cgResult.getStatus());
        System.out.println("  Iterations: " + cgResult.getIterations());
        System.out.println("  Patterns added: " + cgResult.getPatternsAdded());
        System.out.println("  Objective: " + cgResult.getObjectiveValue());

        if (cgResult.getDwSolution() != null) {
            System.out.println("\nFinal DW Solution:");
            System.out.println(master.visualizeSolution(cgResult.getDwSolution()));

            L2Solution l2FromDW = cgResult.getL2Solution();
            System.out.println("Derived L2 Solution (from DW):");
            System.out.println(l2FromDW.toItemSelectionString());
            System.out.println(l2FromDW.toAssignmentString());
            System.out.println("L2 objective: " + l2.computeObjectiveValue(l2FromDW));

            if (l2FromDW.isInteger()) {
                ClassicSolution classicFromDW = l2FromDW.toClassicSolution();
                System.out.println("\nConverted Classic Solution:");
                System.out.println(classicFromDW.toDetailedString(instance));
                System.out.println("Classic objective: " + classic.computeObjectiveValue(classicFromDW));
            } else {
                System.out.println("\nDerived L2 solution is fractional; skipping classic conversion.");
            }
        }

        System.out.println("\n============================================================");
        System.out.println("DEMONSTRATION COMPLETE");
        System.out.println("============================================================\n");
    }

    // ========== CLI helpers ==========

    private static void runRandomInDirectory(Scanner scanner, Path root) throws IOException {
        Path dir = promptForDirectory(scanner, root);
        List<Path> files = listInstanceFiles(dir);
        if (files.isEmpty()) {
            System.out.println("No instance files found in " + dir);
            return;
        }
        Path chosen = files.get(ThreadLocalRandom.current().nextInt(files.size()));
        System.out.println("Randomly selected: " + chosen);
        runFromFile(chosen);
    }

    private static void runAllInDirectory(Scanner scanner, Path root) throws IOException {
        Path dir = promptForDirectory(scanner, root);
        List<Path> files = listInstanceFiles(dir);
        if (files.isEmpty()) {
            System.out.println("No instance files found in " + dir);
            return;
        }
        for (Path p : files) {
            runFromFile(p);
        }
    }

    private static void runAllInResources(Path root) throws IOException {
        List<Path> files = listInstanceFiles(root);
        if (files.isEmpty()) {
            System.out.println("No instance files found in resources.");
            return;
        }
        for (Path p : files) {
            runFromFile(p);
        }
    }

    private static void runFromFile(Path file) {
        try {
            MKPInstance inst = InstanceReader.readFromFile(file.toString());
            runDemo(inst, file.toString());
        } catch (Exception e) {
            System.out.println("Failed to run instance " + file + ": " + e.getMessage());
        }
    }

    private static Path promptForDirectory(Scanner scanner, Path root) throws IOException {
        List<Path> dirs = listResourceDirectories(root);
        if (dirs.isEmpty()) {
            throw new IOException("No resource directories found under " + root);
        }
        System.out.println("Available resource directories:");
        for (int i = 0; i < dirs.size(); i++) {
            System.out.printf("  %d) %s%n", i + 1, dirs.get(i).getFileName());
        }
        System.out.print("Choose directory [1-" + dirs.size() + ", default 1]: ");
        String dirChoice = scanner.nextLine().trim();
        int idx = 0;
        try {
            if (!dirChoice.isEmpty()) {
                idx = Math.max(0, Math.min(dirs.size() - 1, Integer.parseInt(dirChoice) - 1));
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

    private static MKPInstance buildExampleInstance() {
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
        try {
            return InstanceReader.parseFromString(content, "example");
        } catch (InvalidInstanceException e) {
            throw new RuntimeException("Failed to build example instance", e);
        }
    }
}
