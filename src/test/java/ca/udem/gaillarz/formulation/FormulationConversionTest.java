package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for conversions between formulations.
 * Tests roundtrip: Classic → L2 → DW → L2 → Classic
 */
class FormulationConversionTest {

    private MKPInstance instance;
    private ClassicFormulation classicFormulation;
    private L2RelaxedFormulation l2Formulation;
    private DantzigWolfeMaster dwMaster;

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
        classicFormulation = new ClassicFormulation(instance);
        l2Formulation = classicFormulation.toL2Formulation();
        dwMaster = l2Formulation.toDantzigWolfeFormulation();
    }

    @Test
    void testClassicToL2Conversion() {
        // Create a classic solution
        ClassicSolution classic = new ClassicSolution(2, 5);
        classic.assignItem(0, 0);  // Item 0 to KS 0
        classic.assignItem(0, 4);  // Item 4 to KS 0
        classic.assignItem(1, 1);  // Item 1 to KS 1

        // Convert to L2
        L2Solution l2 = L2Solution.fromClassicSolution(classic, 5);

        // Verify t_j values
        assertEquals(1.0, l2.getItemSelection(0), 1e-5);
        assertEquals(1.0, l2.getItemSelection(1), 1e-5);
        assertEquals(0.0, l2.getItemSelection(2), 1e-5);
        assertEquals(0.0, l2.getItemSelection(3), 1e-5);
        assertEquals(1.0, l2.getItemSelection(4), 1e-5);

        // Verify x_ij values
        assertEquals(1.0, l2.getItemAssignment(0, 0), 1e-5);
        assertEquals(1.0, l2.getItemAssignment(0, 4), 1e-5);
        assertEquals(1.0, l2.getItemAssignment(1, 1), 1e-5);
        assertEquals(0.0, l2.getItemAssignment(0, 1), 1e-5);

        // Verify objectives match
        double classicObj = classicFormulation.computeObjectiveValue(classic);
        double l2Obj = l2Formulation.computeObjectiveValue(l2);
        assertEquals(classicObj, l2Obj, 1e-5);
    }

    @Test
    void testL2ToClassicConversion() {
        // Create integer L2 solution
        double[] t = {1.0, 1.0, 0.0, 0.0, 1.0};
        double[][] x = {
                {1.0, 0.0, 0.0, 0.0, 1.0},  // Items 0, 4 in KS 0
                {0.0, 1.0, 0.0, 0.0, 0.0}   // Item 1 in KS 1
        };
        L2Solution l2 = new L2Solution(t, x);

        // Convert to Classic
        ClassicSolution classic = l2.toClassicSolution();

        // Verify assignments
        assertTrue(classic.isItemInKnapsack(0, 0));
        assertTrue(classic.isItemInKnapsack(0, 4));
        assertTrue(classic.isItemInKnapsack(1, 1));
        assertFalse(classic.isItemInKnapsack(0, 1));

        // Verify objectives match
        double l2Obj = l2Formulation.computeObjectiveValue(l2);
        double classicObj = classicFormulation.computeObjectiveValue(classic);
        assertEquals(l2Obj, classicObj, 1e-5);
    }

    @Test
    void testL2ToClassicConversionFractional() {
        // Create fractional L2 solution
        double[] t = {0.5, 0.5, 0.0, 0.0, 0.0};
        double[][] x = {
                {0.5, 0.0, 0.0, 0.0, 0.0},
                {0.0, 0.5, 0.0, 0.0, 0.0}
        };
        L2Solution l2 = new L2Solution(t, x);

        // Should throw exception when converting fractional to classic
        assertThrows(IllegalStateException.class, l2::toClassicSolution);
    }

    @Test
    void testDWToL2Conversion() {
        // Set up patterns
        // P0: pattern with items {0, 4}
        Set<Integer> p0Items = new HashSet<>();
        p0Items.add(0);
        p0Items.add(4);
        Pattern p0 = Pattern.fromItemIds(p0Items, instance);
        dwMaster.addPatternP0(p0);

        // P1 (KS 0): pattern with items {0, 4}
        Pattern pi0 = Pattern.fromItemIds(p0Items, instance);
        dwMaster.addPatternPI(0, pi0);

        // P2 (KS 1): pattern with item {1}
        Set<Integer> pi1Items = new HashSet<>();
        pi1Items.add(1);
        Pattern pi1 = Pattern.fromItemIds(pi1Items, instance);
        dwMaster.addPatternPI(1, pi1);

        // Create DW solution
        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0), 1.0);
        patternValues.put(PatternVariable.forPI(pi0, 0), 1.0);
        patternValues.put(PatternVariable.forPI(pi1, 1), 1.0);

        DWSolution dw = new DWSolution(patternValues, 5);

        // Convert to L2
        L2Solution l2 = dwMaster.toL2Solution(dw);

        // Verify t_j values (derived from P0)
        assertEquals(1.0, l2.getItemSelection(0), 1e-5);  // Item 0 in P0
        assertEquals(0.0, l2.getItemSelection(1), 1e-5);  // Item 1 not in P0
        assertEquals(0.0, l2.getItemSelection(2), 1e-5);
        assertEquals(0.0, l2.getItemSelection(3), 1e-5);
        assertEquals(1.0, l2.getItemSelection(4), 1e-5);  // Item 4 in P0

        // Verify x_ij values (derived from PI)
        assertEquals(1.0, l2.getItemAssignment(0, 0), 1e-5);  // Item 0 in KS 0
        assertEquals(1.0, l2.getItemAssignment(0, 4), 1e-5);  // Item 4 in KS 0
        assertEquals(1.0, l2.getItemAssignment(1, 1), 1e-5);  // Item 1 in KS 1
    }

    @Test
    void testDWToClassicConversion() {
        // Set up patterns for a simple solution
        Pattern p0 = Pattern.singleItem(0, instance);
        dwMaster.addPatternP0(p0);

        Pattern pi0 = Pattern.singleItem(0, instance);
        dwMaster.addPatternPI(0, pi0);

        Pattern pi1 = Pattern.empty(5);
        dwMaster.addPatternPI(1, pi1);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0), 1.0);
        patternValues.put(PatternVariable.forPI(pi0, 0), 1.0);
        patternValues.put(PatternVariable.forPI(pi1, 1), 1.0);

        DWSolution dw = new DWSolution(patternValues, 5);

        // Convert DW → Classic
        ClassicSolution classic = dwMaster.toClassicSolution(dw);

        // Verify
        assertTrue(classic.isItemInKnapsack(0, 0));
        assertFalse(classic.isItemAssigned(1));
    }

    @Test
    void testRoundtripClassicL2Classic() {
        // Start with Classic solution
        ClassicSolution original = new ClassicSolution(2, 5);
        original.assignItem(0, 0);
        original.assignItem(0, 3);  // Items 0, 3 in KS 0 (w=5+2=7)
        original.assignItem(1, 1);
        original.assignItem(1, 4);  // Items 1, 4 in KS 1 (w=4+1=5)

        // Classic → L2
        L2Solution l2 = L2Solution.fromClassicSolution(original, 5);

        // L2 → Classic
        ClassicSolution recovered = l2.toClassicSolution();

        // Verify same assignments
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                assertEquals(original.isItemInKnapsack(i, j),
                           recovered.isItemInKnapsack(i, j),
                           String.format("Mismatch at i=%d, j=%d", i, j));
            }
        }

        // Verify objectives
        double originalObj = classicFormulation.computeObjectiveValue(original);
        double recoveredObj = classicFormulation.computeObjectiveValue(recovered);
        assertEquals(originalObj, recoveredObj, 1e-5);
    }

    @Test
    void testObjectiveConsistencyAcrossFormulations() {
        // Create equivalent solutions in all three formulations

        // Classic solution
        ClassicSolution classic = new ClassicSolution(2, 5);
        classic.assignItem(0, 0);  // Item 0 in KS 0
        classic.assignItem(1, 1);  // Item 1 in KS 1

        // L2 solution (derived from classic)
        L2Solution l2 = L2Solution.fromClassicSolution(classic, 5);

        // Compute objectives
        double classicObj = classicFormulation.computeObjectiveValue(classic);
        double l2Obj = l2Formulation.computeObjectiveValue(l2);

        // They should match
        assertEquals(classicObj, l2Obj, 1e-5);

        // Now create DW formulation and verify
        Pattern p0Pattern = Pattern.fromItemIds(Set.of(0, 1), instance);  // t values
        dwMaster.addPatternP0(p0Pattern);

        Pattern p0_ks0 = Pattern.singleItem(0, instance);
        Pattern p0_ks1 = Pattern.singleItem(1, instance);
        dwMaster.addPatternPI(0, p0_ks0);
        dwMaster.addPatternPI(1, p0_ks1);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0Pattern), 1.0);
        patternValues.put(PatternVariable.forPI(p0_ks0, 0), 1.0);
        patternValues.put(PatternVariable.forPI(p0_ks1, 1), 1.0);

        DWSolution dw = new DWSolution(patternValues, 5);

        // DW objective should match
        double dwObj = dwMaster.computeObjectiveValue(dw);
        assertEquals(l2Obj, dwObj, 1e-5);
    }

    @Test
    void testFeasibilityPreservation() {
        // Create a feasible classic solution
        ClassicSolution classic = new ClassicSolution(2, 5);
        classic.assignItem(0, 0);  // w=5 in KS 0 (cap=7) ✓
        classic.assignItem(1, 1);  // w=4 in KS 1 (cap=6) ✓

        assertTrue(classicFormulation.isFeasible(classic));

        // Convert to L2
        L2Solution l2 = L2Solution.fromClassicSolution(classic, 5);
        assertTrue(l2Formulation.isFeasible(l2));

        // Convert back
        ClassicSolution recovered = l2.toClassicSolution();
        assertTrue(classicFormulation.isFeasible(recovered));
    }

    @Test
    void testIntegralityCheck() {
        // Integer L2 solution
        double[] tInt = {1.0, 0.0, 1.0, 0.0, 0.0};
        double[][] xInt = {
                {1.0, 0.0, 1.0, 0.0, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0}
        };
        L2Solution l2Int = new L2Solution(tInt, xInt);
        assertTrue(l2Int.isInteger());
        assertTrue(l2Int.areItemSelectionsInteger());
        assertTrue(l2Int.areAssignmentsInteger());

        // Fractional L2 solution
        double[] tFrac = {0.5, 0.5, 0.0, 0.0, 0.0};
        double[][] xFrac = {
                {0.5, 0.0, 0.0, 0.0, 0.0},
                {0.0, 0.5, 0.0, 0.0, 0.0}
        };
        L2Solution l2Frac = new L2Solution(tFrac, xFrac);
        assertFalse(l2Frac.isInteger());
        assertFalse(l2Frac.areItemSelectionsInteger());
        assertFalse(l2Frac.areAssignmentsInteger());
    }

    @Test
    void testFractionalDWSolution() {
        Pattern p1 = Pattern.singleItem(0, instance);
        Pattern p2 = Pattern.singleItem(1, instance);
        dwMaster.addPatternP0(p1);
        dwMaster.addPatternP0(p2);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p1), 0.6);
        patternValues.put(PatternVariable.forP0(p2), 0.4);  // Sum = 1, but fractional

        DWSolution dw = new DWSolution(patternValues, 5);

        // Convert to L2
        L2Solution l2 = dwMaster.toL2Solution(dw);

        // Should have fractional t values
        assertEquals(0.6, l2.getItemSelection(0), 1e-5);
        assertEquals(0.4, l2.getItemSelection(1), 1e-5);
        assertFalse(l2.isInteger());
    }
}
