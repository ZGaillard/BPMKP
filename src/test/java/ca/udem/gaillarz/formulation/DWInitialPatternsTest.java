package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DWInitialPatternsTest {

    @Test
    void seedCreatesBasicPatterns() throws InvalidInstanceException {
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

        // Expect at least empty + singletons + greedy per pool
        int expectedMin = 1 + instance.getNumItems() + 1; // empty + singles + greedy
        assertTrue(master.getPatternsP0().size() >= expectedMin);
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            assertTrue(master.getPatternsPI(i).size() >= expectedMin);
        }
    }
}
