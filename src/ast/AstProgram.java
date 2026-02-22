package ast;
import ast.Dec.AstDecList;
import types.*;

public class AstProgram extends AstNode {

    public AstDecList decList;

    public AstProgram(AstDecList decList) {
        /******************************/
        /* SET A UNIQUE SERIAL NUMBER */
        /******************************/
        serialNumber = AstNodeSerialNumber.getFresh();

        /***************************************/
        /* PRINT CORRESPONDING DERIVATION RULE */
        /***************************************/
        System.out.print("====================== Program -> decList\n");

        /*******************************/
        /* COPY INPUT DATA MEMBERS ... */
        /*******************************/
        this.decList = decList;
    }

    public void printMe() {
        /*****************************/
        /* AST NODE TYPE = PROGRAM   */
        /*****************************/
        System.out.print("AST NODE PROGRAM\n");

        /*****************************/
        /* RECURSIVELY PRINT KIDS   */
        /*****************************/
        if (decList != null) decList.printMe();

        /*********************************/
        /* Print to AST GRAPHVIZ DOT file */
        /*********************************/
        AstGraphviz.getInstance().logNode(
                serialNumber,
                "PROGRAM");

        /****************************************/
        /* PRINT Edges to AST GRAPHVIZ DOT file */
        /****************************************/
        if (decList != null)
            AstGraphviz.getInstance().logEdge(serialNumber, decList.serialNumber);
    }

    @Override
    public Type SemantMe() {
        System.out.println("### SEMANT STARTED (PROGRAM) ###");
        return this.decList.SemantMe();
    }

    public temp.Temp irMe() {
        if (decList != null) {
            // Pass 1: global variable allocates + initializations (in source order, before any function body)
            decList.irMeVarDecs();
            // Pass 2: function bodies (main runs after all globals are initialized)
            decList.irMeFuncDecs();
        }
        return null;
    }
}