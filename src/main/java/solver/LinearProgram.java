package solver;

import java.util.*;

/**
 * Represents a linear program (LP or MIP).
 */
public class LinearProgram {
    private final List<Variable> variables = new ArrayList<>();
    private final List<Constraint> constraints = new ArrayList<>();
    private final Map<Variable, Double> objective = new HashMap<>();
    private final boolean maximize;
    private final String name;

    public LinearProgram(String name, boolean maximize) {
        this.name = name == null ? "lp" : name;
        this.maximize = maximize;
    }

    // ========== Variable Management ==========
    public Variable addVariable(String name, double lb, double ub, VariableType type) {
        Variable var = new Variable(name, lb, ub, type);
        variables.add(var);
        return var;
    }

    public Variable addVariable(String name, double lb, double ub) {
        return addVariable(name, lb, ub, VariableType.CONTINUOUS);
    }

    public Variable addVariable(String name) {
        return addVariable(name, 0.0, Double.POSITIVE_INFINITY, VariableType.CONTINUOUS);
    }

    public Variable addBinaryVariable(String name) {
        return addVariable(name, 0.0, 1.0, VariableType.BINARY);
    }

    public List<Variable> getVariables() {
        return Collections.unmodifiableList(variables);
    }

    public int getNumVariables() {
        return variables.size();
    }

    public Variable getVariable(String name) {
        for (Variable var : variables) {
            if (var.getName().equals(name)) {
                return var;
            }
        }
        return null;
    }

    // ========== Constraint Management ==========
    public Constraint addConstraint(String name, ConstraintSense sense, double rhs) {
        Constraint c = new Constraint(name, sense, rhs);
        constraints.add(c);
        return c;
    }

    public Constraint addLessOrEqual(String name, double rhs) {
        return addConstraint(name, ConstraintSense.LE, rhs);
    }

    public Constraint addEqual(String name, double rhs) {
        return addConstraint(name, ConstraintSense.EQ, rhs);
    }

    public Constraint addGreaterOrEqual(String name, double rhs) {
        return addConstraint(name, ConstraintSense.GE, rhs);
    }

    public List<Constraint> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    public int getNumConstraints() {
        return constraints.size();
    }

    public Constraint getConstraint(String name) {
        for (Constraint c : constraints) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    // ========== Objective ==========
    public void setObjectiveCoefficient(Variable var, double coefficient) {
        objective.put(var, coefficient);
    }

    public void addObjectiveCoefficient(Variable var, double coefficient) {
        objective.merge(var, coefficient, Double::sum);
    }

    public Map<Variable, Double> getObjective() {
        return Collections.unmodifiableMap(objective);
    }

    public boolean isMaximization() {
        return maximize;
    }

    // ========== Utility ==========
    public double evaluateObjective(Map<Variable, Double> values) {
        double obj = 0.0;
        for (Map.Entry<Variable, Double> entry : objective.entrySet()) {
            obj += entry.getValue() * values.getOrDefault(entry.getKey(), 0.0);
        }
        return obj;
    }

    public boolean isFeasible(Map<Variable, Double> values, double tolerance) {
        for (Constraint c : constraints) {
            if (!c.isSatisfied(values, tolerance)) {
                return false;
            }
        }
        // Bounds
        for (Variable v : variables) {
            double val = values.getOrDefault(v, 0.0);
            if (val < v.getLowerBound() - tolerance || val > v.getUpperBound() + tolerance) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("LinearProgram(%s, max=%s, vars=%d, cons=%d)", name, maximize, getNumVariables(), getNumConstraints());
    }

    /**
     * Very small LP writer (not full LP format).
     */
    public String toLPFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append(maximize ? "Maximize\n obj: " : "Minimize\n obj: ");
        boolean first = true;
        for (Map.Entry<Variable, Double> entry : objective.entrySet()) {
            if (!first) sb.append(" + ");
            sb.append(entry.getValue()).append(" ").append(entry.getKey().getName());
            first = false;
        }
        sb.append("\nSubject To\n");
        for (Constraint c : constraints) {
            sb.append(" ").append(c.getName()).append(": ");
            first = true;
            for (Map.Entry<Variable, Double> term : c.getCoefficients().entrySet()) {
                if (!first) sb.append(" + ");
                sb.append(term.getValue()).append(" ").append(term.getKey().getName());
                first = false;
            }
            sb.append(" ");
            sb.append(switch (c.getSense()) {
                case LE -> "<=";
                case EQ -> "=";
                case GE -> ">=";
            });
            sb.append(" ").append(c.getRHS()).append("\n");
        }
        sb.append("Bounds\n");
        for (Variable v : variables) {
            sb.append(" ").append(v.getLowerBound()).append(" <= ").append(v.getName()).append(" <= ").append(v.getUpperBound()).append("\n");
        }
        sb.append("End\n");
        return sb.toString();
    }
}
