package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for L2RelaxedFormulation.
 */
class L2RelaxedFormulationTest {

    private MKPInstance instance;
    private L2RelaxedFormulation formulation;

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
        formulation = new L2RelaxedFormulation(instance);
    }

    @Test
    void testTotalCapacity() {
        assertEquals(13, formulation.getTotalCapacity());
    }

    @Test
    void testObjectiveValue() {
        // Create a solution with t[0]=1, t[1]=1, t[2]=0, t[3]=0, t[4]=0
        double[] t = {1.0, 1.0, 0.0, 0.0, 0.0};
        double[][] x = {
                {1.0, 0.0, 0.0, 0.0, 0.0},  // Item 0 in knapsack 0
                {0.0, 1.0, 0.0, 0.0, 0.0}   // Item 1 in knapsack 1
        };
        L2Solution solution = new L2Solution(t, x);

        // Objective = Σ p_j * t_j = 10*1 + 8*1 = 18
        assertEquals(18.0, formulation.computeObjectiveValue(solution), 1e-5);
    }

    @Test
    void testFeasibleSolution() {
        // Feasible solution
        double[] t = {1.0, 1.0, 0.0, 0.0, 0.0};
        double[][] x = {
                {1.0, 0.0, 0.0, 0.0, 0.0},  // Item 0 (w=5) in KS 0 (cap=7)
                {0.0, 1.0, 0.0, 0.0, 0.0}   // Item 1 (w=4) in KS 1 (cap=6)
        };
        L2Solution solution = new L2Solution(t, x);

        assertTrue(formulation.isFeasible(solution));
        assertTrue(formulation.checkKnapsackCapacities(solution));
        assertTrue(formulation.checkLinkingConstraints(solution));
        assertTrue(formulation.checkAggregatedCapacity(solution));
    }

    @Test
    void testKnapsackCapacityViolation() {
        // KS 0 capacity violated: 5 + 4 = 9 > 7
        double[] t = {1.0, 1.0, 0.0, 0.0, 0.0};
        double[][] x = {
                {1.0, 1.0, 0.0, 0.0, 0.0},  // Items 0,1 in KS 0
                {0.0, 0.0, 0.0, 0.0, 0.0}
        };
        L2Solution solution = new L2Solution(t, x);

        assertFalse(formulation.checkKnapsackCapacities(solution));
        assertFalse(formulation.isFeasible(solution));
    }

    @Test
    void testLinkingConstraintViolation() {
        // t[0] = 1 but item 0 is not assigned anywhere
        double[] t = {1.0, 0.0, 0.0, 0.0, 0.0};
        double[][] x = {
                {0.0, 0.0, 0.0, 0.0, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0}
        };
        L2Solution solution = new L2Solution(t, x);

        assertFalse(formulation.checkLinkingConstraints(solution));
        assertFalse(formulation.isFeasible(solution));
    }

    @Test
    void testAggregatedCapacityViolation() {
        // Total weight of selected items exceeds total capacity
        // All items: 5+4+3+2+1 = 15 > 13
        double[] t = {1.0, 1.0, 1.0, 1.0, 1.0};
        double[][] x = {
                {1.0, 1.0, 0.0, 0.0, 0.0},  // w = 9
                {0.0, 0.0, 1.0, 1.0, 1.0}   // w = 6
        };
        L2Solution solution = new L2Solution(t, x);

        assertFalse(formulation.checkAggregatedCapacity(solution));
    }

    @Test
    void testLagrangianObjective() {
        double[] t = {1.0, 1.0, 0.0, 0.0, 0.0};
        double[][] x = {
                {1.0, 0.0, 0.0, 0.0, 0.0},
                {0.0, 1.0, 0.0, 0.0, 0.0}
        };
        L2Solution solution = new L2Solution(t, x);

        // μ = [2, 1, 0, 0, 0]
        double[] mu = {2.0, 1.0, 0.0, 0.0, 0.0};

        // Lagrangian = Σ_j (p_j - μ_j) * t_j + Σ_j μ_j * (Σ_i x_ij)
        // = (10-2)*1 + (8-1)*1 + 0 + 0 + 0 + 2*1 + 1*1 + 0 + 0 + 0
        // = 8 + 7 + 2 + 1 = 18
        double expected = (10 - 2) * 1.0 + (8 - 1) * 1.0 + 2.0 * 1.0 + 1.0 * 1.0;
        assertEquals(expected, formulation.computeLagrangianObjective(solution, mu), 1e-5);
    }

    @Test
    void testFractionalSolution() {
        // Fractional solution
        double[] t = {0.5, 0.5, 0.0, 0.0, 0.0};
        double[][] x = {
                {0.5, 0.0, 0.0, 0.0, 0.0},
                {0.0, 0.5, 0.0, 0.0, 0.0}
        };
        L2Solution solution = new L2Solution(t, x);

        // Still feasible (just fractional)
        assertTrue(formulation.isFeasible(solution));
        assertEquals(9.0, formulation.computeObjectiveValue(solution), 1e-5);  // 10*0.5 + 8*0.5
    }

    @Test
    void testConversionToDantzigWolfe() {
        DantzigWolfeMaster dw = formulation.toDantzigWolfeFormulation();

        assertNotNull(dw);
        assertEquals(instance, dw.getInstance());
        assertEquals(formulation, dw.getL2Formulation());
    }

    @Test
    void testConversionBackToClassic() {
        ClassicFormulation classic = formulation.toClassicFormulation();

        assertNotNull(classic);
        assertEquals(instance, classic.instance());
    }

    @Test
    void testMathematicalString() {
        String mathStr = formulation.toMathematicalString();

        assertNotNull(mathStr);
        assertTrue(mathStr.contains("L2 Relaxed Formulation"));
        assertTrue(mathStr.contains("max"));
        assertTrue(mathStr.contains("Linking constraints"));
        assertTrue(mathStr.contains("Aggregated capacity"));
    }

    @Test
    void testLagrangianString() {
        double[] mu = {1.0, 1.0, 1.0, 1.0, 1.0};
        String lagStr = formulation.toLagrangianString(mu);

        assertNotNull(lagStr);
        assertTrue(lagStr.contains("Lagrangian"));
        assertTrue(lagStr.contains("μ ="));
    }

    @Test
    void testVisualizeSolution() {
        double[] t = {1.0, 1.0, 0.0, 0.0, 0.0};
        double[][] x = {
                {1.0, 0.0, 0.0, 0.0, 0.0},
                {0.0, 1.0, 0.0, 0.0, 0.0}
        };
        L2Solution solution = new L2Solution(t, x);

        String viz = formulation.visualizeSolution(solution);

        assertNotNull(viz);
        assertTrue(viz.contains("Objective value"));
        assertTrue(viz.contains("Feasible"));
    }

    @Test
    void testToString() {
        String str = formulation.toString();
        assertTrue(str.contains("L2RelaxedMKP"));
        assertTrue(str.contains("total_cap=13"));
    }
}

