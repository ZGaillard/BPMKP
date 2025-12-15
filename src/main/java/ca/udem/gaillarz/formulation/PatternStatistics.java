package ca.udem.gaillarz.formulation;

/**
 * Simple statistics container for pattern pools.
 */
public record PatternStatistics(int totalPatterns, int minItemsPerPattern, int maxItemsPerPattern,
                                double avgItemsPerPattern, int minWeight, int maxWeight,
                                double avgCapacityUtilization) {

    @Override
    public String toString() {
        return String.format(
                "Pattern Statistics:%n" +
                        "  Total patterns: %d%n" +
                        "  Items per pattern: min=%d, max=%d, avg=%.1f%n" +
                        "  Weight: min=%d, max=%d%n" +
                        "  Avg capacity utilization: %.1f%%",
                totalPatterns, minItemsPerPattern, maxItemsPerPattern, avgItemsPerPattern,
                minWeight, maxWeight, avgCapacityUtilization * 100);
    }
}
