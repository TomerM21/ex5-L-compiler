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

public class IrCommandFieldLoad extends IrCommand
{
	public Temp dst;           // destination temp
	public Temp object;        // object temp
	public String fieldName;
	
	public IrCommandFieldLoad(Temp dst, Temp object, String fieldName)
	{
		this.dst = dst;
		this.object = object;
		this.fieldName = fieldName;
	}
	
	@Override
	public String toString() {
		return "Temp_" + dst.getSerialNumber() + " := Temp_" + object.getSerialNumber() + "." + fieldName;
	}

	public Set<String> getReadTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + object.getSerialNumber());
		return result;
	}

	public Set<String> getWriteTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + dst.getSerialNumber());
		return result;
	}
	@Override
public void mipsMe(MipsGenerator mg,Map<String,String> regMap,String funcName) {

    String rObj = reg("Temp_" + object.getSerialNumber(), regMap);
    String rd   = reg("Temp_" + dst.getSerialNumber(), regMap);

    mg.emitNilCheck(rObj);

    int offset = getClassFieldOffset(objectClassName, fieldName) * 4;
    mg.emit("lw " + rd + ", " + offset + "(" + rObj + ")\n");
}
}
