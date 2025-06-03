package compiler.phase.memory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import compiler.common.report.Report;
import compiler.phase.abstr.*;
import compiler.phase.seman.*;

/**
 * Computing memory layout: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemEvaluator implements AST.FullVisitor<Object, Object> 
{
	// Holder for frame
	private class Frame
	{
		public long depth;
		public long offset;
		public long locsSize;
		public long argsSize;
		public long frameSize;

		public Frame(long depth, long offset, long locsSize, long argsSize, long frameSize) 
		{
			this.depth 		= depth;
			this.offset 	= offset;
			this.locsSize 	= locsSize;
			this.argsSize 	= argsSize;
			this.frameSize 	= frameSize;
		}

		public Frame saveFrame()
		{
			final Frame frame = new Frame(depth, offset, locsSize, argsSize, frameSize);
			this.reset();
			return frame;
		}

		public void loadFrame(final Frame loadFrom)
		{	
			this.depth 		= loadFrom.depth;
			this.offset 	= loadFrom.offset;
			this.locsSize 	= loadFrom.locsSize;
			this.argsSize 	= loadFrom.argsSize;
			this.frameSize 	= loadFrom.frameSize;
		}

		public void reset()
		{
			this.depth 		= 0;
			this.offset 	= 0;
			this.locsSize 	= 0;
			this.argsSize 	= 0;
			this.frameSize 	= 0;
		}
	}

	// Current frame
	private Frame currentFrame;

	// SizeEvaluator
	private SizeEvaluator sizeEvaluator;

	// Return adddress size 
	private long returnAddressSize;

	// Static link size
	private long staticLinkSize;

	// Align to bytes
	private long alignTo;

	// Constructor
	public MemEvaluator() 
	{	
		currentFrame = new Frame(0, 0, 0, 0, 0);
		sizeEvaluator = new SizeEvaluator();

		returnAddressSize 	= 8l;
		staticLinkSize 		= 8l;
		alignTo 			= 8l;
	};

	// Aligns size to alignTo
	private long align(long size)
	{
		return (size / alignTo) * alignTo + (size % alignTo == 0 ? 0l : alignTo);
	}

	// ----- Definitions -----

    @Override
	public Object visit(AST.DefFunDefn defFunDefn, Object arg) 
    {
		final Frame savedFrame = currentFrame.saveFrame();

		TYP.FunType funType = (TYP.FunType)SemAn.ofType.get(defFunDefn);
		long funSize = funType.accept(sizeEvaluator,null);

		defFunDefn.type.accept(this, null);

		currentFrame.depth = savedFrame.depth + 1;

		// Parameters
		currentFrame.offset = staticLinkSize;
		for (AST.ParDefn parDefn : defFunDefn.pars) 
		{
			parDefn.accept(this, null);

			TYP.Type typePar = SemAn.ofType.get(parDefn);
			typePar.accept(sizeEvaluator, null);
		}

		// currentFrame.offset = -staticLinkSize;
		currentFrame.offset = 0;
		defFunDefn.stmts.accept(this, null);

		long returnValueSize = align(funType.resType.accept(sizeEvaluator, null));
		// if (currentFrame.argsSize == 0)
		// 	currentFrame.argsSize = returnValueSize;

		if (savedFrame.depth > 0)
			currentFrame.argsSize += staticLinkSize;
	
		long frameSize = currentFrame.locsSize + currentFrame.argsSize + returnAddressSize + returnValueSize;	
	
		MEM.Label label = savedFrame.depth == 0 ? new MEM.Label(defFunDefn.name) : new MEM.Label();

        MEM.Frame frame = new MEM.Frame(
			label, 
			savedFrame.depth, 
			currentFrame.locsSize, 
			currentFrame.argsSize,
			frameSize
		);
        Memory.frames.put(defFunDefn, frame);

		currentFrame.loadFrame(savedFrame);

		return null;
	}

	@Override
	public Object visit(AST.ExtFunDefn extFunDefn, Object arg) 
	{
		final Frame savedFrame = currentFrame.saveFrame();

		TYP.FunType funType = (TYP.FunType)SemAn.ofType.get(extFunDefn);
		long funSize = funType.accept(sizeEvaluator,null);

		extFunDefn.type.accept(this, null);

		currentFrame.depth += 1;

		// Parameters
		currentFrame.offset = staticLinkSize;
		for (AST.ParDefn parDefn : extFunDefn.pars) 
		{
			parDefn.accept(this, null);

			TYP.Type typePar = SemAn.ofType.get(parDefn);
			typePar.accept(sizeEvaluator, null);
		}

		currentFrame.loadFrame(savedFrame);

		return null;
	}
	
	@Override
	public Object visit(AST.VarDefn varDefn, Object arg) 
	{ 
		varDefn.type.accept(this, arg);

		TYP.Type type = SemAn.ofType.get(varDefn);
		long size = type.accept(sizeEvaluator,null);

		if (currentFrame.depth == 0)
		{
			MEM.Label label = new MEM.Label(varDefn.name);
			Memory.accesses.put(varDefn, new MEM.AbsAccess(size, label));
		}
		else 
		{
			long sizeAlign = align(size);

			currentFrame.offset -= sizeAlign;
			currentFrame.locsSize += sizeAlign;

            Memory.accesses.put(varDefn, new MEM.RelAccess(size, currentFrame.offset, currentFrame.depth));
		}

		return null;
	}

	@Override
	public Object visit(AST.ParDefn parDefn, Object arg) 
	{
		parDefn.type.accept(this, arg);

		TYP.Type type = SemAn.ofType.get(parDefn);
		long size = type.accept(sizeEvaluator,null);

		Memory.accesses.put(parDefn, new MEM.RelAccess(size, currentFrame.offset, currentFrame.depth));
		currentFrame.offset += align(size);

		return null;
	}

	@Override
	public Object visit(AST.CompDefn compDefn, Object arg) 
	{
		compDefn.type.accept(this, arg);

		TYP.Type type = SemAn.ofType.get(compDefn);
		long size = type.accept(sizeEvaluator,null);

		Memory.accesses.put(compDefn, new MEM.RelAccess(size, currentFrame.offset, -1));

		return null;
	}

	// ----- Types -----

	@Override
	public Object visit(AST.StrType strType, Object arg) 
	{
		long savedOffset = currentFrame.offset;

		currentFrame.offset = 0;
		for (AST.CompDefn comp : strType.comps)
		{
			comp.accept(this, null);

			long size = Memory.accesses.get(comp).size;
			currentFrame.offset += align(size);
		}

		currentFrame.offset = savedOffset;

		return null;
	}

	@Override
	public Object visit(AST.UniType uniType, Object arg) 
	{
		long savedOffset = currentFrame.offset;

		currentFrame.offset = 0;
		uniType.comps.accept(this, arg);

		currentFrame.offset = savedOffset;

		return null;
	}

	// ----- Expressions -----

	@Override
	public Object visit(AST.AtomExpr atomExpr, Object arg) 
	{
		TYP.Type type = SemAn.ofType.get(atomExpr);
		if (type instanceof TYP.PtrType && ((TYP.PtrType)type).baseType == TYP.CharType.type)
		{
			long size = ((TYP.PtrType)type).baseType.accept(sizeEvaluator, null);

			String stringValue = atomExpr.value.substring(1, atomExpr.value.length() - 1);

        	// Replace \0xNN with actual characters
        	Pattern pattern = Pattern.compile("\\\\0x([0-9A-Fa-f]{2})");
        	Matcher matcher = pattern.matcher(stringValue);
        	StringBuffer processed = new StringBuffer();

        	while (matcher.find()) 
			{
        	    int code = Integer.parseInt(matcher.group(1), 16);
        	    matcher.appendReplacement(processed, Matcher.quoteReplacement(Character.toString((char)code)));
        	}
			
        	matcher.appendTail(processed);
			stringValue = processed.toString();

			Memory.strings.put(atomExpr, new MEM.AbsAccess(
				size * (stringValue.length() + 1),
				new MEM.Label(),
				stringValue
			));
		}

		return null;
	}

	@Override
	public Object visit(AST.CallExpr callExpr, Object arg) 
	{
		callExpr.funExpr.accept(this, arg);
		callExpr.argExprs.accept(this, arg);

		long argsSize = 0;

		for (AST.Expr argExpr : callExpr.argExprs)
		{
			argExpr.accept(this, arg);

			TYP.Type argType = SemAn.ofType.get(argExpr);
			long argSize = argType.accept(sizeEvaluator, null);

			argsSize += align(argSize);
		}

		currentFrame.argsSize = Long.max(currentFrame.argsSize, argsSize);

		return null;
	}
}
