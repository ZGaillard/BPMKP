package solver;

import ca.udem.gaillarz.solver.lp.Constraint;
import ca.udem.gaillarz.solver.lp.LinearProgram;
import ca.udem.gaillarz.solver.lp.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinearProgramTest {

    @Test
    void testSimpleLPStructure() {
        LinearProgram lp = new LinearProgram("test", true);
        Variable x = lp.addVariable("x", 0, Double.POSITIVE_INFINITY);
        Variable y = lp.addVariable("y", 0, Double.POSITIVE_INFINITY);

        lp.setObjectiveCoefficient(x, 1.0);
        lp.setObjectiveCoefficient(y, 2.0);

        Constraint c = lp.addLessOrEqual("c1", 5.0);
        c.addTerm(x, 1.0);
        c.addTerm(y, 1.0);

        assertEquals(2, lp.getNumVariables());
        assertEquals(1, lp.getNumConstraints());
        assertEquals(c, lp.getConstraint("c1"));
    }
}
