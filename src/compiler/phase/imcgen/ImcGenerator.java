package compiler.phase.imcgen;

import java.util.*;
import java.util.function.Predicate;

import compiler.common.report.Report;
import compiler.phase.abstr.*;
import compiler.phase.memory.*;
import compiler.phase.seman.*;

/**
 * Intermediate code generator.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class ImcGenerator implements AST.FullVisitor<Long, Object> 
{
    private MEM.Temp fp;
    private MEM.Temp rv;
    private MEM.Label exitLabel;
    private long currentDepth;

    	/** Tester for nodes that represent name usage. */
	private static final Predicate<AST.Node> validForAddr = (AST.Node node) -> (node instanceof AST.Expr);

    /** Mapping of expressions to addresses. */
    private final SemAn.Attribute<IMC.Expr> addr = new SemAn.Attribute<IMC.Expr>(validForAddr);

    public ImcGenerator()
    {
        fp = null;
        rv = null;
        exitLabel = null;
        currentDepth = 0;
    };

    @Override
	public Long visit(AST.DefFunDefn defFunDefn, Object arg) 
    {
        MEM.Frame funFrame = Memory.frames.get(defFunDefn);

        MEM.Temp savedFp = fp;
        MEM.Temp savedRv = rv;
        MEM.Label savedExitLabel = exitLabel;
        long savedCurrentDepth = currentDepth;

        ImcGen.entryLabel.put(defFunDefn, new MEM.Label());
        ImcGen.exitLabel.put(defFunDefn, new MEM.Label());

        fp = funFrame.FP;
        rv = funFrame.RV;
        currentDepth = funFrame.depth;
        exitLabel = ImcGen.exitLabel.get(defFunDefn);

		defFunDefn.pars.accept(this, arg);
		defFunDefn.type.accept(this, arg);
		defFunDefn.stmts.accept(this, arg);

        fp = savedFp;
        rv = savedRv;
        currentDepth = savedCurrentDepth;
        exitLabel = savedExitLabel;

		return null;
	}

    // ----- Addresses -----

    // ----- Expressions -----

    // SEM:1, SEM:6, SEM:7, SEM:8, SEM:9, SEM:10
    @Override
	public Long visit(AST.AtomExpr atomExpr, Object arg) 
    {
        switch (atomExpr.type) 
        {
            // SEM:6
            case PTR:
                ImcGen.expr.put(atomExpr, new IMC.CONST(0));
                break;

            // SEM:7, SEM:8
            case BOOL:
                if (atomExpr.value.equals("true"))
                    ImcGen.expr.put(atomExpr, new IMC.CONST(1));
                else if (atomExpr.value.equals("false"))
                    ImcGen.expr.put(atomExpr, new IMC.CONST(0));
                else
                    throw new Report.Error(atomExpr, "Invalid boolean value: " + atomExpr.value);
                break;
            
            // SEM:9
            case CHAR:
                long charCode;
                if (atomExpr.value.startsWith("'\\0x")) 
                {
                    String hex = atomExpr.value.substring(4, atomExpr.value.length() - 1);
                    charCode = Long.parseLong(hex, 16);
                }
                else if (atomExpr.value.startsWith("'\\"))
                    charCode = atomExpr.value.charAt(2);
                else 
                    charCode = atomExpr.value.charAt(1);
                
                ImcGen.expr.put(atomExpr, new IMC.CONST(charCode));
                break;
            
            // SEM:10
            case INT:
                ImcGen.expr.put(atomExpr, new IMC.CONST(Long.parseLong(atomExpr.value)));
                break;

            //  SEM:1
            case STR:
                MEM.AbsAccess access = Memory.strings.get(atomExpr);
                IMC.NAME name = new IMC.NAME(access.label);
                addr.put(atomExpr, name);
                ImcGen.expr.put(atomExpr, name);

                break;
        
            default:
                break;
        }

		return null;
	}

    // SEM:11, SEM:13
    @Override
	public Long visit(AST.PfxExpr pfxExpr, Object arg) 
    {
		pfxExpr.subExpr.accept(this, arg);

        IMC.Expr expr = ImcGen.expr.get(pfxExpr.subExpr);

        switch (pfxExpr.oper) 
        {
            // SEM:11
            case ADD:
                ImcGen.expr.put(pfxExpr, expr);
                break;

            // SEM:11
            case SUB:
                ImcGen.expr.put(pfxExpr, new IMC.UNOP(IMC.UNOP.Oper.NEG, expr));
                break;

            // SEM:11
            case NOT:
                ImcGen.expr.put(pfxExpr, new IMC.UNOP(IMC.UNOP.Oper.NOT, expr));
                break;

            // SEM:13
            case PTR:
                IMC.Expr addrExpr = addr.get(pfxExpr.subExpr);
                if (addrExpr == null)
                    ImcGen.expr.put(pfxExpr, ImcGen.expr.get(pfxExpr.subExpr));
                else 
                    ImcGen.expr.put(pfxExpr, addrExpr);
                break;
        
            default:
                break;
        }

		return null;
	}

    // SEM:12
    @Override
	public Long visit(AST.BinExpr binExpr, Object arg) 
    {
		binExpr.fstExpr.accept(this, arg);
		binExpr.sndExpr.accept(this, arg);

        IMC.Expr fstExpr = ImcGen.expr.get(binExpr.fstExpr);
        IMC.Expr sndExpr = ImcGen.expr.get(binExpr.sndExpr);

        switch (binExpr.oper) 
        {
            case OR:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.OR, fstExpr, sndExpr));
                break;
        
            case AND:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.AND, fstExpr, sndExpr));
                break;
        
            case EQU:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.EQU, fstExpr, sndExpr));
                break;
        
            case NEQ:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.NEQ, fstExpr, sndExpr));
                break;
        
            case LTH:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.LTH, fstExpr, sndExpr));
                break;
        
            case GTH:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.GTH, fstExpr, sndExpr));
                break;
        
            case LEQ:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.LEQ, fstExpr, sndExpr));
                break;
        
            case GEQ:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.GEQ, fstExpr, sndExpr));
                break;

            case MUL:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.MUL, fstExpr, sndExpr));
                break;
        
            case DIV:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.DIV, fstExpr, sndExpr));
                break;
        
            case MOD:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.MOD, fstExpr, sndExpr));
                break;
        
            case ADD:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.ADD, fstExpr, sndExpr));
                break;
        
            case SUB:
                ImcGen.expr.put(binExpr, new IMC.BINOP(IMC.BINOP.Oper.SUB, fstExpr, sndExpr));
                break;
        
            default:
                break;
        }

		return null;
	}

    // SEM:5, SEM:14
    @Override
	public Long visit(AST.SfxExpr sfxExpr, Object arg) 
    {
		sfxExpr.subExpr.accept(this, arg);

        IMC.Expr expr = ImcGen.expr.get(sfxExpr.subExpr);

        switch (sfxExpr.oper) 
        {
            case PTR:
                TYP.Type type = SemAn.ofType.get(sfxExpr);
                Long size = type.accept(new SizeEvaluator(), null);

                // SEM:5
                addr.put(sfxExpr, expr);

                // SEM:14
                ImcGen.expr.put(sfxExpr, size == 1 ? new IMC.MEM1(expr) : new IMC.MEM8(expr));

                break;
        
            default:
                break;
        }

		return null;
	}

    // SEM:2, SEM:15
    @Override
	public Long visit(AST.NameExpr nameExpr, Object arg) 
    {
        AST.Defn defn = SemAn.defAt.get(nameExpr);
        if (defn == null) 
            throw new Report.Error(nameExpr, "Undefined identifier: " + nameExpr.name);

        MEM.Access access = null;
        if ((defn instanceof AST.VarDefn) || (defn instanceof AST.ParDefn) || (defn instanceof AST.CompDefn))
        {
            access = Memory.accesses.get(defn);

            if (access == null) 
                throw new Report.Error(nameExpr, "Memory access not defined for: " + nameExpr.name);

            if (access instanceof MEM.AbsAccess)
            {
                // Globals
                MEM.AbsAccess absAccess = (MEM.AbsAccess)access;

                IMC.NAME name = new IMC.NAME(absAccess.label);
                IMC.Expr memExpr = absAccess.size >= 8l ? new IMC.MEM8(name) : new IMC.MEM1(name);

                addr.put(nameExpr, name);
                ImcGen.expr.put(nameExpr, memExpr);
            }
            else 
            {
                if (fp == null)
                    throw new Report.Error(nameExpr, "Frame pointer is not set");

                MEM.RelAccess relAccess = (MEM.RelAccess)access;

                long depthDiff = currentDepth - relAccess.depth;

                IMC.Expr fpExpr = new IMC.TEMP(fp);
                for (int i = 0; i <= depthDiff; ++i)
                    fpExpr = new IMC.MEM8(fpExpr);

                IMC.Expr address = new IMC.BINOP(
                    IMC.BINOP.Oper.ADD,
                    fpExpr,
                    new IMC.CONST(relAccess.offset)
                );

                IMC.Expr memExpr = relAccess.size >= 8l ? new IMC.MEM8(address) : new IMC.MEM1(address);

                addr.put(nameExpr, address);
                ImcGen.expr.put(nameExpr, memExpr);
            }
        }
        else if (defn instanceof AST.DefFunDefn)
        {
            MEM.Frame frame = Memory.frames.get(defn);
            if (frame == null) 
                throw new Report.Error(nameExpr, "Frame not defined for: " + nameExpr.name);
 
            IMC.NAME name = new IMC.NAME(frame.label);

            addr.put(nameExpr, name);
            ImcGen.expr.put(nameExpr, name);
        }
        else if (defn instanceof AST.ExtFunDefn)
        {
            IMC.NAME name = new IMC.NAME(new MEM.Label(defn.name));

            addr.put(nameExpr, name);
            ImcGen.expr.put(nameExpr, name);
        }     
        else 
            throw new Report.Error(nameExpr, "Unsupported access type");

        return null;
	}

    // SEM:?
	@Override
	public Long visit(AST.SizeExpr sizeExpr, Object arg) 
    {
		sizeExpr.type.accept(this, arg);
        
        TYP.Type type = SemAn.isType.get(sizeExpr.type);
        long size = type.accept(new SizeEvaluator(), null);

        ImcGen.expr.put(sizeExpr, new IMC.CONST(size));

		return null;
	}

    // SEM:16 NOT APPLICABLE

    // SEM:3, SEM:17
    @Override
    public Long visit(AST.ArrExpr arrExpr, Object arg) 
    {
        arrExpr.arrExpr.accept(this, arg);
        arrExpr.idx.accept(this, arg);

        TYP.Type arrExprType = SemAn.ofType.get(arrExpr.arrExpr);
        if (!(arrExprType instanceof TYP.ArrType))
            throw new Report.Error(arrExpr, "Expression is not an array");

        TYP.ArrType arrType = (TYP.ArrType)arrExprType;
        long elemSize = arrType.elemType.accept(new SizeEvaluator(), null);

        IMC.Expr arrExprAddr = addr.get(arrExpr.arrExpr);
        IMC.Expr idxExpr = ImcGen.expr.get(arrExpr.idx);

        IMC.Expr arrOffsetExpr = new IMC.BINOP(
            IMC.BINOP.Oper.MUL, 
            idxExpr, 
            new IMC.CONST(elemSize)
        );

        IMC.Expr arrAddrExpr = new IMC.BINOP(
            IMC.BINOP.Oper.ADD, 
            arrExprAddr,
            arrOffsetExpr
        );

        addr.put(arrExpr, arrAddrExpr);
        ImcGen.expr.put(arrExpr, new IMC.MEM8(arrAddrExpr));

        return null;
    }

    // SEM:4, SEM:18
    @Override
	public Long visit(AST.CompExpr compExpr, Object arg) 
    {
		compExpr.recExpr.accept(this, arg);

        IMC.Expr baseAddress = addr.get(compExpr.recExpr);
        if (baseAddress == null) 
            throw new Report.Error(compExpr, "Base address of the record is null.");

        AST.Defn compDefn = SemAn.mapPartDefn.get(compExpr);
        if (compDefn == null)
            throw new Report.Error(compExpr, "No definition to match this.");

        MEM.Access compAccess = Memory.accesses.get((AST.CompDefn)compDefn);
        if (compAccess == null)
            throw new Report.Error(compExpr, "No memory access for this component.");
        else if (!(compAccess instanceof MEM.RelAccess))
            throw new Report.Error(compExpr, "Memory access is not relative.");

        MEM.RelAccess compRelAccess = (MEM.RelAccess)compAccess;

        IMC.Expr compAddrExpr = new IMC.BINOP(
            IMC.BINOP.Oper.ADD,
            baseAddress,
            new IMC.CONST(compRelAccess.offset) 
        );
     
        addr.put(compExpr, compAddrExpr);
        ImcGen.expr.put(compExpr, new IMC.MEM8(compAddrExpr));

        return null;
	}

    // SEM:19
    @Override
	public Long visit(AST.CallExpr callExpr, Object arg) 
    {
		callExpr.funExpr.accept(this, arg);

        Vector<IMC.Expr> args = new Vector<IMC.Expr>(callExpr.argExprs.size());
        Vector<Long> offs = new Vector<Long>(callExpr.argExprs.size());

        long offset = 0l;

        args.add(new IMC.TEMP(fp));
        offs.add(offset);
        

        for (AST.Expr argExpr : callExpr.argExprs)
        {
            // Long accessOffset = argExpr.accept(this, arg);
            argExpr.accept(this, arg);

            // if (accessOffset == null)
            //     accessOffset = 0l;

            IMC.Expr expr = ImcGen.expr.get(argExpr);
            if (expr == null)
                throw new Report.Error(callExpr, "Argument expression is null");

            args.add(expr);

            // TODO Offsets
            offset += 8l;
            offs.add(offset);
        }

        ImcGen.expr.put(callExpr, new IMC.CALL(ImcGen.expr.get(callExpr.funExpr), offs, args));

		return null;
	}

    // SEM:20, SEM:21, SEM:22
    @Override
	public Long visit(AST.CastExpr castExpr, Object arg) 
    {
		castExpr.type.accept(this, arg);
		castExpr.expr.accept(this, arg);

        IMC.Expr expr = ImcGen.expr.get(castExpr.expr);
        TYP.Type type = SemAn.isType.get(castExpr.type);

        if (type instanceof TYP.BoolType)
        {
            // SEM::21
            ImcGen.expr.put(castExpr, new IMC.BINOP(IMC.BINOP.Oper.MOD, expr, new IMC.CONST(2)));
        }
        else if (type instanceof TYP.CharType)
        {
            // SEM:22
            ImcGen.expr.put(castExpr, new IMC.BINOP(IMC.BINOP.Oper.MOD, expr, new IMC.CONST(256)));
        }
        else 
        {
            // SEM:20
            ImcGen.expr.put(castExpr, expr);
        }

		return null;
	}

    // ----- Statements -----

    // SEM:23
    @Override
	public Long visit(AST.ExprStmt callStmt, Object arg) 
    {
		callStmt.expr.accept(this, arg);
        
        IMC.Expr expr = ImcGen.expr.get(callStmt.expr);
        ImcGen.stmt.put(callStmt, new IMC.ESTMT(expr));

		return null;
	}

    // SEM:24
    @Override
	public Long visit(AST.AssignStmt assignStmt, Object arg) 
    {
		assignStmt.dstExpr.accept(this, arg);
		assignStmt.srcExpr.accept(this, arg);

        IMC.Expr dstAddrExpr = new IMC.MEM8(addr.get(assignStmt.dstExpr));
        IMC.Expr srcExpr = ImcGen.expr.get(assignStmt.srcExpr);
      

        ImcGen.stmt.put(assignStmt, new IMC.MOVE(dstAddrExpr, srcExpr));

		return null;
	}

    // SEM:25, SEM:26
    @Override
	public Long visit(AST.IfThenStmt ifThenStmt, Object arg) 
    {
		ifThenStmt.condExpr.accept(this, true);

        IMC.Expr condExpr = ImcGen.expr.get(ifThenStmt.condExpr);

        if (condExpr == null)
            throw new Report.Error(ifThenStmt, "Condition expression is null");

        MEM.Label thenLabel = new MEM.Label();   
        MEM.Label falseLabel = new MEM.Label();
        MEM.Label endLabel = new MEM.Label();


        Vector<IMC.Stmt> stmts = new Vector<>();
        stmts.add(new IMC.CJUMP(condExpr, new IMC.NAME(thenLabel), new IMC.NAME(falseLabel)));

        stmts.add(new IMC.LABEL(falseLabel));
        stmts.add(new IMC.JUMP(new IMC.NAME(endLabel)));

        stmts.add(new IMC.LABEL(thenLabel));
        Vector<IMC.Stmt> thenStmts = new Vector<>();
        for (AST.Stmt stmt : ifThenStmt.thenStmt)
        {
            stmt.accept(this, arg);
            IMC.Stmt imcStmt = ImcGen.stmt.get(stmt);
            if (imcStmt == null)
                throw new Report.Error(ifThenStmt, "Statement is null");
            
            thenStmts.add(imcStmt);
        }
        stmts.add(new IMC.STMTS(thenStmts));
        stmts.add(new IMC.JUMP(new IMC.NAME(endLabel)));

        stmts.add(new IMC.LABEL(endLabel));

        ImcGen.stmt.put(ifThenStmt, new IMC.STMTS(stmts));

		return null;
	}

    // SEM:27, SEM:28
    @Override
	public Long visit(AST.IfThenElseStmt ifThenElseStmt, Object arg) 
    {
		ifThenElseStmt.condExpr.accept(this, true);

        IMC.Expr condExpr = ImcGen.expr.get(ifThenElseStmt.condExpr);

        if (condExpr == null)
            throw new Report.Error(ifThenElseStmt, "Condition expression is null");

        MEM.Label thenLabel = new MEM.Label();
        MEM.Label elseLabel = new MEM.Label();
        MEM.Label endLabel = new MEM.Label();
        
        Vector<IMC.Stmt> stmts = new Vector<>();
        stmts.add(new IMC.CJUMP(condExpr, new IMC.NAME(thenLabel), new IMC.NAME(elseLabel)));
        
        stmts.add(new IMC.LABEL(elseLabel));
        Vector<IMC.Stmt> elseStmts = new Vector<>();
        for (AST.Stmt stmt : ifThenElseStmt.elseStmt)
        {
            stmt.accept(this, arg);
            IMC.Stmt imcStmt = ImcGen.stmt.get(stmt);
            if (imcStmt == null)
                throw new Report.Error(ifThenElseStmt, "Statement is null");
            
            elseStmts.add(imcStmt);
        }
        stmts.add(new IMC.STMTS(elseStmts));
        stmts.add(new IMC.JUMP(new IMC.NAME(endLabel)));

        stmts.add(new IMC.LABEL(thenLabel));
        Vector<IMC.Stmt> thenStmts = new Vector<>();
        for (AST.Stmt stmt : ifThenElseStmt.thenStmt)
        {
            stmt.accept(this, arg);
            IMC.Stmt imcStmt = ImcGen.stmt.get(stmt);
            if (imcStmt == null)
                throw new Report.Error(ifThenElseStmt, "Statement is null");
            
            thenStmts.add(imcStmt);
        }
        stmts.add(new IMC.STMTS(thenStmts));
        // stmts.add(new IMC.JUMP(new IMC.NAME(endLabel)));
        
        stmts.add(new IMC.LABEL(endLabel));
        
        ImcGen.stmt.put(ifThenElseStmt, new IMC.STMTS(stmts));

		return null;
	}

    // SEM:29, SEM:30
    @Override
	public Long visit(AST.WhileStmt whileStmt, Object arg) 
    {
		whileStmt.condExpr.accept(this, true);

        IMC.Expr condExpr = ImcGen.expr.get(whileStmt.condExpr);

        if (condExpr == null)
            throw new Report.Error(whileStmt, "Condition expression is null");

        MEM.Label startLabel = new MEM.Label();
        MEM.Label stmtsLabel = new MEM.Label();
        MEM.Label falseLabel = new MEM.Label();
        MEM.Label endLabel = new MEM.Label();

        Vector<IMC.Stmt> stmts = new Vector<>();
        stmts.add(new IMC.LABEL(startLabel));
        stmts.add(new IMC.CJUMP(condExpr, new IMC.NAME(stmtsLabel), new IMC.NAME(falseLabel)));
        
        stmts.add(new IMC.LABEL(falseLabel));
        stmts.add(new IMC.JUMP(new IMC.NAME(endLabel)));

        stmts.add(new IMC.LABEL(stmtsLabel));
        Vector<IMC.Stmt> whileStmts = new Vector<>();
        for (AST.Stmt stmt : whileStmt.stmts)
        {
            stmt.accept(this, arg);
            IMC.Stmt imcStmt = ImcGen.stmt.get(stmt);
            if (imcStmt == null)
                throw new Report.Error(whileStmt, "Statement is null");
            
            whileStmts.add(imcStmt);
        }
        stmts.add(new IMC.STMTS(whileStmts));
        stmts.add(new IMC.JUMP(new IMC.NAME(startLabel)));

        stmts.add(new IMC.LABEL(endLabel));

        ImcGen.stmt.put(whileStmt, new IMC.STMTS(stmts));

		return null;
	}

    // SEM:31?
    @Override
	public Long visit(AST.LetStmt letStmt, Object arg) 
    {
        letStmt.defns.accept(this, arg);
        Vector<IMC.Stmt> stmts = new Vector<>();

        for (AST.Stmt stmt : letStmt.stmts)
        {
            stmt.accept(this, arg);

            IMC.Stmt imcStmt = ImcGen.stmt.get(stmt);
            if (imcStmt == null)
                throw new Report.Error(letStmt, "Statement is null");
            
            stmts.add(imcStmt);
        }

        ImcGen.stmt.put(letStmt, new IMC.STMTS(stmts));

		return null;
	}
    
    // SEM:32
	@Override
	public Long visit(AST.ReturnStmt returnStmt, Object arg) 
    {
		returnStmt.retExpr.accept(this, arg);

        IMC.Expr expr = ImcGen.expr.get(returnStmt.retExpr);
        if (expr == null)
            throw new Report.Error(returnStmt, "Return expression is null");

        Vector<IMC.Stmt> stmts = new Vector<>();

        stmts.add(new IMC.MOVE(new IMC.TEMP(rv), expr));
        stmts.add(new IMC.JUMP(new IMC.NAME(exitLabel)));

        ImcGen.stmt.put(returnStmt, new IMC.STMTS(stmts));

		return null;
	}
}

