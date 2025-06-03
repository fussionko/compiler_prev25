package compiler.phase.asmgen;

import java.util.*;

import compiler.phase.memory.*;

/**
 * Assembly code for RISCV.
 * 
 * @author fussionko
 */
public class ASM 
{
    public static MEM.Temp zero = new MEM.Temp();
    public static MEM.Temp ra   = new MEM.Temp();
    public static MEM.Temp sp   = new MEM.Temp();
    public static MEM.Temp gp   = new MEM.Temp();
    public static MEM.Temp tp   = new MEM.Temp();
    public static MEM.Temp alr  = new MEM.Temp();
    public static MEM.Temp fp   = new MEM.Temp();
    public static MEM.Temp a0   = new MEM.Temp();

    public static abstract class Instr 
    {
        // Information about which temporary variables are assigned a value
        public final Vector<MEM.Temp> defs;

        // Information about which temporary variables instruction uses
        public final Vector<MEM.Temp> uses;

        public final String mnemonic;

        public Instr(String mnemonic, Vector<MEM.Temp> defs, Vector<MEM.Temp> uses) 
        {
            this.mnemonic = mnemonic;
            this.defs = defs;
            this.uses = uses;
        }

        @Override
        public abstract String toString();

        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return toString();
        }

        /**
         * Returns a new instruction with the given temps substituted.
         * The mapping is from old temp to new temp.
         */
        public abstract Instr copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap);
    }

    public static abstract class JumpInstr extends Instr
    {
        public MEM.Label trueLabel;
        public MEM.Label falseLabel;

        public JumpInstr(String mnemonic, Vector<MEM.Temp> defs, Vector<MEM.Temp> uses, MEM.Label trueLabel, MEM.Label falseLabel) 
        {
            super(mnemonic, defs, uses);
            this.trueLabel = trueLabel;
            this.falseLabel = falseLabel;
        }
    }

    public static abstract class MoveInstr extends Instr 
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs;

        public MoveInstr(String mnemonic, MEM.Temp rd, MEM.Temp rs) 
        {
            super(mnemonic, new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs)));
            this.rd = rd;
            this.rs = rs;
        }
    }

    public static class Mv extends MoveInstr
    {
        public Mv(MEM.Temp rd, MEM.Temp rs1) 
        {
            super("mv", rd, rs1);
        }

        @Override
        public Mv copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Mv(tempMap.getOrDefault(this.rd, this.rd), tempMap.getOrDefault(this.rs, this.rs));
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rd, rd.toString()) + "," +
                regMap.getOrDefault(rs, rs.toString());
        }
    }

    public static class Label extends Instr
    {
        public final MEM.Label label;
            
        public Label(MEM.Label label) 
        {
            super("", new Vector<>(), new Vector<>());
            this.label = label;
        }

        @Override
        public Label copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Label(label);
        }

        @Override
        public String toString() 
        {
            return label.name + ":";
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return label.name + ":";
        }
    }

    // Type R Instructions
    public static abstract class TypeR extends Instr
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs1;
        public final MEM.Temp rs2;

        public TypeR(String mnemonic, MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super(mnemonic, new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs1, rs2)));
            this.rd = rd;
            this.rs1 = rs1;
            this.rs2 = rs2;
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs1 + "," + rs2;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rd, rd.toString()) + "," +
                regMap.getOrDefault(rs1, rs1.toString()) + "," +
                regMap.getOrDefault(rs2, rs2.toString());
        }
    }

    public static class Add extends TypeR
    {
        public Add(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("add", rd, rs1, rs2);
        }

        @Override
        public Add copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Add(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
    }

    public static class Sub extends TypeR 
    {
        public Sub(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("sub", rd, rs1, rs2);
        }

        @Override
        public Sub copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Sub(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
    }

    public static class Or extends TypeR 
    {
        public Or(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("or", rd, rs1, rs2);
        }
        
        @Override
        public Or copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Or(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
    }

    public static class And extends TypeR 
    {
        public And(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("and", rd, rs1, rs2);
        }
        
        @Override
        public And copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new And(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
    }

    public static class Mul extends TypeR 
    {
        public Mul(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("mul", rd, rs1, rs2);
        }
        
        @Override
        public Mul copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Mul(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
    }

    public static class Div extends TypeR 
    {
        public Div(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("div", rd, rs1, rs2);
        }
        
        @Override
        public Div copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Div(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
    }

    public static class Rem extends TypeR 
    {
        public Rem(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("rem", rd, rs1, rs2);
        }
        
        @Override
        public Rem copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Rem(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
    }

    public static class Xor extends TypeR 
    {
        public Xor(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("xor", rd, rs1, rs2);
        }
        
        @Override
        public Xor copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Xor(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
        
    }

    public static class Slt extends TypeR 
    {
        public Slt(MEM.Temp rd, MEM.Temp rs1, MEM.Temp rs2) 
        {
            super("slt", rd, rs1, rs2);
        }

        @Override
        public Slt copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Slt(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2));
        }
    }

    // Type I Instructions
    public static abstract class TypeI extends Instr
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs1;
        public final Long imm;

        public TypeI(String mnemonic, MEM.Temp rd, MEM.Temp rs1, Long imm) 
        {
            super(mnemonic, new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs1)));
            this.rd = rd;
            this.rs1 = rs1;
            this.imm = imm;
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs1 + "," + imm;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rd, rd.toString()) + "," +
                regMap.getOrDefault(rs1, rs1.toString()) + "," + imm;
        }
    }

    public static class Addi extends TypeI 
    {
        public Addi(MEM.Temp rd, MEM.Temp rs1, Long imm) 
        {
            super("addi", rd, rs1, imm);
        }
        
        @Override
        public Addi copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Addi(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), imm);
        }
    }

    public static class Ori extends TypeI 
    {
        public Ori(MEM.Temp rd, MEM.Temp rs1, Long imm) 
        {
            super("ori", rd, rs1, imm);
        }
        
        @Override
        public Ori copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Ori(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), imm);
        }
    }

    public static class Xori extends TypeI 
    {
        public Xori(MEM.Temp rd, MEM.Temp rs1, Long imm) 
        {
            super("xori", rd, rs1, imm);
        }
        
        @Override
        public Xori copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Xori(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), imm);
        }
    }

    public static class Slli extends TypeI 
    {
        public Slli(MEM.Temp rd, MEM.Temp rs1, Long imm) 
        {
            super("slli", rd, rs1, imm);
        }
        
        @Override
        public Slli copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Slli(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), imm);
        }
    }

    public static abstract class TypeILoad extends TypeI
    {
        public TypeILoad(String mnemonic, MEM.Temp rd, MEM.Temp rs1, Long imm) 
        {
            super(mnemonic, rd, rs1, imm);
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + imm + "(" + rs1 + ")";
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rd, rd.toString()) + "," + imm + 
                "(" + regMap.getOrDefault(rs1, rs1.toString()) + ")";
        }
    }

    public static class Ld extends TypeILoad 
    {
        public Ld(MEM.Temp rd, MEM.Temp rs1, Long imm) 
        {
            super("ld", rd, rs1, imm);
        }
        
        @Override
        public Ld copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Ld(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), imm);
        }
    }   

    public static class Lb extends TypeILoad 
    {
        public Lb(MEM.Temp rd, MEM.Temp rs1, Long imm) 
        {
            super("lb", rd, rs1, imm);
        }
        
        @Override
        public Lb copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Lb(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), imm);
        }
    }   
    
    // Type J instructions
    public static abstract class TypeJ extends JumpInstr
    {
        public final MEM.Temp rs1;
        public final MEM.Temp rs2;

        public TypeJ(String mnemonic, MEM.Temp rs1, MEM.Temp rs2, MEM.Label trueLabel, MEM.Label falseLabel) 
        {
            super(mnemonic, new Vector<>(), new Vector<>(Arrays.asList(rs1, rs2)), trueLabel, falseLabel);
            this.rs1 = rs1;
            this.rs2 = rs2;
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rs1 + "," + rs2 + "," + trueLabel.name;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rs1, rs1.toString()) + "," +
                regMap.getOrDefault(rs2, rs2.toString()) + "," + trueLabel.name;
        }
    }

    public static class Beq extends TypeJ
    {
        public Beq(MEM.Temp rs1, MEM.Temp rs2, MEM.Label trueLabel, MEM.Label falseLabel) 
        {
            super("beq", rs1, rs2, trueLabel, falseLabel);
        }

        @Override
        public Beq copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Beq(tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2), trueLabel, falseLabel);
        }
    }

    public static class Bne extends TypeJ
    {
        public Bne(MEM.Temp rs1, MEM.Temp rs2, MEM.Label trueLabel, MEM.Label falseLabel) 
        {
            super("bne", rs1, rs2, trueLabel, falseLabel);
        }

        @Override
        public Bne copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Bne(tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2), trueLabel, falseLabel);
        }
    }

    public static class Blt extends TypeJ
    {
        public Blt(MEM.Temp rs1, MEM.Temp rs2, MEM.Label trueLabel, MEM.Label falseLabel) 
        {
            super("blt", rs1, rs2, trueLabel, falseLabel);
        }

        @Override
        public Blt copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Blt(tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2), trueLabel, falseLabel);
        }
    }

    public static class Bge extends TypeJ
    {
        public Bge(MEM.Temp rs1, MEM.Temp rs2, MEM.Label trueLabel, MEM.Label falseLabel) 
        {
            super("bge", rs1, rs2, trueLabel, falseLabel);
        }

        @Override
        public Bge copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Bge(tempMap.getOrDefault(rs1, rs1), tempMap.getOrDefault(rs2, rs2), trueLabel, falseLabel);
        }
    }

    // Type U instructions
    public static abstract class TypeU extends Instr
    {
        public final MEM.Temp rd;
        public final Long imm;
    
        public TypeU(String mnemonic, MEM.Temp rd, Long imm) 
        {
            super(mnemonic, new Vector<>(Arrays.asList(rd)), new Vector<>());
            this.rd = rd;
            this.imm = imm;
        }
    
        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + imm;
        }
    
        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + regMap.getOrDefault(rd, rd.toString()) + "," + imm;
        }
    }

    public static class Lui extends TypeU 
    {
        public Lui(MEM.Temp rd, Long imm) 
        {
            super("lui", rd, imm);
        }

        @Override
        public Lui copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Lui(tempMap.getOrDefault(rd, rd), imm);
        }
    }


    // Type S instructions
    public static abstract class TypeS extends Instr
    {
        public final MEM.Temp rs2;
        public final MEM.Temp rs1;
        public final Long offset;
    
        public TypeS(String mnemonic, MEM.Temp rs2, MEM.Temp rs1, Long offset) 
        {
            super(mnemonic, new Vector<>(), new Vector<>(Arrays.asList(rs2, rs1)));
            this.rs2 = rs2;
            this.rs1 = rs1;
            this.offset = offset;
        }
    
        @Override
        public String toString() 
        {
            return mnemonic + " " + rs2 + "," + offset + "(" + rs1 + ")";
        }
    
        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + 
                regMap.getOrDefault(rs2, rs2.toString()) + "," + offset + 
                "(" + regMap.getOrDefault(rs1, rs1.toString()) + ")";
        }
    }

    public static class Sd extends TypeS 
    {
        public Sd(MEM.Temp rs2, MEM.Temp rs1, Long offset) 
        {
            super("sd", rs2, rs1, offset);
        }
        
        @Override
        public Sd copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Sd(tempMap.getOrDefault(rs2, rs2), tempMap.getOrDefault(rs1, rs1), offset);
        }
    }

    public static class Sb extends TypeS 
    {
        public Sb(MEM.Temp rs2, MEM.Temp rs1, Long offset) 
        {
            super("sb", rs2, rs1, offset);
        }

        @Override
        public Sb copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Sb(tempMap.getOrDefault(rs2, rs2), tempMap.getOrDefault(rs1, rs1), offset);
        }
    }

    // PSEUDO
   
    public static class Li extends Instr
    {
        public final MEM.Temp rd;
        public final Long imm;

        public Li(MEM.Temp rd, Long imm) 
        {
            super("li", new Vector<>(Arrays.asList(rd)), new Vector<>());
            this.rd = rd;
            this.imm = imm;
        }
        
        @Override
        public Li copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Li(tempMap.getOrDefault(rd, rd), imm);
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + imm;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + regMap.getOrDefault(rd, rd.toString()) + "," + imm;
        }
    }

    public static class La extends Instr
    {
        public final MEM.Temp rd;
        public final MEM.Label label;

        public La(MEM.Temp rd, MEM.Label label) 
        {
            super("la", new Vector<>(Arrays.asList(rd)), new Vector<>());
            this.rd = rd;
            this.label = label;
        }

        @Override
        public La copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new La(tempMap.getOrDefault(rd, rd), label);
        }


        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + label.name;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + regMap.getOrDefault(rd, rd.toString()) + "," + label.name;
        }
    }

    public static class Neg extends Instr
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs1;

        public Neg(MEM.Temp rd, MEM.Temp rs1) 
        {
            super("neg", new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs1)));
            this.rd = rd;
            this.rs1 = rs1;
        }

        @Override
        public Neg copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Neg(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1));
        }


        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs1;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + 
                regMap.getOrDefault(rd, rd.toString()) + "," + 
                regMap.getOrDefault(rs1, rs1.toString());
        }
    }

    public static class Not extends Instr
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs1;

        public Not(MEM.Temp rd, MEM.Temp rs1) 
        {
            super("not", new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs1)));
            this.rd = rd;
            this.rs1 = rs1;
        }

        @Override
        public Not copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Not(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1));
        }


        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs1;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + 
                regMap.getOrDefault(rd, rd.toString()) + "," + 
                regMap.getOrDefault(rs1, rs1.toString());
        }
    }

    public static class Sgtz extends Instr
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs1;

        public Sgtz(MEM.Temp rd, MEM.Temp rs1) 
        {
            super("sgtz", new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs1)));
            this.rd = rd;
            this.rs1 = rs1;
        }

        @Override
        public Sgtz copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Sgtz(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1));
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs1;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + 
                regMap.getOrDefault(rd, rd.toString()) + "," + 
                regMap.getOrDefault(rs1, rs1.toString());
        }
    }


    public static class J extends Instr 
    {
        public final MEM.Label offsetLabel;

        public J(MEM.Label offsetLabel) 
        {
            super("j", new Vector<>(), new Vector<>());
            this.offsetLabel = offsetLabel;
        }

        @Override
        public J copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new J(offsetLabel);
        }


        @Override
        public String toString() 
        {
            return mnemonic + " " + offsetLabel.name;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + offsetLabel.name;
        }
    }

    public static class Jr extends Instr 
    {
        public final MEM.Temp r;

        public Jr(MEM.Temp r) 
        {
            super("jr", new Vector<>(), new Vector<>(Arrays.asList(r)));
            this.r = r;
        }

        @Override
        public Jr copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Jr(r);
        }


        @Override
        public String toString() 
        {
            return mnemonic + " " + r;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + regMap.getOrDefault(r, r.toString());
        }
    }

    public static class Ret extends Instr 
    {
        public Ret() 
        {
            super("ret", new Vector<>(), new Vector<>());
        }

        @Override
        public Ret copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Ret();
        }


        @Override
        public String toString() 
        {
            return mnemonic;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic;
        }
    } 

    public static abstract class FunInstr extends Instr
    {
        public final int numOfArgs;

        public FunInstr(String mnemonic, Vector<MEM.Temp> defs, Vector<MEM.Temp> uses, int numOfArgs) 
        {
            super(mnemonic, defs, uses);
            this.numOfArgs = numOfArgs;
        }
    }

    public static class Call extends FunInstr 
    {
        public final MEM.Label functionLabel;

        public Call(MEM.Label functionLabel, int numOfArgs) 
        {
            super("call", new Vector<>(), new Vector<>(), numOfArgs);
            this.functionLabel  = functionLabel;
        }

        @Override
        public Call copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Call(functionLabel, numOfArgs);
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + functionLabel.name;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " + functionLabel.name;
        }
    }


    public static class Jalr extends FunInstr 
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs1;
        public final Long offset;

        public Jalr(MEM.Temp rd, MEM.Temp rs1, Long offset, int numOfArgs) 
        {
            super("jalr", new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs1)), numOfArgs);
            this.rd         = rd;
            this.rs1        = rs1;
            this.offset     = offset;
        }

        @Override
        public Jalr copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Jalr(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1), offset, numOfArgs);
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs1 + "," + offset;
        }

        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rd, rd.toString()) + "," + 
                regMap.getOrDefault(rs1, rs1.toString()) + "," +
                offset;
        }
    }


    public static class Beqz extends JumpInstr
    {
        public final MEM.Temp rs1;

        public Beqz(MEM.Temp rs1, MEM.Label trueLabel, MEM.Label falseLabel) 
        {
            super("beqz", new Vector<>(), new Vector<>(Arrays.asList(rs1)), trueLabel, falseLabel);
            this.rs1 = rs1;

        }  
        
        @Override
        public Beqz copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Beqz(tempMap.getOrDefault(rs1, rs1), trueLabel, falseLabel);
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rs1 + "," + trueLabel.name;
        }   
        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rs1, rs1.toString()) + "," + trueLabel.name;
        }
    }

    public static class Bnez extends JumpInstr
    {
        public final MEM.Temp rs1;

        public Bnez(MEM.Temp rs1, MEM.Label trueLabel, MEM.Label falseLabel) 
        {
            super("bnez", new Vector<>(), new Vector<>(Arrays.asList(rs1)), trueLabel, falseLabel);
            this.rs1 = rs1;

        }   

        @Override
        public Bnez copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Bnez(tempMap.getOrDefault(rs1, rs1), trueLabel, falseLabel);
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rs1 + "," + trueLabel.name;
        }   
        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rs1, rs1.toString()) + "," + trueLabel.name;
        }
    }

    public static class Seqz extends Instr
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs1;

        public Seqz(MEM.Temp rd, MEM.Temp rs1) 
        {
            super("seqz", new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs1)));
            this.rd = rd;
            this.rs1 = rs1;

        }   

        @Override
        public Seqz copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Seqz(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1));
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs1;
        }   
        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rd, rd.toString()) + "," + 
                regMap.getOrDefault(rs1, rs1.toString());
        }
    }

    public static class Snez extends Instr
    {
        public final MEM.Temp rd;
        public final MEM.Temp rs1;

        public Snez(MEM.Temp rd, MEM.Temp rs1) 
        {
            super("snez", new Vector<>(Arrays.asList(rd)), new Vector<>(Arrays.asList(rs1)));
            this.rd = rd;
            this.rs1 = rs1;

        }   

        @Override
        public Snez copyWithTempMap(Map<MEM.Temp, MEM.Temp> tempMap) 
        {
            return new Snez(tempMap.getOrDefault(rd, rd), tempMap.getOrDefault(rs1, rs1));
        }

        @Override
        public String toString() 
        {
            return mnemonic + " " + rd + "," + rs1;
        }   
        @Override
        public String toString(Map<MEM.Temp, String> regMap) 
        {
            return mnemonic + " " +
                regMap.getOrDefault(rd, rd.toString()) + "," + 
                regMap.getOrDefault(rs1, rs1.toString());
        }
    }
}
