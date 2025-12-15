package ca.udem.gaillarz.formulation;

import java.util.Arrays;

/**
 * Dual values from DW master LP solution.
 *
 * @param mu  item consistency duals
 * @param pi  pattern selection duals (P0 then P_i)
 * @param tau upper bound dual
 */
public record DualValues(double[] mu, double[] pi, double tau) {
    public DualValues(double[] mu, double[] pi, double tau) {
        this.mu = Arrays.copyOf(mu, mu.length);
        this.pi = Arrays.copyOf(pi, pi.length);
        this.tau = tau;
    }

    public double getMu(int itemId) {
        return mu[itemId];
    }

    @Override
    public double[] mu() {
        return Arrays.copyOf(mu, mu.length);
    }

    public double getPi(int poolId) {
        return pi[poolId];
    }

    @Override
    public double[] pi() {
        return Arrays.copyOf(pi, pi.length);
    }

    @Override
    public String toString() {
        return "DualValues{" +
                "mu=" + Arrays.toString(mu) +
                ", pi=" + Arrays.toString(pi) +
                ", tau=" + tau +
                '}';
    }
}
