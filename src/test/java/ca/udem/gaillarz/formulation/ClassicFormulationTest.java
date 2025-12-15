package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClassicFormulation.
 */
class ClassicFormulationTest {

    private MKPInstance instance;
    private ClassicFormulation formulation;

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
        formulation = new ClassicFormulation(instance);
    }

    @Test
    void testEmptySolution() {
        ClassicSolution solution = new ClassicSolution(2, 5);

        assertEquals(0.0, formulation.computeObjectiveValue(solution));
        assertTrue(formulation.isFeasible(solution));
        assertTrue(formulation.checkCapacityConstraints(solution));
        assertTrue(formulation.checkAssignmentConstraints(solution));
    }

    @Test
    void testSingleItemAssignment() {
        ClassicSolution solution = new ClassicSolution(2, 5);
        solution.assignItem(0, 0);  // Assign item 0 to knapsack 0

        assertEquals(10.0, formulation.computeObjectiveValue(solution));
        assertTrue(formulation.isFeasible(solution));
        assertEquals(5, formulation.getKnapsackWeight(solution, 0));
        assertEquals(0, formulation.getKnapsackWeight(solution, 1));
    }

    @Test
    void testMultipleItemsAssignment() {
        ClassicSolution solution = new ClassicSolution(2, 5);
        solution.assignItem(0, 0);  // Item 0 (w=5, p=10) to KS 0
        solution.assignItem(0, 4);  // Item 4 (w=1, p=4) to KS 0
        solution.assignItem(1, 1);  // Item 1 (w=4, p=8) to KS 1
        solution.assignItem(1, 3);  // Item 3 (w=2, p=5) to KS 1

        assertEquals(27.0, formulation.computeObjectiveValue(solution));
        assertTrue(formulation.isFeasible(solution));

        assertEquals(6, formulation.getKnapsackWeight(solution, 0));  // 5 + 1
        assertEquals(6, formulation.getKnapsackWeight(solution, 1));  // 4 + 2
    }

    @Test
    void testCapacityViolation() {
        ClassicSolution solution = new ClassicSolution(2, 5);
        solution.assignItem(0, 0);  // Item 0 (w=5) to KS 0 (cap=7)
        solution.assignItem(0, 1);  // Item 1 (w=4) to KS 0 - total = 9 > 7

        assertFalse(formulation.isFeasible(solution));
        assertFalse(formulation.checkCapacityConstraints(solution));
        assertTrue(formulation.checkAssignmentConstraints(solution));
    }

    @Test
    void testGetItemsInKnapsack() {
        ClassicSolution solution = new ClassicSolution(2, 5);
        solution.assignItem(0, 0);
        solution.assignItem(0, 2);
        solution.assignItem(0, 4);

        Set<Integer> items = formulation.getItemsInKnapsack(solution, 0);
        assertEquals(3, items.size());
        assertTrue(items.contains(0));
        assertTrue(items.contains(2));
        assertTrue(items.contains(4));

        Set<Integer> items1 = formulation.getItemsInKnapsack(solution, 1);
        assertTrue(items1.isEmpty());
    }

    @Test
    void testConversionToL2() {
        L2RelaxedFormulation l2 = formulation.toL2Formulation();

        assertNotNull(l2);
        assertEquals(instance, l2.getInstance());
        assertEquals(13, l2.getTotalCapacity());  // 7 + 6
    }

    @Test
    void testMathematicalString() {
        String mathStr = formulation.toMathematicalString();

        assertNotNull(mathStr);
        assertTrue(mathStr.contains("Classic MKP Formulation"));
        assertTrue(mathStr.contains("max"));
        assertTrue(mathStr.contains("s.t."));
    }

    @Test
    void testVisualizeSolution() {
        ClassicSolution solution = new ClassicSolution(2, 5);
        solution.assignItem(0, 0);
        solution.assignItem(1, 1);

        String viz = formulation.visualizeSolution(solution);

        assertNotNull(viz);
        assertTrue(viz.contains("Objective value"));
        assertTrue(viz.contains("Feasible"));
    }

    @Test
    void testDimensionMismatch() {
        ClassicSolution wrongSolution = new ClassicSolution(3, 5);  // Wrong number of knapsacks

        assertThrows(FormulationException.class, () ->
            formulation.computeObjectiveValue(wrongSolution));
    }

    @Test
    void testToString() {
        String str = formulation.toString();
        assertTrue(str.contains("ClassicMKP"));
        assertTrue(str.contains("n=5"));
        assertTrue(str.contains("m=2"));
    }
}

