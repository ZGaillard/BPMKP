package solver;

/**
 * Status codes for column generation runs.
 */
public enum CGStatus {
    OPTIMAL,
    FEASIBLE,
    INFEASIBLE,
    UNBOUNDED,
    ITERATION_LIMIT,
    TIME_LIMIT
}
