package compiler.phase.regall;

import java.util.*;

public class REG
{   
    static Vector<String> regList = new Vector<>(Arrays.asList(
        "x0", "x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x10", "x11",
        "x12", "x13", "x14", "x15", "x16", "x17", "x18", "x19", "x20", "x21", "x22",
        "x23", "x24", "x25", "x26", "x27", "x28", "x29", "x30", "x31"
    ));

    static Vector<String> regListABI = new Vector<>(Arrays.asList(
        "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2", "s0", "s1",
        "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "s2", "s3", "s4",
        "s5", "s6", "s7", "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
    ));

    // Skip ra (prologue), a0
    static Vector<Integer> callerSavedRegs = new Vector<>(Arrays.asList(
        6, 7, 11, 12, 13, 14, 15, 16, 17, 28, 29, 30, 31
    ));

    // Sp not added (prologue)
    static Vector<Integer> calleSavedRegs = new Vector<>(Arrays.asList(
        9, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27
    ));

    static Set<Integer> skipRegs = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 8, 10));

    static int regA0 = 10;

    static int minRegs = 6;
    static int maxRegs = 32;
}