/***********/
/* PACKAGE */
/***********/
package ir;

/*******************/
/* GENERAL IMPORTS */
/*******************/
import java.util.*;

/*******************/
/* PROJECT IMPORTS */
/*******************/
import temp.*;

public class IrCommandNewClass extends IrCommand
{
	public Temp dst;        // destination temp
	public String className;
	
	public IrCommandNewClass(Temp dst, String className)
	{
		this.dst = dst;
		this.className = className;
	}
	
	@Override
	public String toString() {
		return "Temp_" + dst.getSerialNumber() + " := new " + className + "()";
	}

	public Set<String> getReadTemps() {
		return new HashSet<>();
	}

	public Set<String> getWriteTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + dst.getSerialNumber());
		return result;
	}
	@Override
public void mipsMe(MipsGenerator mg,Map<String,String> regMap,String funcName) {

    int numFields = getClassTotalFieldCount(className); // from registry
    int size = numFields * 4;

    // allocate heap
    mg.emit("li $a0, " + size + "\n");
    mg.emit("li $v0, 9\n");
    mg.emit("syscall\n");

    // store pointer in destination
    String rd = reg("Temp_" + dst.getSerialNumber(), regMap);
    mg.emit("move " + rd + ", $v0\n");

    // zero-initialize fields
    for (int i = 0; i < numFields; i++) {
        mg.emit("sw $zero, " + (i * 4) + "($v0)\n");
    }
}
}
