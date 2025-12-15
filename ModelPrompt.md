# Implementation Prompt: MKP Formulation Hierarchy

## Overview

Implement the complete formulation hierarchy for the Multiple Knapsack Problem (MKP), from reading instances to the Dantzig-Wolfe master problem formulation. This includes:

1. **MKP Instance** - Raw problem data
2. **Classic Formulation** - Standard MKP formulation (equations 1-4)
3. **L2 Relaxed Formulation** - Lagrangian relaxation with t_j variables (equations 13-18)
4. **Dantzig-Wolfe Master Formulation** - Pattern-based reformulation (equations 28-33)

The key insight is that each formulation can be converted to adjacent formulations, with the DW master being able to convert back to L2 (deriving t_j and x_ij from pattern values y_a).

---

## Mathematical Formulations

### 1. Classic MKP Formulation (Equations 1-4)

```
(MKP) max Σ_{i=1}^{m} Σ_{j=1}^{n} p_j * x_ij                    (1)

s.t.  Σ_{j=1}^{n} w_j * x_ij ≤ c_i        for i = 1,...,m      (2)
      
      Σ_{i=1}^{m} x_ij ≤ 1                 for j = 1,...,n      (3)
      
      x_ij ∈ {0,1}                         for all i,j          (4)
```

**Variables:**
- `x_ij` = 1 if item j is assigned to knapsack i, 0 otherwise

**Constraints:**
- (2) Capacity constraints: total weight in each knapsack ≤ capacity
- (3) Assignment constraints: each item assigned to at most one knapsack

---

### 2. L2 Formulation (Equations 13-18)

```
max Σ_{j=1}^{n} p_j * t_j                                       (13)

s.t.  Σ_{j=1}^{n} w_j * x_ij ≤ c_i        for i = 1,...,m      (14)
      
      t_j ≤ Σ_{i=1}^{m} x_ij               for j = 1,...,n      (15)
      
      Σ_{j=1}^{n} w_j * t_j ≤ Σ_{i=1}^{m} c_i                  (16)
      
      t_j ∈ {0,1}                          for j = 1,...,n      (17)
      
      x_ij ∈ {0,1}                         for all i,j          (18)
```

**New Variables:**
- `t_j` = 1 if item j is selected (assigned to some knapsack), 0 otherwise

**Key Additions:**
- (15) Linking constraints: t_j can only be 1 if item j is assigned somewhere
- (16) Aggregated capacity constraint: total weight of selected items ≤ total capacity

**Note:** Constraint (16) is redundant in the formulation but critical for the Lagrangian relaxation and DW decomposition.

---

### 2b. Lagrangian Relaxation L2(μ) (Equation 19)

For a vector μ ∈ ℝ^n₊ of non-negative Lagrangian multipliers, the **Lagrangian relaxation L2(μ)** is obtained by dualizing constraint (15):

```
(L2(μ))  max Σ_{j=1}^{n} (p_j - μ_j) * t_j + Σ_{j=1}^{n} μ_j * (Σ_{i=1}^{m} x_ij)    (19)

s.t.     Σ_{j=1}^{n} w_j * x_ij ≤ c_i        for i = 1,...,m      (14)
         
         Σ_{j=1}^{n} w_j * t_j ≤ Σ_{i=1}^{m} c_i                  (16)
         
         x_ij ∈ {0,1}                         for all i,j          (18)
         
         t_j ∈ {0,1}                          for j = 1,...,n      (17)
```

**Key Properties:**
- Constraint (15) has been **relaxed** (removed from hard constraints)
- Violations of (15) are **penalized** in the objective via the Lagrangian term
- For any μ ≥ 0: `z_L2(μ) ≥ z_L2` (provides upper bound)
- The **Lagrangian dual** is: `z_L2 = min{z_L2(μ) | μ ≥ 0}`

**Decomposition Structure:**
- For fixed μ, L2(μ) **decomposes** into m+1 independent subproblems:
    - **m knapsack problems** (one per knapsack i): solve for x_ij with profits μ_j
    - **1 aggregated problem**: solve for t_j with profits (p_j - μ_j)

**This is the foundation for:**
1. The Dantzig-Wolfe decomposition (treating μ as dual variables)
2. Column generation (pricing subproblems are derived from this)
3. Providing tight upper bounds (better than surrogate relaxation)

---

### 3. Dantzig-Wolfe Master Formulation (Equations 28-33)

**Pattern Definitions:**
- **P_0**: Set of all feasible patterns respecting aggregated capacity
  ```
  P_0 = {a ∈ {0,1}^n | Σ_{j=1}^{n} w_j * a_j ≤ Σ_{i=1}^{m} c_i}
  ```

- **P_i**: Set of all feasible patterns for knapsack i
  ```
  P_i = {a ∈ {0,1}^n | Σ_{j=1}^{n} w_j * a_j ≤ c_i}  for i = 1,...,m
  ```

- **a_j**: Indicator, a_j = 1 if item j is in pattern a, 0 otherwise

**Formulation:**

```
max Σ_{a∈P_0} (Σ_{j=1}^{n} p_j * a_j) * y_a - Σ_{j=1}^{n} p_j * s_j     (28)

s.t.  Σ_{a∈P_0} a_j * y_a ≤ Σ_{i=1}^{m} Σ_{a∈P_i} a_j * y_a + s_j  
                                             for j = 1,...,n      (29)

      Σ_{a∈P_i} y_a = 1                      for i = 0,...,m      (30)

      Σ_{j=1}^{n} p_j * (Σ_{a∈P_0} a_j * y_a) ≤ UB                (31)

      y_a ∈ {0,1}                            for all a, all i     (32)

      0 ≤ s_j ≤ 1                            for j = 1,...,n      (33)
```

**Variables:**
- `y_a` = 1 if pattern a is selected, 0 otherwise
- `s_j` = binary dual cut variables (for stabilization)

**Constraints:**
- (29) Item-consistency constraints: ensure items selected in P_0 are assigned in P_i
- (30) Pattern selection: exactly one pattern selected for P_0 and each P_i
- (31) Upper bound constraint (UB is typically ⌊value of LP relaxation⌋)
- (33) Dual cut variables are continuous in [0,1]

**Key Relationships to L2:**
```
t_j = Σ_{a∈P_0} a_j * y_a                    (derive item selection)

x_ij = Σ_{a∈P_i} a_j * y_a                   (derive item assignment)
```

---

## Implementation Requirements

### 1. Core Data Structures

#### Item.java
```java
public class Item {
    private final int id;
    private final int weight;  // w_j
    private final int profit;  // p_j
    
    public Item(int id, int weight, int profit);
    public int getId();
    public int getWeight();
    public int getProfit();
    
    @Override
    public boolean equals(Object o);
    @Override
    public int hashCode();
    @Override
    public String toString();
}
```

#### Knapsack.java
```java
public class Knapsack {
    private final int id;
    private final int capacity;  // c_i
    
    public Knapsack(int id, int capacity);
    public int getId();
    public int getCapacity();
    
    @Override
    public boolean equals(Object o);
    @Override
    public int hashCode();
    @Override
    public String toString();
}
```

#### MKPInstance.java
```java
public class MKPInstance {
    private final List<Item> items;
    private final List<Knapsack> knapsacks;
    private final String name;  // Optional instance name
    
    public MKPInstance(List<Item> items, List<Knapsack> knapsacks);
    public MKPInstance(List<Item> items, List<Knapsack> knapsacks, String name);
    
    // Getters
    public int getNumItems();  // n
    public int getNumKnapsacks();  // m
    public Item getItem(int j);
    public Knapsack getKnapsack(int i);
    public List<Item> getItems();
    public List<Knapsack> getKnapsacks();
    public String getName();
    
    // Computed properties
    public int getTotalCapacity();  // Σ_{i=1}^{m} c_i
    public int getTotalWeight();    // Σ_{j=1}^{n} w_j
    public int getTotalProfit();    // Σ_{j=1}^{n} p_j
    public double getAverageProfitWeightRatio();  // Avg(p_j / w_j)
    
    // Validation
    public boolean isValid();
    
    // ========== VISUALIZATION ==========
    
    /**
     * Compact string representation
     * Example: "MKP(n=10, m=3, total_cap=50)"
     */
    @Override
    public String toString();
    
    /**
     * Detailed multi-line representation showing all items and knapsacks
     * 
     * Example output:
     * ┌─────────────────────────────────────┐
     * │ MKP Instance: test_01               │
     * │ Items: 5, Knapsacks: 2              │
     * ├─────────────────────────────────────┤
     * │ Items:                              │
     * │   [0] profit=10, weight=5  (r=2.00) │
     * │   [1] profit=8,  weight=4  (r=2.00) │
     * │   [2] profit=6,  weight=3  (r=2.00) │
     * │   [3] profit=5,  weight=2  (r=2.50) │
     * │   [4] profit=4,  weight=1  (r=4.00) │
     * ├─────────────────────────────────────┤
     * │ Knapsacks:                          │
     * │   [0] capacity=7                    │
     * │   [1] capacity=6                    │
     * ├─────────────────────────────────────┤
     * │ Total capacity: 13                  │
     * │ Total weight:   15                  │
     * │ Total profit:   33                  │
     * └─────────────────────────────────────┘
     */
    public String toDetailedString();
    
    /**
     * ASCII table representation
     * 
     * Example output:
     * Item Summary:
     * +----+--------+--------+-------+
     * | ID | Profit | Weight | Ratio |
     * +----+--------+--------+-------+
     * |  0 |     10 |      5 |  2.00 |
     * |  1 |      8 |      4 |  2.00 |
     * |  2 |      6 |      3 |  2.00 |
     * |  3 |      5 |      2 |  2.50 |
     * |  4 |      4 |      1 |  4.00 |
     * +----+--------+--------+-------+
     * 
     * Knapsack Summary:
     * +----+----------+
     * | ID | Capacity |
     * +----+----------+
     * |  0 |        7 |
     * |  1 |        6 |
     * +----+----------+
     */
    public String toTable();
}
```

---

### 2. Instance Reader

#### InstanceReader.java

**File Format:**
```
n m
p_1 w_1
p_2 w_2
...
p_n w_n
c_1
c_2
...
c_m
```

**Example:**
```
5 2
10 5
8 4
6 3
5 2
4 1
7
6
```

**Implementation:**
```java
public class InstanceReader {
    
    /**
     * Read MKP instance from file
     * 
     * @param filepath Path to instance file
     * @return MKPInstance object
     * @throws IOException if file cannot be read
     * @throws InvalidInstanceException if instance format is invalid
     */
    public static MKPInstance readFromFile(String filepath) 
        throws IOException, InvalidInstanceException;
    
    /**
     * Write MKP instance to file
     * 
     * @param instance Instance to write
     * @param filepath Output file path
     * @throws IOException if file cannot be written
     */
    public static void writeToFile(MKPInstance instance, String filepath) 
        throws IOException;
    
    /**
     * Parse instance from string (useful for testing)
     */
    public static MKPInstance parseFromString(String content) 
        throws InvalidInstanceException;
}

public class InvalidInstanceException extends Exception {
    public InvalidInstanceException(String message);
    public InvalidInstanceException(String message, Throwable cause);
}
```

**Validation Rules:**
- n, m must be positive integers
- All weights, profits, capacities must be positive
- At least one item must fit in at least one knapsack
- Total item weight should exceed at least one knapsack capacity (non-trivial)

---

### 3. Classic Formulation

#### ClassicFormulation.java

```java
public class ClassicFormulation {
    private final MKPInstance instance;
    
    public ClassicFormulation(MKPInstance instance);
    
    public MKPInstance getInstance();
    
    /**
     * Compute objective value: Σ_i Σ_j p_j * x_ij (equation 1)
     */
    public double computeObjectiveValue(ClassicSolution solution);
    
    /**
     * Check if solution satisfies all constraints
     */
    public boolean isFeasible(ClassicSolution solution);
    
    /**
     * Check capacity constraints (equation 2)
     */
    public boolean checkCapacityConstraints(ClassicSolution solution);
    
    /**
     * Check assignment constraints (equation 3)
     */
    public boolean checkAssignmentConstraints(ClassicSolution solution);
    
    /**
     * Get total weight in knapsack i
     */
    public int getKnapsackWeight(ClassicSolution solution, int knapsackId);
    
    /**
     * Get items assigned to knapsack i
     */
    public Set<Integer> getItemsInKnapsack(ClassicSolution solution, int knapsackId);
    
    /**
     * Convert to L2 relaxed formulation
     */
    public L2RelaxedFormulation toL2Formulation();
    
    // ========== VISUALIZATION ==========
    
    /**
     * Compact representation showing formulation type and size
     * Example: "ClassicMKP(n=10, m=3)"
     */
    @Override
    public String toString();
    
    /**
     * Display the mathematical formulation with actual values
     * 
     * Example output:
     * Classic MKP Formulation
     * ═══════════════════════════════════════
     * 
     * max  10*x[0][0] + 10*x[1][0] + 8*x[0][1] + 8*x[1][1] + ...
     * 
     * s.t. Capacity constraints:
     *      5*x[0][0] + 4*x[0][1] + 3*x[0][2] + 2*x[0][3] + 1*x[0][4] ≤ 7
     *      5*x[1][0] + 4*x[1][1] + 3*x[1][2] + 2*x[1][3] + 1*x[1][4] ≤ 6
     * 
     *      Assignment constraints:
     *      x[0][0] + x[1][0] ≤ 1
     *      x[0][1] + x[1][1] ≤ 1
     *      ...
     * 
     *      x[i][j] ∈ {0,1}
     */
    public String toMathematicalString();
    
    /**
     * Visualize a solution in the context of this formulation
     */
    public String visualizeSolution(ClassicSolution solution);
}
```

#### ClassicSolution.java

```java
public class ClassicSolution {
    private final boolean[][] assignment;  // x_ij values
    private final int numKnapsacks;
    private final int numItems;
    
    /**
     * Create solution from assignment matrix
     * assignment[i][j] = true if item j assigned to knapsack i
     */
    public ClassicSolution(boolean[][] assignment);
    
    /**
     * Create empty solution (all items unassigned)
     */
    public ClassicSolution(int numKnapsacks, int numItems);
    
    // Query methods
    public boolean isItemAssigned(int itemId);
    public boolean isItemInKnapsack(int knapsackId, int itemId);
    public int getKnapsackForItem(int itemId);  // -1 if unassigned
    
    // Modification methods
    public void assignItem(int knapsackId, int itemId);
    public void unassignItem(int itemId);
    
    // Export
    public boolean[][] getAssignment();  // Returns copy
    
    // ========== VISUALIZATION ==========
    
    /**
     * Compact representation
     * Example: "ClassicSolution(assigned=3/5)"
     */
    @Override
    public String toString();
    
    /**
     * Matrix representation showing assignment
     * 
     * Example output:
     * Assignment Matrix (x_ij):
     *        Item 0  Item 1  Item 2  Item 3  Item 4
     * KS 0:    1       0       1       0       0
     * KS 1:    0       1       0       1       1
     */
    public String toMatrixString();
    
    /**
     * Visual representation with items in each knapsack
     * 
     * Example output:
     * ┌──────────────────────────────┐
     * │ Knapsack 0 (used: 8/7) ⚠️    │
     * │   Items: [0, 2]              │
     * │   Weights: 5, 3              │
     * │   Profits: 10, 6             │
     * │   Total: weight=8, profit=16 │
     * ├──────────────────────────────┤
     * │ Knapsack 1 (used: 7/6) ⚠️    │
     * │   Items: [1, 3, 4]           │
     * │   Weights: 4, 2, 1           │
     * │   Profits: 8, 5, 4           │
     * │   Total: weight=7, profit=17 │
     * ├──────────────────────────────┤
     * │ Unassigned: []               │
     * └──────────────────────────────┘
     * Total profit: 33
     * Feasible: ✗ (capacity violations)
     */
    public String toDetailedString(MKPInstance instance);
}
```

---

### 4. L2 Relaxed Formulation

#### L2RelaxedFormulation.java

```java
public class L2RelaxedFormulation {
    private final MKPInstance instance;
    private final int totalCapacity;  // Σ_i c_i (cached)
    
    public L2RelaxedFormulation(MKPInstance instance);
    
    public MKPInstance getInstance();
    public int getTotalCapacity();
    
    /**
     * Compute objective value: Σ_j p_j * t_j (equation 13)
     */
    public double computeObjectiveValue(L2Solution solution);
    
    /**
     * Compute Lagrangian objective L2(μ) (equation 19)
     * Objective = Σ_j (p_j - μ_j)*t_j + Σ_j μ_j*(Σ_i x_ij)
     */
    public double computeLagrangianObjective(L2Solution solution, double[] mu);
    
    /**
     * Check if solution satisfies all constraints (equations 14-18)
     */
    public boolean isFeasible(L2Solution solution);
    
    /**
     * Check knapsack capacity constraints (equation 14)
     */
    public boolean checkKnapsackCapacities(L2Solution solution);
    
    /**
     * Check linking constraints: t_j ≤ Σ_i x_ij (equation 15)
     */
    public boolean checkLinkingConstraints(L2Solution solution);
    
    /**
     * Check aggregated capacity: Σ_j w_j*t_j ≤ Σ_i c_i (equation 16)
     */
    public boolean checkAggregatedCapacity(L2Solution solution);
    
    /**
     * Convert to Dantzig-Wolfe master formulation
     */
    public DantzigWolfeMaster toDantzigWolfeFormulation();
    
    /**
     * Convert back to classic formulation
     */
    public ClassicFormulation toClassicFormulation();
    
    // ========== VISUALIZATION ==========
    
    /**
     * Compact representation
     * Example: "L2RelaxedMKP(n=10, m=3, total_cap=50)"
     */
    @Override
    public String toString();
    
    /**
     * Display the mathematical formulation
     * 
     * Example output:
     * L2 Relaxed Formulation
     * ═══════════════════════════════════════
     * 
     * max  10*t[0] + 8*t[1] + 6*t[2] + 5*t[3] + 4*t[4]
     * 
     * s.t. Knapsack capacity constraints:
     *      5*x[0][0] + 4*x[0][1] + 3*x[0][2] + 2*x[0][3] + 1*x[0][4] ≤ 7
     *      5*x[1][0] + 4*x[1][1] + 3*x[1][2] + 2*x[1][3] + 1*x[1][4] ≤ 6
     * 
     *      Linking constraints:
     *      t[0] ≤ x[0][0] + x[1][0]
     *      t[1] ≤ x[0][1] + x[1][1]
     *      ...
     * 
     *      Aggregated capacity:
     *      5*t[0] + 4*t[1] + 3*t[2] + 2*t[3] + 1*t[4] ≤ 13
     * 
     *      t[j] ∈ {0,1}, x[i][j] ∈ {0,1}
     */
    public String toMathematicalString();
    
    /**
     * Display Lagrangian relaxation for given μ
     * 
     * Example output:
     * L2(μ) Lagrangian Relaxation
     * ═══════════════════════════════════════
     * μ = [2.0, 1.5, 1.0, 0.5, 0.0]
     * 
     * max  (10-2.0)*t[0] + (8-1.5)*t[1] + ... 
     *      + 2.0*(x[0][0] + x[1][0]) + ...
     *    = 8.0*t[0] + 6.5*t[1] + ... + 2.0*Σ_i x[i][0] + ...
     * 
     * s.t. [constraints 14, 16, 17, 18]
     * 
     * Note: Constraint (15) has been dualized
     */
    public String toLagrangianString(double[] mu);
    
    /**
     * Visualize a solution
     */
    public String visualizeSolution(L2Solution solution);
}
```

#### L2Solution.java

```java
public class L2Solution {
    private final double[] t;     // Item selection variables (t_j)
    private final double[][] x;   // Assignment variables (x_ij)
    
    /**
     * Create L2 solution from t_j and x_ij values
     */
    public L2Solution(double[] t, double[][] x);
    
    /**
     * Create from classic solution (derives t_j from x_ij)
     */
    public static L2Solution fromClassicSolution(ClassicSolution classic, int numItems);
    
    // Query methods for t_j
    public double getItemSelection(int itemId);
    public boolean isItemSelected(int itemId);  // t_j ≈ 1
    public boolean isItemSelectionInteger(int itemId);
    
    // Query methods for x_ij
    public double getItemAssignment(int knapsackId, int itemId);
    public boolean isItemInKnapsack(int knapsackId, int itemId);  // x_ij ≈ 1
    public boolean isItemAssignmentInteger(int knapsackId, int itemId);
    
    // Integrality checks
    public boolean areItemSelectionsInteger();  // All t_j integer
    public boolean areAssignmentsInteger();     // All x_ij integer
    public boolean isInteger();                  // Both t_j and x_ij integer
    
    // Export
    public double[] getItemSelections();   // Returns copy of t
    public double[][] getAssignments();    // Returns copy of x
    
    // Conversion
    public ClassicSolution toClassicSolution() throws IllegalStateException;
    
    // ========== VISUALIZATION ==========
    
    /**
     * Compact representation
     * Example: "L2Solution(selected=3.5/5, integer_t=yes, integer_x=no)"
     */
    @Override
    public String toString();
    
    /**
     * Show t_j values
     * 
     * Example output:
     * Item Selection (t_j):
     * ┌──────┬─────────┬─────────┐
     * │ Item │   t_j   │ Integer │
     * ├──────┼─────────┼─────────┤
     * │   0  │  1.000  │    ✓    │
     * │   1  │  0.750  │    ✗    │
     * │   2  │  1.000  │    ✓    │
     * │   3  │  0.500  │    ✗    │
     * │   4  │  0.000  │    ✓    │
     * └──────┴─────────┴─────────┘
     */
    public String toItemSelectionString();
    
    /**
     * Show x_ij values
     * 
     * Example output:
     * Assignment Matrix (x_ij):
     *        Item 0  Item 1  Item 2  Item 3  Item 4
     * KS 0:  1.000   0.500   1.000   0.000   0.000
     * KS 1:  0.000   0.250   0.000   0.500   0.000
     */
    public String toAssignmentString();
    
    /**
     * Combined detailed view
     * 
     * Example output:
     * ┌────────────────────────────────────────────┐
     * │ L2 Solution                                │
     * ├────────────────────────────────────────────┤
     * │ Item Selection (t_j):                      │
     * │   t[0] = 1.000 ✓  t[1] = 0.750 ✗           │
     * │   t[2] = 1.000 ✓  t[3] = 0.500 ✗           │
     * │   t[4] = 0.000 ✓                           │
     * │                                            │
     * │ Total selected: 3.25 items                 │
     * │ Integer t_j: YES                           │
     * │ Integer x_ij: NO                           │
     * ├────────────────────────────────────────────┤
     * │ Assignment details:                        │
     * │   [Assignment matrix shown above]          │
     * └────────────────────────────────────────────┘
     */
    public String toDetailedString(MKPInstance instance);
}
```

---

### 5. Pattern and Dantzig-Wolfe Formulation

#### Pattern.java

```java
public class Pattern {
    private final boolean[] items;      // a_j indicators
    private final int totalWeight;      // Σ_j w_j * a_j (cached)
    private final double totalProfit;   // Σ_j p_j * a_j (cached)
    private final int id;               // Unique pattern ID
    
    /**
     * Create pattern from item indicators
     * 
     * @param items Array where items[j] = true if item j in pattern
     * @param instance MKP instance (to compute weight/profit)
     */
    public Pattern(boolean[] items, MKPInstance instance);
    
    /**
     * Create pattern from item IDs
     */
    public static Pattern fromItemIds(Set<Integer> itemIds, MKPInstance instance);
    
    /**
     * Create single-item pattern
     */
    public static Pattern singleItem(int itemId, MKPInstance instance);
    
    /**
     * Create empty pattern
     */
    public static Pattern empty(int numItems);
    
    // Query methods
    public boolean containsItem(int itemId);
    public int getTotalWeight();
    public double getTotalProfit();
    public int getNumItems();
    public Set<Integer> getItemIds();
    public boolean[] getItems();  // Returns copy
    
    /**
     * Check if pattern respects capacity
     */
    public boolean isFeasible(int capacity);
    
    /**
     * Get profit/weight ratio
     */
    public double getEfficiency();
    
    // ========== VISUALIZATION ==========
    
    /**
     * Compact representation
     * Example: "Pattern(items={0,2,4}, w=10, p=20.0)"
     */
    @Override
    public String toString();
    
    /**
     * Detailed representation
     * 
     * Example output:
     * ┌─────────────────────────┐
     * │ Pattern #42             │
     * ├─────────────────────────┤
     * │ Items: {0, 2, 4}        │
     * │ Count: 3                │
     * │ Total Weight: 10        │
     * │ Total Profit: 20.0      │
     * │ Efficiency: 2.00        │
     * │ Feasible (cap≤15): ✓    │
     * └─────────────────────────┘
     */
    public String toDetailedString(int capacity);
    
    /**
     * Show pattern with item details
     * 
     * Example output:
     * Pattern #42: {0, 2, 4}
     * +------+--------+--------+
     * | Item | Weight | Profit |
     * +------+--------+--------+
     * |   0  |      5 |   10.0 |
     * |   2  |      3 |    6.0 |
     * |   4  |      2 |    4.0 |
     * +------+--------+--------+
     * Total:       10     20.0
     */
    public String toTableString(MKPInstance instance);
    
    @Override
    public boolean equals(Object o);
    @Override
    public int hashCode();
}
```

#### DantzigWolfeMaster.java

```java
public class DantzigWolfeMaster {
    private final MKPInstance instance;
    private final L2RelaxedFormulation l2Formulation;  // Reference for conversion
    
    // Pattern pools
    private final List<Pattern> patternsP0;           // P_0 patterns
    private final List<List<Pattern>> patternsPI;     // P_i patterns (one list per knapsack)
    
    // Upper bound for constraint (31)
    private double upperBound;
    
    public DantzigWolfeMaster(L2RelaxedFormulation l2Formulation);
    
    public MKPInstance getInstance();
    public L2RelaxedFormulation getL2Formulation();
    
    // ========== Pattern Management ==========
    
    /**
     * Add pattern to P_0 (aggregated capacity)
     * Only adds if pattern is feasible for total capacity
     */
    public void addPatternP0(Pattern pattern);
    
    /**
     * Add pattern to P_i (knapsack i)
     * Only adds if pattern is feasible for knapsack capacity
     */
    public void addPatternPI(int knapsackId, Pattern pattern);
    
    /**
     * Get all patterns in P_0
     */
    public List<Pattern> getPatternsP0();
    
    /**
     * Get all patterns for knapsack i
     */
    public List<Pattern> getPatternsPI(int knapsackId);
    
    /**
     * Get total number of patterns across all sets
     */
    public int getTotalPatternCount();
    
    /**
     * Remove pattern from P_0
     */
    public boolean removePatternP0(Pattern pattern);
    
    /**
     * Remove pattern from P_i
     */
    public boolean removePatternPI(int knapsackId, Pattern pattern);
    
    /**
     * Clear all patterns
     */
    public void clearAllPatterns();
    
    /**
     * Set upper bound for constraint (31)
     */
    public void setUpperBound(double ub);
    
    public double getUpperBound();
    
    // ========== Objective and Feasibility ==========
    
    /**
     * Compute objective value (equation 28)
     * Objective = Σ_{a∈P_0} (Σ_j p_j * a_j) * y_a - Σ_j p_j * s_j
     */
    public double computeObjectiveValue(DWSolution solution);
    
    /**
     * Check if solution satisfies all constraints (equations 29-33)
     */
    public boolean isFeasible(DWSolution solution);
    
    /**
     * Check item-consistency constraints (equation 29)
     * Σ_{a∈P_0} a_j*y_a ≤ Σ_{i=1}^m Σ_{a∈P_i} a_j*y_a + s_j
     */
    public boolean checkItemConsistency(DWSolution solution);
    
    /**
     * Check pattern selection constraints (equation 30)
     * Σ_{a∈P_i} y_a = 1 for i = 0,...,m
     */
    public boolean checkPatternSelection(DWSolution solution);
    
    /**
     * Check upper bound constraint (equation 31)
     * Σ_j p_j * (Σ_{a∈P_0} a_j*y_a) ≤ UB
     */
    public boolean checkUpperBound(DWSolution solution);
    
    // ========== Conversion Methods (KEY FUNCTIONALITY) ==========
    
    /**
     * Convert DW solution to L2 solution
     * 
     * Derives:
     *   t_j = Σ_{a∈P_0} a_j * y_a
     *   x_ij = Σ_{a∈P_i} a_j * y_a
     * 
     * This is the main way to go from DW back to L2
     */
    public L2Solution toL2Solution(DWSolution dwSolution);
    
    /**
     * Convert L2 solution to Classic solution
     * Only works if L2 solution is integer
     * 
     * @throws IllegalArgumentException if L2 solution is fractional
     */
    public ClassicSolution toClassicSolution(L2Solution l2Solution);
    
    /**
     * Direct conversion from DW to Classic (combines above two)
     * Only works if derived L2 solution would be integer
     */
    public ClassicSolution toClassicSolution(DWSolution dwSolution);
    
    // ========== Helper Methods ==========
    
    /**
     * Compute t_j value from DW solution
     * t_j = Σ_{a∈P_0} a_j * y_a
     */
    public double computeItemSelection(DWSolution solution, int itemId);
    
    /**
     * Compute x_ij value from DW solution
     * x_ij = Σ_{a∈P_i} a_j * y_a
     */
    public double computeItemAssignment(DWSolution solution, int knapsackId, int itemId);
    
    /**
     * Get total profit from P_0 patterns in solution
     */
    public double getTotalProfitP0(DWSolution solution);
    
    // ========== VISUALIZATION ==========
    
    /**
     * Compact representation
     * Example: "DWMKP(n=10, m=3, |P_0|=25, |P_i|=[12,10,11], UB=100.5)"
     */
    @Override
    public String toString();
    
    /**
     * Display formulation structure
     * 
     * Example output:
     * Dantzig-Wolfe Master Formulation
     * ═══════════════════════════════════════
     * Instance: n=5, m=2
     * 
     * Pattern Pools:
     *   P_0: 15 patterns (total capacity = 13)
     *   P_1: 8 patterns  (capacity = 7)
     *   P_2: 7 patterns  (capacity = 6)
     * 
     * Total patterns: 30
     * Upper bound: 33.0
     * 
     * Variables:
     *   y_a: 30 pattern variables
     *   s_j: 5 dual cut variables
     */
    public String toStructureString();
    
    /**
     * Show patterns in each pool
     * 
     * Example output:
     * Pattern Pool P_0 (15 patterns):
     * ┌────┬──────────────┬────────┬────────┬────────────┐
     * │ ID │    Items     │ Weight │ Profit │ Efficiency │
     * ├────┼──────────────┼────────┼────────┼────────────┤
     * │  0 │ {0}          │      5 │   10.0 │       2.00 │
     * │  1 │ {1}          │      4 │    8.0 │       2.00 │
     * │  2 │ {0, 1}       │      9 │   18.0 │       2.00 │
     * │  3 │ {2, 3, 4}    │      6 │   15.0 │       2.50 │
     * │... │ ...          │    ... │    ... │        ... │
     * └────┴──────────────┴────────┴────────┴────────────┘
     * 
     * Pattern Pool P_1 (8 patterns):
     * [similar table]
     */
    public String toPatternsString();
    
    /**
     * Visualize a DW solution
     */
    public String visualizeSolution(DWSolution solution);
}
```

#### DWSolution.java

```java
public class DWSolution {
    private final Map<Pattern, Double> patternValues;  // y_a values
    private final Map<Integer, Double> dualCutValues;  // s_j values
    
    /**
     * Create DW solution from pattern and dual cut values
     */
    public DWSolution(Map<Pattern, Double> patternValues, 
                      Map<Integer, Double> dualCutValues);
    
    /**
     * Create solution with zero dual cuts
     */
    public DWSolution(Map<Pattern, Double> patternValues, int numItems);
    
    // Query methods
    public double getPatternValue(Pattern pattern);
    public double getDualCutValue(int itemId);
    
    /**
     * Get all patterns with non-zero values
     */
    public Set<Pattern> getActivePatterns();
    
    /**
     * Get all patterns and their values
     */
    public Map<Pattern, Double> getPatternValues();
    
    /**
     * Get all dual cut values
     */
    public Map<Integer, Double> getDualCutValues();
    
    /**
     * Check if pattern values are integer (within tolerance)
     */
    public boolean arePatternValuesInteger();
    
    /**
     * Check if dual cuts are integer
     */
    public boolean areDualCutsInteger();
    
    /**
     * Check if entire solution is integer
     */
    public boolean isInteger();
    
    // ========== VISUALIZATION ==========
    
    /**
     * Compact representation
     * Example: "DWSolution(active_patterns=5/30, integer=no)"
     */
    @Override
    public String toString();
    
    /**
     * Show pattern values
     * 
     * Example output:
     * Active Patterns (y_a > 0):
     * ┌────────┬──────────────┬────────┬──────────┬──────────┐
     * │ Pool   │    Items     │ Profit │   y_a    │ Integer? │
     * ├────────┼──────────────┼────────┼──────────┼──────────┤
     * │ P_0    │ {0, 2}       │   16.0 │   0.750  │    ✗     │
     * │ P_0    │ {1, 3, 4}    │   17.0 │   0.250  │    ✗     │
     * │ P_1    │ {0, 2}       │   16.0 │   1.000  │    ✓     │
     * │ P_2    │ {1, 3}       │   13.0 │   0.500  │    ✗     │
     * │ P_2    │ {4}          │    4.0 │   0.500  │    ✗     │
     * └────────┴──────────────┴────────┴──────────┴──────────┘
     */
    public String toPatternValuesString();
    
    /**
     * Show dual cut values
     * 
     * Example output:
     * Dual Cuts (s_j):
     * ┌──────┬──────────┬──────────┐
     * │ Item │   s_j    │ Integer? │
     * ├──────┼──────────┼──────────┤
     * │   0  │   0.000  │    ✓     │
     * │   1  │   0.250  │    ✗     │
     * │   2  │   0.000  │    ✓     │
     * │   3  │   1.000  │    ✓     │
     * │   4  │   0.000  │    ✓     │
     * └──────┴──────────┴──────────┘
     */
    public String toDualCutsString();
    
    /**
     * Combined detailed view
     * 
     * Example output:
     * ┌────────────────────────────────────────────┐
     * │ Dantzig-Wolfe Solution                     │
     * ├────────────────────────────────────────────┤
     * │ Pattern values: 5 active / 30 total        │
     * │ Pattern values integer: NO                 │
     * │ Dual cuts integer: NO                      │
     * │ Overall integer: NO                        │
     * ├────────────────────────────────────────────┤
     * │ Derived item selections (t_j):             │
     * │   t[0] = 0.750 ✗  (from P_0 patterns)      │
     * │   t[1] = 0.250 ✗                           │
     * │   t[2] = 0.750 ✗                           │
     * │   t[3] = 0.750 ✗                           │
     * │   t[4] = 0.250 ✗                           │
     * ├────────────────────────────────────────────┤
     * │ [Pattern details shown above]              │
     * └────────────────────────────────────────────┘
     */
    public String toDetailedString(DantzigWolfeMaster master);
}
```

---

## Implementation Guidelines

### 1. Type Safety and Immutability

- All solution classes should be **immutable** where possible
- Use **defensive copying** for arrays and collections
- Return `Collections.unmodifiableList()` for list getters
- Clone arrays when passing them to/from constructors

### 2. Validation

Each formulation should validate:
- Dimensions match instance (m knapsacks, n items)
- Variable values are in valid range
- Solutions are internally consistent

### 3. Tolerance for Floating Point

Use a tolerance of `1e-5` for:
- Integer checks: `Math.abs(value - Math.round(value)) < 1e-5`
- Equality checks: `Math.abs(a - b) < 1e-5`
- Constraint satisfaction: `lhs <= rhs + 1e-5`

### 4. Error Handling

```java
public class FormulationException extends RuntimeException {
    public FormulationException(String message);
    public FormulationException(String message, Throwable cause);
}
```

Throw when:
- Converting fractional solution to integer formulation
- Pattern doesn't fit capacity but being added
- Solution dimensions don't match instance

### 5. Testing Requirements

For each formulation, test:

**Feasibility Tests:**
- Valid solutions are recognized as feasible
- Invalid solutions are recognized as infeasible
- Edge cases (empty solution, full solution)

**Objective Tests:**
- Objective computed correctly
- Matches across formulations for same solution

**Conversion Tests:**
- Classic → L2 → DW → L2 → Classic (roundtrip)
- Values preserved correctly
- Integer solutions convert cleanly

**Pattern Tests:**
- Single-item patterns
- Empty patterns
- Full patterns
- Feasibility checks
- Weight/profit computation

### 6. Example Usage

```java
// ========== READ AND VISUALIZE INSTANCE ==========
MKPInstance instance = InstanceReader.readFromFile("instances/small_01.txt");

System.out.println(instance.toDetailedString());
// Shows items, knapsacks, capacities in nice format

// ========== CREATE FORMULATION HIERARCHY ==========
ClassicFormulation classic = new ClassicFormulation(instance);
System.out.println(classic.toMathematicalString());
// Shows the classic MKP formulation with actual coefficients

L2RelaxedFormulation l2 = classic.toL2Formulation();
System.out.println(l2.toMathematicalString());
// Shows the L2 formulation with t_j variables

// Show Lagrangian relaxation for some μ
double[] mu = new double[instance.getNumItems()];
Arrays.fill(mu, 1.0);
System.out.println(l2.toLagrangianString(mu));
// Shows L2(μ) formulation

DantzigWolfeMaster dwMaster = l2.toDantzigWolfeFormulation();

// ========== GENERATE AND VISUALIZE PATTERNS ==========
PatternGenerator generator = new PatternGenerator(instance);

System.out.println("Generating initial patterns...");
for (Pattern p : generator.generateInitialPatternsP0()) {
    dwMaster.addPatternP0(p);
    System.out.println(p.toDetailedString(instance.getTotalCapacity()));
}

for (int i = 0; i < instance.getNumKnapsacks(); i++) {
    for (Pattern p : generator.generateInitialPatternsPI(i)) {
        dwMaster.addPatternPI(i, p);
    }
}

System.out.println(dwMaster.toStructureString());
// Shows pattern pool sizes and formulation structure

System.out.println(dwMaster.toPatternsString());
// Shows all patterns in a table

// ========== CREATE AND VISUALIZE SOLUTION ==========
// Create a dummy DW solution for testing conversion
Map<Pattern, Double> patternVals = new HashMap<>();
// Assume we have some patterns p1, p2, p3
patternVals.put(p1, 0.6);
patternVals.put(p2, 0.4);

Map<Integer, Double> dualCuts = new HashMap<>();
for (int j = 0; j < instance.getNumItems(); j++) {
    dualCuts.put(j, 0.0);
}

DWSolution dwSolution = new DWSolution(patternVals, dualCuts);

System.out.println(dwSolution.toDetailedString(dwMaster));
// Shows pattern values, dual cuts, and derived t_j values

// ========== CONVERT BACK TO L2 ==========
L2Solution l2Solution = dwMaster.toL2Solution(dwSolution);

System.out.println("\n=== Derived L2 Solution ===");
System.out.println(l2Solution.toItemSelectionString());
// Shows t_j values in a table

System.out.println(l2Solution.toAssignmentString());
// Shows x_ij values in a matrix

System.out.println(l2Solution.toDetailedString(instance));
// Complete visualization with feasibility check

// Check integrality
if (l2Solution.areItemSelectionsInteger()) {
    System.out.println("✓ All t_j values are integer!");
} else {
    System.out.println("✗ Some t_j values are fractional");
}

// ========== CONVERT TO CLASSIC IF INTEGER ==========
if (l2Solution.isInteger()) {
    ClassicSolution classicSolution = dwMaster.toClassicSolution(l2Solution);
    
    System.out.println("\n=== Classic Solution ===");
    System.out.println(classicSolution.toDetailedString(instance));
    // Shows items in each knapsack with weights/profits
    
    // Verify feasibility
    if (classic.isFeasible(classicSolution)) {
        double objective = classic.computeObjectiveValue(classicSolution);
        System.out.printf("✓ Found feasible integer solution!\n");
        System.out.printf("  Objective value: %.2f\n", objective);
    } else {
        System.out.println("✗ Solution is not feasible!");
    }
}

// ========== COMPARE ACROSS FORMULATIONS ==========
// If we have integer solutions, verify objectives match
if (l2Solution.isInteger()) {
    double objClassic = classic.computeObjectiveValue(classicSolution);
    double objL2 = l2.computeObjectiveValue(l2Solution);
    double objDW = dwMaster.computeObjectiveValue(dwSolution);
    
    System.out.printf("\nObjective values:\n");
    System.out.printf("  Classic:  %.4f\n", objClassic);
    System.out.printf("  L2:       %.4f\n", objL2);
    System.out.printf("  DW:       %.4f\n", objDW);
    
    if (Math.abs(objClassic - objL2) < 1e-6 && 
        Math.abs(objL2 - objDW) < 1e-6) {
        System.out.println("✓ All objectives match!");
    }
}
```

**Expected Output:**
```
┌─────────────────────────────────────┐
│ MKP Instance: small_01              │
│ Items: 5, Knapsacks: 2              │
├─────────────────────────────────────┤
│ Items:                              │
│   [0] profit=10, weight=5  (r=2.00) │
│   [1] profit=8,  weight=4  (r=2.00) │
│   [2] profit=6,  weight=3  (r=2.00) │
│   [3] profit=5,  weight=2  (r=2.50) │
│   [4] profit=4,  weight=1  (r=4.00) │
├─────────────────────────────────────┤
│ Knapsacks:                          │
│   [0] capacity=7                    │
│   [1] capacity=6                    │
├─────────────────────────────────────┤
│ Total capacity: 13                  │
│ Total weight:   15                  │
│ Total profit:   33                  │
└─────────────────────────────────────┘

[... formulation displays ...]

Item Selection (t_j):
┌──────┬─────────┬─────────┐
│ Item │   t_j   │ Integer │
├──────┼─────────┼─────────┤
│   0  │  1.000  │    ✓    │
│   1  │  0.750  │    ✗    │
│   2  │  1.000  │    ✓    │
│   3  │  0.500  │    ✗    │
│   4  │  0.000  │    ✓    │
└──────┴─────────┴─────────┘

[... more output ...]
```

---

## Deliverables

1. **Core Classes:**
    - `Item.java`
    - `Knapsack.java`
    - `MKPInstance.java`

2. **Instance I/O:**
    - `InstanceReader.java`
    - `InvalidInstanceException.java`

3. **Classic Formulation:**
    - `ClassicFormulation.java`
    - `ClassicSolution.java`

4. **L2 Formulation:**
    - `L2RelaxedFormulation.java`
    - `L2Solution.java`

5. **Dantzig-Wolfe Formulation:**
    - `Pattern.java`
    - `DantzigWolfeMaster.java`
    - `DWSolution.java`

6. **Utilities:**
    - `FormulationException.java`

7. **Tests:**
    - `InstanceReaderTest.java`
    - `ClassicFormulationTest.java`
    - `L2RelaxedFormulationTest.java`
    - `PatternTest.java`
    - `DantzigWolfeMasterTest.java`
    - `FormulationConversionTest.java` (tests conversions between all formulations)

8. **Example Files:**
    - `test_instance_01.txt` (3 items, 2 knapsacks - trivial)
    - `test_instance_02.txt` (5 items, 2 knapsacks - simple)
    - `test_instance_03.txt` (10 items, 3 knapsacks - medium)

9. **Documentation:**
    - Javadoc for all public classes and methods
    - README explaining the formulation hierarchy
    - Examples of converting between formulations

---

## Key Implementation Notes

1. **Pattern Identity:**
    - Patterns should be compared by content (item set), not reference
    - Implement proper `equals()` and `hashCode()` based on items array
    - Consider using pattern IDs for debugging/logging

2. **Conversion Accuracy:**
    - The conversion DW → L2 is exact (mathematical relationship)
    - Verify conversions with assertions and tests
    - Log warnings if derived values are unexpected

3. **Performance Considerations:**
    - Cache computed values (totalWeight, totalProfit in Pattern)
    - Use efficient data structures (HashMap for pattern lookups)
    - Avoid creating unnecessary Pattern objects

4. **Future Integration:**
    - These formulations will be used by:
        * Column Generation (adds patterns to DW master)
        * Pricing Problems (generates new patterns)
        * Branch-and-Price (checks integrality, converts solutions)
    - Design interfaces to be friendly for these use cases

---

## Success Criteria

Your implementation is complete when:

1. ✅ All instances can be read from files correctly
2. ✅ All three formulations can be created and used
3. ✅ Objective values computed correctly for all formulations
4. ✅ Feasibility checks work for all formulations
5. ✅ Conversions work: Classic ↔ L2 ↔ DW
6. ✅ Pattern management (add/remove/query) works correctly
7. ✅ The conversion DW → L2 correctly derives t_j and x_ij from y_a
8. ✅ All unit tests pass
9. ✅ Code is well-documented with Javadoc
10. ✅ Example programs demonstrate usage

---

## Next Steps

After completing this implementation, you will:

1. Implement **PatternGenerator** to create initial patterns
2. Implement **Column Generation** to solve the DW master LP
3. Implement **Pricing Problems** to generate new profitable patterns
4. These components will use the formulations you build here

This foundational work is critical - everything else builds on top of these formulations!