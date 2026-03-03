/***********/
/* PACKAGE */
/***********/
package ir;

import java.util.*;

/*******************/
/* GENERAL IMPORTS */
/*******************/

/*******************/
/* PROJECT IMPORTS */
/*******************/
import temp.*;

public class IrCommandStore extends IrCommand
{
	String varName;
	Temp src;
	
	public IrCommandStore(String varName, Temp src)
	{
		this.src      = src;
		this.varName = varName;
	}
	
	@Override
	public String toString() {
		return varName + " := Temp_" + src.getSerialNumber();
	}

    @Override
    public Set<String> getWriteVariables() {
        Set<String> s = new HashSet<>();
        if (varName != null) s.add(varName);
        return s;
    }

	public Set<String> getReadTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + src.getSerialNumber());
		return result;
	}

	public Set<String> getWriteTemps() {
		return new HashSet<>();
	}
@Override
public void mipsMe(MipsGenerator mg,Map<String,String> regMap, String funcName) {

    // resolve source register
    String rVal = reg("Temp_" + src.getSerialNumber(), regMap);

    Ir irSingleton = Ir.getInstance();

    if (irSingleton.isParam(varName)) {

        // store into parameter slot on stack
        int idx = irSingleton.getParamIndex(varName);
        int fpOffset = 8 + idx * 4;
        mg.emit("sw " + rVal + ", " + fpOffset + "($fp)\n");

    } else if (irSingleton.isGlobal(varName)) {

        // store into global variable in .data
        mg.emit("sw " + rVal + ", " + varName + "\n");

    } else {

        // store into local variable slot
        int slot = mg.resolveLocal(varName);
        mg.emit("sw " + rVal + ", " + slot + "($fp)\n");
    }
}
} 
