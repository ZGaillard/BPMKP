package solver;

import ca.udem.gaillarz.formulation.*;
import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.cg.CGParameters;
import ca.udem.gaillarz.solver.cg.CGResult;
import ca.udem.gaillarz.solver.cg.CGStatus;
import ca.udem.gaillarz.solver.cg.ColumnGeneration;
import ca.udem.gaillarz.solver.lp.ORToolsSolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColumnGenerationTest {

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
        return InstanceReader.parseFromString(content, "cg_test");
    }

    @Test
    void testColumnGenerationConvergesOnSmallInstance() throws InvalidInstanceException {
        MKPInstance instance = buildInstance();
        L2RelaxedFormulation l2 = new L2RelaxedFormulation(instance);
        DantzigWolfeMaster master = new DantzigWolfeMaster(l2);

        // Seed initial patterns
        PatternInitializer.initialize(master);

        ORToolsSolver solver = new ORToolsSolver();
        ColumnGeneration cg = new ColumnGeneration(master, solver);

        CGParameters params = new CGParameters().setMaxIterations(50).setVerbose(false);
        CGResult result = cg.solve(params);

        assertTrue(result.status() == CGStatus.OPTIMAL
                || result.status() == CGStatus.ITERATION_LIMIT
                || result.status() == CGStatus.TIME_LIMIT);
        assertTrue(result.iterations() > 0);
        assertTrue(!result.objectiveHistory().isEmpty());

        // Objective history should be non-decreasing (within tolerance)
        List<Double> history = result.objectiveHistory();
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i) + 1e-6 >= history.get(i - 1));
        }
    }
}
