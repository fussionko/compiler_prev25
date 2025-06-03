package compiler.phase.regall;

import java.util.*;

import compiler.phase.livean.*;
import compiler.phase.memory.*;

public class GraphColor 
{
    public Map<MEM.Temp, Integer> colorMap;
    public Map<Integer, MEM.Temp> colorToTemp;
    public Set<Integer> regUsed;

    private Vector<String> regList;
    private Set<Integer> skipRegs;
    private Set<MEM.Temp> skipSpillRegs;

    private int regStartAt;
    private int regEndAt;

    GraphColor(Vector<String> regList, Set<Integer> skipRegs, Set<MEM.Temp> skipSpillRegs, int regStartAt)
    {
        this.regList        = regList;
        this.skipRegs       = skipRegs;
        this.skipSpillRegs  = skipSpillRegs;
        this.regStartAt     = regStartAt;
        this.regEndAt       = regList.size();
    }

    private Map<MEM.Temp, Integer> initDegree(final HashMap<MEM.Temp, HashMap<MEM.Temp, Boolean>> matrix, final Set<MEM.Temp> nodes, final Map<MEM.Temp, Integer> initMap)
    {
        Map<MEM.Temp, Integer> degree = new HashMap<>();

        // Initialize degrees
        for (MEM.Temp t : nodes) 
        {
            int deg = 0;

            if (initMap.containsKey(t))
            {
                degree.put(t, Integer.MAX_VALUE);
            }
            else 
            {
                for (Boolean b : matrix.get(t).values()) 
                    if (b) 
                        deg++;
                degree.put(t, deg);
            }
        }

        return degree;
    }

    private Stack<MEM.Temp> simplifySpill(final int numRegsUsed, final HashMap<MEM.Temp, HashMap<MEM.Temp, Boolean>> matrix, Map<MEM.Temp, Integer> degree, final Map<MEM.Temp, Integer> initMap)
    {
        Stack<MEM.Temp> stack = new Stack<>();
        // Set<MEM.Temp> spilled = new HashSet<>();
        Set<MEM.Temp> nodes = new HashSet<>(matrix.keySet());
        Set<MEM.Temp> removed = new HashSet<>();


        // Simplify & Spill
        while (removed.size() < nodes.size())
        {
            boolean found = false;
            for (MEM.Temp t : nodes) 
            {
                if (removed.contains(t)) 
                    continue;

                if (degree.get(t) < numRegsUsed) 
                {
                    stack.push(t);
                    removed.add(t);

                    // Decrease degree of neighbors
                    for (MEM.Temp n : matrix.get(t).keySet()) 
                    {
                        if (matrix.get(t).get(n) && degree.containsKey(n))
                        {  
                            if (!initMap.containsKey(n))
                            {
                                degree.put(n, degree.get(n) - 1);
                            }
                        }
                    }

                    found = true;
                    break;
                }
            }

            if (!found) 
            {
                // Spill: pick any remaining node
                for (MEM.Temp t : nodes) 
                {
                    if (!removed.contains(t)) 
                    {
                        stack.push(t);
                        removed.add(t);
                        // spilled.add(t);
                        break;
                    }
                }
            }
        }

        return stack;
    }

    public MEM.Temp color(final int numRegsUsed, final InterferanceGraph interferanceGraph, Map<MEM.Temp, Integer> initMap)
    {
        final HashMap<MEM.Temp, HashMap<MEM.Temp, Boolean>> matrix = interferanceGraph.getInterferanceMatrix();

        Map<MEM.Temp, Integer> degree   = initDegree(matrix, matrix.keySet(), initMap);
        Stack<MEM.Temp> stack           = simplifySpill(numRegsUsed, matrix, degree, initMap);

        MEM.Temp spill = null;
        int maxDegree = Integer.MIN_VALUE;

        colorMap    = new HashMap<>();
        colorToTemp = new HashMap<>();
        regUsed     = new HashSet<>();
        if (initMap == null)
            initMap = new HashMap<>();

        // Assign colors
        while (!stack.isEmpty()) 
        {
            MEM.Temp t = stack.pop();
            Set<Integer> used = new HashSet<>();
            for (MEM.Temp n : matrix.get(t).keySet()) 
            {
                if (matrix.get(t).get(n) && colorMap.containsKey(n))
                    used.add(colorMap.get(n));
            }

            // Precoloring: if t is in initMap, use its color
            if (initMap.containsKey(t))
            {
                final int color = initMap.get(t);
                colorMap.put(t, color);
                colorToTemp.put(color, t);
                regUsed.add(color);
                continue;
            }

            // Find first available color
            int color = -1;
            for (int i = regStartAt; i < numRegsUsed; i++) 
            {
                if (skipRegs.contains(i)) 
                    continue;
                if (!used.contains(i)) 
                {
                    color = i;
                    break;
                }
            }

            if (color == -1) 
            {
                if (degree.get(t) > maxDegree && !skipSpillRegs.contains(t))
                {
                    maxDegree = degree.get(t);
                    spill = t;
                }
            } 
            else 
            {
                colorMap.put(t, color);
                colorToTemp.put(color, t);
                regUsed.add(color);
            }
        }

        return spill;
    }
}
