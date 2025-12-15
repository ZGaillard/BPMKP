package solver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a linear constraint.
 */
public class Constraint {
    private final String name;
    private final Map<Variable, Double> coefficients = new HashMap<>();
    private final ConstraintSense sense;
    private final double rhs;

    public Constraint(String name, ConstraintSense sense, double rhs) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Constraint name cannot be null or blank");
        }
        this.name = name;
        this.sense = sense == null ? ConstraintSense.LE : sense;
        this.rhs = rhs;
    }

    public void addTerm(Variable var, double coefficient) {
        if (var == null) {
            throw new IllegalArgumentException("Variable cannot be null");
        }
        coefficients.merge(var, coefficient, Double::sum);
    }

    public void addTerm(Variable var, double coefficient, double existingCoeff) {
        coefficients.put(var, coefficient + existingCoeff);
    }

    public String getName() {
        return name;
    }

    public Map<Variable, Double> getCoefficients() {
        return Collections.unmodifiableMap(coefficients);
    }

    public ConstraintSense getSense() {
        return sense;
    }

    public double getRHS() {
        return rhs;
    }

    /**
     * Evaluate LHS for given variable values.
     */
    public double evaluate(Map<Variable, Double> values) {
        double sum = 0.0;
        for (Map.Entry<Variable, Double> entry : coefficients.entrySet()) {
            double value = values.getOrDefault(entry.getKey(), 0.0);
            sum += entry.getValue() * value;
        }
        return sum;
    }

    /**
     * Check if constraint is satisfied.
     */
    public boolean isSatisfied(Map<Variable, Double> values, double tolerance) {
        double lhs = evaluate(values);
        return switch (sense) {
            case LE -> lhs <= rhs + tolerance;
            case EQ -> Math.abs(lhs - rhs) <= tolerance;
            case GE -> lhs >= rhs - tolerance;
        };
    }

    @Override
    public String toString() {
        return String.format("Constraint(%s, %s rhs=%s)", name, sense, rhs);
    }
}
