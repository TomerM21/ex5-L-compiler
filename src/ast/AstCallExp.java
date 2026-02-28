package ast;

import ast.Exp.AstExp;
import ast.Exp.AstExpList;
import ast.Var.AstVar;
import types.Type;

public class AstCallExp extends AstExp {

    public AstVar receiver;       // null if calling f()
    public String methodName;
    public AstExpList args;       // null if no arguments

    /******************/
    /* CONSTRUCTOR(S) */
    /******************/
    public AstCallExp(AstVar receiver, String methodName, AstExpList args)
    {
        /******************************/
        /* SET A UNIQUE SERIAL NUMBER */
        /******************************/
        serialNumber = AstNodeSerialNumber.getFresh();

        /***************************************/
        /* PRINT CORRESPONDING DERIVATION RULE */
        /***************************************/
        if (receiver == null && args == null)
            System.out.print("====================== callExp -> ID LPAREN RPAREN\n");
        else if (receiver == null)
            System.out.print("====================== callExp -> ID LPAREN expList RPAREN\n");
        else if (args == null)
            System.out.print("====================== callExp -> var DOT ID LPAREN RPAREN\n");
        else
            System.out.print("====================== callExp -> var DOT ID LPAREN expList RPAREN\n");

        /*******************************/
        /* COPY INPUT DATA MEMBERS ... */
        /*******************************/
        this.receiver = receiver;
        this.methodName = methodName;
        this.args = args;
    }

    /*****************************************************/
    /* The printing message for a method-call AST node    */
    /*****************************************************/
    public void printMe()
    {
        /*************************************/
        /* AST NODE TYPE = AST EXP METHOD     */
        /*************************************/
        System.out.format("AST NODE METHOD CALL (%s)\n", methodName);

        /*****************************/
        /* RECURSIVELY PRINT KIDS   */
        /*****************************/
        if (receiver != null) receiver.printMe();
        if (args != null) args.printMe();

        /*********************************/
        /* Print to AST GRAPHVIZ DOT file */
        /*********************************/
        AstGraphviz.getInstance().logNode(
                serialNumber,
                String.format("METHOD\nCALL(%s)", methodName));

        /****************************************/
        /* PRINT Edges to AST GRAPHVIZ DOT file */
        /****************************************/
        if (receiver != null)
            AstGraphviz.getInstance().logEdge(serialNumber, receiver.serialNumber);

        if (args != null)
            AstGraphviz.getInstance().logEdge(serialNumber, args.serialNumber);
    }
    @Override
    public Type SemantMe() {
        types.TypeFunction funcType = null;
        private String resolvedClassName = null; // set when receiver != null

        // 1. Find the Function
        if (receiver == null) {
            // Regular function call: foo()
            // First check if we're inside a class and it's a method of the current class
            symboltable.SymbolTable tbl = symboltable.SymbolTable.getInstance();
            if (tbl.currentClass != null) {
                funcType = tbl.currentClass.lookupMethod(methodName);
            }
            
            // If not found in class, look in symbol table for global functions
            if (funcType == null) {
                Type t = tbl.find(methodName);
                if (t == null || !(t instanceof types.TypeFunction)) {
                    System.out.format(">> ERROR: Function %s not found\n", methodName);
                    error();
                }
                funcType = (types.TypeFunction) t;
            }
        } else {
            // Method call: obj.foo()
            Type recvType = receiver.SemantMe();
            if (!(recvType instanceof types.TypeClass)) {
                System.out.println(">> ERROR: Method call on non-class type");
                error();
            }
            types.TypeClass tc = (types.TypeClass) recvType;
            funcType = tc.lookupMethod(methodName);
            if (funcType == null) {
                System.out.format(">> ERROR: Method %s not found in class %s\n", methodName, tc.name);
                error();
            }
        }

        // 2. Validate Arguments
        types.TypeList paramList = funcType.params;
        
        types.TypeList argList = null;
        if (args != null) {
            argList = (types.TypeList) args.SemantMe(); 
        }

        // --- DELETED UNUSED VARIABLES HERE ---
        
        // Iterate using the lists directly
        types.TypeList pNode = paramList;
        types.TypeList aNode = argList;

        while (pNode != null && aNode != null) {
            if (!ast.Helpers.HelperFunctions.canAssign(pNode.head, aNode.head)) {
                System.out.println(">> ERROR: Argument type mismatch");
                error();
            }
            pNode = pNode.tail;
            aNode = aNode.tail;
        }

        // 3. Check Argument Count
        if (pNode != null || aNode != null) {
            System.out.println(">> ERROR: Argument count mismatch");
            error();
        }
        if (receiver != null) {
        this.resolvedClassName = tc.name;
        }
        return funcType.returnType;
    }

    public temp.Temp irMe()
    {
       // Evaluate args left-to-right
    ir.TempList argList = null;

    // For method calls, self is the first argument
    if (receiver != null) {
        temp.Temp selfTemp = receiver.irMe();
        argList = new ir.TempList(selfTemp, null);
    }

    // Evaluate explicit args and append
    if (args != null) {
        ir.TempList explicitArgs = args.irMe();
        if (argList == null) {
            argList = explicitArgs;
        } else {
            // Append explicitArgs to end of argList
            ir.TempList cursor = argList;
            while (cursor.tail != null) cursor = cursor.tail;
            cursor.tail = explicitArgs;
        }
    }

    // Built-ins
    if ("PrintInt".equals(methodName) && argList != null) {
        ir.Ir.getInstance().AddIrCommand(new ir.IrCommandPrintInt(argList.head));
        return null;
    }
    if ("PrintString".equals(methodName) && argList != null) {
        ir.Ir.getInstance().AddIrCommand(new ir.IrCommandPrintString(argList.head));
        return null;
    }

    // Determine callee label
    String callee = (resolvedClassName != null)
        ? resolvedClassName + "_" + methodName
        : methodName;

    temp.Temp result = temp.TempFactory.getInstance().getFreshTemp();
    ir.Ir.getInstance().AddIrCommand(new ir.IrCommandCall(result, callee, argList));
    return result;
    }
}