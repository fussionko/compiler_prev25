package compiler.phase.imclin;

import java.util.*;

import compiler.phase.abstr.*;
import compiler.phase.imcgen.*;
import compiler.phase.memory.*;


public class ChunkGenerator implements AST.FullVisitor<Object, Object> 
{
    private class LinearVisitor implements IMC.FullVisitor<IMC.TEMP, Object>
    {
        Vector<IMC.Stmt> linearStmts;

        MEM.Temp fp;
        MEM.Temp rv;

        LinearVisitor(MEM.Temp fp, MEM.Temp rv)
        {
            linearStmts = new Vector<IMC.Stmt>();

            this.fp = fp;
            this.rv = rv;
        }

        @Override
        public IMC.TEMP visit(IMC.BINOP binOp, Object arg) 
		{
            IMC.TEMP fstExprTemp = binOp.fstExpr.accept(this, arg);
            IMC.TEMP sndExprTemp = binOp.sndExpr.accept(this, arg);

            // IMC.Expr fstExpr = fstExprTemp == null ? binOp.fstExpr : fstExprTemp;
            // IMC.Expr sndExpr = sndExprTemp == null ? binOp.sndExpr : sndExprTemp;
            
            // IMC.TEMP fstBinOpTemp = null;
            // IMC.TEMP sndBinOpTemp = null;
            
            // if (arg instanceof Boolean && (Boolean)arg == true)
            // {
            //     if (binOp.oper == IMC.BINOP.Oper.AND || binOp.oper == IMC.BINOP.Oper.OR)
            //     {
            //         if (binOp.fstExpr instanceof IMC.MEM1 || 
            //             (binOp.fstExpr instanceof IMC.BINOP && ((IMC.BINOP)binOp.fstExpr).oper == IMC.BINOP.Oper.MOD))
            //         {
            //             fstBinOpTemp = new IMC.TEMP(new MEM.Temp());
            //             linearStmts.add(new IMC.MOVE(
            //                 fstBinOpTemp, 
            //                 new IMC.BINOP(IMC.BINOP.Oper.EQU, fstExpr, new IMC.CONST(1))
            //             ));
            //         }

            //         if (binOp.sndExpr instanceof IMC.MEM1 ||
            //             (binOp.sndExpr instanceof IMC.BINOP && ((IMC.BINOP)binOp.sndExpr).oper == IMC.BINOP.Oper.MOD))
            //         {
            //             sndBinOpTemp = new IMC.TEMP(new MEM.Temp());
            //             linearStmts.add(new IMC.MOVE(
            //                 sndBinOpTemp, 
            //                 new IMC.BINOP(IMC.BINOP.Oper.EQU, sndExpr, new IMC.CONST(1))
            //             ));
            //         }
            //     }
            // }

            IMC.TEMP binOpTemp = new IMC.TEMP(new MEM.Temp());
            linearStmts.add(new IMC.MOVE(binOpTemp, new IMC.BINOP(binOp.oper, 
                    fstExprTemp == null ? binOp.fstExpr : fstExprTemp, 
                    sndExprTemp == null ? binOp.sndExpr : sndExprTemp
                )
            ));

            // linearStmts.add(new IMC.MOVE(binOpTemp, new IMC.BINOP(binOp.oper, 
            //         fstBinOpTemp == null ? fstExpr : fstBinOpTemp, 
            //         sndBinOpTemp == null ? sndExpr : sndBinOpTemp
            //     )
            // ));

            return binOpTemp;
		}

        @Override
		public IMC.TEMP visit(IMC.CALL call, Object arg) 
		{
            Vector<IMC.Expr> args = new Vector<IMC.Expr>(); 
            for (int i = 0; i < call.args.size(); i++)
            {
                IMC.TEMP argTemp = call.args.get(i).accept(this, true);

                if (argTemp == null)
                    args.add(call.args.get(i));
                else
                    args.add(argTemp);
            }
			IMC.TEMP addrTemp = call.addr.accept(this, true);


            IMC.TEMP callTemp = new IMC.TEMP(new MEM.Temp());
            linearStmts.add(new IMC.MOVE(callTemp, 
                new IMC.CALL(addrTemp == null ? call.addr : addrTemp, call.offs, args)
            ));

            return callTemp;
		}

        @Override
		public IMC.TEMP visit(IMC.CJUMP cjump, Object arg) 
		{
			IMC.TEMP condTemp = cjump.cond.accept(this, true);
			IMC.TEMP posAddrStmts = cjump.posAddr.accept(this, arg);
			IMC.TEMP negAddrStmts = cjump.negAddr.accept(this, arg);

            // TODO

            linearStmts.add(new IMC.CJUMP(
                condTemp == null ? cjump.cond : condTemp,
                posAddrStmts == null ? cjump.posAddr : posAddrStmts,
                negAddrStmts == null ? cjump.negAddr : negAddrStmts
            ));

			return null;
		}

        @Override
		public IMC.TEMP visit(IMC.CONST constant, Object arg) 
		{
			return null;
		}

        @Override
        public IMC.TEMP visit(IMC.ESTMT eStmt, Object arg) 
		{
            eStmt.expr.accept(this, arg);
			return null;
		}

        @Override
		public IMC.TEMP visit(IMC.JUMP jump, Object arg) 
		{
            IMC.TEMP jumpTemp = jump.addr.accept(this, arg);

            linearStmts.add(jump);

            return jumpTemp;
		}

        @Override
		public IMC.TEMP visit(IMC.LABEL label, Object arg) 
		{
            linearStmts.add(label);

			return null;
		}

        @Override
		public IMC.TEMP visit(IMC.MEM1 mem, Object arg)
		{
            IMC.TEMP addrTemp = mem.addr.accept(this, arg);

            IMC.TEMP memTemp = new IMC.TEMP(new MEM.Temp());
            linearStmts.add(new IMC.MOVE(memTemp, addrTemp == null ? mem : new IMC.MEM1(addrTemp)));

			return memTemp;
		}

        @Override
		public IMC.TEMP visit(IMC.MEM8 mem, Object arg) 
		{
            IMC.TEMP addrTemp = mem.addr.accept(this, arg);

            IMC.TEMP memTemp = new IMC.TEMP(new MEM.Temp());
            linearStmts.add(new IMC.MOVE(memTemp, addrTemp == null ? mem : new IMC.MEM8(addrTemp)));

			return memTemp;
		}

        @Override
		public IMC.TEMP visit(IMC.MOVE move, Object arg) 
		{
            IMC.Expr dst;

            // Special STORE -> MOVE-MEMx
            if (move.dst instanceof IMC.MEM8)
            {
                IMC.TEMP dstAddrTemp = ((IMC.MEM8)move.dst).addr.accept(this, arg);

                if (dstAddrTemp == null)
                    dst = move.dst;
                else 
                    dst = new IMC.MEM8(dstAddrTemp);
            }
            else if (move.dst instanceof IMC.MEM1)
            {
                IMC.TEMP dstAddrTemp = ((IMC.MEM1)move.dst).addr.accept(this, arg);

                if (dstAddrTemp == null)
                    dst = move.dst;
                else 
                    dst = new IMC.MEM1(dstAddrTemp);
            }
            else 
            {
                IMC.TEMP dstTemp = move.dst.accept(this, arg);
                dst = dstTemp == null ? move.dst : dstTemp;
            }

            IMC.TEMP srcTemp = move.src.accept(this, arg);

            linearStmts.add(new IMC.MOVE(dst, srcTemp == null ? move.src : srcTemp));

			return null;
		}

        @Override
		public IMC.TEMP visit(IMC.NAME name, Object arg) 
		{
			return null;
		}

        // @Override
		// public IMC.TEMP visit(IMC.SEXPR sExpr, Object arg) 
		// {
		// 	IMC.TEMP stmtTemp = sExpr.stmt.accept(this, arg);
		// 	IMC.TEMP exprTemp = sExpr.expr.accept(this, arg);

        //     IMC.TEMP sExprTemp = new IMC.TEMP(new MEM.Temp());
        //     linearStmts.add(new IMC.MOVE(sExprTemp, new IMC.SEXPR(null, exprTemp))); // todo

		// 	return sExprTemp;
		// }

        @Override
		public IMC.TEMP visit(IMC.STMTS stmts, Object arg) 
		{
			for (final IMC.Stmt stmt : stmts.stmts)
            {
                stmt.accept(this, arg);

                if (linearStmts.size() >= 2)
                {
                    if ((linearStmts.lastElement() instanceof IMC.JUMP) && (linearStmts.get(linearStmts.size() -2) instanceof IMC.JUMP))
                        linearStmts.removeLast();
                }
            }

			return null;
		}

        @Override
		public IMC.TEMP visit(IMC.TEMP temp, Object arg) 
		{
            if (temp.temp == fp || temp.temp == rv)
                return temp;

            IMC.TEMP tempTemp = new IMC.TEMP(new MEM.Temp());
            linearStmts.add(new IMC.MOVE(tempTemp, new IMC.TEMP(temp.temp)));

            return tempTemp;
		}

        @Override
		public IMC.TEMP visit(IMC.UNOP unOp, Object arg) 
		{
            IMC.TEMP subExprTemp = unOp.subExpr.accept(this, arg);

            IMC.TEMP unOpTemp = new IMC.TEMP(new MEM.Temp());
            linearStmts.add(new IMC.MOVE(unOpTemp, new IMC.UNOP(unOp.oper, subExprTemp == null ? unOp.subExpr : subExprTemp)));
            
            return unOpTemp;
		}
    }

	@Override
	public Object visit(AST.DefFunDefn defFunDefn, Object arg) 
    {
		defFunDefn.pars.accept(this, arg);
		defFunDefn.type.accept(this, arg);

        MEM.Frame frame = Memory.frames.get(defFunDefn);

        MEM.Label entryLabel = ImcGen.entryLabel.get(defFunDefn);
        MEM.Label exitLabel = ImcGen.exitLabel.get(defFunDefn);

        Vector<IMC.Stmt> stmts = new Vector<IMC.Stmt>();
        for (AST.Stmt stmt : defFunDefn.stmts)
        {
            stmt.accept(this, arg);

            IMC.Stmt stmtImc = ImcGen.stmt.get(stmt);

            if (stmtImc != null)
            {
                LinearVisitor linearVisitor = new LinearVisitor(frame.FP, frame.RV);
                stmtImc.accept(linearVisitor, false);
                stmts.addAll(linearVisitor.linearStmts);
                
                //stmts.add(stmtImc);

            }
        }

        
        if (stmts.firstElement() instanceof IMC.LABEL)
        {
            IMC.LABEL firstLabel = (IMC.LABEL)stmts.firstElement();
            if (firstLabel.label != entryLabel)
                stmts.addFirst(new IMC.JUMP(new IMC.NAME(firstLabel.label)));     
        }
        stmts.addFirst(new IMC.LABEL(entryLabel));

        if (stmts.lastElement() instanceof IMC.JUMP)
        {
            // IMC.JUMP lastJump = (IMC.JUMP)stmts.lastElement();
            // if ((lastJump.addr instanceof IMC.NAME) && ((IMC.NAME)lastJump.addr).label != exitLabel)
            //     throw new Report.Error(defFunDefn, "");    
        }
        else 
        {
            stmts.add(new IMC.JUMP(new IMC.NAME(exitLabel)));
        }
        
        LIN.CodeChunk codeChunk = new LIN.CodeChunk(frame, stmts, entryLabel, exitLabel);

        ImcLin.addCodeChunk(codeChunk);

		return null;
	}

    @Override
    public Object visit(AST.VarDefn varDefn, Object arg) 
    {
        varDefn.type.accept(this, arg);

        MEM.Access access = Memory.accesses.get(varDefn);
        if (access != null)
        {
            if (access instanceof MEM.AbsAccess)
            {
                MEM.AbsAccess absAccess = (MEM.AbsAccess) access;
                LIN.DataChunk dataChunk = new LIN.DataChunk(absAccess);
                ImcLin.addDataChunk(dataChunk);
            }
        }

        return null;
    }

    @Override
    public Object visit(AST.ParDefn parDefn, Object arg) 
    {
        parDefn.type.accept(this, arg);

        MEM.Access access = Memory.accesses.get(parDefn);
        if (access != null)
        {
            if (access instanceof MEM.AbsAccess)
            {
                MEM.AbsAccess absAccess = (MEM.AbsAccess) access;
                LIN.DataChunk dataChunk = new LIN.DataChunk(absAccess);
                ImcLin.addDataChunk(dataChunk);
            }
        }

        return null;
    }

    @Override
    public Object visit(AST.CompDefn compDefn, Object arg) 
    {
        compDefn.type.accept(this, arg);

        MEM.Access access = Memory.accesses.get(compDefn);
        if (access != null)
        {
            if (access instanceof MEM.AbsAccess)
            {
                MEM.AbsAccess absAccess = (MEM.AbsAccess) access;
                LIN.DataChunk dataChunk = new LIN.DataChunk(absAccess);
                ImcLin.addDataChunk(dataChunk);
            }
        }

        return null;
    }

    @Override
	public Object visit(AST.AtomExpr atomExpr, Object arg) 
    {
        if (atomExpr.type != AST.AtomExpr.Type.STR)
            return null;

        MEM.Access access = Memory.strings.get(atomExpr);

        if (access != null)
        {
            if (access instanceof MEM.AbsAccess)
            {
                MEM.AbsAccess absAccess = (MEM.AbsAccess) access;
                LIN.DataChunk dataChunk = new LIN.DataChunk(absAccess, true);
                ImcLin.addDataChunk(dataChunk);
            }
        }

		return null;
	}
}
