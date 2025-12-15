package ca.udem.gaillarz.solver.lp;

/**
 * Describes solver capabilities.
 */
public record SolverCapabilities(boolean supportsLP, boolean supportsMIP, boolean supportsDuals,
                                 boolean supportsTimeLimit) {
}
