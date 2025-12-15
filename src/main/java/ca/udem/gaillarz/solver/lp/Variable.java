package ca.udem.gaillarz.solver.lp;

/**
 * Represents a variable in a linear program.
 */
public record Variable(String name, double lowerBound, double upperBound, VariableType type) {
    public Variable(String name) {
        this(name, 0.0, Double.POSITIVE_INFINITY, VariableType.CONTINUOUS);
    }

    public Variable(String name, double lowerBound, double upperBound) {
        this(name, lowerBound, upperBound, VariableType.CONTINUOUS);
    }

    public Variable(String name, double lowerBound, double upperBound, VariableType type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Variable name cannot be null or blank");
        }
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Lower bound cannot exceed upper bound");
        }
        this.name = name;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.type = type == null ? VariableType.CONTINUOUS : type;
    }

    @Override
    public String toString() {
        return String.format("Var(%s, [%s,%s], %s)", name, lowerBound, upperBound, type);
    }
}
