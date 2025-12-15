package solver;

/**
 * Solution status for an LP/MIP solve.
 */
public enum SolutionStatus {
    OPTIMAL,
    FEASIBLE,
    INFEASIBLE,
    UNBOUNDED,
    ERROR,
    NOT_SOLVED;

    public boolean isOptimal() {
        return this == OPTIMAL;
    }

    public boolean isFeasible() {
        return this == OPTIMAL || this == FEASIBLE;
    }

    public boolean isInfeasible() {
        return this == INFEASIBLE;
    }

    public boolean isUnbounded() {
        return this == UNBOUNDED;
    }
}
