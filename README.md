# Branch-and-Price Multiple Knapsack Toolkit

This repository implements a full formulation hierarchy and column-generation loop for the Multiple Knapsack Problem (MKP). It includes classic and relaxed formulations, Dantzig–Wolfe master construction, pattern generation, and a runnable demo that stitches everything together.

## Features
- **Classic, L2, and Dantzig–Wolfe formulations** with helpers to convert solutions between representations. 【F:src/main/java/ca/udem/gaillarz/formulation/ClassicFormulation.java†L12-L73】【F:src/main/java/ca/udem/gaillarz/formulation/L2RelaxedFormulation.java†L14-L74】【F:src/main/java/ca/udem/gaillarz/formulation/DantzigWolfeMaster.java†L28-L96】
- **Pattern management** (initialization, statistics, variables) to seed and expand the master problem. 【F:src/main/java/ca/udem/gaillarz/formulation/PatternInitializer.java†L18-L96】【F:src/main/java/ca/udem/gaillarz/formulation/PatternVariable.java†L10-L65】【F:src/main/java/ca/udem/gaillarz/formulation/PatternStatistics.java†L9-L78】
- **Column generation loop** with OR-Tools-backed LP solving, pricing, and progress reporting. 【F:src/main/java/solver/ColumnGeneration.java†L19-L132】【F:src/main/java/solver/ORToolsSolver.java†L10-L75】【F:src/main/java/solver/CGResult.java†L8-L63】
- **Interactive demo** that loads instances, runs the formulation pipeline, and prints readable tables. 【F:src/main/java/ca/udem/gaillarz/Main.java†L19-L154】

## Project layout
- `src/main/java/ca/udem/gaillarz/model/` — MKP data model (`Item`, `Knapsack`, `MKPInstance`). 【F:src/main/java/ca/udem/gaillarz/model/MKPInstance.java†L8-L147】
- `src/main/java/ca/udem/gaillarz/io/` — Parsing helpers and validation for instance files. 【F:src/main/java/ca/udem/gaillarz/io/InstanceReader.java†L20-L156】
- `src/main/java/ca/udem/gaillarz/formulation/` — Classic/L2/DW formulations, pattern utilities, and conversions. 【F:src/main/java/ca/udem/gaillarz/formulation/DantzigWolfeMaster.java†L28-L130】
- `src/main/java/solver/` — Lightweight linear programming and column-generation framework, including OR-Tools integration. 【F:src/main/java/solver/LinearProgram.java†L7-L94】【F:src/main/java/solver/ORToolsSolver.java†L10-L75】
- `src/main/resources/` — Example instance descriptions and metadata. 【F:src/main/resources/readme.txt†L1-L8】
- `src/test/` — JUnit 5 tests that cover the formulations, pattern generation, and DW master setup. 【F:src/test/java/ca/udem/gaillarz/formulation/DantzigWolfeMasterTest.java†L13-L108】【F:src/test/java/ca/udem/gaillarz/formulation/PatternGeneratorTest.java†L11-L99】

## Getting started
### Prerequisites
- Java 25
- Maven 3.9+
- OR-Tools native libraries are fetched via Maven; no manual install is required.

### Build & test
```bash
mvn compile
mvn test
```

### Run the interactive demo
The `Main` class presents several ways to explore the pipeline:
1. Hardcoded toy example
2. Random instance from a chosen resource directory
3. All instances within a resource directory
4. All discovered instances under `src/main/resources`

Start the demo with:
```bash
mvn exec:java -Dexec.mainClass=ca.udem.gaillarz.Main
```

When prompted, select an option. For file-based runs, the CLI lists available resource folders and picks files ending in `.txt` while ignoring `readme.txt`. 【F:src/main/java/ca/udem/gaillarz/Main.java†L21-L111】【F:src/main/java/ca/udem/gaillarz/Main.java†L158-L214】

## Instance format
Instance files follow the structure documented in `src/main/resources/readme.txt`:
```
# counts
m            # number of knapsacks
n            # number of items

# capacities
c1
c2
...
cm

# item data (weight profit)
w1 p1
w2 p2
...
wn pn
```
【F:src/main/resources/readme.txt†L1-L8】

## Column generation workflow
1. Build the **classic** formulation, check feasibility, and derive an **L2** relaxation.
2. Convert to the **Dantzig–Wolfe master** with initial patterns seeded by `PatternInitializer` (singleton and empty patterns).
3. Run **column generation** using `ColumnGeneration` and `ORToolsSolver`, which iteratively solves the restricted master, extracts duals, prices new patterns, and adds them to the master until optimal or iteration limits are reached. 【F:src/main/java/ca/udem/gaillarz/formulation/PatternInitializer.java†L18-L96】【F:src/main/java/solver/ColumnGeneration.java†L19-L132】
4. Visualize the final DW and derived L2/classic solutions through the demo output. 【F:src/main/java/ca/udem/gaillarz/Main.java†L67-L153】

## License
Academic/research use only.
