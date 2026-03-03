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

public class IrCommandFieldStore extends IrCommand
{
	public Temp object;        // object temp
	public String fieldName;
	public Temp value;         // value to store
	
	public IrCommandFieldStore(Temp object, String fieldName, Temp value)
	{
		this.object = object;
		this.fieldName = fieldName;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "Temp_" + object.getSerialNumber() + "." + fieldName + " := Temp_" + value.getSerialNumber();
	}

	public Set<String> getReadTemps() {
		Set<String> result = new HashSet<>();
		result.add("Temp_" + object.getSerialNumber());
		result.add("Temp_" + value.getSerialNumber());
		return result;
	}

	public Set<String> getWriteTemps() {
		return new HashSet<>();
	}
	@Override
public void mipsMe(MipsGenerator mg,Map<String,String> regMap,String funcName) {

    String rObj = reg("Temp_" + object.getSerialNumber(), regMap);
    String rVal = reg("Temp_" + value.getSerialNumber(), regMap);

    mg.emitNilCheck(rObj);

    int offset = getClassFieldOffset(objectClassName, fieldName) * 4;
    mg.emit("sw " + rVal + ", " + offset + "(" + rObj + ")\n");
}
}
