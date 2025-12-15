package solver;

import ca.udem.gaillarz.formulation.DualValues;
import ca.udem.gaillarz.formulation.Pattern;
import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PricingProblemTest {

    private MKPInstance buildInstance() throws InvalidInstanceException {
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
        return InstanceReader.parseFromString(content, "pricing_test");
    }

    @Test
    void testPricingForP0FindsPositiveReducedCost() throws InvalidInstanceException {
        MKPInstance instance = buildInstance();
        PricingProblem pricing = new PricingProblem(instance);

        // Duals chosen so item 3,4 are attractive in P0
        double[] mu = {2.0, 2.0, 2.0, 0.2, -0.1};
        double[] pi = {0.5, 0.0, 0.0};
        double tau = 0.1;
        DualValues duals = new DualValues(mu, pi, tau);

        PricingResult result = pricing.solveForP0(duals);
        assertNotNull(result);
        assertTrue(result.isP0());
        assertTrue(result.getReducedCost() > 0.0);
        Pattern pattern = result.getPattern();
        assertFalse(pattern.isEmpty());
    }

    @Test
    void testPricingForPIKnapsackPattern() throws InvalidInstanceException {
        MKPInstance instance = buildInstance();
        PricingProblem pricing = new PricingProblem(instance);

        // Make item 0 appealing for knapsack 0
        double[] mu = {5.0, 0.0, 0.0, 0.0, 0.0};
        double[] pi = {0.0, 2.0, 0.0};
        DualValues duals = new DualValues(mu, pi, 0.0);

        PricingResult result = pricing.solveForPI(0, duals);
        assertNotNull(result);
        assertTrue(result.isPI());
        assertEquals(0, result.getPoolIndex());
        assertTrue(result.getReducedCost() > 0.0);
    }

    @Test
    void testNoImprovingPatterns() throws InvalidInstanceException {
        MKPInstance instance = buildInstance();
        PricingProblem pricing = new PricingProblem(instance);

        // Duals that should discourage adding any pattern (very penalizing)
        double[] mu = {50.0, 50.0, 50.0, 50.0, 50.0};
        double[] pi = {200.0, 200.0, 200.0};
        DualValues duals = new DualValues(mu, pi, 0.0);

        assertNull(pricing.solveForP0(duals));
        assertNull(pricing.solveForPI(0, duals));
        assertTrue(pricing.solveAll(duals).isEmpty());
    }
}
