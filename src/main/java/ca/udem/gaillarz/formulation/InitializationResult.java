package ca.udem.gaillarz.formulation;

/**
 * Summary of pattern initialization.
 */
public class InitializationResult {
    private final int patternsP0;
    private final int patternsPI;
    private final int totalPatterns;
    private final double timeSeconds;

    public InitializationResult(int patternsP0, int patternsPI, int totalPatterns, double timeSeconds) {
        this.patternsP0 = patternsP0;
        this.patternsPI = patternsPI;
        this.totalPatterns = totalPatterns;
        this.timeSeconds = timeSeconds;
    }

    public int getPatternsP0() {
        return patternsP0;
    }

    public int getPatternsPI() {
        return patternsPI;
    }

    public int getTotalPatterns() {
        return totalPatterns;
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

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
