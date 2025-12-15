package ca.udem.gaillarz.formulation;

import java.util.List;

/**
 * Helper to populate a Dantzig-Wolfe master with a richer initial pattern pool.
 */
public final class PatternInitializer {
    private PatternInitializer() {
    }

    public static InitializationResult initialize(DantzigWolfeMaster master) {
        long start = System.currentTimeMillis();

        PatternGenerator generator = new PatternGenerator(master.getInstance());

        List<Pattern> p0 = generator.generateInitialPatternsP0();
        for (Pattern pattern : p0) {
            master.addPatternP0(pattern);
        }

        int piCount = 0;
        for (int i = 0; i < master.getInstance().getNumKnapsacks(); i++) {
            List<Pattern> pi = generator.generateInitialPatternsPI(i);
            for (Pattern pattern : pi) {
                master.addPatternPI(i, pattern);
            }
            piCount += pi.size();
        }

        double seconds = (System.currentTimeMillis() - start) / 1000.0;
        return new InitializationResult(p0.size(), piCount, master.getTotalPatternCount(), seconds);
    }
}
