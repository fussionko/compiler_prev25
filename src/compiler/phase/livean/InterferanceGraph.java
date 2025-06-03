package compiler.phase.livean;

import java.util.*;

import compiler.phase.asmgen.*;
import compiler.phase.memory.*;

public class InterferanceGraph 
{
    private HashMap<MEM.Temp, HashMap<MEM.Temp, Boolean>> interferanceMatrix;

    private void setEdge(MEM.Temp a)
    {
        if (!interferanceMatrix.containsKey(a))
            interferanceMatrix.put(a, new HashMap<>());
    }

    private void setEdge(MEM.Temp a, MEM.Temp b)
    {
        if (!interferanceMatrix.containsKey(a))
            interferanceMatrix.put(a, new HashMap<>());

        if (!interferanceMatrix.containsKey(b))
            interferanceMatrix.put(b, new HashMap<>());

        interferanceMatrix.get(a).put(b, true);
        interferanceMatrix.get(b).put(a, true);
    }

    // private void clearEdge(MEM.Temp a, MEM.Temp b)
    // {
    //     interferanceMatrix.get(a).put(b, false);
    //     interferanceMatrix.get(b).put(a, false);
    // }

    private void addEdges(MEM.Temp a, Set<MEM.Temp>bj)
    {
        if (bj != null && !bj.isEmpty())
        {
            for (MEM.Temp b : bj)
            {
                setEdge(a, b);
            }
        }
        else 
            setEdge(a);

    }

    private void addDiffEdges(MEM.Temp a, Set<MEM.Temp>bj, MEM.Temp c)
    {
        if (bj != null && !bj.isEmpty())
        {
            for (MEM.Temp b : bj)
            {
                if (b != c)
                {
                    setEdge(a, b);
                }
            }
        }
        else 
        {
            setEdge(a);
        }
    }

    public HashMap<MEM.Temp, HashMap<MEM.Temp, Boolean>> getInterferanceMatrix()
    {
        return interferanceMatrix;
    }

    public void computeInterferanceMatrix(final Vector<ASM.Instr> instrs, final Vector<Set<MEM.Temp>> out)
    {
        interferanceMatrix = new HashMap<>();

        for (int i = 0; i < instrs.size(); ++i)
        {
            ASM.Instr instr = instrs.get(i);
            if (instr instanceof ASM.MoveInstr)
            {
                if (instr.defs.size() == 1 && instr.uses.size() == 1)
                {
                    addDiffEdges(instr.defs.get(0), out.get(i), instr.uses.get(0));
                }
            }
            else 
            {
                if (instr.defs.size() == 1)
                {
                    addEdges(instr.defs.get(0), out.get(i));
                }
            }
        }
    }   
}
