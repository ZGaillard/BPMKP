package ca.udem.gaillarz.solver.bp;

/**
 * Status of a Branch-and-Price run.
 */
public enum BPStatus {
    OPTIMAL,
    FEASIBLE,
    INFEASIBLE,
    TIME_LIMIT,
    NODE_LIMIT,
    GAP_LIMIT,
    ERROR
}
