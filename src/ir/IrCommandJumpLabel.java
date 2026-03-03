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

public class IrCommandJumpLabel extends IrCommand
{
	String labelName;
	
	public IrCommandJumpLabel(String labelName)
	{
		this.labelName = labelName;
	}

	@Override
    public boolean isJump() { return true; }

    @Override
    public boolean isUnconditionalJump() { return true; }

    @Override
    public String getJumpLabel() { return labelName; }
	
	@Override
	public String toString() {
		return "goto " + labelName;
	}
	@Override
    public void mipsMe(MipsGenerator mg, Map<String, String> regMap, String funcName) {
        // Emit an unconditional jump to the label
        mg.emit("# Jump to label " + labelName + "\n");
        mg.emit("j " + labelName + "\n");
    }
}
