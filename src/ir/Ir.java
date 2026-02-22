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

public class Ir
{
	private IrCommand head=null;
	private IrCommandList tail=null;

	/******************/
	/* Add Ir command */
	/******************/
	public void AddIrCommand(IrCommand cmd)
	{
		if ((head == null) && (tail == null))
		{
			this.head = cmd;
		}
		else if ((head != null) && (tail == null))
		{
			this.tail = new IrCommandList(cmd,null);
		}
		else
		{
			IrCommandList it = tail;
			while ((it != null) && (it.tail != null))
			{
				it = it.tail;
			}
			it.tail = new IrCommandList(cmd,null);
		}
	}

	/**************************************/
	/* USUAL SINGLETON IMPLEMENTATION ... */
	/**************************************/
	private static Ir instance = null;

	/*****************************/
	/* PREVENT INSTANTIATION ... */
	/*****************************/
        private java.util.HashMap<String, String> irNameToOriginalName = new java.util.HashMap<>();

        public void registerIrName(String irName, String originalName) {
                irNameToOriginalName.put(irName, originalName);
        }

        public String getOriginalName(String irName) {
                return irNameToOriginalName.getOrDefault(irName, irName);
        }

	/* GET SINGLETON INSTANCE ... */
	/******************************/
	public static Ir getInstance()
	{
		if (instance == null)
		{
			/*******************************/
			/* [0] The instance itself ... */
			/*******************************/
			instance = new Ir();
		}
		return instance;
	}
	
	/********************************/
	/* GET LIST OF ALL IR COMMANDS  */
	/********************************/
	public java.util.ArrayList<IrCommand> getCommandList()
	{
		java.util.ArrayList<IrCommand> result = new java.util.ArrayList<>();
		
		if (head != null) {
			result.add(head);
		}
		
		IrCommandList current = tail;
		while (current != null) {
			if (current.head != null) {
				result.add(current.head);
			}
			current = current.tail;
		}
		
		return result;
	}
	
	/********************************/
	/* PRINT ALL IR COMMANDS        */
	/********************************/
	public void printIrToFile(String filename) throws java.io.IOException
	{
		java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filename));
		
		java.util.ArrayList<IrCommand> commands = getCommandList();
		writer.println("========== IR COMMANDS ==========");
		writer.println("Total commands: " + commands.size());
		writer.println("=================================");
		
		for (IrCommand cmd : commands) {
			writer.println(cmd.toString());
		}
		
		writer.close();
	}
}
