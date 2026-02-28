package types;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class TypeClass extends Type
{
	/*********************************************************************/
	/* If this class does not extend a father class this should be null  */
	/*********************************************************************/
	public TypeClass father;

	/**************************************************/
	/* Gather up all data members in one place        */
	/* Note that data members coming from the AST are */
	/* packed together with the class methods         */
	/**************************************************/
	public TypeList dataMembers;
	
public LinkedHashMap<String, Type> fields = new LinkedHashMap<>();
public HashMap<String, TypeFunction> methods = new HashMap<>();

	/****************/
	/* CTROR(S) ... */
	/****************/
	public TypeClass(TypeClass father, String name, TypeList dataMembers)
	{
		this.name = name;
		this.father = father;
		this.dataMembers = dataMembers;
	}

	@Override
	public boolean isClass(){ return true;}

	public Type lookupField(String fieldName)
	{
		// Look in current class
		if (fields.containsKey(fieldName)) {
			return fields.get(fieldName);
		}
		// Check father recursively
		if (father != null) {
			return father.lookupField(fieldName);
		}
		// Not found
		return null;
	}

	public TypeFunction lookupMethod(String methodName)
	{
		// Look in current class
		if (methods.containsKey(methodName)) {
			return methods.get(methodName);
		}
		// Check father recursively
		if (father != null) {
			return father.lookupMethod(methodName);
		}
		// Not found
		return null;
	}

	public void splitMembers() {
    	TypeList it = dataMembers;

		while (it != null && it.head != null) {
			Type t = it.head;

			if (t instanceof TypeClassVarDec) {
				TypeClassVarDec varDec = (TypeClassVarDec) t;
				// Check if a method with this name already exists
				if (methods.containsKey(varDec.name)) {
					System.out.format(">> ERROR: Field '%s' has same name as a method in the same class\n", varDec.name);
					ast.Helpers.HelperFunctions.printErrorAndExit(varDec.lineNumber);
				}
				fields.put(varDec.name, varDec.t);
			}
			else if (t instanceof TypeFunction) {
				TypeFunction func = (TypeFunction) t;
				// Check if a field with this name already exists
				if (fields.containsKey(func.name)) {
					System.out.format(">> ERROR: Method '%s' has same name as a field in the same class\n", func.name);
					ast.Helpers.HelperFunctions.printErrorAndExit(func.lineNumber);
				}
				methods.put(func.name, func);
			}
			it = it.tail;
		}
	}
	public int getFieldOffset(String fieldName) {
    int offset = 0;
    if (father != null) {
        int fatherResult = father.getFieldOffset(fieldName);
        if (fatherResult >= 0) return fatherResult;
        offset = father.totalFieldCount();
    }
    int i = 0;
    for (String key : fields.keySet()) {
        if (key.equals(fieldName)) return offset + i;
        i++;
    }
    return -1;
}

public int totalFieldCount() {
    int parentCount = (father != null) ? father.totalFieldCount() : 0;
    return parentCount + fields.size();
}
}
