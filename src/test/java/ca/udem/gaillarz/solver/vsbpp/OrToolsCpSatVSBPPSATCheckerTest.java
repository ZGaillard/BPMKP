package ca.udem.gaillarz.solver.vsbpp;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OrToolsCpSatVSBPPSATCheckerTest {

    private MKPInstance instanceFeasible() throws InvalidInstanceException {
        String content = """
                2
                2
                6
                4
                6\t6
                4\t4
                """;
        return InstanceReader.parseFromString(content, "feasible");
    }

    private MKPInstance instanceInfeasibleSplit() throws InvalidInstanceException {
        String content = """
                2
                2
                6
                4
                5\t5
                5\t5
                """;
        return InstanceReader.parseFromString(content, "infeasible_split");
    }

    private MKPInstance instanceTooHeavy() throws InvalidInstanceException {
        String content = """
                2
                2
                5
                5
                6\t6
                1\t1
                """;
        return InstanceReader.parseFromString(content, "too_heavy");
    }

    @Test
    void testFeasibleSelection() throws InvalidInstanceException {
        MKPInstance instance = instanceFeasible();
        VSBPPSATChecker checker = new OrToolsCpSatVSBPPSATChecker();
        VSBPPSATResult result = checker.checkFeasibility(instance, Set.of(0, 1), 1000);

        assertTrue(result.isFeasible());
        int[] map = result.itemToBin();
        assertNotNull(map);
        assertEquals(0, map[0]);
        assertEquals(1, map[1]);
    }

    @Test
    void testInfeasibleSplitAcrossBins() throws InvalidInstanceException {
        MKPInstance instance = instanceInfeasibleSplit();
        VSBPPSATChecker checker = new OrToolsCpSatVSBPPSATChecker();
        VSBPPSATResult result = checker.checkFeasibility(instance, Set.of(0, 1), 1000);

        assertTrue(result.isInfeasible());
    }

    @Test
    void testItemTooHeavy() throws InvalidInstanceException {
        MKPInstance instance = instanceTooHeavy();
        VSBPPSATChecker checker = new OrToolsCpSatVSBPPSATChecker();
        VSBPPSATResult result = checker.checkFeasibility(instance, Set.of(0), 1000);

        assertTrue(result.isInfeasible());
    }
}
