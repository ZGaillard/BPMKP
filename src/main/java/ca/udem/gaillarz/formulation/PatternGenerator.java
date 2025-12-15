package ca.udem.gaillarz.formulation;

import ca.udem.gaillarz.model.Item;
import ca.udem.gaillarz.model.MKPInstance;
import ca.udem.gaillarz.solver.KnapsackSolver;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates initial patterns for the Dantzig-Wolfe master problem.
 */
public class PatternGenerator {

    private final MKPInstance instance;

    public PatternGenerator(MKPInstance instance) {
        this.instance = Objects.requireNonNull(instance, "instance");
    }

    // ========== P_0 Patterns (Aggregated Capacity) ==========

    public List<Pattern> generateInitialPatternsP0() {
        List<Pattern> patterns = new ArrayList<>();
        int totalCapacity = instance.getTotalCapacity();

        patterns.addAll(generateSingleItemPatterns(totalCapacity));
        patterns.addAll(generateGreedyPatterns(totalCapacity, 5));

        Pattern core = generateCorePattern(totalCapacity);
        if (core != null && !patterns.contains(core)) {
            patterns.add(core);
        }

        if (instance.getNumItems() <= 20) {
            patterns.addAll(generateTwoItemPatterns(totalCapacity));
        }

        return patterns.stream().distinct().collect(Collectors.toList());
    }

    // ========== P_i Patterns ==========

    public List<Pattern> generateInitialPatternsPI(int knapsackId) {
        List<Pattern> patterns = new ArrayList<>();
        int capacity = instance.getKnapsack(knapsackId).getCapacity();

        patterns.addAll(generateSingleItemPatterns(capacity));
        patterns.addAll(generateGreedyPatterns(capacity, 3));

        Pattern core = generateCorePattern(capacity);
        if (core != null && !patterns.contains(core)) {
            patterns.add(core);
        }

        return patterns.stream().distinct().collect(Collectors.toList());
    }

    // ========== Strategies ==========

    private List<Pattern> generateSingleItemPatterns(int capacity) {
        List<Pattern> patterns = new ArrayList<>();
        for (int j = 0; j < instance.getNumItems(); j++) {
            Item item = instance.getItem(j);
            if (item.getWeight() <= capacity) {
                patterns.add(Pattern.singleItem(j, instance));
            }
        }
        return patterns;
    }

    private List<Pattern> generateGreedyPatterns(int capacity, int numVariants) {
        List<Pattern> patterns = new ArrayList<>();

        List<Item> byEfficiency = new ArrayList<>(instance.getItems());
        byEfficiency.sort((a, b) -> Double.compare(b.getProfitWeightRatio(), a.getProfitWeightRatio()));
        patterns.add(greedyPack(byEfficiency, capacity));

        if (numVariants > 1) {
            List<Item> byProfit = new ArrayList<>(instance.getItems());
            byProfit.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));
            patterns.add(greedyPack(byProfit, capacity));
        }
        if (numVariants > 2) {
            List<Item> byWeight = new ArrayList<>(instance.getItems());
            byWeight.sort(Comparator.comparingInt(Item::getWeight));
            patterns.add(greedyPack(byWeight, capacity));
        }
        if (numVariants > 3) {
            List<Item> reversed = new ArrayList<>(byEfficiency);
            Collections.reverse(reversed);
            patterns.add(greedyPack(reversed, capacity));
        }
        if (numVariants > 4) {
            List<Item> shuffled = new ArrayList<>(instance.getItems());
            Collections.shuffle(shuffled, new Random(42));
            patterns.add(greedyPack(shuffled, capacity));
        }

        return patterns;
    }

    private Pattern greedyPack(List<Item> items, int capacity) {
        Set<Integer> selected = new HashSet<>();
        int used = 0;

        for (Item item : items) {
            if (used + item.getWeight() <= capacity) {
                selected.add(item.getId());
                used += item.getWeight();
            }
        }

        return Pattern.fromItemIds(selected, instance);
    }

    private Pattern generateCorePattern(int capacity) {
        KnapsackSolver solver = new KnapsackSolver(instance.getItems(), capacity);
        var result = solver.solve();
        if (result.isEmpty()) {
            return null;
        }
        return Pattern.fromItemIds(result.getSelectedItemIds(), instance);
    }

    private List<Pattern> generateTwoItemPatterns(int capacity) {
        List<Pattern> patterns = new ArrayList<>();
        int n = instance.getNumItems();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Item a = instance.getItem(i);
                Item b = instance.getItem(j);
                if (a.getWeight() + b.getWeight() <= capacity) {
                    patterns.add(Pattern.fromItemIds(Set.of(i, j), instance));
                }
            }
        }
        return patterns;
    }

    private List<Pattern> generateThreeItemPatterns(int capacity) {
        List<Pattern> patterns = new ArrayList<>();
        int n = instance.getNumItems();
        if (n > 15) {
            return patterns;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int k = j + 1; k < n; k++) {
                    int total = instance.getItem(i).getWeight()
                            + instance.getItem(j).getWeight()
                            + instance.getItem(k).getWeight();
                    if (total <= capacity) {
                        patterns.add(Pattern.fromItemIds(Set.of(i, j, k), instance));
                    }
                }
            }
        }
        return patterns;
    }

    // ========== Statistics ==========

    public PatternStatistics getStatistics(List<Pattern> patterns, int capacity) {
        if (patterns.isEmpty()) {
            return new PatternStatistics(0, 0, 0, 0.0, 0, 0, 0.0);
        }

        int totalPatterns = patterns.size();
        int minItems = patterns.stream().mapToInt(Pattern::getNumItems).min().orElse(0);
        int maxItems = patterns.stream().mapToInt(Pattern::getNumItems).max().orElse(0);
        double avgItems = patterns.stream().mapToInt(Pattern::getNumItems).average().orElse(0);

        int minWeight = patterns.stream().mapToInt(Pattern::getTotalWeight).min().orElse(0);
        int maxWeight = patterns.stream().mapToInt(Pattern::getTotalWeight).max().orElse(0);
        double avgUtil = patterns.stream()
                .mapToDouble(p -> (double) p.getTotalWeight() / capacity)
                .average()
                .orElse(0.0);

        return new PatternStatistics(totalPatterns, minItems, maxItems, avgItems, minWeight, maxWeight, avgUtil);
    }
}
