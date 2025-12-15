package ca.udem.gaillarz.benchmark;

/**
 * Lightweight progress display for long runs.
 */
public class ProgressTracker {
    private int total;
    private int completed;
    private long startMs;

    public void start(String setName, int total) {
        this.total = total;
        this.completed = 0;
        this.startMs = System.currentTimeMillis();
        System.out.printf("%n[Progress] Starting set %s (%d instances)%n", setName, total);
    }

    public void step(String name) {
        completed++;
        System.out.printf("[Progress] [%d/%d] %s%n", completed, total, name);
    }

    public void finish(String setName) {
        double mins = (System.currentTimeMillis() - startMs) / 60000.0;
        System.out.printf("[Progress] Completed set %s in %.1f minutes%n", setName, mins);
    }
}
