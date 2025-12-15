package solver;

import ca.udem.gaillarz.formulation.L2Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BranchingRuleTest {

    @Test
    void testMostFractionalSelection() {
        double[] t = {0.0, 0.3, 0.51, 0.8, 1.0};
        double[][] x = {
                {0.0, 0.0, 0.51, 0.0, 0.0}
        };
        L2Solution l2 = new L2Solution(t, x);

        BranchingRule rule = new BranchingRule(t.length, BranchingStrategy.MOST_FRACTIONAL);
        int item = rule.selectBranchItem(l2, new BranchNode());
        assertEquals(2, item); // 0.51 is closest to 0.5 among candidates
    }
}
