# Branch-and-Price for the Multiple Knapsack Problem

## What’s inside

- **Model & IO**: `model/` holds `Item`, `Knapsack`, `MKPInstance`; `io/` parses and validates instance files.
- **Formulations**: Classic and L2 relaxations, Dantzig–Wolfe master, pattern initialization/generation, and conversion
  utilities in `formulation/`.
- **Solvers**: Lightweight LP wrapper (`solver/lp`), column generation (`solver/cg`), branch-and-price driver with
  no-good cuts (`solver/bp`), and VSBPP SAT checker for fractional assignments (`solver/vsbpp`).
- **CLI demo**: `Main` stitches everything together; pick instances, run the solver, and inspect solutions from the
  console.
- **Tests**: JUnit 5 coverage for formulations, column generation, and the solver glue in `src/test/java`.

## Submission report highlights

This README plus `benchmark_analysis.ipynb` form the report I’m submitting with the code. Headline results on the
provided `SMALL` batch (`benchmark_results/time-limit600_gap0.01_max-nodes1000/SMALL_results.csv`):

- 179 runs loaded; ~58.7% solved to proven optimality and ~41.3% stopped at the 1% gap limit.
- Runtime: median 0.64s, mean 12.74s, 90th percentile 38.06s, max 204s (heavy tails from a few hard instances).
- Branching remains shallow: median 3 nodes, mean ~40 nodes per instance.
- The notebook writes plots/tables to `analysis_out/` so they can be dropped straight into the report without rerun.
- If I add FK_* datasets later, I’ll re-run the notebook and append them as “extended benchmarks.”

Project structure (key paths):

- `src/main/java/ca/udem/gaillarz/model/` – MKP data classes.
- `src/main/java/ca/udem/gaillarz/io/` – Instance reader/validator.
- `src/main/java/ca/udem/gaillarz/formulation/` – Classic/L2/DW formulations, patterns, conversions.
- `src/main/java/ca/udem/gaillarz/solver/` – LP layer, column generation, branch-and-price, SAT checker.
- `src/main/resources/` – Benchmark set instance files
- `src/test/java/` – Unit tests.

## Prerequisites

- Java 25
- Maven 3.9+
- JUnit 5 (included via Maven) (needed for tests only)
- Internet access for Maven to download OR-Tools (`com.google.ortools:ortools-java`).

## Build, test, run

```bash
# compile
mvn compile

# run tests
mvn test

# launch CLI demo
mvn exec:java -Dexec.mainClass=ca.udem.gaillarz.Main

# launch benchmark runner
mvn exec:java -Dexec.mainClass=ca.udem.gaillarz.benchmark.MainBenchmark
```

You 

## CLI usage

When the CLI starts you can:

- `1` Pick one instance file from a chosen resources subdirectory.
- `2` Solve all instances in a chosen resources subdirectory (prints a summary at the end).
- `v` Toggle verbose logging on/off (persists across runs within the session).
- `0` Exit.

### Benchmark runner

Batch benchmark all provided instance sets (SMALL, FK_1..FK_4) with default limits and emit CSV/JSON summaries under
`benchmark_results/`:

```bash
mvn exec:java -Dexec.mainClass=ca.udem.gaillarz.benchmark.MainBenchmark
```

## Instance format

Located under `src/main/resources/`. Each `.txt` file follows:

```
m            # number of knapsacks
n            # number of items
c1..cm       # knapsack capacities (one per line)
w p          # weight/profit for each item (n lines)
```

See `src/main/resources/readme.txt` for the exact layout.

## Implementation notes

- **Column generation**: Builds a restricted master LP from the DW master, extracts duals, solves knapsack-based
  pricing, and iterates until optimality or limits (`CGParameters`).
- **Branch-and-price**: Manages nodes with bounds/pruning, applies branching filters, re-runs column generation per
  node, and uses a SAT-based checker to repair fractional assignments.
- **Outputs**: `BPResult` reports status, objective, best bound, gap, node counts, and runtime; solutions can be
  rendered in classic form for readability.

### Flow and key classes

- **DW master build**: `L2RelaxedFormulation.toDantzigWolfeFormulation()` constructs the master; `PatternInitializer`
  seeds empty/singleton and greedy/core patterns; `DWMasterLPBuilder` turns the DW model into a restricted LP.
- **Restricted master solve**: `ColumnGeneration` calls the LP solver (default `ORToolsSolver`), reads duals, and
  invokes `PricingProblem` (knapsack subproblem) to inject profitable patterns; terminates on optimality, iteration
  limit, or no improving column.
- **Branching**: `BranchAndPrice` runs a best-first search over `BranchNode`s ordered by upper bound. `BranchingRule`
  picks a fractional item-selection variable `t_j`, rebuilds a node-specific master, and re-enters column generation.
- **No-good cuts**: `NoGoodCutManager` tracks excluded item-selection sets (P0 patterns) discovered infeasible by SAT.
- **Fractional repair**: When item selections are integral but assignments are fractional, `OrToolsCpSatVSBPPSATChecker`
  solves a SAT/CP feasibility fix before further branching.
- **Statistics & summaries**: Each `BPResult` retains LB/UB, gap, runtime, and node counts; CLI batch runs print
  aggregate success/failure/optimality plus per-instance objective, gap, and time.

## Contributing & troubleshooting

- Ensure Java 25 is on your `PATH` (`java -version`).
- If OR-Tools native libs fail to load, rerun Maven with a clean local repo or check platform compatibility.
- Add new instances under `src/main/resources/<folder>/` with `.txt` extension.
