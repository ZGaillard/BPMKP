package solver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VariableTest {

    @Test
    void testVariableCreation() {
        Variable v = new Variable("x", 0, 10, VariableType.CONTINUOUS);
        assertEquals("x", v.getName());
        assertEquals(0.0, v.getLowerBound());
        assertEquals(10.0, v.getUpperBound());
        assertEquals(VariableType.CONTINUOUS, v.getType());
    }

    @Test
    void testBinaryVariable() {
        Variable v = new Variable("y", 0, 1, VariableType.BINARY);
        assertEquals(VariableType.BINARY, v.getType());
        assertEquals(0.0, v.getLowerBound());
        assertEquals(1.0, v.getUpperBound());
    }
}
