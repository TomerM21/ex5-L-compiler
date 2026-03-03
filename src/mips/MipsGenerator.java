/***********/
/* PACKAGE */
/***********/
package mips;

/*******************/
/* GENERAL IMPORTS */
/*******************/
import java.io.PrintWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/*******************/
/* PROJECT IMPORTS */
/*******************/
import temp.*;

public class MipsGenerator
{
	private PrintWriter out;
    private StringBuilder dataSec = new StringBuilder();
    private StringBuilder textSec = new StringBuilder();

    private String currentFunc = null;

    // maps scoped IR var name → fp-relative offset
    private Map<String, Integer> localOffset = new HashMap<>();
    private int nextLocalSlot = -44;

    // singleton
    private static MipsGenerator instance = null;

    private boolean globalPhase = true;

    private int labelCounter = 0;

    private MipsGenerator() {}

    public static void init(String outputPath) throws Exception {
        instance = new MipsGenerator();
        instance.out = new PrintWriter(outputPath);

        // error strings into .data
        instance.dataSec.append("string_access_violation: .asciiz \"Access Violation\"\n");
        instance.dataSec.append("string_illegal_div_by_0: .asciiz \"Illegal Division By Zero\"\n");
        instance.dataSec.append("string_invalid_ptr_dref: .asciiz \"Invalid Pointer Dereference\"\n");
    }

    public static MipsGenerator getInstance() {
        return instance;
    }
	public void emit(String s) {
    textSec.append(s);
}

public boolean isGlobalPhase() {
    return globalPhase;
}

public void endGlobalPhase() {
    globalPhase = false;
}

public String freshLabel(String base) {
    return base + "_" + (labelCounter++);
}
public void startFunction(String name, int numLocalSlots) {//prologue

    globalPhase = false;

    currentFunc = name;

    localOffset.clear();
    nextLocalSlot = -44;

    String label = name.equals("main") ? "user_main" : name;
    emit(label + ":\n");

    emit("# prologue\n");

    emit("subu $sp, $sp, 4\n");
    emit("sw $ra, 0($sp)\n");

    emit("subu $sp, $sp, 4\n");
    emit("sw $fp, 0($sp)\n");

    emit("move $fp, $sp\n");

    for (int i = 0; i <= 9; i++) {
        emit("subu $sp, $sp, 4\n");
        emit("sw $t" + i + ", 0($sp)\n");
    }

    emit("subu $sp, $sp, " + (numLocalSlots * 4) + "\n");
}
public void endFunction(String name) {//epilogue

    emit(name + "_epilogue:\n");

    emit("move $sp, $fp\n");

    for (int i = 0; i <= 9; i++) {
        emit("lw $t" + i + ", -" + (4 * (i + 1)) + "($sp)\n");
    }

    emit("lw $fp, 0($sp)\n");
    emit("lw $ra, 4($sp)\n");

    emit("addu $sp, $sp, 8\n");
    emit("jr $ra\n");
}
public int resolveLocal(String irVarName) {
    if (!localOffset.containsKey(irVarName)) {
        localOffset.put(irVarName, nextLocalSlot);
        nextLocalSlot -= 4;
    }
    return localOffset.get(irVarName);
}
public void globalVarAllocate(String irVarName) {
    dataSec.append(irVarName + ": .word 0\n");
}

public String globalStringAllocate(String ptrLabel, String strValue) {

    String realLabel = ptrLabel + "_str";

    dataSec.append(realLabel + ": .asciiz \"" + strValue + "\"\n");
    dataSec.append(ptrLabel + ": .word " + realLabel + "\n");

    return ptrLabel;
}
public void emitMipsMain() {
    emit("main:\n");
    emit("jal user_main\n");
    emit("li $v0, 10\n");
    emit("syscall\n");
}
public void emitRuntimeHandlers() {

    emit("__div_by_zero_handler:\n");
    emit("la $a0, string_illegal_div_by_0\n");
    emit("li $v0, 4\n");
    emit("syscall\n");
    emit("li $v0, 10\n");
    emit("syscall\n");

    emit("__nil_handler:\n");
    emit("la $a0, string_invalid_ptr_dref\n");
    emit("li $v0, 4\n");
    emit("syscall\n");
    emit("li $v0, 10\n");
    emit("syscall\n");

    emit("__bounds_handler:\n");
    emit("la $a0, string_access_violation\n");
    emit("li $v0, 4\n");
    emit("syscall\n");
    emit("li $v0, 10\n");
    emit("syscall\n");
}
public void emitSaturate(String reg) {

    String skipMax = freshLabel("sat_max");
    String skipMin = freshLabel("sat_min");

    emit("li $s0, 32767\n");
    emit("ble " + reg + ", $s0, " + skipMax + "\n");
    emit("move " + reg + ", $s0\n");
    emit(skipMax + ":\n");

    emit("li $s0, -32768\n");
    emit("bge " + reg + ", $s0, " + skipMin + "\n");
    emit("move " + reg + ", $s0\n");
    emit(skipMin + ":\n");
}
public void emitNilCheck(String reg) {
    emit("beqz " + reg + ", __nil_handler\n");
}
public void emitDivByZeroCheck(String reg) {
    emit("beqz " + reg + ", __div_by_zero_handler\n");
}
public void emitBoundsCheck(String arrReg, String idxReg) {

    String ok = freshLabel("bounds_ok");

    emit("bltz " + idxReg + ", __bounds_handler\n");
    emit("lw $s1, 0(" + arrReg + ")\n");
    emit("blt " + idxReg + ", $s1, " + ok + "\n");
    emit("j __bounds_handler\n");
    emit(ok + ":\n");
}
public void finalizeFile() {

    out.println(".data");
    out.print(dataSec.toString());

    out.println("\n.text");
    out.print(textSec.toString());

    out.close();
}
}
