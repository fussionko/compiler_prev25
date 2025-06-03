package compiler.phase.asmgen;

import java.util.*;

import compiler.phase.*;
import compiler.phase.imclin.*;


public class AsmGen extends Phase
{
	public final static Vector<Vector<ASM.Instr>> codeChunkInstructions = new Vector<Vector<ASM.Instr>>();
	public static Vector<LIN.CodeChunk> codeChunks;

	public final static Vector<ASM.Instr> dataChunkInstructions = new Vector<ASM.Instr>();
	public static Vector<LIN.DataChunk> dataChunks;

    /**
	 * Constructs a new phase for the generating processor instructions.
	 */
	public AsmGen() 
	{
		super("asmgen");
	}

	public void log() 
	{
		Logger asmLogger = new Logger(logger);
	}


	public void addCodeChunks(Vector<LIN.CodeChunk> addCodeChunks) 
	{
		codeChunks = addCodeChunks;
	}

	public void addDataChunks(Vector<LIN.DataChunk> addDataChunks) 
	{
		dataChunks = addDataChunks;
	}

	public void generateInstructions()
	{
		for (final LIN.CodeChunk codeChunk : codeChunks) 
		{
			codeChunkInstructions.add(new AsmGenerator().generateInstructions(codeChunk));
		}
	}

	public void printInstructions() 
	{
		for (int i = 0; i < codeChunks.size(); ++i)
		{
			System.out.println(codeChunks.get(i).frame.label.name + ":");
			for (ASM.Instr instr : codeChunkInstructions.get(i)) 
			{
				if (!(instr instanceof ASM.Label))
					System.out.print("    ");

				System.out.println(instr);
			}
		}
	}

	// ===== LOGGER =====

	public static class Logger
	{
		/** The logger the log should be written to. */
		private final compiler.common.logger.Logger logger;

		/**
		 * Constructs a new visitor with a logger the log should be written to.
		 * 
		 * @param logger The logger the log should be written to.
		 */
		public Logger(compiler.common.logger.Logger logger) 
		{
			this.logger = logger;
		}
	}
}
