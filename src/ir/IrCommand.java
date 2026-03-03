/***********/
/* PACKAGE */
/***********/
package ir;
import java.util.Map;
import java.util.*;

/*******************/
/* GENERAL IMPORTS */
/*******************/

/*******************/
/* PROJECT IMPORTS */
/*******************/

public abstract class IrCommand
{
    /*****************/
    /* Label Factory */
    /*****************/
    protected static int labelCounter = 0;
    public abstract void mipsMe(
        mips.MipsGenerator mg,
        Map<String,String> regMap,
        String funcName);
        protected String reg(String tempName, Map<String,String> regMap) {
    return regMap.getOrDefault(tempName, "$t0");
}
    public    static String getFreshLabel(String msg)
    {
        return String.format("Label_%d_%s", labelCounter++,msg);
    }

    // By default, IR commands do not read or write named variables.
    // Subclasses that interact with named variables should override these.
    public Set<String> getReadVariables() {
        return new HashSet<>();
    }

    public Set<String> getWriteVariables() {
        return new HashSet<>();
    }

    // Track temp variable dependencies
    public Set<String> getReadTemps() {
        return new HashSet<>();
    }

    public Set<String> getWriteTemps() {
        return new HashSet<>();
    }

    public boolean isJump() {
        return false;
    }

    public boolean isUnconditionalJump() {
        return false;
    }

    public boolean isConditionalJump() {
        return false;
    }

    public String getJumpLabel() {
        return null; 
    }

	/***********************************/
	/* Abstract method for IR printing */
	/***********************************/
	public abstract String toString();
}
