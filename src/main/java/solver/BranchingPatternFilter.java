package solver;

import ca.udem.gaillarz.formulation.DantzigWolfeMaster;
import ca.udem.gaillarz.formulation.Pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility to filter patterns from the master based on branching decisions.
 */
public final class BranchingPatternFilter {
    private BranchingPatternFilter() {}

    /**
     * Remove patterns that violate forbidden-item decisions.
     *
     * @return number of patterns removed
     */
    public static int filterPatterns(DantzigWolfeMaster master, BranchNode node) {
        int removed = 0;
        Set<Integer> forbidden = node.getForbiddenItems();

        List<Pattern> removeP0 = new ArrayList<>();
        for (Pattern p : master.getPatternsP0()) {
            if (containsAny(p, forbidden)) {
                removeP0.add(p);
            }
        }
        for (Pattern p : removeP0) {
            if (master.removePatternP0(p)) removed++;
        }

        for (int i = 0; i < master.getInstance().getNumKnapsacks(); i++) {
            List<Pattern> removePi = new ArrayList<>();
            for (Pattern p : master.getPatternsPI(i)) {
                if (containsAny(p, forbidden)) {
                    removePi.add(p);
                }
            }
            for (Pattern p : removePi) {
                if (master.removePatternPI(i, p)) removed++;
            }
        }

        return removed;
    }

    private static boolean containsAny(Pattern pattern, Set<Integer> forbidden) {
        for (int item : forbidden) {
            if (pattern.containsItem(item)) return true;
        }
        return false;
    }
}
