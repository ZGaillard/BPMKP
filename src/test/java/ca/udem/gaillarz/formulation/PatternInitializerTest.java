package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PatternInitializerTest {

    @Test
    void testInitializeMaster() throws IOException, InvalidInstanceException {
        MKPInstance instance = InstanceReader.parseFromString("""
                2
                5
                10
                8
                5 10
                4 8
                3 6
                7 14
                2 3
                """, "integration");

        L2RelaxedFormulation l2 = new L2RelaxedFormulation(instance);
        DantzigWolfeMaster master = new DantzigWolfeMaster(l2);

        InitializationResult result = PatternInitializer.initialize(master);

        assertTrue(result.getPatternsP0() > 0);
        assertTrue(result.getPatternsPI() > 0);
        assertEquals(master.getTotalPatternCount(), result.getTotalPatterns());

        for (Pattern p : master.getPatternsP0()) {
            assertTrue(p.isFeasible(instance.getTotalCapacity()));
        }
        for (int i = 0; i < instance.getNumKnapsacks(); i++) {
            int capacity = instance.getKnapsack(i).getCapacity();
            for (Pattern p : master.getPatternsPI(i)) {
                assertTrue(p.isFeasible(capacity));
            }
        }
    }
}
