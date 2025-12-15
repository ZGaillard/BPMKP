package solver;

import ca.udem.gaillarz.formulation.Pattern;
import ca.udem.gaillarz.io.InstanceReader;
import ca.udem.gaillarz.io.InvalidInstanceException;
import ca.udem.gaillarz.model.MKPInstance;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BranchNodeTest {

    private MKPInstance buildInstance() throws InvalidInstanceException {
        String content = """
                1
                3
                5
                2\t5
                2\t4
                1\t3
                """;
        return InstanceReader.parseFromString(content, "branch_node_test");
    }

    @Test
    void testNodeCreation() {
        BranchNode root = new BranchNode();
        assertTrue(root.isRoot());
        assertEquals(0, root.getDepth());
        assertTrue(root.getFixedItems().isEmpty());
    }

    @Test
    void testBranchingCreatesChildren() {
        BranchNode root = new BranchNode();
        BranchNode[] children = root.createChildren(0);

        assertEquals(2, children.length);
        assertEquals(0, children[0].getItemFixing(0));
        assertEquals(1, children[1].getItemFixing(0));
        assertEquals(root, children[0].getParent());
        assertEquals(root, children[1].getParent());
    }

    @Test
    void testPatternCompatibility() throws InvalidInstanceException {
        MKPInstance instance = buildInstance();
        BranchNode root = new BranchNode();
        BranchNode forbidItem1 = new BranchNode(root, 1, 0);

        Pattern p1 = Pattern.singleItem(0, instance);
        Pattern p2 = Pattern.fromItemIds(Set.of(1, 2), instance);

        assertTrue(forbidItem1.isPatternCompatible(p1));
        assertFalse(forbidItem1.isPatternCompatible(p2));
    }

    @Test
    void testBoundsAndPrune() {
        BranchNode node = new BranchNode();
        node.setUpperBound(10.0);
        assertTrue(node.canPrune(10.0));
        assertFalse(node.canPrune(5.0));
    }
}
