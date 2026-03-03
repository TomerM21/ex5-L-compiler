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

public class IrCommandReturn extends IrCommand
{
	public Temp returnValue; // null for void return
	
	public IrCommandReturn(Temp returnValue)
	{
		this.returnValue = returnValue;
	}
	
	@Override
	public String toString() {
		if (returnValue == null) {
			return "return";
		}
		return "return Temp_" + returnValue.getSerialNumber();
	}

	public Set<String> getReadTemps() {
		Set<String> result = new HashSet<>();
		if (returnValue != null) {
			result.add("Temp_" + returnValue.getSerialNumber());
		}
		return result;
	}

	public Set<String> getWriteTemps() {
		return new HashSet<>();
	}
	@Override
	public void mipsMe(MipsGenerator mg, Map<String,String> regMap,String funcName) {
		if (returnValue != null) {
    String r = reg("Temp_" + returnValue.getSerialNumber(), regMap);
    mg.emit("move $v0, " + r + "\n");
}
mg.emit("j " + funcName + "_epilogue\n");
// (no need to emit epilogue here — it's emitted by the end-of-function detection in Main)
	}

}
