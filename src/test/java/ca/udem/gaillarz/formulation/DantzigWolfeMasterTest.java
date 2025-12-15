package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DantzigWolfeMaster.
 */
class DantzigWolfeMasterTest {

    private MKPInstance instance;
    private L2RelaxedFormulation l2;
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
        l2 = new L2RelaxedFormulation(instance);
        dwMaster = new DantzigWolfeMaster(l2);
    }

    @Test
    void testInitialState() {
        assertEquals(0, dwMaster.getPatternsP0().size());
        assertEquals(0, dwMaster.getPatternsPI(0).size());
        assertEquals(0, dwMaster.getPatternsPI(1).size());
        assertEquals(0, dwMaster.getTotalPatternCount());
    }

    @Test
    void testAddPatternP0() {
        Pattern p = Pattern.singleItem(0, instance);
        dwMaster.addPatternP0(p);

        assertEquals(1, dwMaster.getPatternsP0().size());
        assertTrue(dwMaster.getPatternsP0().contains(p));
    }

    @Test
    void testAddPatternPI() {
        Pattern p = Pattern.singleItem(0, instance);
        dwMaster.addPatternPI(0, p);

        assertEquals(1, dwMaster.getPatternsPI(0).size());
        assertEquals(0, dwMaster.getPatternsPI(1).size());
        assertTrue(dwMaster.getPatternsPI(0).contains(p));
    }

    @Test
    void testAddDuplicatePattern() {
        Pattern p1 = Pattern.singleItem(0, instance);
        Pattern p2 = Pattern.singleItem(0, instance);

        dwMaster.addPatternP0(p1);
        dwMaster.addPatternP0(p2);  // Duplicate - should not add

        // Only one pattern should be in P0 (they are equal by content)
        assertEquals(1, dwMaster.getPatternsP0().size());
    }

    @Test
    void testAddInfeasiblePatternP0() {
        // Create pattern that exceeds total capacity (13)
        boolean[] items = {true, true, true, true, true};  // Weight = 15
        Pattern p = new Pattern(items, instance);

        assertThrows(FormulationException.class, () -> dwMaster.addPatternP0(p));
    }

    @Test
    void testAddInfeasiblePatternPI() {
        // Create pattern that exceeds KS 0 capacity (7)
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(0);  // w = 5
        itemIds.add(1);  // w = 4
        Pattern p = Pattern.fromItemIds(itemIds, instance);  // Total w = 9 > 7

        assertThrows(FormulationException.class, () -> dwMaster.addPatternPI(0, p));
    }

    @Test
    void testRemovePattern() {
        Pattern p = Pattern.singleItem(0, instance);
        dwMaster.addPatternP0(p);

        assertTrue(dwMaster.removePatternP0(p));
        assertEquals(0, dwMaster.getPatternsP0().size());
    }

    @Test
    void testClearAllPatterns() {
        dwMaster.addPatternP0(Pattern.singleItem(0, instance));
        dwMaster.addPatternP0(Pattern.singleItem(1, instance));
        dwMaster.addPatternPI(0, Pattern.singleItem(0, instance));
        dwMaster.addPatternPI(1, Pattern.singleItem(1, instance));

        dwMaster.clearAllPatterns();

        assertEquals(0, dwMaster.getTotalPatternCount());
    }

    @Test
    void testUpperBound() {
        assertEquals(33, dwMaster.getUpperBound(), 1e-5);  // Total profit

        dwMaster.setUpperBound(25.0);
        assertEquals(25.0, dwMaster.getUpperBound(), 1e-5);
    }

    @Test
    void testComputeObjectiveValue() {
        // Add pattern to P0
        Set<Integer> itemIds = new HashSet<>();
        itemIds.add(0);
        itemIds.add(2);
        Pattern p0 = Pattern.fromItemIds(itemIds, instance);  // profit = 16
        dwMaster.addPatternP0(p0);

        // Create solution with y_a = 1 for this pattern
        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0), 1.0);

        Map<Integer, Double> dualCuts = new HashMap<>();
        for (int j = 0; j < 5; j++) dualCuts.put(j, 0.0);

        DWSolution solution = new DWSolution(patternValues, dualCuts);

        // Objective = profit of pattern = 10 + 6 = 16
        assertEquals(16.0, dwMaster.computeObjectiveValue(solution), 1e-5);
    }

    @Test
    void testComputeObjectiveWithDualCuts() {
        Pattern p0 = Pattern.singleItem(0, instance);  // profit = 10
        dwMaster.addPatternP0(p0);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0), 1.0);

        Map<Integer, Double> dualCuts = new HashMap<>();
        dualCuts.put(0, 0.5);  // s_0 = 0.5
        for (int j = 1; j < 5; j++) dualCuts.put(j, 0.0);

        DWSolution solution = new DWSolution(patternValues, dualCuts);

        // Objective = 10 - p_0 * s_0 = 10 - 10 * 0.5 = 5
        assertEquals(5.0, dwMaster.computeObjectiveValue(solution), 1e-5);
    }

    @Test
    void testComputeItemSelection() {
        Pattern p1 = Pattern.singleItem(0, instance);
        Pattern p2 = Pattern.singleItem(1, instance);
        dwMaster.addPatternP0(p1);
        dwMaster.addPatternP0(p2);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p1), 0.6);
        patternValues.put(PatternVariable.forP0(p2), 0.4);

        DWSolution solution = new DWSolution(patternValues, 5);

        // t_0 = y_{p1} = 0.6 (since only p1 contains item 0)
        assertEquals(0.6, dwMaster.computeItemSelection(solution, 0), 1e-5);
        // t_1 = y_{p2} = 0.4 (since only p2 contains item 1)
        assertEquals(0.4, dwMaster.computeItemSelection(solution, 1), 1e-5);
        // t_2 = 0 (no pattern contains item 2)
        assertEquals(0.0, dwMaster.computeItemSelection(solution, 2), 1e-5);
    }

    @Test
    void testComputeItemAssignment() {
        Pattern p = Pattern.singleItem(0, instance);
        dwMaster.addPatternPI(0, p);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forPI(p, 0), 1.0);

        DWSolution solution = new DWSolution(patternValues, 5);

        // x_00 = 1 (item 0 in knapsack 0)
        assertEquals(1.0, dwMaster.computeItemAssignment(solution, 0, 0), 1e-5);
        // x_10 = 0 (item 0 not in knapsack 1)
        assertEquals(0.0, dwMaster.computeItemAssignment(solution, 1, 0), 1e-5);
    }

    @Test
    void testToL2Solution() {
        // Add patterns
        Pattern p0 = Pattern.singleItem(0, instance);
        Pattern pi0 = Pattern.singleItem(0, instance);
        Pattern pi1 = Pattern.singleItem(1, instance);

        dwMaster.addPatternP0(p0);
        dwMaster.addPatternPI(0, pi0);
        dwMaster.addPatternPI(1, pi1);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0), 1.0);
        patternValues.put(PatternVariable.forPI(pi0, 0), 1.0);
        patternValues.put(PatternVariable.forPI(pi1, 1), 0.0);

        DWSolution dwSolution = new DWSolution(patternValues, 5);

        L2Solution l2Solution = dwMaster.toL2Solution(dwSolution);

        assertNotNull(l2Solution);
        assertEquals(1.0, l2Solution.getItemSelection(0), 1e-5);  // t_0 = 1
        assertEquals(0.0, l2Solution.getItemSelection(1), 1e-5);  // t_1 = 0
        assertEquals(1.0, l2Solution.getItemAssignment(0, 0), 1e-5);  // x_00 = 1
    }

    @Test
    void testToClassicSolutionFromInteger() {
        // Add patterns for a complete integer solution
        Pattern p0 = Pattern.singleItem(0, instance);
        Pattern pi0 = Pattern.singleItem(0, instance);
        Pattern pi1 = Pattern.empty(5);

        dwMaster.addPatternP0(p0);
        dwMaster.addPatternPI(0, pi0);
        dwMaster.addPatternPI(1, pi1);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0), 1.0);
        patternValues.put(PatternVariable.forPI(pi0, 0), 1.0);
        patternValues.put(PatternVariable.forPI(pi1, 1), 1.0);

        DWSolution dwSolution = new DWSolution(patternValues, 5);

        ClassicSolution classic = dwMaster.toClassicSolution(dwSolution);

        assertNotNull(classic);
        assertTrue(classic.isItemInKnapsack(0, 0));  // Item 0 in KS 0
    }

    @Test
    void testToClassicSolutionFromFractional() {
        Pattern p0 = Pattern.singleItem(0, instance);
        dwMaster.addPatternP0(p0);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0), 0.5);  // Fractional

        DWSolution dwSolution = new DWSolution(patternValues, 5);

        assertThrows(FormulationException.class, () ->
                dwMaster.toClassicSolution(dwSolution));
    }

    @Test
    void testCheckPatternSelection() {
        Pattern p0 = Pattern.singleItem(0, instance);
        Pattern pi0 = Pattern.singleItem(0, instance);
        Pattern pi1 = Pattern.singleItem(1, instance);

        dwMaster.addPatternP0(p0);
        dwMaster.addPatternPI(0, pi0);
        dwMaster.addPatternPI(1, pi1);

        // Valid: sum = 1 for each pool
        Map<PatternVariable, Double> validValues = new HashMap<>();
        validValues.put(PatternVariable.forP0(p0), 1.0);
        validValues.put(PatternVariable.forPI(pi0, 0), 1.0);
        validValues.put(PatternVariable.forPI(pi1, 1), 1.0);
        DWSolution validSolution = new DWSolution(validValues, 5);
        assertTrue(dwMaster.checkPatternSelection(validSolution));

        // Invalid: sum != 1 for P0
        Map<PatternVariable, Double> invalidValues = new HashMap<>();
        invalidValues.put(PatternVariable.forP0(p0), 0.5);
        invalidValues.put(PatternVariable.forPI(pi0, 0), 1.0);
        invalidValues.put(PatternVariable.forPI(pi1, 1), 1.0);
        DWSolution invalidSolution = new DWSolution(invalidValues, 5);
        assertFalse(dwMaster.checkPatternSelection(invalidSolution));
    }

    @Test
    void testToString() {
        String str = dwMaster.toString();
        assertTrue(str.contains("DWMKP"));
        assertTrue(str.contains("n=5"));
        assertTrue(str.contains("m=2"));
    }

    @Test
    void testToStructureString() {
        dwMaster.addPatternP0(Pattern.singleItem(0, instance));
        dwMaster.addPatternPI(0, Pattern.singleItem(0, instance));

        String structure = dwMaster.toStructureString();

        assertNotNull(structure);
        assertTrue(structure.contains("Dantzig-Wolfe"));
        assertTrue(structure.contains("Pattern Pools"));
        assertTrue(structure.contains("P_0:"));
    }

    @Test
    void testToPatternsString() {
        dwMaster.addPatternP0(Pattern.singleItem(0, instance));
        dwMaster.addPatternP0(Pattern.singleItem(1, instance));

        String patterns = dwMaster.toPatternsString();

        assertNotNull(patterns);
        assertTrue(patterns.contains("Pattern Pool P_0"));
    }

    @Test
    void testVisualizeSolution() {
        Pattern p0 = Pattern.singleItem(0, instance);
        dwMaster.addPatternP0(p0);

        Map<PatternVariable, Double> patternValues = new HashMap<>();
        patternValues.put(PatternVariable.forP0(p0), 1.0);

        DWSolution solution = new DWSolution(patternValues, 5);

        String viz = dwMaster.visualizeSolution(solution);

        assertNotNull(viz);
        assertTrue(viz.contains("Objective value"));
    }
}
