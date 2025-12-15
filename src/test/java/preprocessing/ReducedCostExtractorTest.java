package preprocessing;

import ca.udem.gaillarz.formulation.DualValues;
import ca.udem.gaillarz.formulation.L2Solution;
import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.Knapsack;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.preprocessing.ReducedCostExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ReducedCostExtractorTest {

    private MKPInstance createTestInstance() {
        List<Item> items = List.of(
                new Item(0, 2, 5),
                new Item(1, 3, 6),
                new Item(2, 4, 7)
        );
        List<Knapsack> knapsacks = List.of(new Knapsack(0, 10));
        return new MKPInstance(items, knapsacks, "rc_extractor_test");
    }

    @Test
    void testExtractReducedCosts() {
        MKPInstance instance = createTestInstance();
        ReducedCostExtractor extractor = new ReducedCostExtractor(instance);

        double[] t = {0.0, 1.0, 0.25};
        double[][] x = {{0.0, 1.0, 0.25}};
        L2Solution l2Solution = new L2Solution(t, x);

        double[] mu = {1.0, -0.5, 0.25};
        double[] pi = {0.0, 0.0};
        double tau = 0.1;
        DualValues dualValues = new DualValues(mu, pi, tau);

        double[] reducedCosts = extractor.extractReducedCosts(l2Solution, dualValues);
        double base0 = instance.getItem(0).profit() * (1 - tau) - mu[0];
        double base1 = instance.getItem(1).profit() * (1 - tau) - mu[1];
        double base2 = instance.getItem(2).profit() * (1 - tau) - mu[2];

        double[] expected = {base0, -base1, base2};
        assertArrayEquals(expected, reducedCosts, 1e-9);
    }
}
