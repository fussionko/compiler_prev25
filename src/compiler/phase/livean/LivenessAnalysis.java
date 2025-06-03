package compiler.phase.livean;

import java.util.*;

import compiler.phase.*;
import compiler.phase.asmgen.*;

public class LivenessAnalysis extends Phase 
{
	public static Vector<Liveness> livenessesAnalysis = new Vector<>();
	public static Vector<InterferanceGraph> interferanceGraphs = new Vector<>();

    public LivenessAnalysis() 
	{
		super("livean");
	}

    public void analyseLiveness(Vector<Vector<ASM.Instr>> programInstrs)
	{
		for (final Vector<ASM.Instr> instrs : programInstrs)
		{	
			Liveness liveness = new Liveness();
			liveness.computeLiveness(instrs);
			livenessesAnalysis.add(liveness);

			InterferanceGraph interferanceGraph = new InterferanceGraph();
			interferanceGraph.computeInterferanceMatrix(instrs, liveness.getOut());
			interferanceGraphs.add(interferanceGraph);

			// for (int i = 0; i < instrs.size(); ++i)
			// {
			// 	ASM.Instr instr = instrs.get(i);
			// 	if (instr instanceof ASM.Call)
			// 	{
			// 		System.out.println(instr);
			// 		System.out.println(liveness.getIn().get(i));
			// 		System.out.println(liveness.getOut().get(i));
			// 		System.out.println(interferanceGraph.getInterferanceMatrix());
			// 	}
			// }
			// System.out.println();
			// System.out.println();
		}	
	}

	public static void analyseLiveness(int livenessIndex, Vector<ASM.Instr> instrs)
	{
		Liveness liveness = new Liveness();
		liveness.computeLiveness(instrs);
		livenessesAnalysis.set(livenessIndex, liveness);

		InterferanceGraph interferanceGraph = new InterferanceGraph();
		interferanceGraph.computeInterferanceMatrix(instrs, liveness.getOut());
		interferanceGraphs.set(livenessIndex, interferanceGraph);	
	}

    public void log() 
	{
		Logger asmLogger = new Logger(logger);
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
