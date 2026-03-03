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

public class IrCommandConstString extends IrCommand
{
	public Temp t;
	public String value;
	
	public IrCommandConstString(Temp t, String value)
	{
		this.t = t;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "Temp_" + t.getSerialNumber() + " := \"" + value + "\"";
	}

	public Set<String> getReadTemps() {
		return new HashSet<>();
	}

	public Set<String> getWriteTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + t.getSerialNumber());
		return result;
	}
	@Override
	public void mipsMe(MipsGenerator mg, Map<String,String> regMap,String funcName) {
    String strLabel = mg.globalStringAllocate("str_" + freshId(), value);
	String r = reg("Temp_" + t.getSerialNumber(), regMap);
	mg.emit("la " + r + ", " + strLabel + "\n");
	}
}
