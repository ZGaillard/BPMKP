package preprocessing;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.Knapsack;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.preprocessing.ReducedCostFixer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReducedCostFixerTest {

    private MKPInstance createTestInstance() {
        List<Item> items = List.of(
                new Item(0, 2, 5),
                new Item(1, 3, 6),
                new Item(2, 4, 8),
                new Item(3, 5, 10),
                new Item(4, 1, 3)
        );
        List<Knapsack> knapsacks = List.of(new Knapsack(0, 15));
        return new MKPInstance(items, knapsacks, "rc_fixer_test");
    }

    @Test
    void testReducedCostFixing() {
        MKPInstance instance = createTestInstance();
        ReducedCostFixer fixer = new ReducedCostFixer(instance);

        double upperBound = 1000;
        double lowerBound = 900;
        double[] reducedCosts = {150, -50, 200, -180, 50};

        int fixed = fixer.fixByReducedCosts(upperBound, lowerBound, reducedCosts);

        assertEquals(2, fixed);
        assertEquals(0, fixer.getFixedValue(2));
        assertEquals(1, fixer.getFixedValue(3));
        assertNull(fixer.getFixedValue(0));
        assertNull(fixer.getFixedValue(1));
        assertNull(fixer.getFixedValue(4));
    }

    @Test
    void testNoFixingsWhenGapSmall() {
        MKPInstance instance = createTestInstance();
        ReducedCostFixer fixer = new ReducedCostFixer(instance);

        double upperBound = 1000;
        double lowerBound = 999.5;
        double[] reducedCosts = {50, -30, 40, -20, 10};

        int fixed = fixer.fixByReducedCosts(upperBound, lowerBound, reducedCosts);
        assertEquals(0, fixed);
        assertTrue(fixer.getFixedItems().isEmpty());
    }
}
