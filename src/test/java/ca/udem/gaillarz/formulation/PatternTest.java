package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Pattern.
 */
class PatternTest {

    private MKPInstance instance;

    @BeforeEach
    void setUp() throws InvalidInstanceException {
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
        instance = InstanceReader.parseFromString(content, "test");
    }

    @Test
    void testEmptyPattern() {
        Pattern pattern = Pattern.empty(5);

        assertTrue(pattern.isEmpty());
        assertEquals(0, pattern.getTotalWeight());
        assertEquals(0.0, pattern.getTotalProfit(), 1e-5);
        assertEquals(0, pattern.getNumItems());
        assertTrue(pattern.getItemIds().isEmpty());
    }

    @Test
    void testSingleItemPattern() {
        Pattern pattern = Pattern.singleItem(0, instance);

        assertFalse(pattern.isEmpty());
        assertEquals(5, pattern.getTotalWeight());  // Item 0 weight
        assertEquals(10.0, pattern.getTotalProfit(), 1e-5);  // Item 0 profit
        assertEquals(1, pattern.getNumItems());
        assertTrue(pattern.containsItem(0));
        assertFalse(pattern.containsItem(1));
    }

    @Test
    void testFromItemIds() {
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(0);
        itemIds.add(2);
        itemIds.add(4);

        Pattern pattern = Pattern.fromItemIds(itemIds, instance);

        assertEquals(3, pattern.getNumItems());
        assertEquals(9, pattern.getTotalWeight());  // 5 + 3 + 1
        assertEquals(20.0, pattern.getTotalProfit(), 1e-5);  // 10 + 6 + 4

        assertTrue(pattern.containsItem(0));
        assertTrue(pattern.containsItem(2));
        assertTrue(pattern.containsItem(4));
        assertFalse(pattern.containsItem(1));
        assertFalse(pattern.containsItem(3));
    }

    @Test
    void testFromBooleanArray() {
        boolean[] items = {true, false, true, false, true};
        Pattern pattern = new Pattern(items, instance);

        assertEquals(3, pattern.getNumItems());
        assertEquals(9, pattern.getTotalWeight());
        assertEquals(20.0, pattern.getTotalProfit(), 1e-5);
    }

    @Test
    void testFeasibility() {
        // Pattern with items 0, 1: weight = 5 + 4 = 9
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(0);
        itemIds.add(1);
        Pattern pattern = Pattern.fromItemIds(itemIds, instance);

        assertTrue(pattern.isFeasible(10));   // 9 <= 10
        assertTrue(pattern.isFeasible(9));    // 9 <= 9
        assertFalse(pattern.isFeasible(8));   // 9 > 8
        assertFalse(pattern.isFeasible(7));   // 9 > 7 (KS 0 capacity)
    }

    @Test
    void testEfficiency() {
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(0);
        itemIds.add(1);
        Pattern pattern = Pattern.fromItemIds(itemIds, instance);

        // profit = 18, weight = 9
        assertEquals(2.0, pattern.getEfficiency(), 1e-5);  // 18/9 = 2.0
    }

    @Test
    void testEmptyPatternEfficiency() {
        Pattern pattern = Pattern.empty(5);
        assertEquals(0.0, pattern.getEfficiency(), 1e-5);
    }

    @Test
    void testGetItems() {
        boolean[] items = {true, false, true, false, true};
        Pattern pattern = new Pattern(items, instance);

        boolean[] returnedItems = pattern.getItems();

        // Should be a copy
        assertArrayEquals(items, returnedItems);

        // Modifying returned array should not affect pattern
        returnedItems[0] = false;
        assertTrue(pattern.containsItem(0));
    }

    @Test
    void testGetItemIds() {
        Set<Integer> originalIds = new HashSet<>();
        originalIds.add(1);
        originalIds.add(3);
        Pattern pattern = Pattern.fromItemIds(originalIds, instance);

        Set<Integer> ids = pattern.getItemIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(1));
        assertTrue(ids.contains(3));
    }

    @Test
    void testEquality() {
        boolean[] items1 = {true, false, true, false, false};
        boolean[] items2 = {true, false, true, false, false};
        boolean[] items3 = {true, true, false, false, false};

        Pattern p1 = new Pattern(items1, instance);
        Pattern p2 = new Pattern(items2, instance);
        Pattern p3 = new Pattern(items3, instance);

        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void testDetailedString() {
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(0);
        itemIds.add(2);
        Pattern pattern = Pattern.fromItemIds(itemIds, instance);

        String detailed = pattern.toDetailedString(10);

        assertNotNull(detailed);
        assertTrue(detailed.contains("Pattern"));
        assertTrue(detailed.contains("Weight"));
        assertTrue(detailed.contains("Profit"));
    }

    @Test
    void testTableString() {
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(0);
        itemIds.add(2);
        Pattern pattern = Pattern.fromItemIds(itemIds, instance);

        String table = pattern.toTableString(instance);

        assertNotNull(table);
        assertTrue(table.contains("Item"));
        assertTrue(table.contains("Weight"));
        assertTrue(table.contains("Profit"));
    }

    @Test
    void testToString() {
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(0);
        Pattern pattern = Pattern.fromItemIds(itemIds, instance);

        String str = pattern.toString();
        assertTrue(str.contains("Pattern"));
        assertTrue(str.contains("w=5"));
        assertTrue(str.contains("p=10"));
    }

    @Test
    void testInvalidItemId() {
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(10);  // Invalid item ID

        assertThrows(IllegalArgumentException.class, () ->
            Pattern.fromItemIds(itemIds, instance));
    }

    @Test
    void testNullItems() {
        assertThrows(IllegalArgumentException.class, () ->
            new Pattern(null, instance));
    }

    @Test
    void testNullInstance() {
        boolean[] items = {true, false, true, false, true};
        assertThrows(IllegalArgumentException.class, () ->
            new Pattern(items, null));
    }

    @Test
    void testWrongArrayLength() {
        boolean[] items = {true, false, true};  // Wrong length

        assertThrows(IllegalArgumentException.class, () ->
            new Pattern(items, instance));
    }
}
