package compiler.phase.livean;

import java.util.*;

import compiler.common.report.*;
import compiler.phase.memory.*;
import compiler.phase.asmgen.*;

public class Liveness
{
    private Vector<Set<Integer>> pred;
    private Vector<Set<Integer>> succ;

    private Vector<Set<MEM.Temp>> in;
    private Vector<Set<MEM.Temp>> out;

    private int liveness_calc_iter_max;

    Liveness()
    {
        pred = new Vector<>();
        succ = new Vector<>();

        in  = new Vector<>();
        out = new Vector<>();

        liveness_calc_iter_max = 25;
    }

    public Vector<Set<MEM.Temp>> getIn()
    {
        return in;
    }

    public Vector<Set<MEM.Temp>> getOut()
    {
        return out;
    }

    private void printInOut()
    {
        System.out.println("------------------------------------------");
        // for (int i = 0; i < pred.size(); ++i)
        // {
        //     System.out.println(i + ": " + instrs.get(i) + " -> " + pred.get(i) + " | " + succ.get(i));
        // }

        for (int i = in.size() - 1; i >= 0; --i)
        {
            System.out.print(i + "  ");
            for (MEM.Temp o : out.get(i))
            {
                System.out.print(o.temp + ",");
            }
            System.out.print("  ");

            for (MEM.Temp o : in.get(i))
            {
                System.out.print(o.temp + ",");
            }

            System.out.println();
        }
    }

    private void calcFlow(Vector<ASM.Instr> instrs)
    {
        HashMap<MEM.Label, Integer> labelMap = new HashMap<>();

        for (int i = 0; i < instrs.size(); ++i)
        {
            final ASM.Instr instr = instrs.get(i);
            if (instr instanceof ASM.Label)
            {
                labelMap.put(((ASM.Label)instr).label, i);
            }

            pred.add(new HashSet<>());
            succ.add(new HashSet<>());
        }

        // succ.get(0).add(1);
        // pred.lastElement().add(instrs.size() - 2);
        for (int i = 0; i < instrs.size(); ++i)
        {
            // if (!(instrs.get(i - 1) instanceof ASM.J))
            // {
            //     pred.get(i).add(i - 1);
            // }

            final ASM.Instr instr = instrs.get(i);
            if (instr instanceof ASM.JumpInstr)
            {
                ASM.JumpInstr jumpInstr = (ASM.JumpInstr)instr;

                if (labelMap.containsKey(jumpInstr.falseLabel)) 
                {
                    int jumpFalseIndex = labelMap.get(jumpInstr.falseLabel);

                    succ.get(i).add(jumpFalseIndex);
                    pred.get(jumpFalseIndex).add(i);
                }

                if (labelMap.containsKey(jumpInstr.trueLabel))
                {
                    int jumpTrueIndex = labelMap.get(jumpInstr.trueLabel);

                    succ.get(i).add(jumpTrueIndex);
                    pred.get(jumpTrueIndex).add(i);  
                }
            }
            else if (instr instanceof ASM.J)
            {
                ASM.J jInstr = (ASM.J)instr;
                if (labelMap.containsKey(jInstr.offsetLabel))
                {
                    int jumpIndex = labelMap.get(jInstr.offsetLabel);

                    succ.get(i).add(jumpIndex);
                    pred.get(jumpIndex).add(i);
                }
            }
            // else 
            // {
            //     succ.get(i).add(i + 1);
            // }
            else if ((i + 1) < instrs.size())
            {
                succ.get(i).add(i + 1);
                pred.get(i + 1).add(i);
            }
        }
        // System.out.println(instrs.firstElement());
        // System.out.println(instrs.size() + " " + instrs);
        // System.out.println(pred);
        // System.out.println(succ);

    }

    private boolean checkFixedPoint(Vector<Set<MEM.Temp>> inPrev, Vector<Set<MEM.Temp>> outPrev)
    {
        return inPrev.equals(in) && outPrev.equals(out);
    }

    private void livenessIter(final Vector<ASM.Instr> instrs)
    {
        for (int n = instrs.size() - 1; n >= 0; --n)
        {
            Set<MEM.Temp> newOut = new HashSet<>();
            Set<MEM.Temp> newIn = new HashSet<>();

            // Out -> union in[s], s = succ[n]
            for (int s : succ.get(n))
            {
                newOut.addAll(in.get(s));
            }

            // In use[n] union out[n]-def[n]
            // System.out.println(n + " " + instrs.size());
            // System.out.println(instrs);
            newIn.addAll(instrs.get(n).uses);
        
            for (MEM.Temp temp : newOut)
            {
                if (!(instrs.get(n).defs.contains(temp))) 
                {
                    newIn.add(temp);
                }
            }
    
            out.set(n, newOut);
            in.set(n, newIn);
        }
    }

    public void computeLiveness(final Vector<ASM.Instr> instrs)
    {
        calcFlow(instrs);

        for (int i = 0; i < instrs.size(); ++i)
        {
            in.add(new HashSet<>());
            out.add(new HashSet<>());
        }

        boolean success = false;

        livenessIter(instrs);
        for (int i = 1; i < liveness_calc_iter_max; ++i)
        {
            Vector<Set<MEM.Temp>> inPrev = new Vector<>();
            Vector<Set<MEM.Temp>> outPrev = new Vector<>();

            for (int j = 0; j < instrs.size(); ++j)
            {
                inPrev.add(new HashSet<>(in.get(j)));
                outPrev.add(new HashSet<>(out.get(j)));
            }

            livenessIter(instrs);

            if (checkFixedPoint(inPrev, outPrev))
            {
                success = true;
                break;
            }
        }

        if (!success)
            throw new Report.Error("Failed to compute variable liveness.");

        // printInOut();
    }

}