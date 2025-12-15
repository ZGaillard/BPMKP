package preprocessing;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.Knapsack;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.preprocessing.ReducedCostFixer;
import ca.udem.gaillarz.solver.bp.BranchNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PreprocessingIntegrationTest {

    private MKPInstance createTestInstance() {
        List<Item> items = List.of(
                new Item(0, 2, 5),
                new Item(1, 3, 6),
                new Item(2, 4, 8)
        );
        List<Knapsack> knapsacks = List.of(new Knapsack(0, 10));
        return new MKPInstance(items, knapsacks, "rc_integration_test");
    }

    @Test
    void testFixingsAppliedToBranchNodes() {
        MKPInstance instance = createTestInstance();
        ReducedCostFixer fixer = new ReducedCostFixer(instance);

        double[] reducedCosts = {60.0, -80.0, 10.0};
        int fixed = fixer.fixByReducedCosts(100.0, 50.0, reducedCosts);
        assertEquals(2, fixed);

        Map<Integer, Integer> fixings = fixer.getFixedItems();
        BranchNode root = new BranchNode();
        root.addFixings(fixings);

        assertTrue(root.isItemFixed(0));
        assertTrue(root.isItemFixed(1));
        assertEquals(Set.of(0), root.getForbiddenItems());
        assertEquals(Set.of(1), root.getRequiredItems());

        BranchNode child = new BranchNode(root, 2, 0);
        assertTrue(child.isItemFixed(0));
        assertTrue(child.isItemFixed(1));
        assertEquals(0, child.getItemFixing(0));
        assertEquals(1, child.getItemFixing(1));
        assertEquals(0, child.getItemFixing(2));
    }
}
