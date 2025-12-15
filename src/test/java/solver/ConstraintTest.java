package solver;

import ca.udem.gaillarz.solver.lp.Constraint;
import ca.udem.gaillarz.solver.lp.ConstraintSense;
import ca.udem.gaillarz.solver.lp.Variable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintTest {

    @Test
    void testConstraintEvaluation() {
        Constraint c = new Constraint("c1", ConstraintSense.LE, 10);
        Variable x = new Variable("x");
        Variable y = new Variable("y");

        c.addTerm(x, 2.0);  // 2x + 3y ≤ 10
        c.addTerm(y, 3.0);

        Map<Variable, Double> values = new HashMap<>();
        values.put(x, 2.0);
        values.put(y, 1.0);

        assertEquals(7.0, c.evaluate(values), 1e-6);
        assertTrue(c.isSatisfied(values, 1e-6));
    }
}
