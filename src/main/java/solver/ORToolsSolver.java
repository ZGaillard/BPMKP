package solver;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.HashMap;
import java.util.Map;

/**
 * LP solver implementation backed by Google OR-Tools.
 */
public class ORToolsSolver implements LPSolver {

    private final String solverType; // e.g., GLOP_LINEAR_PROGRAMMING

    public ORToolsSolver(String solverType) {
        this.solverType = solverType;
        Loader.loadNativeLibraries();
    }

    public ORToolsSolver() {
        this("GLOP_LINEAR_PROGRAMMING");
    }

    @Override
    public LPSolution solve(LinearProgram lp) {
        return solve(lp, Double.POSITIVE_INFINITY);
    }

    @Override
    public LPSolution solve(LinearProgram lp, double timeLimitSeconds) {
        long start = System.currentTimeMillis();
        try {
            MPSolver solver = MPSolver.createSolver(solverType);
            if (solver == null) {
                throw new RuntimeException("Failed to create OR-Tools solver: " + solverType);
            }
            if (Double.isFinite(timeLimitSeconds)) {
                solver.setTimeLimit((long) (timeLimitSeconds * 1000));
            }

            Map<Variable, MPVariable> varMap = new HashMap<>();
            for (Variable var : lp.getVariables()) {
                double lb = var.getLowerBound();
                double ub = var.getUpperBound();
                MPVariable mpVar;
                switch (var.getType()) {
                    case BINARY -> mpVar = solver.makeBoolVar(var.getName());
                    case INTEGER -> mpVar = solver.makeIntVar(lb, ub, var.getName());
                    case CONTINUOUS -> mpVar = solver.makeNumVar(lb, ub, var.getName());
                    default -> throw new IllegalArgumentException("Unknown variable type: " + var.getType());
                }
                varMap.put(var, mpVar);
            }

            Map<Constraint, MPConstraint> conMap = new HashMap<>();
            for (Constraint con : lp.getConstraints()) {
                MPConstraint mpCon = switch (con.getSense()) {
                    case LE -> solver.makeConstraint(-MPSolver.infinity(), con.getRHS(), con.getName());
                    case EQ -> solver.makeConstraint(con.getRHS(), con.getRHS(), con.getName());
                    case GE -> solver.makeConstraint(con.getRHS(), MPSolver.infinity(), con.getName());
                };
                for (Map.Entry<Variable, Double> term : con.getCoefficients().entrySet()) {
                    mpCon.setCoefficient(varMap.get(term.getKey()), term.getValue());
                }
                conMap.put(con, mpCon);
            }

            MPObjective obj = solver.objective();
            for (Map.Entry<Variable, Double> entry : lp.getObjective().entrySet()) {
                obj.setCoefficient(varMap.get(entry.getKey()), entry.getValue());
            }
            if (lp.isMaximization()) {
                obj.setMaximization();
            } else {
                obj.setMinimization();
            }

            MPSolver.ResultStatus status = solver.solve();
            SolutionStatus solStatus = convertStatus(status);

            Map<Variable, Double> primals = new HashMap<>();
            Map<Constraint, Double> duals = new HashMap<>();
            Map<Variable, Double> reduced = new HashMap<>();

            if (solStatus.isOptimal() || solStatus.isFeasible()) {
                for (Map.Entry<Variable, MPVariable> entry : varMap.entrySet()) {
                    primals.put(entry.getKey(), entry.getValue().solutionValue());
                    reduced.put(entry.getKey(), entry.getValue().reducedCost());
                }
                for (Map.Entry<Constraint, MPConstraint> entry : conMap.entrySet()) {
                    duals.put(entry.getKey(), entry.getValue().dualValue());
                }
            }

            double objVal = (solStatus.isOptimal() || solStatus.isFeasible()) ? obj.value() : Double.NaN;
            double solveTime = (System.currentTimeMillis() - start) / 1000.0;

            return new LPSolution(solStatus, objVal, primals, duals, reduced, solveTime);
        } catch (Exception e) {
            double solveTime = (System.currentTimeMillis() - start) / 1000.0;
            return new LPSolution(SolutionStatus.ERROR, Double.NaN, new HashMap<>(), new HashMap<>(), new HashMap<>(), solveTime);
        }
    }

    @Override
    public String getSolverName() {
        return "OR-Tools (" + solverType + ")";
    }

    @Override
    public SolverCapabilities getCapabilities() {
        boolean supportsMIP = solverType.contains("CBC") || solverType.contains("SCIP") || solverType.contains("SAT");
        return new SolverCapabilities(true, supportsMIP, true, true);
    }

    private SolutionStatus convertStatus(MPSolver.ResultStatus status) {
        return switch (status) {
            case OPTIMAL -> SolutionStatus.OPTIMAL;
            case FEASIBLE -> SolutionStatus.FEASIBLE;
            case INFEASIBLE -> SolutionStatus.INFEASIBLE;
            case UNBOUNDED -> SolutionStatus.UNBOUNDED;
            default -> SolutionStatus.ERROR;
        };
    }
}
