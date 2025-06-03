package compiler.phase.asmgen;

import java.util.*;

import compiler.common.report.*;
import compiler.phase.imcgen.*;
import compiler.phase.imclin.*;
import compiler.phase.memory.*;

public class AsmGenerator
{
    MEM.Temp fp;
    MEM.Temp rv;
    MEM.Label body;
    MEM.Label epilogue;

    public Vector<ASM.Instr> instructions = new Vector<ASM.Instr>();

    // Eh
	private class MaxMunch implements IMC.FullVisitor<MEM.Temp, MEM.Temp>
    {
        private static Vector<Integer> unhandledCondInstrsIndices = new Vector<Integer>();

        private ASM.JumpInstr invertCond(final ASM.TypeJ jumpInstr)
        {
            if (jumpInstr instanceof ASM.Beq) 
            {
                return new ASM.Bne(jumpInstr.rs1, jumpInstr.rs2, jumpInstr.trueLabel, jumpInstr.falseLabel);
            }
            else if (jumpInstr instanceof ASM.Bne) 
            {
                return new ASM.Beq(jumpInstr.rs1, jumpInstr.rs2, jumpInstr.trueLabel, jumpInstr.falseLabel);
            }
            else if (jumpInstr instanceof ASM.Blt) 
            {
                return new ASM.Bge(jumpInstr.rs1, jumpInstr.rs2, jumpInstr.trueLabel, jumpInstr.falseLabel);
            }
            else if (jumpInstr instanceof ASM.Bge) 
            {
                return new ASM.Blt(jumpInstr.rs1, jumpInstr.rs2, jumpInstr.trueLabel, jumpInstr.falseLabel);
            }
            else
            {
                throw new Report.Error("Wrong cond expression to invert");
            }
        }

        private void invertUnhandled(final IMC.BINOP.Oper oper)
        {
            if (unhandledCondInstrsIndices.size() < 2)
                throw new Report.Error("Not enough conditional instructions to invert.");
            
            int fstJumpInstrIndex = unhandledCondInstrsIndices.get(unhandledCondInstrsIndices.size() - 2);
            int sndJumpInstrIndex = unhandledCondInstrsIndices.get(unhandledCondInstrsIndices.size() - 1);

            ASM.JumpInstr fstJumpInstr = (ASM.JumpInstr)instructions.get(fstJumpInstrIndex);
            ASM.JumpInstr sndJumpInstr = (ASM.JumpInstr)instructions.get(sndJumpInstrIndex);

            if (!(sndJumpInstr instanceof ASM.TypeJ)) 
                throw new Report.Error("Second conditional instruction must be of type ASM.TypeJ to invert.");
            sndJumpInstr = invertCond((ASM.TypeJ)sndJumpInstr);

            if (oper == IMC.BINOP.Oper.AND)
            {
                if (!(fstJumpInstr instanceof ASM.TypeJ)) 
                    throw new Report.Error("First conditional instruction must be of type ASM.TypeJ to invert.");
                fstJumpInstr = invertCond((ASM.TypeJ)fstJumpInstr);
            }

            instructions.set(fstJumpInstrIndex, fstJumpInstr);
            instructions.set(sndJumpInstrIndex, sndJumpInstr);

            // if (invertedInstrs.contains(fstJumpInstr))
            //     invertedInstrs.remove(fstJumpInstr);
            // else 
            //     invertedInstrs.add(fstJumpInstr);

            // if (invertedInstrs.contains(sndJumpInstr))
            //     invertedInstrs.remove(sndJumpInstr);
            // else 
            //     invertedInstrs.add(sndJumpInstr);

        }

        @Override
        public MEM.Temp visit(IMC.BINOP binOp, MEM.Temp arg) 
		{
            MEM.Temp rs1 = binOp.fstExpr.accept(this, null);
            MEM.Temp rs2 = binOp.sndExpr.accept(this, null);

            if (arg == null)
            {
                throw new Report.Error("Undefined destination register.");
            }

            switch (binOp.oper) 
            {
                case OR:
                    // invertUnhandled(binOp.oper);
                    instructions.add(new ASM.Or(arg, rs1, rs2));
                    break;
            
                case AND:
                    // invertUnhandled(binOp.oper);
                    instructions.add(new ASM.And(arg, rs1, rs2));
                    break;
            
                case EQU:
                    // ASM.Beq beq = new ASM.Beq(rs1, rs2, null, null);
                    // unhandledCondInstrsIndices.add(instructions.size());
                    // instructions.add(beq);
                    instructions.add(new ASM.Xor(arg, rs1, rs2));
                    instructions.add(new ASM.Seqz(arg, arg));

                    break;
            
                case NEQ:
                    // ASM.Bne bne = new ASM.Bne(rs1, rs2, null, null);
                    // unhandledCondInstrsIndices.add(instructions.size());
                    // instructions.add(bne);
                    instructions.add(new ASM.Xor(arg, rs1, rs2));
                    instructions.add(new ASM.Snez(arg, arg));
                    break;
            
                case LTH:
                    // ASM.Blt blt = new ASM.Blt(rs1, rs2, null, null);
                    // unhandledCondInstrsIndices.add(instructions.size());
                    // instructions.add(blt);
                    instructions.add(new ASM.Slt(arg, rs1, rs2));
                    break;
            
                case GTH:
                    // ASM.Blt bgt = new ASM.Blt(rs2, rs1, null, null);
                    // unhandledCondInstrsIndices.add(instructions.size());
                    // instructions.add(bgt);
                    instructions.add(new ASM.Slt(arg, rs2, rs1));
                    break;
            
                case LEQ:
                    // ASM.Bge ble = new ASM.Bge(rs2, rs1, null, null);
                    // unhandledCondInstrsIndices.add(instructions.size());
                    // instructions.add(ble);
                    instructions.add(new ASM.Slt(arg, rs2, rs1));
                    instructions.add(new ASM.Seqz(arg, arg));

                    break;
            
                case GEQ:
                    // ASM.Bge bge = new ASM.Bge(rs1, rs2, null, null);
                    // unhandledCondInstrsIndices.add(instructions.size());
                    // instructions.add(bge);
                    instructions.add(new ASM.Slt(arg, rs1, rs2));
                    instructions.add(new ASM.Seqz(arg, arg));

                    break;

                case MUL:
                    instructions.add(new ASM.Mul(arg, rs1, rs2));
                    break;
            
                case DIV:
                    instructions.add(new ASM.Div(arg, rs1, rs2));
                    break;
            
                case MOD:
                    instructions.add(new ASM.Rem(arg, rs1, rs2));
                    break;
            
                case ADD:
                    instructions.add(new ASM.Add(arg, rs1, rs2));
                    break;
            
                case SUB:
                    instructions.add(new ASM.Sub(arg, rs1, rs2));
                    break;
            
                default:
                    break;
            }

			return null;
		}

        @Override
		public MEM.Temp visit(IMC.CALL call, MEM.Temp arg) 
		{
            //TODO
            long offset = 0;
            final long argSize = 8;

            // for (int i = call.args.size() - 1; i >= 0; i--) {
            for (int i = 0; i < call.args.size(); i++)
            {
                IMC.Expr expr = call.args.get(i);
                MEM.Temp argTemp;

                if (expr instanceof IMC.NAME)
                {
                    argTemp = new MEM.Temp();
                    instructions.add(new ASM.La(argTemp, ((IMC.NAME)expr).label));
                }
                else 
                {
                    argTemp = expr.accept(this, null);
                }

                instructions.add(new ASM.Sd(argTemp, ASM.sp, offset));
                offset += argSize;
            }

            if (call.addr instanceof IMC.NAME)
            {
                instructions.add(new ASM.Call(((IMC.NAME)call.addr).label, call.args.size()));
            }
            else if (call.addr instanceof IMC.TEMP)
            {
                MEM.Temp funTemp = ((IMC.TEMP)call.addr).temp;
                instructions.add(new ASM.Jalr(ASM.ra, funTemp, 0l, call.args.size()));
            }
            else 
            {
                throw new Report.Error("Unsupported call address type: " + call.addr.getClass());
            }

            instructions.add(new ASM.Mv(arg, ASM.a0));

			return null;
		}
        
        @Override
		public MEM.Temp visit(IMC.CJUMP cjump, MEM.Temp arg) 
		{
            if (!(cjump.posAddr instanceof IMC.NAME) || !(cjump.negAddr instanceof IMC.NAME))
                throw new Report.Error("Invalid jump address type.");
        
            MEM.Label trueLabel     = ((IMC.NAME)cjump.posAddr).label;
            MEM.Label falseLabel    = ((IMC.NAME)cjump.negAddr).label;

            // if (unhandledCondInstrsIndices.isEmpty())
            // {
            //     instructions.add(new ASM.Bnez(((IMC.TEMP)cjump.cond).temp, trueLabel, falseLabel));
            // }
            // else 
            // {
            //     for (final int jumpInstrIndex : unhandledCondInstrsIndices)
            //     {
            //         if (invertedInstrs.contains((ASM.JumpInstr)instructions.get(jumpInstrIndex)))
            //         {
            //             ((ASM.JumpInstr)instructions.get(jumpInstrIndex)).trueLabel     = falseLabel;
            //             ((ASM.JumpInstr)instructions.get(jumpInstrIndex)).falseLabel    = trueLabel;
            //         }
            //         else 
            //         {
            //             ((ASM.JumpInstr)instructions.get(jumpInstrIndex)).trueLabel     = trueLabel;
            //             ((ASM.JumpInstr)instructions.get(jumpInstrIndex)).falseLabel    = falseLabel;
            //         }
            //     }
    
            //     unhandledCondInstrsIndices.clear();
            // }

            if (!(cjump.cond instanceof IMC.TEMP))
                throw new Report.Error("No condition.");

            instructions.add(new ASM.Bnez(((IMC.TEMP)cjump.cond).temp, trueLabel, falseLabel));

			return null;
		}
        
        @Override
		public MEM.Temp visit(IMC.CONST constant, MEM.Temp arg) 
		{
            MEM.Temp temp = new MEM.Temp();
            instructions.add(new ASM.Li(temp, constant.value));

			return temp;
		}
        
        @Override
		public MEM.Temp visit(IMC.JUMP jump, MEM.Temp arg) 
		{
            MEM.Label label = ((IMC.NAME)jump.addr).label;
            instructions.add(new ASM.J(label));

			return null;
		}
        
        @Override
		public MEM.Temp visit(IMC.LABEL label, MEM.Temp arg) 
		{
            // Skip false label
            // if (instructions.isEmpty() || !(instructions.lastElement() instanceof ASM.JumpInstr))
            instructions.add(new ASM.Label(label.label));

			return null;
		}
        
        @Override
		public MEM.Temp visit(IMC.MEM1 mem, MEM.Temp arg)
		{
			return mem.addr.accept(this, arg);
		}
        
        @Override
		public MEM.Temp visit(IMC.MEM8 mem, MEM.Temp arg) 
		{
			return mem.addr.accept(this, arg);
		}
        
        @Override
		public MEM.Temp visit(IMC.MOVE move, MEM.Temp arg) 
		{
            if (move.dst instanceof IMC.MEM8 || move.dst instanceof IMC.MEM1)
            {           
                // IMC.Expr expr = move.dst instanceof IMC.MEM8 ? ((IMC.MEM8)move.dst).addr : ((IMC.MEM1)move.dst).addr;
                // if (expr instanceof IMC.NAME)
                // {
                //     arg = new MEM.Temp();
                //     instructions.add(new ASM.La(arg, ((IMC.NAME)expr).label));
                // }
                // else if (expr instanceof IMC.TEMP)
                // {
                //     arg = ((IMC.TEMP)expr).temp;
                // }
                // else 
                //     throw new Report.Error("Error");
                
                MEM.Temp dst = move.dst.accept(this, arg);
                MEM.Temp src = move.src.accept(this, dst);
                
                instructions.add(new ASM.Sd(src, dst, 0l));                
            }
            else if (move.src instanceof IMC.MEM8 || move.src instanceof IMC.MEM1)
            {
                // Load -> MOVE(TEMP, MEM8(TEMP))
                // IMC.Expr expr = move.src instanceof IMC.MEM8 ? ((IMC.MEM8)move.src).addr : ((IMC.MEM1)move.src).addr;
                // if (expr instanceof IMC.NAME)
                // {
                //     arg = new MEM.Temp();
                //     instructions.add(new ASM.La(arg, ((IMC.NAME)expr).label));
                // }
                // else if (expr instanceof IMC.TEMP)
                // {
                //     arg = ((IMC.TEMP)expr).temp;
                // }
                // else 
                //     throw new Report.Error("Error");

                MEM.Temp src = move.src.accept(this, arg);
                MEM.Temp dst = move.dst.accept(this, src);

                instructions.add(new ASM.Ld(dst, src, 0l));
            }
            else if (move.dst instanceof IMC.TEMP)
            {
                if (move.src instanceof IMC.TEMP)
                {
                    instructions.add(new ASM.Mv(move.dst.accept(this, null), move.src.accept(this, null)));
                }
                else 
                {
                    arg = move.dst.accept(this, null);
                    MEM.Temp src = move.src.accept(this, arg);

                    if (move.src instanceof IMC.CONST)
                    {
                        instructions.add(new ASM.Mv(arg, src));
                    }
                }
            }
            
			return null;
		}
        
        @Override
		public MEM.Temp visit(IMC.NAME name, MEM.Temp arg) 
		{
            MEM.Temp temp = new MEM.Temp();
            instructions.add(new ASM.La(temp, name.label));
            return temp;
			// return null;
		}
        
        @Override
		public MEM.Temp visit(IMC.TEMP temp, MEM.Temp arg) 
		{
            if (temp.temp == fp)
                return ASM.fp;
            else if (temp.temp == rv)
                return ASM.a0;
			return temp.temp;
		}
        
        @Override
		public MEM.Temp visit(IMC.UNOP unOp, MEM.Temp arg) 
		{
            switch (unOp.oper) {
                case NOT:
                    instructions.add(new ASM.Seqz(arg, unOp.subExpr.accept(this, null)));
                    break;

                case NEG:
                    instructions.add(new ASM.Neg(arg, unOp.subExpr.accept(this, null)));
                    break;
            
                default:
                    break;
            }

			return null;
		}
    }

    private void addPrologue()
    {
        instructions.add(new ASM.Addi(ASM.sp, ASM.sp, -32l));
        instructions.add(new ASM.Sd(ASM.ra, ASM.sp, 24l));
        instructions.add(new ASM.Sd(ASM.fp, ASM.sp, 16l));
        instructions.add(new ASM.Addi(ASM.fp, ASM.sp, 32l));
    }

    private void addEpilogue()
    {
        instructions.add(new ASM.Label(epilogue));
        instructions.add(new ASM.Sd(ASM.ra, ASM.sp, 24l));
        instructions.add(new ASM.Sd(ASM.fp, ASM.sp, 16l));
        instructions.add(new ASM.Addi(ASM.sp, ASM.sp, 32l));
        instructions.add(new ASM.Jr(ASM.ra));
    }

    public Vector<ASM.Instr> generateInstructions(LIN.CodeChunk codeChunk)
    {
        fp = codeChunk.frame.FP;
        rv = codeChunk.frame.RV;
        body = codeChunk.entryLabel;
        epilogue = codeChunk.exitLabel;

        final Vector<IMC.Stmt> stmts = codeChunk.stmts();
        final int stmsSize = stmts.size();

        // addPrologue();

        // Skip last jump to epilogue
        for (int i = 0; i < stmsSize - 1; ++i)
        {
            MaxMunch maxMunch = new MaxMunch();
            stmts.get(i).accept(maxMunch, null);
        }
        instructions.add(new ASM.Label(epilogue));
        // addEpilogue();

        return this.instructions;
    }
}
