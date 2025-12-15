package ca.udem.gaillarz.formulation;

/**
 * Simple statistics container for pattern pools.
 */
public class PatternStatistics {
    private final int totalPatterns;
    private final int minItemsPerPattern;
    private final int maxItemsPerPattern;
    private final double avgItemsPerPattern;
    private final int minWeight;
    private final int maxWeight;
    private final double avgCapacityUtilization;

    public PatternStatistics(int totalPatterns,
                             int minItemsPerPattern,
                             int maxItemsPerPattern,
                             double avgItemsPerPattern,
                             int minWeight,
                             int maxWeight,
                             double avgCapacityUtilization) {
        this.totalPatterns = totalPatterns;
        this.minItemsPerPattern = minItemsPerPattern;
        this.maxItemsPerPattern = maxItemsPerPattern;
        this.avgItemsPerPattern = avgItemsPerPattern;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        this.avgCapacityUtilization = avgCapacityUtilization;
    }

    public int getTotalPatterns() {
        return totalPatterns;
    }

    public int getMinItemsPerPattern() {
        return minItemsPerPattern;
    }

    public int getMaxItemsPerPattern() {
        return maxItemsPerPattern;
    }

    public double getAvgItemsPerPattern() {
        return avgItemsPerPattern;
    }

    public int getMinWeight() {
        return minWeight;
    }

    public int getMaxWeight() {
        return maxWeight;
    }

    public double getAvgCapacityUtilization() {
        return avgCapacityUtilization;
    }

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
