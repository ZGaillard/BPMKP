package ca.udem.gaillarz.solver.knapsack;

import ca.udem.gaillarz.model.Item;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnapsackSolverTest {

    @Test
    void testSimpleKnapsack() {
        // Capacity 7 -> optimal picks items 1 and 2 (profit 14, weight 7)
        List<Item> items = Arrays.asList(
                new Item(0, 5, 10),
                new Item(1, 4, 8),
                new Item(2, 3, 6)
        );

        KnapsackSolver solver = new KnapsackSolver(items, 7);
        KnapsackResult result = solver.solve();

        assertEquals(14.0, result.getOptimalProfit(), 1e-6);
        assertTrue(result.getSelectedItemIds().contains(1));
        assertTrue(result.getSelectedItemIds().contains(2));
        assertFalse(result.getSelectedItemIds().contains(0));
    }

    @Test
    void testFullKnapsack() {
        List<Item> items = Arrays.asList(
                new Item(0, 2, 5),
                new Item(1, 3, 7)
        );

        KnapsackSolver solver = new KnapsackSolver(items, 10);
        KnapsackResult result = solver.solve();

        assertEquals(12.0, result.getOptimalProfit(), 1e-6);
        assertEquals(2, result.getNumItems());
    }

    @Test
    void testEmptyKnapsack() {
        List<Item> items = List.of(new Item(0, 100, 50));

        KnapsackSolver solver = new KnapsackSolver(items, 10);
        KnapsackResult result = solver.solve();

        assertTrue(result.isEmpty());
        assertEquals(0.0, result.getOptimalProfit(), 1e-6);
    }

    @Test
    void testCustomProfits() {
        List<Item> items = Arrays.asList(
                new Item(0, 5, 10),
                new Item(1, 4, 8)
        );

        double[] customProfits = {5.0, 10.0}; // swap preference

        KnapsackSolver solver = new KnapsackSolver(items, 10, customProfits);
        KnapsackResult result = solver.solve();

        assertTrue(result.getSelectedItemIds().contains(1));
    }

    @Test
    void testSpaceOptimizedMatchesFull() {
        List<Item> items = Arrays.asList(
                new Item(0, 5, 10),
                new Item(1, 4, 8),
                new Item(2, 3, 6)
        );

        KnapsackSolver solver = new KnapsackSolver(items, 7);
        KnapsackResult dense = solver.solve();
        KnapsackResult compact = solver.solveSpaceOptimized();

        assertEquals(dense.getOptimalProfit(), compact.getOptimalProfit(), 1e-6);
        assertEquals(dense.getSelectedItemIds(), compact.getSelectedItemIds());
    }
}
