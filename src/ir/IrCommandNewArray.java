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

public class IrCommandNewArray extends IrCommand
{
	public Temp dst;        // destination temp
	public Temp size;       // size temp
	public String typeName; // element type
	
	public IrCommandNewArray(Temp dst, Temp size, String typeName)
	{
		this.dst = dst;
		this.size = size;
		this.typeName = typeName;
	}
	
	@Override
	public String toString() {
		return "Temp_" + dst.getSerialNumber() + " := new " + typeName + "[Temp_" + size.getSerialNumber() + "]";
	}

	public Set<String> getReadTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + size.getSerialNumber());
		return result;
	}

	public Set<String> getWriteTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + dst.getSerialNumber());
		return result;
	}
	@Override
public void mipsMe(MipsGenerator mg,
                   Map<String,String> regMap,
                   String funcName) {

    String rs = reg("Temp_" + size.getSerialNumber(), regMap);
    String rd = reg("Temp_" + dst.getSerialNumber(), regMap);

    // allocate (size + 1) * 4 bytes (slot 0 stores length)
    mg.emit("move $s0, " + rs + "\n");
    mg.emit("addi $a0, $s0, 1\n");
    mg.emit("sll $a0, $a0, 2\n");
    mg.emit("li $v0, 9\n");
    mg.emit("syscall\n");

    // store length
    mg.emit("sw $s0, 0($v0)\n");

    // store pointer in destination
    mg.emit("move " + rd + ", $v0\n");
}
}
