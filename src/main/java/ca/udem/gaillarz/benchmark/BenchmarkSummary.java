package ca.udem.gaillarz.benchmark;

import ca.udem.gaillarz.solver.bp.BPStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate stats for a benchmark set.
 */
public class BenchmarkSummary {
    private final String setName;
    private final List<BenchmarkResult> results;
    private final int total;
    private final int solved;
    private final int optimal;
    private final int gapLimited;
    private final int infeasible;
    private final int errors;
    private final double avgGap;
    private final double avgTime;
    private final double totalTime;

    public BenchmarkSummary(String setName, List<BenchmarkResult> results) {
        this.setName = setName;
        this.results = new ArrayList<>(results);
        this.total = results.size();
        int solvedCount = 0;
        int optCount = 0;
        int gapCount = 0;
        int infeas = 0;
        int err = 0;
        double gapSum = 0.0;
        double timeSum = 0.0;
        int gapDen = 0;

        for (BenchmarkResult r : results) {
            if (r.isError()) {
                err++;
                continue;
            }
            solvedCount++;
            timeSum += r.timeSeconds();
            if (Double.isFinite(r.gap())) {
                gapSum += r.gap();
                gapDen++;
            }
            if (r.status() == BPStatus.OPTIMAL) optCount++;
            if (r.status() == BPStatus.GAP_LIMIT) gapCount++;
            if (r.status() == BPStatus.INFEASIBLE) infeas++;
        }

        this.solved = solvedCount;
        this.optimal = optCount;
        this.gapLimited = gapCount;
        this.infeasible = infeas;
        this.errors = err;
        this.avgGap = gapDen == 0 ? 0.0 : gapSum / gapDen;
        this.avgTime = solvedCount == 0 ? 0.0 : timeSum / solvedCount;
        this.totalTime = timeSum;
    }

    public String format() {
        return "Set " + setName + ": " +
                String.format("total=%d solved=%d optimal=%d gap_limit=%d infeasible=%d errors=%d%n",
                        total, solved, optimal, gapLimited, infeasible, errors) +
                String.format("  avg gap=%.2f%% avg time=%.2fs total time=%.1fs%n",
                        avgGap * 100, avgTime, totalTime);
    }

    public List<BenchmarkResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    public String getSetName() {
        return setName;
    }

    public int getTotal() {
        return total;
    }

    public int getSolved() {
        return solved;
    }

    public int getOptimal() {
        return optimal;
    }

    public int getGapLimited() {
        return gapLimited;
    }

    public int getInfeasible() {
        return infeasible;
    }

    public int getErrors() {
        return errors;
    }

    public double getAvgGap() {
        return avgGap;
    }

    public double getAvgTime() {
        return avgTime;
    }

    public double getTotalTime() {
        return totalTime;
    }
}
