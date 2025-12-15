package solver;

/**
 * Describes solver capabilities.
 */
public class SolverCapabilities {
    private final boolean supportsLP;
    private final boolean supportsMIP;
    private final boolean supportsDuals;
    private final boolean supportsTimeLimit;

    public SolverCapabilities(boolean supportsLP, boolean supportsMIP, boolean supportsDuals, boolean supportsTimeLimit) {
        this.supportsLP = supportsLP;
        this.supportsMIP = supportsMIP;
        this.supportsDuals = supportsDuals;
        this.supportsTimeLimit = supportsTimeLimit;
    }

    public boolean supportsLP() {
        return supportsLP;
    }

    public boolean supportsMIP() {
        return supportsMIP;
    }

    public boolean supportsDuals() {
        return supportsDuals;
    }

    public boolean supportsTimeLimit() {
        return supportsTimeLimit;
    }
}
