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

public class IrCommandPrintInt extends IrCommand
{
	Temp t;
	
	public IrCommandPrintInt(Temp t)
	{
		this.t = t;
	}
	
	@Override
	public String toString() {
		return "PrintInt(Temp_" + t.getSerialNumber() + ")";
	}

	public Set<String> getReadTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + t.getSerialNumber());
		return result;
	}

	public Set<String> getWriteTemps() {
		return new HashSet<>();
	}
	@Override
public void mipsMe(MipsGenerator mg,Map<String,String> regMap,String funcName) {

    String r = reg("Temp_" + t.getSerialNumber(), regMap);

    // inline PrintInt
    mg.emit("# inline implementation of PrintInt\n");
    mg.emit("move $a0, " + r + "\n");
    mg.emit("li $v0, 1\n");
    mg.emit("syscall\n");

    // print trailing space
    mg.emit("li $a0, 32\n");
    mg.emit("li $v0, 11\n");
    mg.emit("syscall\n");
}
}
