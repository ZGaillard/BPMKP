package ca.udem.gaillarz.io;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.Knapsack;
import ca.udem.gaillarz.model.MKPInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes MKP instances from/to files.
 *
 * <p>File format (as per readme.txt):
 * <pre>
 * Number of knapsacks (m)
 * Number of items (n)
 * For each knapsack i (i=1,...,m): its capacity
 * For each item j (j=1,...,n): its weight (wj) and its profit (pj) [tab-separated]
 * </pre>
 */
public class InstanceReader {

    /**
     * Read MKP instance from file.
     *
     * @param filepath Path to instance file
     * @return MKPInstance object
     * @throws IOException              if file cannot be read
     * @throws InvalidInstanceException if instance format is invalid
     */
    public static MKPInstance readFromFile(String filepath)
            throws IOException, InvalidInstanceException {
        Path path = Paths.get(filepath);
        String content = Files.readString(path);
        String name = path.getFileName().toString();
        if (name.endsWith(".txt")) {
            name = name.substring(0, name.length() - 4);
        }
        return parseFromString(content, name);
    }

    /**
     * Read MKP instance from file with automatic name extraction.
     *
     * @param file File object pointing to instance file
     * @return MKPInstance object
     * @throws IOException              if file cannot be read
     * @throws InvalidInstanceException if instance format is invalid
     */
    public static MKPInstance readFromFile(File file)
            throws IOException, InvalidInstanceException {
        return readFromFile(file.getAbsolutePath());
    }

    /**
     * Parse instance from string (useful for testing).
     *
     * @param content File content as string
     * @return MKPInstance object
     * @throws InvalidInstanceException if instance format is invalid
     */
    public static MKPInstance parseFromString(String content)
            throws InvalidInstanceException {
        return parseFromString(content, "unnamed");
    }

    /**
     * Parse instance from string with a name.
     *
     * @param content File content as string
     * @param name    Instance name
     * @return MKPInstance object
     * @throws InvalidInstanceException if instance format is invalid
     */
    public static MKPInstance parseFromString(String content, String name)
            throws InvalidInstanceException {
        try {
            String[] lines = content.split("\\r?\\n");
            int lineIndex = 0;

            // Skip empty lines at the beginning
            while (lineIndex < lines.length && lines[lineIndex].trim().isEmpty()) {
                lineIndex++;
            }

            if (lineIndex >= lines.length) {
                throw new InvalidInstanceException("File is empty");
            }

            // Read m (number of knapsacks)
            int m = Integer.parseInt(lines[lineIndex++].trim());
            if (m <= 0) {
                throw new InvalidInstanceException("Number of knapsacks must be positive, got: " + m);
            }

            // Skip empty lines
            while (lineIndex < lines.length && lines[lineIndex].trim().isEmpty()) {
                lineIndex++;
            }

            // Read n (number of items)
            int n = Integer.parseInt(lines[lineIndex++].trim());
            if (n <= 0) {
                throw new InvalidInstanceException("Number of items must be positive, got: " + n);
            }

            // Read knapsack capacities
            List<Knapsack> knapsacks = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                // Skip empty lines
                while (lineIndex < lines.length && lines[lineIndex].trim().isEmpty()) {
                    lineIndex++;
                }

                if (lineIndex >= lines.length) {
                    throw new InvalidInstanceException("Expected " + m + " knapsack capacities, got only " + i);
                }

                int capacity = Integer.parseInt(lines[lineIndex++].trim());
                if (capacity <= 0) {
                    throw new InvalidInstanceException("Knapsack capacity must be positive, got: " + capacity);
                }
                knapsacks.add(new Knapsack(i, capacity));
            }

            // Read items (weight, profit)
            List<Item> items = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                // Skip empty lines
                while (lineIndex < lines.length && lines[lineIndex].trim().isEmpty()) {
                    lineIndex++;
                }

                if (lineIndex >= lines.length) {
                    throw new InvalidInstanceException("Expected " + n + " items, got only " + j);
                }

                String line = lines[lineIndex++].trim();
                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    throw new InvalidInstanceException("Invalid item format at line " + lineIndex + ": " + line);
                }

                int weight = Integer.parseInt(parts[0]);
                int profit = Integer.parseInt(parts[1]);

                if (weight <= 0) {
                    throw new InvalidInstanceException("Item weight must be positive, got: " + weight);
                }
                if (profit <= 0) {
                    throw new InvalidInstanceException("Item profit must be positive, got: " + profit);
                }

                items.add(new Item(j, weight, profit));
            }

            MKPInstance instance = new MKPInstance(items, knapsacks, name);

            // Validation: at least one item should fit in at least one knapsack
            if (!instance.isValid()) {
                throw new InvalidInstanceException("Invalid instance: no item fits in any knapsack");
            }

            return instance;

        } catch (NumberFormatException e) {
            throw new InvalidInstanceException("Invalid number format: " + e.getMessage(), e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidInstanceException("Unexpected end of file", e);
        }
    }

    /**
     * Write MKP instance to file.
     *
     * @param instance Instance to write
     * @param filepath Output file path
     * @throws IOException if file cannot be written
     */
    public static void writeToFile(MKPInstance instance, String filepath)
            throws IOException {
        Path path = Paths.get(filepath);
        Files.writeString(path, instanceToString(instance));
    }

    /**
     * Convert instance to string representation (file format).
     *
     * @param instance Instance to convert
     * @return String representation in file format
     */
    public static String instanceToString(MKPInstance instance) {
        StringBuilder sb = new StringBuilder();

        // Number of knapsacks
        sb.append(instance.getNumKnapsacks()).append("\n");
        // Number of items
        sb.append(instance.getNumItems()).append("\n");

        // Knapsack capacities
        for (Knapsack ks : instance.getKnapsacks()) {
            sb.append(ks.capacity()).append("\n");
        }

        // Items (weight, profit)
        for (Item item : instance.getItems()) {
            sb.append(item.weight()).append("\t").append(item.profit()).append("\n");
        }

        return sb.toString();
    }
}

