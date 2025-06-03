package compiler.phase.regall;

import java.util.*;

import compiler.common.report.*;

import compiler.phase.*;
import compiler.phase.asmgen.*;
import compiler.phase.livean.*;
import compiler.phase.memory.*;
import compiler.phase.imclin.*;

public class Regall extends Phase
{
	// RISC-V default
	private static int numberOfRegisters = REG.regList.size();

	private static Vector<Map<MEM.Temp, String>> registerNames = new Vector<>();

	private static int numAllocateAttempts = 10000;

	public static Vector<Vector<ASM.Instr>> instructions;

	private static Set<MEM.Temp> constRegs = new HashSet<>(Arrays.asList(ASM.zero, ASM.ra, ASM.sp, ASM.gp, ASM.tp, ASM.alr, ASM.fp, ASM.a0));
	private static Vector<LIN.CodeChunk> codeChunks = ImcLin.codeChunks();

    /**
	 * Constructs a new phase for the generating processor instructions.
	 */
	public Regall() 
	{
		super("regall");
	}

	private class StackAllocSize 
	{
		public final Long maxOffsetSlot;
		public final Long maxArgsSize;

		public StackAllocSize(long maxOffsetSlot, long maxArgsSize)
		{
			this.maxOffsetSlot 	= maxOffsetSlot;
			this.maxArgsSize 	= maxArgsSize;
		}
	}

	private Map<MEM.Temp, Integer> precolor(Vector<ASM.Instr> instrs) 
	{
		Map<MEM.Temp, Integer> map = new HashMap<>();
		map.put(ASM.zero, 0);
		map.put(ASM.ra, 1);
		map.put(ASM.sp, 2);
		map.put(ASM.gp, 3);
		map.put(ASM.tp, 4);
		map.put(ASM.alr, 5);
		map.put(ASM.fp, 8);
		map.put(ASM.a0, 10);

		return map;
	}

	public void allocateRegisters(Integer numOfRegisters, final Vector<Vector<ASM.Instr>> instrs)
	{
		if (numOfRegisters == null)
			numOfRegisters = numberOfRegisters;
		if (numOfRegisters > REG.maxRegs)
			throw new Report.Error("Too many registers specified, max: " + REG.regList.size());
	    if (numOfRegisters < REG.minRegs)
			throw new Report.Error("Number of registers must be " + REG.minRegs + " or higher");

		instructions = new Vector<>();
		for (Vector<ASM.Instr> chunk : instrs) 
		{
    		instructions.add(new Vector<>(chunk));
		}

		for (int i = 0; i < LivenessAnalysis.interferanceGraphs.size(); ++i)
		{

			GraphColor graphColor = new GraphColor(REG.regListABI, REG.skipRegs, constRegs, REG.minRegs);

			Map<MEM.Temp, Long> offsetSlotsMap = new HashMap<>();
			long offsetSlot = codeChunks.get(i).frame.locsSize / 8;

			int attempt = 0;
			// System.out.println(i +  " : " + instructions.get(i));

			for (; attempt < numAllocateAttempts; ++attempt)
			{
				Map<MEM.Temp, Integer> precolorMap = precolor(instructions.get(i));
				final MEM.Temp spill = graphColor.color(numOfRegisters, LivenessAnalysis.interferanceGraphs.get(i), precolorMap);

				// No spill continue
				if (spill == null)
					break;

				offsetSlotsMap.put(spill, offsetSlot);
				offsetSlot++;

				// Map<MEM.Temp, Long> spillOffsets = getOffsets(new HashSet<>(Arrays.asList(spill)));
				instructions.set(i, rewriteInstrs(instructions.get(i), spill, offsetSlotsMap));

				LivenessAnalysis.analyseLiveness(i, instructions.get(i));
				LivenessAnalysis.interferanceGraphs.set(i, LivenessAnalysis.interferanceGraphs.get(i));
			}
			if (attempt >= numAllocateAttempts)
				throw new Report.Error("Cannot allocate registers for this function");

			
			// Add instructions to save caller regs
			offsetSlot++;
			StackAllocSize stackAllocSize = saveCallerRegs(i, LivenessAnalysis.livenessesAnalysis.get(i).getOut(), graphColor, offsetSlot);
			offsetSlot = stackAllocSize.maxOffsetSlot;
			
			Set<Integer> calleRegsUsed = new HashSet<>();
	
			for (Integer reg : graphColor.regUsed)
			{
				if (REG.calleSavedRegs.contains(reg))
				{
					calleRegsUsed.add(reg);
					// offsetSlotsMap.put(graphColor.colorToTemp.get(reg), offsetSlot);
					// offsetSlot++;
				}
			}
			// System.out.println(REG.calleSavedRegs);
			// System.out.println(calleRegsUsed);
			// System.out.println(graphColor.colorToTemp);

			// System.out.println(offsetSlot +" " +stackAllocSize.maxArgsSize + " " + calleRegsUsed.size());
			modifyPrologueEpilogue(i, calleRegsUsed, graphColor.colorToTemp, offsetSlot, stackAllocSize.maxArgsSize, calleRegsUsed.size());

			LivenessAnalysis.analyseLiveness(i, instructions.get(i));
			LivenessAnalysis.interferanceGraphs.set(i, LivenessAnalysis.interferanceGraphs.get(i));

			Map<MEM.Temp, Integer> precolorMap = precolor(instructions.get(i));
			for (MEM.Temp temp : graphColor.colorMap.keySet())
			{
				if (precolorMap.containsKey(temp))
					continue;

				precolorMap.put(temp, graphColor.colorMap.get(temp));
			}

			final MEM.Temp errorSpill = graphColor.color(numOfRegisters, LivenessAnalysis.interferanceGraphs.get(i), precolorMap);
			if (errorSpill != null)
				throw new Report.Error("Spill on prologue, epilogue, calle or caller saved registers");

			// instructions.set(i, saveCallerRegs(instructions.get(i), LivenessAnalysis.livenessesAnalysis.get(i), graphColors.get(i)));

			registerNames.add(convertColorMapToRegName(graphColor.colorMap, REG.regListABI));
		}
	}

	private Map<MEM.Temp, String> convertColorMapToRegName(final Map<MEM.Temp, Integer> colorMap, Vector<String> regNames)
	{
		Map<MEM.Temp, String> regNamesMap = new HashMap<>();

		for (MEM.Temp temp : colorMap.keySet())
		{
			regNamesMap.put(temp, regNames.get(colorMap.get(temp)));
		}

		return regNamesMap;
	}

	private Long calcOffsetFP(long slot)
	{
		return slot * -8l;
	}

	private Long calcOffsetSP(long slot)
	{
		return slot * 8l;
	}

	private Vector<ASM.Instr> rewriteInstrs(final Vector<ASM.Instr> instrs, final MEM.Temp spill, final Map<MEM.Temp, Long> spillOffsetSlots)
	{
		Vector<ASM.Instr> rewritenInstrs = new Vector<>();

		for (ASM.Instr instr : instrs)
		{
			ASM.Instr modifedInstr = null;
			if (instr.uses.contains(spill))
			{
				MEM.Temp tempUse = new MEM.Temp();
				rewritenInstrs.add(new ASM.Ld(tempUse, ASM.fp, calcOffsetFP(spillOffsetSlots.get(spill))));
				modifedInstr = changeInstrTemps(instr, instr.uses, spill, tempUse);
			}

			MEM.Temp tempDef = null;
			boolean addDef = false;
			if (instr.defs.contains(spill))
			{
				addDef = true;
				tempDef = new MEM.Temp();
				if (modifedInstr == null)
					modifedInstr = changeInstrTemps(instr, instr.defs, spill, tempDef);
				else 
					modifedInstr = changeInstrTemps(modifedInstr, modifedInstr.defs, spill, tempDef);
			}

			rewritenInstrs.add(modifedInstr);

			if (addDef)
			{
				// System.out.println("200: " + tempDef);
				rewritenInstrs.add(new ASM.Sd(tempDef, ASM.fp, calcOffsetFP(spillOffsetSlots.get(spill))));
			}
		}

		return rewritenInstrs;
	}

	private ASM.Instr changeInstrTemps(final ASM.Instr instr, final Vector<MEM.Temp> temps, MEM.Temp spill, MEM.Temp changeTemp)
	{
		Map<MEM.Temp, MEM.Temp> tempMap = new HashMap<>();
		
		if (changeTemp == null)
			return instr;
		
		for (MEM.Temp temp : temps)
		{
			if (temp == spill)
				tempMap.put(temp, changeTemp);
		}

		return instr.copyWithTempMap(tempMap);
	}

	private void modifyPrologueEpilogue(int instrIndex, final Set<Integer> calleRegsUsed, final Map<Integer, MEM.Temp> regToTemp, long localSlots, long calleSlots, long argsSlots)
	{ 
		Vector<ASM.Instr> newInstrs = new Vector<>();

		final long frameSize 	= localSlots + calleSlots + argsSlots + 4;
		final long infoSize 	= calleSlots + argsSlots + 4; 

		Map<MEM.Temp, Long> offsetSlotsMap = new HashMap<>();

		// First 4 is epilogue -> 
		newInstrs.add(new ASM.Addi(ASM.sp, ASM.sp, frameSize * -8l));
        newInstrs.add(new ASM.Sd(ASM.ra, ASM.sp, (infoSize - 1) * 8));
        newInstrs.add(new ASM.Sd(ASM.fp, ASM.sp, (infoSize - 2) * 8));

		long tmpOffsetSlot = infoSize - 3l;

		// System.out.println(calleRegsUsed);
		// System.out.println(instructions.get(instrIndex));
		for (Integer reg : calleRegsUsed)
		{
			MEM.Temp temp = regToTemp.get(reg);
			// System.out.println("246: " + temp + " " + reg);
			newInstrs.add(new ASM.Sd(temp, ASM.sp, tmpOffsetSlot * 8));
			offsetSlotsMap.put(temp, tmpOffsetSlot);
			tmpOffsetSlot--;
		}

        newInstrs.add(new ASM.Addi(ASM.fp, ASM.sp, frameSize * 8l));

		// final int size = instructions.get(instrIndex).size();
		newInstrs.addAll(instructions.get(instrIndex));

		for (Integer reg : calleRegsUsed)
		{
			MEM.Temp temp = regToTemp.get(reg);
			long slot = offsetSlotsMap.get(temp);
			newInstrs.add(new ASM.Ld(temp, ASM.sp, slot * 8));
		}

        newInstrs.add(new ASM.Ld(ASM.fp, ASM.sp, (infoSize - 2) * 8));
        newInstrs.add(new ASM.Ld(ASM.ra, ASM.sp, (infoSize - 1) * 8));
        newInstrs.add(new ASM.Addi(ASM.sp, ASM.sp, frameSize * 8));
        newInstrs.add(new ASM.Jr(ASM.ra));
	
		instructions.set(instrIndex, newInstrs);

	}

	private StackAllocSize saveCallerRegs(final int instrIndex, final Vector<Set<MEM.Temp>> out, final GraphColor graphColor, long offsetSlotStart)
	{
		Vector<ASM.Instr> newInstrs = new Vector<>();

		long maxSlot = offsetSlotStart;
		long maxArgs = 0l;

		for (int i = 0; i < instructions.get(instrIndex).size(); ++i)
		{
			final ASM.Instr instr = instructions.get(instrIndex).get(i);

			Map<Integer, Long> saveOffsetSlots = null;
			if (instr instanceof ASM.FunInstr)
			{
				saveOffsetSlots = new HashMap<>();
				long offsetSlot = offsetSlotStart;

				final Set<MEM.Temp> liveOut = out.get(i);
				for (MEM.Temp liveOutTemp : liveOut)
				{
					final int color = graphColor.colorMap.get(liveOutTemp);
					if (REG.callerSavedRegs.contains(color))
					{
						newInstrs.add(new ASM.Sd(liveOutTemp, ASM.fp, calcOffsetFP(offsetSlot)));
						saveOffsetSlots.put(color, offsetSlot);
						offsetSlot++;
					}
				}

				if (offsetSlot > maxSlot)
				{
					maxSlot = offsetSlot;
				}

				int args = 0;

            	if (instr instanceof ASM.Call)
            	{
            	    args = ((ASM.Call)instr).numOfArgs;
            	}
            	else if (instr instanceof ASM.Jalr)
            	{
            	    args = ((ASM.Jalr)instr).numOfArgs;
            	}
            	else 
            	{
            	    throw new Report.Error("Unsupported call address type");
            	}

				if (args > maxArgs)
				{
					maxArgs = args;
				}
			}

			newInstrs.add(instr);

			if (saveOffsetSlots != null && !saveOffsetSlots.isEmpty())
			{
				for (final Integer regColor : saveOffsetSlots.keySet())
				{
					newInstrs.add(new ASM.Ld(graphColor.colorToTemp.get(regColor), ASM.fp, calcOffsetFP(saveOffsetSlots.get(regColor))));
				}
			}


		}

		instructions.set(instrIndex, newInstrs);

		return new StackAllocSize(maxSlot, maxArgs);
	}

	public static Vector<String> getInstructions()
	{
		Vector<String> instrs = new Vector<>();
		for (int i = 0; i < codeChunks.size(); ++i)
		{
			instrs.add(codeChunks.get(i).frame.label.name + ":");
			for (ASM.Instr instr : instructions.get(i)) 
			{
				if (!(instr instanceof ASM.Label))
				{
					instrs.add("    " + instr.toString(registerNames.get(i)));
				}
				else 
				{
					instrs.add(instr.toString(registerNames.get(i)));
				}
			}
		}
		return instrs;
	}

	public static void printInstructions() 
	{
		Vector<String> instrs = getInstructions();

		for (String instr : instrs)
		{
			System.out.println(instr);
		}
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