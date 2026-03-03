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

public class IrCommandArrayStore extends IrCommand
{
	public Temp array;      // array temp
	public Temp index;      // index temp
	public Temp value;      // value to store
	
	public IrCommandArrayStore(Temp array, Temp index, Temp value)
	{
		this.array = array;
		this.index = index;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "Temp_" + array.getSerialNumber() + "[Temp_" + index.getSerialNumber() + "] := Temp_" + value.getSerialNumber();
	}

	public Set<String> getReadTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + array.getSerialNumber());
		result.add("Temp_" + index.getSerialNumber());
		result.add("Temp_" + value.getSerialNumber());
		return result;
	}

	public Set<String> getWriteTemps() {
		return new HashSet<>();
	}
	@Override
public void mipsMe(MipsGenerator mg, Map<String,String> regMap,String funcName) {

    String rArr = reg("Temp_" + array.getSerialNumber(), regMap);
    String rIdx = reg("Temp_" + index.getSerialNumber(), regMap);
    String rVal = reg("Temp_" + value.getSerialNumber(), regMap);

    mg.emitNilCheck(rArr);
    mg.emitBoundsCheck(rArr, rIdx);

    mg.emit("move $s0, " + rIdx + "\n");
    mg.emit("addi $s0, $s0, 1\n");   // skip length slot
    mg.emit("sll $s0, $s0, 2\n");    // multiply by 4
    mg.emit("add $s0, " + rArr + ", $s0\n"); // compute address
    mg.emit("sw " + rVal + ", 0($s0)\n");    // store element
}
}
