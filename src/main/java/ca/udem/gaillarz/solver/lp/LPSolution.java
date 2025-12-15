package ca.udem.gaillarz.solver.lp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Solution to a linear program.
 */
public record LPSolution(SolutionStatus status, double objectiveValue, Map<Variable, Double> primalValues,
                         Map<Constraint, Double> dualValues, Map<Variable, Double> reducedCosts, double solveTime) {
    public LPSolution(SolutionStatus status,
                      double objectiveValue,
                      Map<Variable, Double> primalValues,
                      Map<Constraint, Double> dualValues,
                      Map<Variable, Double> reducedCosts,
                      double solveTime) {
        this.status = status == null ? SolutionStatus.ERROR : status;
        this.objectiveValue = objectiveValue;
        this.primalValues = new HashMap<>(primalValues);
        this.dualValues = new HashMap<>(dualValues);
        this.reducedCosts = new HashMap<>(reducedCosts);
        this.solveTime = solveTime;
    }

    public boolean isOptimal() {
        return status.isOptimal();
    }

    public boolean isFeasible() {
        return status.isFeasible();
    }

    public boolean isInfeasible() {
        return status.isInfeasible();
    }

    public boolean isUnbounded() {
        return status.isUnbounded();
    }

    // ========== Primal ==========
    public double getPrimalValue(Variable var) {
        return primalValues.getOrDefault(var, 0.0);
    }

    @Override
    public Map<Variable, Double> primalValues() {
        return Collections.unmodifiableMap(primalValues);
    }

    public Map<Variable, Double> getPrimalValues(String namePrefix) {
        return primalValues.entrySet().stream()
                .filter(e -> e.getKey().name().startsWith(namePrefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // ========== Dual ==========
    public double getDualValue(Constraint constraint) {
        return dualValues.getOrDefault(constraint, 0.0);
    }

    @Override
    public Map<Constraint, Double> dualValues() {
        return Collections.unmodifiableMap(dualValues);
    }

    public Map<Constraint, Double> getDualValues(String namePrefix) {
        return dualValues.entrySet().stream()
                .filter(e -> e.getKey().getName().startsWith(namePrefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // ========== Reduced costs ==========
    public double getReducedCost(Variable var) {
        return reducedCosts.getOrDefault(var, 0.0);
    }

    @Override
    public Map<Variable, Double> reducedCosts() {
        return Collections.unmodifiableMap(reducedCosts);
    }

    @Override
    public String toString() {
        return String.format("LPSolution(status=%s, obj=%s, time=%.3fs)", status, objectiveValue, solveTime);
    }

    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this).append("\n");
        sb.append("Primal values:\n");
        primalValues.forEach((v, val) -> sb.append("  ").append(v.name()).append(" = ").append(val).append("\n"));
        if (!dualValues.isEmpty()) {
            sb.append("Dual values:\n");
            dualValues.forEach((c, val) -> sb.append("  ").append(c.getName()).append(" = ").append(val).append("\n"));
        }
        return sb.toString();
    }
}
