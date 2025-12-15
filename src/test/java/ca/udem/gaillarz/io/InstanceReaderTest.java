package ca.udem.gaillarz.io;

import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for InstanceReader.
 */
class InstanceReaderTest {

    @Test
    void testParseSimpleInstance() throws InvalidInstanceException {
        String content = """
                2
                3
                10
                8
                5\t10
                4\t8
                3\t6
                """;

        MKPInstance instance = InstanceReader.parseFromString(content, "test");

        assertEquals(3, instance.getNumItems());
        assertEquals(2, instance.getNumKnapsacks());

        // Check knapsacks
        assertEquals(10, instance.getKnapsack(0).capacity());
        assertEquals(8, instance.getKnapsack(1).capacity());

        // Check items (weight, profit)
        assertEquals(5, instance.getItem(0).weight());
        assertEquals(10, instance.getItem(0).profit());
        assertEquals(4, instance.getItem(1).weight());
        assertEquals(8, instance.getItem(1).profit());
        assertEquals(3, instance.getItem(2).weight());
        assertEquals(6, instance.getItem(2).profit());
    }

    @Test
    void testParseTotalCapacity() throws InvalidInstanceException {
        String content = """
                3
                2
                100
                150
                200
                10\t20
                15\t30
                """;

        MKPInstance instance = InstanceReader.parseFromString(content);

        assertEquals(450, instance.getTotalCapacity());
        assertEquals(25, instance.getTotalWeight());
        assertEquals(50, instance.getTotalProfit());
    }

    @Test
    void testWriteAndReadBack(@TempDir Path tempDir) throws IOException, InvalidInstanceException {
        // Create an instance
        String content = """
                2
                3
                10
                8
                5\t10
                4\t8
                3\t6
                """;

        MKPInstance original = InstanceReader.parseFromString(content, "test");

        // Write to file
        Path file = tempDir.resolve("test_instance.txt");
        InstanceReader.writeToFile(original, file.toString());

        // Read back
        MKPInstance loaded = InstanceReader.readFromFile(file.toString());

        // Verify
        assertEquals(original.getNumItems(), loaded.getNumItems());
        assertEquals(original.getNumKnapsacks(), loaded.getNumKnapsacks());
        assertEquals(original.getTotalCapacity(), loaded.getTotalCapacity());
        assertEquals(original.getTotalProfit(), loaded.getTotalProfit());

        for (int i = 0; i < original.getNumKnapsacks(); i++) {
            assertEquals(original.getKnapsack(i).capacity(), loaded.getKnapsack(i).capacity());
        }

        for (int j = 0; j < original.getNumItems(); j++) {
            assertEquals(original.getItem(j).weight(), loaded.getItem(j).weight());
            assertEquals(original.getItem(j).profit(), loaded.getItem(j).profit());
        }
    }

    @Test
    void testInvalidNumberOfKnapsacks() {
        String content = """
                0
                3
                10
                5\t10
                4\t8
                3\t6
                """;

        assertThrows(InvalidInstanceException.class, () ->
                InstanceReader.parseFromString(content));
    }

    @Test
    void testInvalidNumberOfItems() {
        String content = """
                2
                -1
                10
                8
                """;

        assertThrows(InvalidInstanceException.class, () ->
                InstanceReader.parseFromString(content));
    }

    @Test
    void testMissingData() {
        String content = """
                2
                5
                10
                8
                5\t10
                """;

        assertThrows(InvalidInstanceException.class, () ->
                InstanceReader.parseFromString(content));
    }

    @Test
    void testInstanceToString() throws InvalidInstanceException {
        String content = """
                2
                3
                10
                8
                5\t10
                4\t8
                3\t6
                """;

        MKPInstance instance = InstanceReader.parseFromString(content, "test");
        String output = InstanceReader.instanceToString(instance);

        // Parse the output and verify it's equivalent
        MKPInstance reparsed = InstanceReader.parseFromString(output);

        assertEquals(instance.getNumItems(), reparsed.getNumItems());
        assertEquals(instance.getNumKnapsacks(), reparsed.getNumKnapsacks());
    }

    @Test
    void testSpaceSeparatedValues() throws InvalidInstanceException {
        String content = """
                2
                3
                10
                8
                5 10
                4 8
                3 6
                """;

        MKPInstance instance = InstanceReader.parseFromString(content);

        assertEquals(3, instance.getNumItems());
        assertEquals(5, instance.getItem(0).weight());
        assertEquals(10, instance.getItem(0).profit());
    }

    @Test
    void testEmptyLines() throws InvalidInstanceException {
        String content = """
                
                2
                
                3
                10
                
                8
                5\t10
                4\t8
                
                3\t6
                """;

        MKPInstance instance = InstanceReader.parseFromString(content);

        assertEquals(3, instance.getNumItems());
        assertEquals(2, instance.getNumKnapsacks());
    }
}

