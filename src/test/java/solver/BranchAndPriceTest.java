package solver;

import ca.udem.gaillarz.formulation.ClassicSolution;
import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.bp.BPResult;
import ca.udem.gaillarz.solver.bp.BranchAndPrice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BranchAndPriceTest {

    private MKPInstance smallInstance() throws InvalidInstanceException {
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
        return InstanceReader.parseFromString(content, "bp_test");
    }

    @Test
    void testSolveSmallInstance() throws InvalidInstanceException {
        MKPInstance instance = smallInstance();
        BranchAndPrice solver = new BranchAndPrice(instance)
                .setVerbose(false)
                .setMaxNodes(50)
                .setGapTolerance(0.02);

        BPResult result = solver.solve();

        assertTrue(result.hasSolution());
        assertTrue(result.gap() <= 0.05);

        ClassicSolution sol = result.solution();
        assertNotNull(sol);
        // Known optimal for this small instance is 28 (from demo/CG run)
        assertEquals(28.0, result.objectiveValue(), 1e-5);
    }
}
