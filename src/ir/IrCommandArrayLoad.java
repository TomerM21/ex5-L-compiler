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

public class IrCommandArrayLoad extends IrCommand
{
	public Temp dst;        // destination temp
	public Temp array;      // array temp
	public Temp index;      // index temp
	
	public IrCommandArrayLoad(Temp dst, Temp array, Temp index)
	{
		this.dst = dst;
		this.array = array;
		this.index = index;
	}
	
	@Override
	public String toString() {
		return "Temp_" + dst.getSerialNumber() + " := Temp_" + array.getSerialNumber() + "[Temp_" + index.getSerialNumber() + "]";
	}

	public Set<String> getReadTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + array.getSerialNumber());
		result.add("Temp_" + index.getSerialNumber());
		return result;
	}

	public Set<String> getWriteTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + dst.getSerialNumber());
		return result;
	}
	@Override
public void mipsMe(MipsGenerator mg,Map<String,String> regMap,String funcName) {

    String rArr = reg("Temp_" + array.getSerialNumber(), regMap);
    String rIdx = reg("Temp_" + index.getSerialNumber(), regMap);
    String rd   = reg("Temp_" + dst.getSerialNumber(), regMap);

    mg.emitNilCheck(rArr);
    mg.emitBoundsCheck(rArr, rIdx);

    mg.emit("move $s0, " + rIdx + "\n");
    mg.emit("addi $s0, $s0, 1\n");   // skip length slot
    mg.emit("sll $s0, $s0, 2\n");    // multiply by 4
    mg.emit("add $s0, " + rArr + ", $s0\n"); // compute address
    mg.emit("lw " + rd + ", 0($s0)\n");      // load element
}
}
