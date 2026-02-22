import java.io.*;
import java.util.*;
import java_cup.runtime.Symbol;
import ast.*;
import ast.Helpers.HelperFunctions;
//import ast.Dec.AstDecList;

public class Main
{
	static public void main(String argv[])
	{
		Lexer l;
		Parser p;
		AstProgram ast;
		FileReader fileReader = null;
		PrintWriter fileWriter = null;
		String inputFileName = argv[0];
		String outputFileName = argv[1];
		
		try
		{
			/********************************/
			/* [1] Initialize a file reader */
			/********************************/
			fileReader = new FileReader(inputFileName);

			/********************************/
			/* [2] Initialize a file writer */
			/********************************/
			fileWriter = new PrintWriter(outputFileName);
			
			/****************************************/
			/* [2.5] Set file writer in HelperFunctions */
			/****************************************/
			HelperFunctions.setFileWriter(fileWriter);
			
			/******************************/
			/* [3] Initialize a new lexer */
			/******************************/
			l = new Lexer(fileReader);
			
			/*******************************/
			/* [4] Initialize a new parser */
			/*******************************/
			p = new Parser(l, fileWriter);

			/***********************************/
			/* [5] 3 ... 2 ... 1 ... Parse !!! */
			/***********************************/
			//ast = (AstDecList) p.parse().value;
			ast = (AstProgram) p.parse().value;
			
			/*************************/
			/* [6] Print the AST ... */
			/*************************/
			// ast.printMe();

			/**************************/
			/* [7] Semant the AST ... */
			/**************************/
			ast.SemantMe();
			
			/*********************************/
			/* [8] Generate IR from AST ...  */
			/*********************************/
			ast.irMe();//was written before tomerm edit
			
			// Get IR commands -tomerm edit from here
   			List<ir.IrCommand> irCommands = ir.Ir.getInstance().getCommandList();

// Build CFG
                        cfg.Fullcfggraph controlFlowGraph = cfg.Fullcfggraph.buildFromIR(irCommands);

                        // Run dataflow analysis
                        Set<String> uninitializedVars = controlFlowGraph.runDataflowAnalysis();

                        // Convert scope-qualified IR names (e.g. "x_0") back to original source names (e.g. "x")
                        Set<String> originalVarNames = new java.util.HashSet<>();
                        for (String irName : uninitializedVars) {
                                originalVarNames.add(ir.Ir.getInstance().getOriginalName(irName));
                        }

                        /****************************************/
                        /* [9] Output dataflow analysis results */
                        /****************************************/
                        if (originalVarNames.isEmpty()) {
                                // No uninitialized variables detected
                                fileWriter.write("!OK");
                        } else {
                                // Sort and output each uninitialized variable on separate line
                                List<String> sortedVars = new ArrayList<>(originalVarNames);
				Collections.sort(sortedVars);
				for (int i = 0; i < sortedVars.size(); i++) {
					fileWriter.write(sortedVars.get(i));
					if (i < sortedVars.size() - 1) {
						fileWriter.write("\n");
					}
				}
			}
			
			/*************************************/
			/* [10] Finalize AST GRAPHIZ DOT file */
			/*************************************/
			// AstGraphviz.getInstance().finalizeFile();
    	}
			     
		catch (Exception e)
		{
			e.printStackTrace();
			fileWriter.write("ERROR");
		}

		finally 
		{
            // ALWAYS close the file to flush the buffer
            if (fileWriter != null) {
                fileWriter.close();
            }
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}
}

