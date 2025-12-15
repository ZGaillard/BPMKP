package ca.udem.gaillarz.solver.bp;

/**
 * Status of a node in the branch-and-bound tree.
 */
public enum NodeStatus {
    OPEN,         // Not yet processed
    PROCESSING,   // Currently being solved
    SOLVED,       // LP solved, may need branching
    PRUNED,       // Pruned by bounds
    INFEASIBLE,   // No feasible solution
    INTEGER,      // Integer solution found
    FATHOMED      // Closed for any reason
}
