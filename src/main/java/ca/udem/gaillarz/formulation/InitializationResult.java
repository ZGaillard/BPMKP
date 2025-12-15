package ca.udem.gaillarz.formulation;

/**
 * Summary of pattern initialization.
 */
public record InitializationResult(int patternsP0, int patternsPI, int totalPatterns, double timeSeconds) {

    @Override
    public String toString() {
        return String.format(
                "Pattern Initialization:%n" +
                        "  P_0 patterns: %d%n" +
                        "  P_i patterns: %d%n" +
                        "  Total: %d%n" +
                        "  Time: %.3f seconds",
                patternsP0, patternsPI, totalPatterns, timeSeconds);
    }
}
