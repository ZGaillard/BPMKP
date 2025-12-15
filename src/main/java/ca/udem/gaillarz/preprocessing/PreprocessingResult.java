package ca.udem.gaillarz.preprocessing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Results from reduced cost preprocessing.
 */
public class PreprocessingResult {

    private final Map<Integer, Integer> fixedItems;
    private final int totalItems;
    private final double preprocessingTime;

    public PreprocessingResult(Map<Integer, Integer> fixedItems, int totalItems, double preprocessingTime) {
        this.fixedItems = new HashMap<>(fixedItems);
        this.totalItems = totalItems;
        this.preprocessingTime = preprocessingTime;
    }

    public Map<Integer, Integer> getFixedItems() {
        return Collections.unmodifiableMap(fixedItems);
    }

    public int getNumFixings() {
        return fixedItems.size();
    }

    public double getPercentageFixed() {
        if (totalItems == 0) {
            return 0.0;
        }
        return (100.0 * fixedItems.size()) / totalItems;
    }

    public boolean hasFixings() {
        return !fixedItems.isEmpty();
    }

    public double getTime() {
        return preprocessingTime;
    }

    @Override
    public String toString() {
        return String.format("Preprocessing: Fixed %d/%d items (%.1f%%) in %.3fs",
                fixedItems.size(), totalItems, getPercentageFixed(), preprocessingTime);
    }
}
