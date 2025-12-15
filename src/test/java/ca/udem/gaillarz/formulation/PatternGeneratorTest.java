package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.Knapsack;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatternGeneratorTest {

    private MKPInstance buildInstance() {
        List<Item> items = List.of(
                new Item(0, 5, 10),
                new Item(1, 4, 8),
                new Item(2, 3, 6),
                new Item(3, 7, 14),
                new Item(4, 2, 3)
        );
        List<Knapsack> knapsacks = List.of(
                new Knapsack(0, 10),
                new Knapsack(1, 8)
        );
        return new MKPInstance(items, knapsacks, "test");
    }

    @Test
    void testSingleItemPatterns() {
        MKPInstance instance = buildInstance();
        PatternGenerator generator = new PatternGenerator(instance);

        List<Pattern> patterns = generator.generateInitialPatternsP0();

        long singles = patterns.stream().filter(p -> p.getNumItems() == 1).count();
        assertEquals(instance.getNumItems(), singles);
    }

    @Test
    void testGreedyPatternsExist() {
        MKPInstance instance = buildInstance();
        PatternGenerator generator = new PatternGenerator(instance);

        List<Pattern> patterns = generator.generateInitialPatternsP0();

        long multiItem = patterns.stream().filter(p -> p.getNumItems() > 1).count();
        assertTrue(multiItem > 0);
    }

    @Test
    void testCorePatternIsOptimalForCapacity() {
        MKPInstance instance = buildInstance();
        PatternGenerator generator = new PatternGenerator(instance);

        List<Pattern> patterns = generator.generateInitialPatternsP0();
        Pattern best = patterns.stream()
                .max(Comparator.comparingDouble(Pattern::getTotalProfit))
                .orElseThrow();

        double expected = bruteForceBestProfit(instance, instance.getTotalCapacity());
        assertEquals(expected, best.getTotalProfit(), 1e-6);
        assertTrue(best.isFeasible(instance.getTotalCapacity()));
    }

    @Test
    void testPatternStatistics() {
        MKPInstance instance = buildInstance();
        PatternGenerator generator = new PatternGenerator(instance);

        List<Pattern> patterns = generator.generateInitialPatternsP0();
        PatternStatistics stats = generator.getStatistics(patterns, instance.getTotalCapacity());

        assertTrue(stats.totalPatterns() > 0);
        assertTrue(stats.avgCapacityUtilization() > 0.0);
        assertTrue(stats.avgCapacityUtilization() <= 1.0);
    }

    private double bruteForceBestProfit(MKPInstance instance, int capacity) {
        int n = instance.getNumItems();
        double best = 0.0;
        for (int mask = 0; mask < (1 << n); mask++) {
            int weight = 0;
            int profit = 0;
            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) != 0) {
                    weight += instance.getItem(j).weight();
                    profit += instance.getItem(j).profit();
                }
            }
            if (weight <= capacity && profit > best) {
                best = profit;
            }
        }
        return best;
    }
}
