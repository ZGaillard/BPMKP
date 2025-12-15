package solver;

import ca.udem.gaillarz.solver.lp.Variable;
import ca.udem.gaillarz.solver.lp.VariableType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VariableTest {

    @Test
    void testVariableCreation() {
        Variable v = new Variable("x", 0, 10, VariableType.CONTINUOUS);
        assertEquals("x", v.name());
        assertEquals(0.0, v.lowerBound());
        assertEquals(10.0, v.upperBound());
        assertEquals(VariableType.CONTINUOUS, v.type());
    }

    @Test
    void testBinaryVariable() {
        Variable v = new Variable("y", 0, 1, VariableType.BINARY);
        assertEquals(VariableType.BINARY, v.type());
        assertEquals(0.0, v.lowerBound());
        assertEquals(1.0, v.upperBound());
    }
}
