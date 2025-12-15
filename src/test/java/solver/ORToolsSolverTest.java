package solver;

import ca.udem.gaillarz.solver.lp.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ORToolsSolverTest {

    @Test
    void testSolveSimpleLP() {
        // max x + 2y
        // s.t. x + y <= 5, x,y >= 0
        // optimum: x=0, y=5, obj=10
        LinearProgram lp = new LinearProgram("test", true);
        Variable x = lp.addVariable("x");
        Variable y = lp.addVariable("y");
        lp.setObjectiveCoefficient(x, 1.0);
        lp.setObjectiveCoefficient(y, 2.0);
        Constraint c = lp.addLessOrEqual("c1", 5.0);
        c.addTerm(x, 1.0);
        c.addTerm(y, 1.0);

        ORToolsSolver solver = new ORToolsSolver();
        LPSolution sol = solver.solve(lp);

        assertTrue(sol.isOptimal(), "LP should be optimal");
        assertEquals(10.0, sol.objectiveValue(), 1e-6);
        assertEquals(0.0, sol.getPrimalValue(x), 1e-6);
        assertEquals(5.0, sol.getPrimalValue(y), 1e-6);
        // dual of c1 should be 2.0 for this LP
        assertEquals(2.0, sol.getDualValue(c), 1e-6);
    }
}
