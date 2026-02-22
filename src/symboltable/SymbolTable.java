/***********/
/* PACKAGE */
/***********/
package symboltable;

/*******************/
/* GENERAL IMPORTS */
/*******************/
import java.io.PrintWriter;

/*******************/
/* PROJECT IMPORTS */
/*******************/
import types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/****************/
/* SYMBOL TABLE */
/****************/
public class SymbolTable
{
	//private int hashArraySize = 13;
	
	/**********************************************/
	/* The actual symbol table data structure ... */
	/**********************************************/
	private List<Map<String, Type>> tableList;
	private int scopeIndex = 0;
	
	/* Enter a variable, function, class type or array type to the symbol table */
	/****************************************************************************/
	public void enter(String name, Type t)
	{
		Map<String, Type> currScope = tableList.get(scopeIndex);
		currScope.put(name, t);
		printMe();
	}

	/***********************************************/
	/* Find the inner-most scope element with name */
	/***********************************************/
	public Type findInScope(String name) {
		return findInScope(name, scopeIndex);
	}

	public Type findInScope(String name, int currScopeIdx) {
		Map<String, Type> currScope = tableList.get(currScopeIdx);
        return currScope.getOrDefault(name, null);
	}

	public Type find(String name)
	{
		for (int i = scopeIndex; i >= 0; i--) {
			Type tempType = findInScope(name, i);
			if (tempType != null) {
				return tempType;
			}	 
		}
		return null;
	}

	public int getScopeIndex() { return scopeIndex; }

	public int getScopeIndexOf(String name)
	{
		for (int i = scopeIndex; i >= 0; i--) {
			if (findInScope(name, i) != null) {
				return i;
			}
		}
		return -1;
	}

	public Type currentFunctionReturnType = null;
	public TypeClass currentClass = null;  // Track the current class context for field lookups

	/***************************************************************************/
	/* begine scope = Enter the <SCOPE-BOUNDARY> element to the data structure */
	/***************************************************************************/
	public void beginScope()
	{
		tableList.add(new HashMap<>());
		scopeIndex++;

		printMe();
	}

	/********************************************************************************/
	/* end scope = Keep popping elements out of the data structure,                 */
	/* from most recent element entered, until a <NEW-SCOPE> element is encountered */
	/********************************************************************************/
	public void endScope()
	{
		tableList.remove(scopeIndex);
		scopeIndex--;
		printMe();
	}
	
	public static int n=0;
	
	public void printMe()
	{
		// int i=0;
		// int j=0;
		// String dirname="./output/";
		// String filename=String.format("SYMBOL_TABLE_%d_IN_GRAPHVIZ_DOT_FORMAT.txt",n++);
		
		// try
		// {
		// 	/*******************************************/
		// 	/* [1] Open Graphviz text file for writing */
		// 	/*******************************************/
		// 	PrintWriter fileWriter = new PrintWriter(dirname+filename);

		// 	/*********************************/
		// 	/* [2] Write Graphviz dot prolog */
		// 	/*********************************/
		// 	fileWriter.print("digraph structs {\n");
		// 	fileWriter.print("rankdir = LR\n");
		// 	fileWriter.print("node [shape=record];\n");

		// 	/*******************************/
		// 	/* [3] Write Hash Table Itself */
		// 	/*******************************/
		// 	fileWriter.print("hashTable [label=\"");
		// 	for (i=0;i<hashArraySize-1;i++) { fileWriter.format("<f%d>\n%d\n|",i,i); }
		// 	fileWriter.format("<f%d>\n%d\n\"];\n",hashArraySize-1,hashArraySize-1);
		
		// 	/****************************************************************************/
		// 	/* [4] Loop over hash table array and print all linked lists per array cell */
		// 	/****************************************************************************/
		// 	for (i=0;i<hashArraySize;i++)
		// 	{
		// 		if (table[i] != null)
		// 		{
		// 			/*****************************************************/
		// 			/* [4a] Print hash table array[i] -> entry(i,0) edge */
		// 			/*****************************************************/
		// 			fileWriter.format("hashTable:f%d -> node_%d_0:f0;\n",i,i);
		// 		}
		// 		j=0;
		// 		for (SymbolTableEntry it = table[i]; it!=null; it=it.next)
		// 		{
		// 			/*******************************/
		// 			/* [4b] Print entry(i,it) node */
		// 			/*******************************/
		// 			fileWriter.format("node_%d_%d ",i,j);
		// 			fileWriter.format("[label=\"<f0>%s|<f1>%s|<f2>prevtop=%d|<f3>next\"];\n",
		// 				it.name,
		// 				it.type.name,
		// 				it.prevtopIndex);

		// 			if (it.next != null)
		// 			{
		// 				/***************************************************/
		// 				/* [4c] Print entry(i,it) -> entry(i,it.next) edge */
		// 				/***************************************************/
		// 				fileWriter.format(
		// 					"node_%d_%d -> node_%d_%d [style=invis,weight=10];\n",
		// 					i,j,i,j+1);
		// 				fileWriter.format(
		// 					"node_%d_%d:f3 -> node_%d_%d:f0;\n",
		// 					i,j,i,j+1);
		// 			}
		// 			j++;
		// 		}
		// 	}
		// 	fileWriter.print("}\n");
		// 	fileWriter.close();
		// }
		// catch (Exception e)
		// {
		// 	e.printStackTrace();
		// }		
	}
	
	/**************************************/
	/* USUAL SINGLETON IMPLEMENTATION ... */
	/**************************************/
	private static SymbolTable instance = null;

	/*****************************/
	/* PREVENT INSTANTIATION ... */
	/*****************************/
	protected SymbolTable() {
		this.tableList = new ArrayList<>();
		tableList.add(new HashMap<>());
	}

	/******************************/
	/* GET SINGLETON INSTANCE ... */
	/******************************/
	public static SymbolTable getInstance()
	{
		if (instance == null)
		{
			/*******************************/
			/* [0] The instance itself ... */
			/*******************************/
			instance = new SymbolTable();

			/*****************************************/
			/* [1] Enter primitive types int, string */
			/*****************************************/
			instance.enter("int",   TypeInt.getInstance());
			instance.enter("string", TypeString.getInstance());
			instance.enter("void", TypeVoid.getInstance());
			instance.enter("nil", TypeNil.getInstance());

			/*************************************/
			/* [2] How should we handle void ??? */
			/*************************************/

			/***************************************/
			/* [3] Enter library function PrintInt */
			/***************************************/
			instance.enter(
				"PrintInt",
				new TypeFunction(
					TypeVoid.getInstance(),
					"PrintInt",
					new TypeList(
						TypeInt.getInstance(),
						null)));
			
			/*****************************************/
			/* [4] Enter library function PrintString */
			/*****************************************/
			instance.enter(
				"PrintString",
				new TypeFunction(
					TypeVoid.getInstance(),
					"PrintString",
					new TypeList(
						TypeString.getInstance(),
						null)));
			
		}
		return instance;
	}
}
