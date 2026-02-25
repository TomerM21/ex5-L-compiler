# Ex5 — Full MIPS Code Generation: Implementation Plan

> Compiler course, TAU 0368-3133 · Due 15/3/2026  
> This plan is self-contained. Any team member can pick up any section independently.

---

## Overview

Transform the ex4 (dataflow-analysis) compiler into a full MIPS code generator for the complete L language.

**Three main tracks (can be done in parallel after reading this doc):**

| Track | Who | What |
|-------|-----|------|
| A | Member 1 | Fix IR generation gaps (function labels, class methods, method calls, string ops) |
| B | Member 2 | Liveness analysis + interference graph + register allocator |
| C | Member 3 | Full `MipsGenerator` rewrite + `mipsMe` on every IR command |
| D | All | Wire everything in `Main.java` + testing |

---

## Critical Background: What Exists & What's Broken

The codebase passes all ex1–ex4 tests. For ex5, several IR generation gaps must be fixed **first**, before any MIPS work.

### Current gaps

| # | File | Problem |
|---|------|---------|
| 1 | `src/ast/Dec/AstFuncDec.java` `irMe()` | Never emits an `IrCommandLabel` — functions have no entry marker in the IR |
| 2 | `src/ast/Dec/AstDecList.java` `irMeFuncDecs()` | Skips `AstClassDec` nodes — class methods never generate IR |
| 3 | `src/ast/AstCallExp.java` `irMe()` | Ignores `receiver` for method calls; never passes `self`; ignores `PrintString` |
| 4 | `src/ast/Exp/AstExpBinop.java` `irMe()` | Uses `IrCommandBinopEqIntegers` for all `=` regardless of type; uses add for string `+` |
| 5 | `src/types/TypeClass.java` | `fields` is a `HashMap` — field order is non-deterministic, breaking class memory layout |
| 6 | `src/ast/Exp/AstNewExp.java` | `irMe()` likely unimplemented (ex4 didn't need it) |
| 7 | `src/mips/MipsGenerator.java` | Stub only; not wired into `Main.java`; no real MIPS output |

---

## Track A — Fix IR Generation

**Owner: _____________**

### A1. Fix `TypeClass` field ordering

**File:** `src/types/TypeClass.java`

Change:
```java
public HashMap<String, Type> fields = new HashMap<>();
```
to:
```java
public LinkedHashMap<String, Type> fields = new LinkedHashMap<>();
```
(Also add `import java.util.LinkedHashMap;`)

Add helper method:
```java
/**
 * Returns the 0-based index of a field in this class's full layout
 * (inherited fields from father come first, in their declaration order).
 * Returns -1 if not found.
 */
public int getFieldOffset(String fieldName) {
    int offset = 0;
    if (father != null) {
        int fatherResult = father.getFieldOffset(fieldName);
        if (fatherResult >= 0) return fatherResult;
        offset = father.totalFieldCount();
    }
    int i = 0;
    for (String key : fields.keySet()) {
        if (key.equals(fieldName)) return offset + i;
        i++;
    }
    return -1;
}

public int totalFieldCount() {
    int parentCount = (father != null) ? father.totalFieldCount() : 0;
    return parentCount + fields.size();
}
```

### A2. Add param registry to `Ir` singleton

**File:** `src/ir/Ir.java`

Add these fields and methods:
```java
// Maps scoped IR param name (e.g. "x_1") -> 0-based argument index
private Map<String, Integer> paramIndex = new HashMap<>();

public void registerParam(String irName, int index) {
    paramIndex.put(irName, index);
}

public boolean isParam(String irName) {
    return paramIndex.containsKey(irName);
}

public int getParamIndex(String irName) {
    return paramIndex.get(irName);
}
```

### A3. Add `isFunctionEntry` flag to `IrCommandLabel`

**File:** `src/ir/IrCommandLabel.java`

Add field:
```java
public boolean isFunctionEntry = false;
```

Add second constructor:
```java
public IrCommandLabel(String labelName, boolean isFunctionEntry) {
    this.labelName = labelName;
    this.isFunctionEntry = isFunctionEntry;
}
```

### A4. Fix `AstFuncDec.irMe()`

**File:** `src/ast/Dec/AstFuncDec.java`

Replace the current `irMe()` body:
```java
public Temp irMe() {
    SymbolTable tbl = SymbolTable.getInstance();

    // Determine function name (may be prefixed with class name)
    String funcLabel = (tbl.currentClass != null)
        ? tbl.currentClass.name + "_" + name
        : name;

    // Emit function-entry label
    IrCommandLabel label = new IrCommandLabel(funcLabel, true);
    Ir.getInstance().AddIrCommand(label);

    // Register parameters (so MIPS generator knows they come from caller's stack)
    if (typeList != null) {
        AstTypeList it = typeList;
        int idx = 0;
        // If this is a method, slot 0 is "self" — start params at slot 1
        if (tbl.currentClass != null) idx = 1;
        while (it != null) {
            String scopedName = it.name + "_" + tbl.getScopeIndexOf(it.name);
            Ir.getInstance().registerParam(scopedName, idx);
            idx++;
            it = it.tail;
        }
    }

    // Emit body
    tbl.beginScope();
    if (stmtList != null) stmtList.irMe();
    tbl.endScope();

    return null;
}
```

> **Note:** Check how `getScopeIndexOf` works — it returns the scope index suffix appended to the variable's IR name. Adjust if the API differs.

### A5. Fix `AstClassDec.irMe()`

**File:** `src/ast/Dec/AstClassDec.java`

Replace the current no-op `irMe()`:
```java
public temp.Temp irMe() {
    SymbolTable tbl = SymbolTable.getInstance();
    TypeClass previousClass = tbl.currentClass;
    tbl.currentClass = (TypeClass) tbl.find(name);  // get own TypeClass

    // Emit IR for each method in declaration order
    if (dataMemberList != null) {
        AstCFieldList it = dataMemberList;
        while (it != null) {
            if (it.head instanceof AstFuncDec) {
                ((AstFuncDec) it.head).irMe();
            }
            it = it.tail;
        }
    }

    tbl.currentClass = previousClass;
    return null;
}
```

> **Note:** `AstCFieldList` stores both field declarations and method declarations. Only call `irMe()` on the method ones (cast check).

### A6. Fix `AstDecList.irMeFuncDecs()`

**File:** `src/ast/Dec/AstDecList.java`

Change:
```java
public void irMeFuncDecs() {
    if (head instanceof AstFuncDec) head.irMe();
    if (tail != null) tail.irMeFuncDecs();
}
```
to:
```java
public void irMeFuncDecs() {
    if (head instanceof AstFuncDec || head instanceof AstClassDec) head.irMe();
    if (tail != null) tail.irMeFuncDecs();
}
```

### A7. Fix `AstCallExp.irMe()`

**File:** `src/ast/AstCallExp.java`

First, add a field set during `SemantMe()`:
```java
private String resolvedClassName = null; // set when receiver != null
```

In `SemantMe()`, just before `return funcType.returnType;`, add:
```java
if (receiver != null) {
    this.resolvedClassName = tc.name;
}
```

Replace `irMe()`:
```java
public temp.Temp irMe() {
    // Evaluate args left-to-right
    ir.TempList argList = null;

    // For method calls, self is the first argument
    if (receiver != null) {
        temp.Temp selfTemp = receiver.irMe();
        argList = new ir.TempList(selfTemp, null);
    }

    // Evaluate explicit args and append
    if (args != null) {
        ir.TempList explicitArgs = args.irMe();
        if (argList == null) {
            argList = explicitArgs;
        } else {
            // Append explicitArgs to end of argList
            ir.TempList cursor = argList;
            while (cursor.tail != null) cursor = cursor.tail;
            cursor.tail = explicitArgs;
        }
    }

    // Built-ins
    if ("PrintInt".equals(methodName) && argList != null) {
        ir.Ir.getInstance().AddIrCommand(new ir.IrCommandPrintInt(argList.head));
        return null;
    }
    if ("PrintString".equals(methodName) && argList != null) {
        ir.Ir.getInstance().AddIrCommand(new ir.IrCommandPrintString(argList.head));
        return null;
    }

    // Determine callee label
    String callee = (resolvedClassName != null)
        ? resolvedClassName + "_" + methodName
        : methodName;

    temp.Temp result = temp.TempFactory.getInstance().getFreshTemp();
    ir.Ir.getInstance().AddIrCommand(new ir.IrCommandCall(result, callee, argList));
    return result;
}
```

### A8. Create `IrCommandPrintString`

**New file:** `src/ir/IrCommandPrintString.java`

Copy the structure of `IrCommandPrintInt.java` but name it `IrCommandPrintString`.

### A9. Create `IrCommandBinopConcatStrings` and `IrCommandBinopEqPointers`

**New files:**

- `src/ir/IrCommandBinopConcatStrings.java` — same shape as `IrCommandBinopAddIntegers` (two temps → dst)
- `src/ir/IrCommandBinopEqPointers.java` — same shape as `IrCommandBinopEqIntegers`

### A10. Fix `AstExpBinop.irMe()` for string and class types

**File:** `src/ast/Exp/AstExpBinop.java`

In `irMe()`, when `op == 6 (=)` (equality), check the type of the left operand (stored during `SemantMe()` — add a field `Type leftType`):
```java
private Type leftType; // set in SemantMe()
```

In `SemantMe()` store the type of the left child when checking. Then in `irMe()`:
```java
// op == 6: equality
if (leftType instanceof TypeString) {
    // string content equality → calls __str_eq at runtime
    ir.Ir.getInstance().AddIrCommand(new ir.IrCommandBinopEqStrings(dst, t1, t2));
} else if (leftType instanceof TypeInt) {
    ir.Ir.getInstance().AddIrCommand(new ir.IrCommandBinopEqIntegers(dst, t1, t2));
} else {
    // class, array, nil → pointer equality
    ir.Ir.getInstance().AddIrCommand(new ir.IrCommandBinopEqPointers(dst, t1, t2));
}

// op == 0: add
if (leftType instanceof TypeString) {
    ir.Ir.getInstance().AddIrCommand(new ir.IrCommandBinopConcatStrings(dst, t1, t2));
} else {
    ir.Ir.getInstance().AddIrCommand(new ir.IrCommandBinopAddIntegers(dst, t1, t2));
}
```

### A11. Implement `AstNewExp.irMe()`

**File:** `src/ast/Exp/AstNewExp.java`

```java
public temp.Temp irMe() {
    temp.Temp dst = temp.TempFactory.getInstance().getFreshTemp();

    if (/* new ClassName */ newClassType != null) {
        ir.Ir.getInstance().AddIrCommand(new ir.IrCommandNewClass(dst, className));
        // Emit field initializations (constant literals only per spec)
        // For each field with a non-nil/non-zero initializer, emit FieldStore
        // ... (iterate over TypeClass.fields, check for initializer values stored during semantic pass)
    } else {
        // new type[exp]
        temp.Temp sizeTemp = sizeExp.irMe();
        ir.Ir.getInstance().AddIrCommand(new ir.IrCommandNewArray(dst, sizeTemp, elementTypeName));
    }
    return dst;
}
```

> **Note:** Check what fields `AstNewExp` currently has — adjust variable names to match.

---

## Track B — Liveness Analysis & Register Allocation

**Owner: _____________**

### B1. Create `LivenessAnalysis.java`

**New file:** `src/cfg/LivenessAnalysis.java`

```java
package cfg;

import ir.IrCommand;
import ir.IrCommandLabel;
import java.util.*;

/**
 * Computes liveness for temporaries (Temp_N) within one function's IR segment.
 * Named variables are accessed via lw/sw and do not need register allocation.
 */
public class LivenessAnalysis {
    private List<IrCommand> commands;
    // per-instruction live_out sets (index matches commands list)
    private List<Set<String>> liveOut;

    public LivenessAnalysis(List<IrCommand> commands) {
        this.commands = commands;
    }

    public void compute() {
        // 1. Build basic blocks from commands (reuse Fullcfggraph partitioning logic)
        // 2. Compute use/def per block (temps only — filter out names not starting with "Temp_")
        // 3. Backward dataflow: live_in[B] = use[B] ∪ (live_out[B] − def[B])
        //                       live_out[B] = ∪ live_in[succ(B)]
        // 4. Iterate until stable (worklist)
        // 5. Reconstruct per-instruction live_out by backwards simulation within each block
    }

    /** Returns live_out set for command at index i. Call after compute(). */
    public Set<String> getLiveOutAt(int i) {
        return liveOut.get(i);
    }

    public List<IrCommand> getCommands() { return commands; }
}
```

**Implementation notes:**
- Only track things whose names start with `"Temp_"` (named variables are in memory, not registers).
- Use `IrCommand.getReadTemps()` and `IrCommand.getWriteTemps()` (already implemented on all IR command classes) to get use/def per instruction.
- The CFG structure is identical to the one already built in `Fullcfggraph` — reuse or extend it.

### B2. Create `InterferenceGraph.java`

**New file:** `src/cfg/InterferenceGraph.java`

```java
package cfg;

import java.util.*;

public class InterferenceGraph {
    // adjacency set: node → set of interfering nodes
    private Map<String, Set<String>> adj = new HashMap<>();

    public void addNode(String temp) {
        adj.putIfAbsent(temp, new HashSet<>());
    }

    public void addEdge(String a, String b) {
        if (a.equals(b)) return;
        adj.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        adj.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    public Set<String> neighbors(String node) {
        return adj.getOrDefault(node, Collections.emptySet());
    }

    public int degree(String node) {
        return neighbors(node).size();
    }

    public Set<String> nodes() {
        return adj.keySet();
    }

    /** Build from liveness analysis results. */
    public static InterferenceGraph build(LivenessAnalysis la) {
        InterferenceGraph g = new InterferenceGraph();
        List<ir.IrCommand> cmds = la.getCommands();
        for (int i = 0; i < cmds.size(); i++) {
            ir.IrCommand c = cmds.get(i);
            Set<String> written = c.getWriteTemps();
            Set<String> liveAfter = la.getLiveOutAt(i);
            for (String def : written) {
                g.addNode(def);
                for (String live : liveAfter) {
                    if (!live.equals(def)) {
                        g.addEdge(def, live);
                    }
                }
            }
            // ensure all read temps are nodes too
            for (String r : c.getReadTemps()) g.addNode(r);
        }
        return g;
    }
}
```

### B3. Create `RegisterAllocator.java`

**New file:** `src/regalloc/RegisterAllocator.java`

```java
package regalloc;

import cfg.InterferenceGraph;
import java.util.*;

public class RegisterAllocator {
    private static final int K = 10; // $t0 – $t9
    private static final String[] REGS = {
        "$t0","$t1","$t2","$t3","$t4","$t5","$t6","$t7","$t8","$t9"
    };
    private final InterferenceGraph originalGraph;

    public RegisterAllocator(InterferenceGraph graph) {
        this.originalGraph = graph;
    }

    /**
     * Returns a coloring map Temp_N → "$tK".
     * Prints "Register Allocation Failed" and exits if coloring is impossible.
     */
    public Map<String, String> allocate() {
        // --- Simplification phase ---
        Deque<String> stack = new ArrayDeque<>();
        Set<String> removed = new HashSet<>();
        // Work on a virtual copy (track active degrees)
        Map<String, Set<String>> activeDeg = new HashMap<>();
        for (String n : originalGraph.nodes()) {
            activeDeg.put(n, new HashSet<>(originalGraph.neighbors(n)));
        }

        while (activeDeg.size() > removed.size()) {
            String best = null;
            for (String n : activeDeg.keySet()) {
                if (removed.contains(n)) continue;
                long deg = activeDeg.get(n).stream().filter(nb -> !removed.contains(nb)).count();
                if (deg < K) { best = n; break; }
            }
            if (best == null) {
                System.out.println("Register Allocation Failed");
                System.exit(1);
            }
            stack.push(best);
            removed.add(best);
        }

        // --- Reconstruction phase ---
        Map<String, String> coloring = new HashMap<>();
        while (!stack.isEmpty()) {
            String n = stack.pop();
            Set<String> usedColors = new HashSet<>();
            for (String nb : originalGraph.neighbors(n)) {
                if (coloring.containsKey(nb)) usedColors.add(coloring.get(nb));
            }
            for (String reg : REGS) {
                if (!usedColors.contains(reg)) {
                    coloring.put(n, reg);
                    break;
                }
            }
            if (!coloring.containsKey(n)) {
                // Should not happen if simplification succeeded
                System.out.println("Register Allocation Failed");
                System.exit(1);
            }
        }
        return coloring;
    }
}
```

---

## Track C — MIPS Generator

**Owner: _____________**

### C1. Rewrite `MipsGenerator.java`

**File:** `src/mips/MipsGenerator.java`  
This is a complete replacement.

#### Stack frame layout (memorize this)
```
High address
  ...
  arg[1]        at fp + 12
  arg[0]        at fp + 8
  saved $ra     at fp + 4     ← $fp points here after prologue
  saved $fp     at fp + 0
  saved $t0     at fp - 4
  saved $t1     at fp - 8
  ...
  saved $t9     at fp - 40
  local[0]      at fp - 44   ← first local variable
  local[1]      at fp - 48
  ...
Low address ($sp)
```

#### Key fields
```java
private PrintWriter out;
private StringBuilder dataSec = new StringBuilder();  // buffered .data entries
private String currentFunc = null;
// maps scoped IR var name → fp-relative offset (negative integers)
private Map<String, Integer> localOffset = new HashMap<>();
private int nextLocalSlot = -44; // decrements by 4 for each new local

// singleton
private static MipsGenerator instance = null;
public static void init(String outputPath) throws Exception { ... }
public static MipsGenerator getInstance() { return instance; }
```

#### Initialization
```java
public static void init(String outputPath) throws Exception {
    instance = new MipsGenerator();
    instance.out = new PrintWriter(outputPath);
    // error strings go into .data
    instance.dataSec.append("string_access_violation: .asciiz \"Access Violation\"\n");
    instance.dataSec.append("string_illegal_div_by_0: .asciiz \"Illegal Division By Zero\"\n");
    instance.dataSec.append("string_invalid_ptr_dref: .asciiz \"Invalid Pointer Dereference\"\n");
}
```

#### Method: `startFunction(String name, int numLocalSlots)`
Emits prologue exactly as in `examples/test_1.s`:
```
[name == "main" ? "user_main" : name]:
# prologue
subu $sp, $sp, 4
sw $ra 0($sp)
subu $sp, $sp, 4
sw $fp 0($sp)
move $fp, $sp
subu $sp, $sp, 4
sw $t0, 0($sp)
...
subu $sp, $sp, 4
sw $t9, 0($sp)
subu $sp, $sp, [numLocalSlots * 4]
```
Reset `localOffset` map and `nextLocalSlot = -44` for each new function.

#### Method: `endFunction(String name)`
Emits epilogue label and restore sequence:
```
[name]_epilogue:
move $sp, $fp
lw $t0, -4($sp)
...
lw $t9, -40($sp)
lw $fp, 0($sp)
lw $ra, 4($sp)
addu $sp, $sp, 8
jr $ra
```

#### Method: `int resolveLocal(String irVarName)`
Returns the fp-relative offset for a local variable, allocating a new slot if needed:
```java
public int resolveLocal(String irVarName) {
    if (!localOffset.containsKey(irVarName)) {
        localOffset.put(irVarName, nextLocalSlot);
        nextLocalSlot -= 4;
    }
    return localOffset.get(irVarName);
}
```

#### Method: `globalVarAllocate(String irVarName)`
Appends to `dataSec`:
```
irVarName: .word 0
```

#### Method: `globalStringAllocate(String ptrLabel, String strValue)`
Appends to `dataSec`:
```
ptrLabel_str: .asciiz "strValue"
ptrLabel: .word ptrLabel_str
```

#### Method: `emitMipsMain()`
```
main:
jal user_main
li $v0, 10
syscall
```

#### Method: `emitRuntimeHandlers()`
Emits three error handler labels into text section:
```
__div_by_zero_handler:
  la $a0, string_illegal_div_by_0
  li $v0, 4
  syscall
  li $v0, 10
  syscall

__nil_handler:
  la $a0, string_invalid_ptr_dref
  li $v0, 4
  syscall
  li $v0, 10
  syscall

__bounds_handler:
  la $a0, string_access_violation
  li $v0, 4
  syscall
  li $v0, 10
  syscall
```

#### Method: `finalizeFile()`
Write buffered `.data` section, then `.text`, then all the text content, then close.

#### Saturation helper (call after every integer arithmetic op)
```java
public void emitSaturate(String reg) {
    // clamp reg to [-32768, 32767]
    String skipMax = freshLabel("sat_max");
    String skipMin = freshLabel("sat_min");
    out.printf("li $s0, 32767\n");
    out.printf("ble %s, $s0, %s\n", reg, skipMax);
    out.printf("move %s, $s0\n", reg);
    out.printf("%s:\n", skipMax);
    out.printf("li $s0, -32768\n");
    out.printf("bge %s, $s0, %s\n", reg, skipMin);
    out.printf("move %s, $s0\n", reg);
    out.printf("%s:\n", skipMin);
}
```

#### Nil check helper
```java
public void emitNilCheck(String reg) {
    out.printf("beqz %s, __nil_handler\n", reg);
}
```

#### Div-by-zero check helper
```java
public void emitDivByZeroCheck(String reg) {
    out.printf("beqz %s, __div_by_zero_handler\n", reg);
}
```

#### Bounds check helper
```java
public void emitBoundsCheck(String arrReg, String idxReg) {
    // index must be >= 0 and < array[0] (length stored at offset 0)
    String ok = freshLabel("bounds_ok");
    out.printf("bltz %s, __bounds_handler\n", idxReg);
    out.printf("lw $s1, 0(%s)\n", arrReg);      // load stored length
    out.printf("blt %s, $s1, %s\n", idxReg, ok);
    out.printf("j __bounds_handler\n");
    out.printf("%s:\n", ok);
}
```

#### String runtime functions
Emit `__str_concat` and `__str_eq` once per output file (use a flag to avoid double-emit). These are MIPS subroutines called via `jal`. Example signatures:
- `__str_concat`: args in `$a0` (ptr1) and `$a1` (ptr2); result in `$v0`
- `__str_eq`: args in `$a0` and `$a1`; result in `$v0` (1=equal, 0=not)

Implementation is a byte loop — allocate heap for concat, loop for eq.

---

### C2. Add `mipsMe` to every `IrCommand` subclass

Each class gets:
```java
public void mipsMe(mips.MipsGenerator mg, Map<String,String> regMap, String funcName)
```

**Register resolution convention:**  
- `Map<String,String> regMap` maps `"Temp_N" → "$tK"` (from register allocator)
- Named variables (not `Temp_*`) are accessed via `lw`/`sw` to their memory location
- `$s0–$s4` are free for use as scratch within any `mipsMe` impl

Helper to resolve a temp to its register:
```java
private String reg(String tempName, Map<String,String> regMap) {
    return regMap.getOrDefault(tempName, "$t0"); // should always be found
}
```

#### `IrCommandLabel.mipsMe`
```java
if (isFunctionEntry) {
    // Count local slots needed (pre-pass done by Main.java; pass count in separately OR
    // use a fixed large number and trim in epilogue — simplest: use 64 slots = 256 bytes)
    mg.startFunction(labelName, 64);
} else {
    mg.emit(labelName + ":\n");
}
```

#### `IrCommandReturn.mipsMe`
```java
if (returnValue != null) {
    String r = reg("Temp_" + returnValue.getSerialNumber(), regMap);
    mg.emit("move $v0, " + r + "\n");
}
mg.emit("j " + funcName + "_epilogue\n");
// (no need to emit epilogue here — it's emitted by the end-of-function detection in Main)
```

After detecting the function boundary in Main.java, call `mg.endFunction(funcName)`.

#### `IRcommandConstInt.mipsMe`
```java
String r = reg("Temp_" + t.getSerialNumber(), regMap);
mg.emit("li " + r + ", " + value + "\n");
```

#### `IrCommandConstString.mipsMe`
```java
String strLabel = mg.globalStringAllocate("str_" + freshId(), value);
String r = reg("Temp_" + t.getSerialNumber(), regMap);
mg.emit("la " + r + ", " + strLabel + "\n");
```

#### `IrCommandLoad.mipsMe`
```java
String r = reg("Temp_" + dst.getSerialNumber(), regMap);
Ir irSingleton = Ir.getInstance();
if (irSingleton.isParam(varName)) {
    int idx = irSingleton.getParamIndex(varName);
    int fpOffset = 8 + idx * 4;
    mg.emit("lw " + r + ", " + fpOffset + "($fp)\n");
} else if (/* is global */) {
    mg.emit("lw " + r + ", " + varName + "\n");
} else {
    int slot = mg.resolveLocal(varName);
    mg.emit("lw " + r + ", " + slot + "($fp)\n");
}
```

To detect globals: check `Ir.getInstance().isGlobal(varName)`. Add `Set<String> globals` and `registerGlobal(name)` to `Ir`, called from `IrCommandAllocate` when emitted before the first function label.

#### `IrCommandStore.mipsMe`
Mirror of Load — `sw` to global label or `sw` to fp-relative slot.

#### `IrCommandAllocate.mipsMe`
```java
if (/* before any function */ mg.isGlobalPhase()) {
    mg.globalVarAllocate(varName);
} else {
    mg.resolveLocal(varName); // just allocates the slot, no instruction
}
```

#### `IrCommandBinopAddIntegers.mipsMe`
```java
String r1 = reg("Temp_"+t1.getSerialNumber(), regMap);
String r2 = reg("Temp_"+t2.getSerialNumber(), regMap);
String rd = reg("Temp_"+dst.getSerialNumber(), regMap);
mg.emit("add " + rd + ", " + r1 + ", " + r2 + "\n");
mg.emitSaturate(rd);
```

#### `IrCommandBinopSubIntegers.mipsMe`, `IrCommandBinopMulIntegers.mipsMe`
Same pattern with `sub`/`mul` + saturation.

#### `IrCommandBinopDivIntegers.mipsMe`
```java
String r1 = reg(...), r2 = reg(...), rd = reg(...);
mg.emitDivByZeroCheck(r2);
mg.emit("div " + r1 + ", " + r2 + "\n");
mg.emit("mflo " + rd + "\n");
mg.emitSaturate(rd);
```

#### `IrCommandBinopLtIntegers.mipsMe`
```java
mg.emit("slt " + rd + ", " + r1 + ", " + r2 + "\n");
```

#### `IrCommandBinopGtIntegers.mipsMe`
```java
mg.emit("sgt " + rd + ", " + r1 + ", " + r2 + "\n");
```

#### `IrCommandBinopEqIntegers.mipsMe`
```java
mg.emit("seq " + rd + ", " + r1 + ", " + r2 + "\n");
```

#### `IrCommandBinopEqPointers.mipsMe`
```java
mg.emit("seq " + rd + ", " + r1 + ", " + r2 + "\n");
```

#### `IrCommandBinopConcatStrings.mipsMe`
```java
mg.emit("move $a0, " + r1 + "\n");
mg.emit("move $a1, " + r2 + "\n");
mg.emit("jal __str_concat\n");
mg.emit("move " + rd + ", $v0\n");
```

#### `IrCommandBinopEqStrings.mipsMe` (new class)
```java
mg.emit("move $a0, " + r1 + "\n");
mg.emit("move $a1, " + r2 + "\n");
mg.emit("jal __str_eq\n");
mg.emit("move " + rd + ", $v0\n");
```

#### `IrCommandJumpLabel.mipsMe`
```java
mg.emit("j " + labelName + "\n");
```

#### `IrCommandJumpIfEqToZero.mipsMe`
```java
String r = reg("Temp_"+t.getSerialNumber(), regMap);
mg.emit("beqz " + r + ", " + label + "\n");
```

#### `IrCommandCall.mipsMe`
```java
// Push args right-to-left
TempList[] argsArr = toArray(args);
for (int i = argsArr.length - 1; i >= 0; i--) {
    String r = reg("Temp_"+argsArr[i].getSerialNumber(), regMap);
    mg.emit("subu $sp, $sp, 4\n");
    mg.emit("sw " + r + ", 0($sp)\n");
}
String callee = funcName.equals("main") ? "user_main" : funcName;
mg.emit("jal " + callee + "\n");
mg.emit("addu $sp, $sp, " + (argsArr.length * 4) + "\n");
if (dst != null) {
    String rd = reg("Temp_"+dst.getSerialNumber(), regMap);
    mg.emit("move " + rd + ", $v0\n");
}
```

#### `IrCommandPrintInt.mipsMe`
```java
String r = reg("Temp_"+t.getSerialNumber(), regMap);
mg.emit("# inline implementation of PrintInt\n");
mg.emit("move $a0, " + r + "\n");
mg.emit("li $v0, 1\n");
mg.emit("syscall\n");
// trailing space
mg.emit("li $a0, 32\n");
mg.emit("li $v0, 11\n");
mg.emit("syscall\n");
```

#### `IrCommandPrintString.mipsMe`
```java
String r = reg("Temp_"+t.getSerialNumber(), regMap);
mg.emit("# inline implementation of PrintString\n");
mg.emit("move $a0, " + r + "\n");
mg.emit("li $v0, 4\n");
mg.emit("syscall\n");
```

#### `IrCommandNewClass.mipsMe`
```java
// Lookup class field count from TypeClass
int numFields = getClassTotalFieldCount(className); // use a static registry or pass TypeClass
int size = numFields * 4;
mg.emit("li $a0, " + size + "\n");
mg.emit("li $v0, 9\n");
mg.emit("syscall\n");
String rd = reg("Temp_"+dst.getSerialNumber(), regMap);
mg.emit("move " + rd + ", $v0\n");
// Zero-initialize all fields
for (int i = 0; i < numFields; i++) {
    mg.emit("sw $zero, " + (i*4) + "($v0)\n");
}
```

To get field counts, pass a `Map<String, TypeClass>` (all class types from symbol table) into `mipsMe`, or store it in a static registry.

#### `IrCommandNewArray.mipsMe`
```java
String rs = reg("Temp_"+size.getSerialNumber(), regMap);
String rd = reg("Temp_"+dst.getSerialNumber(), regMap);
// allocate (size + 1) * 4 bytes: slot 0 stores the length
mg.emit("move $s0, " + rs + "\n");
mg.emit("addi $a0, $s0, 1\n");
mg.emit("sll $a0, $a0, 2\n");
mg.emit("li $v0, 9\n");
mg.emit("syscall\n");
mg.emit("sw $s0, 0($v0)\n");        // store length at index 0
mg.emit("move " + rd + ", $v0\n");
```

#### `IrCommandFieldLoad.mipsMe`
```java
String rObj = reg("Temp_"+object.getSerialNumber(), regMap);
String rd   = reg("Temp_"+dst.getSerialNumber(), regMap);
mg.emitNilCheck(rObj);
int offset = getClassFieldOffset(objectClassName, fieldName) * 4;
mg.emit("lw " + rd + ", " + offset + "(" + rObj + ")\n");
```

#### `IrCommandFieldStore.mipsMe`
```java
String rObj = reg("Temp_"+object.getSerialNumber(), regMap);
String rVal = reg("Temp_"+value.getSerialNumber(), regMap);
mg.emitNilCheck(rObj);
int offset = getClassFieldOffset(objectClassName, fieldName) * 4;
mg.emit("sw " + rVal + ", " + offset + "(" + rObj + ")\n");
```

#### `IrCommandArrayLoad.mipsMe`
```java
String rArr = reg(...arr...), rIdx = reg(...idx...), rd = reg(...dst...);
mg.emitNilCheck(rArr);
mg.emitBoundsCheck(rArr, rIdx);
mg.emit("move $s0, " + rIdx + "\n");
mg.emit("addi $s0, $s0, 1\n");       // skip length slot
mg.emit("sll $s0, $s0, 2\n");
mg.emit("add $s0, " + rArr + ", $s0\n");
mg.emit("lw " + rd + ", 0($s0)\n");
```

#### `IrCommandArrayStore.mipsMe`
Same indexing as load, but ends with `sw` instead of `lw`.

---

> **Important for C:** To know a class's field count and field offsets inside `mipsMe`, you need access to the type information. The cleanest approach is to store a `Map<String, TypeClass> classRegistry` (populated once during semantic analysis) and pass it into `mipsMe`. Add a static `TypeRegistry` class or add it to the `Ir` singleton.

---

## Track D — `Main.java` Wiring & Testing

**Owner: _____________** (do this after A, B, C are done)

### D1. New `Main.java` pipeline

Replace everything after `ast.irMe()` with:

```java
// Get flat IR command list
List<IrCommand> irCommands = Ir.getInstance().getCommandList();

// Split into global preamble + per-function segments
// Global preamble: commands before the first IrCommandLabel with isFunctionEntry==true
List<IrCommand> globalPreamble = new ArrayList<>();
Map<String, List<IrCommand>> funcSegments = new LinkedHashMap<>(); // preserve order
String curFunc = null;
for (IrCommand c : irCommands) {
    if (c instanceof IrCommandLabel && ((IrCommandLabel)c).isFunctionEntry) {
        curFunc = ((IrCommandLabel)c).labelName;
        funcSegments.put(curFunc, new ArrayList<>());
    }
    if (curFunc == null) globalPreamble.add(c);
    else funcSegments.get(curFunc).add(c);
}

// Register allocation per function
Map<String, Map<String,String>> allRegAllocs = new LinkedHashMap<>();
for (Map.Entry<String, List<IrCommand>> e : funcSegments.entrySet()) {
    LivenessAnalysis la = new LivenessAnalysis(e.getValue());
    la.compute();
    InterferenceGraph ig = InterferenceGraph.build(la);
    RegisterAllocator ra = new RegisterAllocator(ig);
    allRegAllocs.put(e.getKey(), ra.allocate()); // exits on failure
}

// MIPS generation
MipsGenerator.init(outputFileName); // use argv[1], not hardcoded path
MipsGenerator mg = MipsGenerator.getInstance();

// Emit global .data allocations
for (IrCommand c : globalPreamble) {
    if (c instanceof IrCommandAllocate) c.mipsMe(mg, null, null);
}

// Emit global initializer code (non-allocate preamble commands) inline before user_main
// These will be emitted at the START of user_main body by a special flag:
mg.setPendingGlobalInits(globalPreamble); // store them; startFunction will emit them

// Emit per-function MIPS
for (Map.Entry<String, List<IrCommand>> e : funcSegments.entrySet()) {
    String fn = e.getKey();
    for (IrCommand c : e.getValue()) {
        c.mipsMe(mg, allRegAllocs.get(fn), fn);
    }
    mg.endFunction(fn);  // emit epilogue after last command of function
}

// Emit MIPS main stub + runtime handlers
mg.emitMipsMain();
mg.emitRuntimeHandlers();
mg.finalizeFile();
```

### D2. Output format

| Situation | Output to `argv[1]` |
|-----------|---------------------|
| Lex error | `ERROR` |
| Syntax error at line N | `ERROR(N)` |
| Semantic error at line N | `ERROR(N)` |
| Register alloc fails | `Register Allocation Failed` (printed by allocator, then `System.exit(1)`) |
| Success | MIPS assembly |

Verify that `HelperFunctions.printErrorAndExit(lineNum)` writes `ERROR(N)` to the file writer and exits. If not, fix it.

### D3. Fix `Makefile` for ex5

- Change output JAR name from `ANALYZER` to `COMPILER`
- Change `make everything` to run COMPILER and then SPIM on the result
- Do NOT hardcode any paths

### D4. Testing checklist

```
# Build
make

# Test examples
java -jar COMPILER examples/test_1.c /tmp/out.s && spim -file /tmp/out.s
# Expected: prints 3

java -jar COMPILER examples/test_2.c /tmp/out.s && spim -file /tmp/out.s
# Expected: prints abcd

# Run all tests TEST_01 through TEST_17
for i in $(seq -w 1 17); do
  java -jar COMPILER input/TEST_$(printf %02d $i)*.txt /tmp/out.s
  spim -file /tmp/out.s > /tmp/actual.txt
  diff expected_output/TEST_${i}_OUTPUT.txt /tmp/actual.txt && echo "PASS $i" || echo "FAIL $i"
done

# Register allocation failure test (provided by grader)
java -jar COMPILER input/TEST_REGALLOC_FAIL.txt /tmp/out.txt
cat /tmp/out.txt  # should contain: Register Allocation Failed

# Runtime error tests
java -jar COMPILER input/TEST_08_Access_Violation.txt /tmp/out.s && spim -file /tmp/out.s
java -jar COMPILER input/TEST_09_Access_Violation.txt /tmp/out.s && spim -file /tmp/out.s
```

---

## Dependency Order

```
A1 (TypeClass field order)
  └─► A11 (AstNewExp — needs field layout)
  └─► C (FieldLoad/FieldStore mipsMe — needs getFieldOffset)

A2–A3 (Ir param registry + IrCommandLabel flag)
  └─► A4 (AstFuncDec.irMe — emits label + registers params)
  └─► D1 (Main.java splits IR by function-entry labels)

A5–A6 (class method IR)
  └─► B (liveness — now has method bodies to analyse)
  └─► C (mipsMe for method calls)

A7–A10 (call expression + new IR command types)
  └─► C (mipsMe implementations for new command types)

B1–B3 (liveness + interference + allocator)
  └─► D1 (wiring: pass regMap to mipsMe)

C1–C2 (MipsGenerator + mipsMe on all commands)
  └─► D1 (wiring)
```

---

## Quick Reference: IR Command → MIPS Summary

| IR Command | Key MIPS ops |
|---|---|
| `IrCommandLabel(fn, true)` | function prologue |
| `IrCommandLabel(lbl, false)` | `lbl:` |
| `IRcommandConstInt` | `li $tK, val` |
| `IrCommandConstString` | `.data` entry + `la $tK, strLabel` |
| `IrCommandLoad` (param) | `lw $tK, (8+idx*4)($fp)` |
| `IrCommandLoad` (local) | `lw $tK, slot($fp)` |
| `IrCommandLoad` (global) | `lw $tK, varName` |
| `IrCommandStore` (local) | `sw $tK, slot($fp)` |
| `IrCommandStore` (global) | `sw $tK, varName` |
| `IrCommandAllocate` (global) | `.data varName: .word 0` |
| `IrCommandAllocate` (local) | allocate slot, no instruction |
| `IrCommandBinopAdd` | `add` + saturate |
| `IrCommandBinopSub` | `sub` + saturate |
| `IrCommandBinopMul` | `mul` + saturate |
| `IrCommandBinopDiv` | nil check divisor + `div`/`mflo` + saturate |
| `IrCommandBinopLt` | `slt` |
| `IrCommandBinopGt` | `sgt` |
| `IrCommandBinopEqIntegers` | `seq` |
| `IrCommandBinopEqPointers` | `seq` (pointer) |
| `IrCommandBinopConcatStrings` | `jal __str_concat` |
| `IrCommandBinopEqStrings` | `jal __str_eq` |
| `IrCommandJumpLabel` | `j label` |
| `IrCommandJumpIfEqToZero` | `beqz $tK, label` |
| `IrCommandCall` | push args R-to-L, `jal`, pop, `move dst, $v0` |
| `IrCommandReturn` | `move $v0, $tK` + `j fn_epilogue` |
| `IrCommandPrintInt` | `li $v0,1` + `syscall` + space |
| `IrCommandPrintString` | `li $v0,4` + `syscall` |
| `IrCommandNewClass` | `li $a0, N*4` + `li $v0,9` + `syscall` + zero-init fields |
| `IrCommandNewArray` | alloc `(len+1)*4` + store len at [0] |
| `IrCommandFieldLoad` | nil check obj + `lw $tK, offset($tObj)` |
| `IrCommandFieldStore` | nil check obj + `sw $tVal, offset($tObj)` |
| `IrCommandArrayLoad` | nil check + bounds check + index compute + `lw` |
| `IrCommandArrayStore` | nil check + bounds check + index compute + `sw` |
EOF