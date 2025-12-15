# MKP Formulation Hierarchy

This project implements a complete formulation hierarchy for the Multiple Knapsack Problem (MKP), from reading instances to the Dantzig-Wolfe master problem formulation.

## Overview

The implementation includes four main formulations:

1. **MKP Instance** - Raw problem data (items with weights/profits, knapsacks with capacities)
2. **Classic Formulation** - Standard MKP formulation (equations 1-4)
3. **L2 Relaxed Formulation** - Lagrangian relaxation with t_j variables (equations 13-18)
4. **Dantzig-Wolfe Master Formulation** - Pattern-based reformulation (equations 28-33)

## Mathematical Formulations

### Classic MKP (Equations 1-4)

```
max  Σᵢ Σⱼ pⱼ · xᵢⱼ                          (1)
s.t. Σⱼ wⱼ · xᵢⱼ ≤ cᵢ     for all i          (2)
     Σᵢ xᵢⱼ ≤ 1           for all j          (3)
     xᵢⱼ ∈ {0,1}                             (4)
```

### L2 Relaxed Formulation (Equations 13-18)

Introduces item selection variables `tⱼ`:

```
max  Σⱼ pⱼ · tⱼ                              (13)
s.t. Σⱼ wⱼ · xᵢⱼ ≤ cᵢ     for all i          (14)
     tⱼ ≤ Σᵢ xᵢⱼ          for all j          (15)
     Σⱼ wⱼ · tⱼ ≤ Σᵢ cᵢ                      (16)
     tⱼ ∈ {0,1}, xᵢⱼ ∈ {0,1}                 (17-18)
```

### Dantzig-Wolfe Master (Equations 28-33)

Uses patterns (subsets of items) as decision variables:

- **P₀**: Patterns respecting aggregated capacity
- **Pᵢ**: Patterns respecting knapsack i's capacity

Key relationships:
```
tⱼ = Σₐ∈P₀ aⱼ · yₐ     (derive item selection)
xᵢⱼ = Σₐ∈Pᵢ aⱼ · yₐ   (derive item assignment)
```

## Project Structure

```
src/main/java/ca/udem/gaillarz/
├── model/
│   ├── Item.java            # Item with weight and profit
│   ├── Knapsack.java        # Knapsack with capacity
│   └── MKPInstance.java     # Complete problem instance
├── io/
│   ├── InstanceReader.java       # Read/write instance files
│   └── InvalidInstanceException.java
├── formulation/
│   ├── ClassicFormulation.java   # Classic MKP formulation
│   ├── ClassicSolution.java      # Solution with x_ij variables
│   ├── L2RelaxedFormulation.java # L2 formulation with t_j
│   ├── L2Solution.java           # Solution with t_j and x_ij
│   ├── Pattern.java              # Pattern (subset of items)
│   ├── DantzigWolfeMaster.java   # DW master formulation
│   ├── DWSolution.java           # Solution with y_a patterns
│   └── FormulationException.java
└── Main.java                     # Demo application
```

## Instance File Format

```
m                    # Number of knapsacks
n                    # Number of items
c₁                   # Capacity of knapsack 1
c₂                   # Capacity of knapsack 2
...
cₘ                   # Capacity of knapsack m
w₁   p₁              # Weight and profit of item 1
w₂   p₂              # Weight and profit of item 2
...
wₙ   pₙ              # Weight and profit of item n
```

## Usage Examples

### Reading an Instance

```java
MKPInstance instance = InstanceReader.readFromFile("instance.txt");
System.out.println(instance.toDetailedString());
```

### Creating and Using Formulations

```java
// Create formulation hierarchy
ClassicFormulation classic = new ClassicFormulation(instance);
L2RelaxedFormulation l2 = classic.toL2Formulation();
DantzigWolfeMaster dw = l2.toDantzigWolfeFormulation();

// Display mathematical formulation
System.out.println(classic.toMathematicalString());
```

### Working with Solutions

```java
// Create a classic solution
ClassicSolution solution = new ClassicSolution(m, n);
solution.assignItem(0, 0);  // Assign item 0 to knapsack 0
solution.assignItem(1, 1);  // Assign item 1 to knapsack 1

// Check feasibility and compute objective
boolean feasible = classic.isFeasible(solution);
double objective = classic.computeObjectiveValue(solution);

// Convert to L2 solution
L2Solution l2Solution = L2Solution.fromClassicSolution(solution, n);
```

### Working with Patterns (DW Formulation)

```java
// Create patterns
Pattern p1 = Pattern.singleItem(0, instance);
Pattern p2 = Pattern.fromItemIds(Set.of(0, 2, 4), instance);
Pattern empty = Pattern.empty(n);

// Add to DW master
dw.addPatternP0(p1);
dw.addPatternPI(0, p2);

// Create DW solution
Map<Pattern, Double> patternValues = new HashMap<>();
patternValues.put(p1, 1.0);
DWSolution dwSolution = new DWSolution(patternValues, n);

// Convert DW → L2 → Classic
L2Solution derived = dw.toL2Solution(dwSolution);
if (derived.isInteger()) {
    ClassicSolution classic = derived.toClassicSolution();
}
```

## Building and Testing

```bash
# Compile
mvn compile

# Run tests
mvn test

# Run main demo
mvn exec:java -Dexec.mainClass=ca.udem.gaillarz.Main
```

## Key Design Decisions

1. **Immutability**: Solution classes use defensive copying for arrays/collections
2. **Tolerance**: Uses 1e-5 tolerance for floating-point comparisons
3. **Validation**: Each formulation validates solution dimensions and constraints
4. **Visualization**: Rich string representations for debugging and analysis

## Conversion Relationships

```
Classic ←→ L2 ←→ DW
         ↑
    (can convert back
     to Classic if integer)
```

- Classic → L2: Derives t_j from x_ij values
- L2 → Classic: Only works if solution is integer
- L2 → DW: Creates DW master with pattern pools
- DW → L2: Derives t_j and x_ij from pattern values y_a

## Next Steps

This foundation supports future implementation of:

1. **PatternGenerator** - Create initial patterns
2. **Column Generation** - Solve DW master LP
3. **Pricing Problems** - Generate new profitable patterns
4. **Branch-and-Price** - Complete optimization algorithm

## License

Academic use for research purposes.

