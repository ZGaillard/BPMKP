package ca.udem.gaillarz.solver.lp;

/**
 * Interface for LP/MIP solvers.
 */
public interface LPSolver {
    LPSolution solve(LinearProgram lp);

    LPSolution solve(LinearProgram lp, double timeLimitSeconds);

    String getSolverName();

    SolverCapabilities getCapabilities();
}
