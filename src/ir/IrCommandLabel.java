/***********/
/* PACKAGE */
/***********/
package ir;

/*******************/
/* GENERAL IMPORTS */
/*******************/

/*******************/
/* PROJECT IMPORTS */
/*******************/

public class IrCommandLabel extends IrCommand
{
	String labelName;
	
	public IrCommandLabel(String labelName)
	{
		this.labelName = labelName;
	}

	public String getLabelName() {
        return labelName;
    }
	
	@Override
	public String toString() {
		return labelName + ":";
	}
	@Override
	public void mipsMe(MipsGenerator mg,
                   Map<String,String> regMap,
                   String funcName) {

    if (isFunctionEntry) {

        // simplest safe solution: fixed 64 local slots
        mg.startFunction(labelName, 64);

    } else {

        mg.emit(labelName + ":\n");
    }
}
}
