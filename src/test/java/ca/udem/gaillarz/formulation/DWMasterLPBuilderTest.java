package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.Test;
import ca.udem.gaillarz.solver.lp.LPSolution;
import ca.udem.gaillarz.solver.lp.ORToolsSolver;
import ca.udem.gaillarz.solver.lp.SolutionStatus;

import static org.junit.jupiter.api.Assertions.*;

class DWMasterLPBuilderTest {

    @Test
    void testBuildAndSolveDWMasterLP() throws InvalidInstanceException {
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
        MKPInstance instance = InstanceReader.parseFromString(content, "test");
        DantzigWolfeMaster master = new DantzigWolfeMaster(new L2RelaxedFormulation(instance));
        master.seedInitialPatterns();

        DWMasterLPBuilder builder = new DWMasterLPBuilder(master);
        var lp = builder.buildLP();

        ORToolsSolver solver = new ORToolsSolver();
        LPSolution lpSol = solver.solve(lp);

        assertTrue(lpSol.status() == SolutionStatus.OPTIMAL || lpSol.status() == SolutionStatus.FEASIBLE);

        DWSolution dwSol = builder.extractDWSolution(lpSol, lp);
        assertNotNull(dwSol);
        DualValues duals = builder.extractDualValues(lpSol, lp);
        assertNotNull(duals);
        assertEquals(instance.getNumItems(), duals.mu().length);
        assertEquals(instance.getNumKnapsacks() + 1, duals.pi().length);
    }
}
