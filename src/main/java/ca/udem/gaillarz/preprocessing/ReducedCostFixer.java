package ca.udem.gaillarz.preprocessing;

import ca.udem.gaillarz.model.MKPInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fixes variables based on reduced costs from LP relaxation.
 */
public class ReducedCostFixer {

    private static final double TOLERANCE = 1e-6;

    private final MKPInstance instance;
    private final Map<Integer, Integer> fixedItems;

    public ReducedCostFixer(MKPInstance instance) {
        this.instance = instance;
        this.fixedItems = new HashMap<>();
    }

    /**
     * Fix variables based on reduced costs.
     *
     * @param upperBound   Root LP bound (z_UB)
     * @param lowerBound   Best integer solution (z_LB) from heuristic
     * @param reducedCosts Reduced cost for each item j
     * @return Number of variables fixed
     */
    public int fixByReducedCosts(double upperBound, double lowerBound, double[] reducedCosts) {
        fixedItems.clear();

        if (reducedCosts == null || reducedCosts.length < instance.getNumItems()) {
            throw new IllegalArgumentException("Reduced cost array is null or too small for instance size");
        }

        if (lowerBound <= 0 || !Double.isFinite(upperBound)) {
            System.out.println("[RC Fix] Invalid bounds - skipping RC fixing");
            System.out.printf("  Upper bound: %.2f, Lower bound: %.2f%n", upperBound, lowerBound);
            return 0;
        }

        if (upperBound <= lowerBound + TOLERANCE) {
            System.out.println("[RC Fix] Gap too small - solution already optimal or near-optimal");
            return 0;
        }

        double gap = upperBound - lowerBound;
        int numFixed = 0;

        System.out.printf("[RC Fix] Gap = %.2f (UB=%.2f, LB=%.2f)%n", gap, upperBound, lowerBound);

        for (int j = 0; j < instance.getNumItems(); j++) {
            double rc = reducedCosts[j];

            if (rc > gap + TOLERANCE) {
                fixedItems.put(j, 0);
                numFixed++;
                System.out.printf("[RC Fix] t_%d ← 0  (RC=%.2f > gap=%.2f)%n", j, rc, gap);
            } else if (rc < -(gap + TOLERANCE)) {
                fixedItems.put(j, 1);
                numFixed++;
                System.out.printf("[RC Fix] t_%d ← 1  (RC=%.2f < -gap=%.2f)%n", j, rc, gap);
            }
        }

        if (numFixed > 0) {
            System.out.printf("[RC Fix] Fixed %d/%d variables (%.1f%%)%n", numFixed, instance.getNumItems(),
                    100.0 * numFixed / instance.getNumItems());
        } else {
            System.out.println("[RC Fix] No variables fixed");
        }

        return numFixed;
    }

    public Map<Integer, Integer> getFixedItems() {
        return Collections.unmodifiableMap(fixedItems);
    }

    public void clear() {
        fixedItems.clear();
    }

    public boolean isFixed(int itemId) {
        return fixedItems.containsKey(itemId);
    }

    public Integer getFixedValue(int itemId) {
        return fixedItems.get(itemId);
    }

    public Set<Integer> getItemsFixedToZero() {
        Set<Integer> items = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : fixedItems.entrySet()) {
            if (entry.getValue() == 0) {
                items.add(entry.getKey());
            }
        }
        return items;
    }

    public Set<Integer> getItemsFixedToOne() {
        Set<Integer> items = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : fixedItems.entrySet()) {
            if (entry.getValue() == 1) {
                items.add(entry.getKey());
            }
        }
        return items;
    }

    public String getSummary() {
        int fixedTo0 = (int) fixedItems.values().stream().filter(v -> v == 0).count();
        int fixedTo1 = (int) fixedItems.values().stream().filter(v -> v == 1).count();
        return String.format("RC Fixings: %d total (%d to 0, %d to 1)",
                fixedItems.size(), fixedTo0, fixedTo1);
    }
}
