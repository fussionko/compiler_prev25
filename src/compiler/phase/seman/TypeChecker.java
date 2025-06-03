package compiler.phase.seman;

import java.util.*;
import java.util.function.Predicate;

import compiler.common.report.*;
import compiler.phase.abstr.*;

/**
 * Type checker.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class TypeChecker implements AST.FullVisitor<TYP.Type, Object> 
{
	/** Constructs a new name checker. */
	public TypeChecker() 
	{
		hasMain = false;
	}

	/**
	 * Checks if two types are equivalent.
	 * 
	 * @param t1 The first type.
	 * @param t2 The second type.
	 * @return True if the types are equivalent, false otherwise.
	 */
	private static boolean areEqu(TYP.Type t1, TYP.Type t2)
	{
		return areEqu(t1, t2, new HashSet<>());
	}

	private static boolean areEqu(TYP.Type t1, TYP.Type t2, Set<TYP.Type> visited) 
	{
		// EQU:1
		if (t1 == t2) 
			return true;

		if (visited.contains(t1) || visited.contains(t2)) 
			return true;

		visited.add(t1);
		visited.add(t2);

		TYP.Type t1Act = t1.actualType();
		TYP.Type t2Act = t2.actualType();

		// EQU:2
		if (t1Act instanceof TYP.NameType && t2Act instanceof TYP.NameType)
		{
			TYP.NameType t1Name = (TYP.NameType)t1;
			TYP.NameType t2Name = (TYP.NameType)t2;
			return t1Name.name.equals(t2Name.name); //&& areEqu(t1Act, t2Act);
		}

		// EQU:3
		if (t1Act instanceof TYP.ArrType && t2Act instanceof TYP.ArrType)
		{
			TYP.ArrType t1Arr = (TYP.ArrType)t1Act;
			TYP.ArrType t2Arr = (TYP.ArrType)t2Act;
			return t1Arr.numElems == t2Arr.numElems && areEqu(t1Arr.elemType, t2Arr.elemType, visited);
		}

		// EQU:4
		if (t1Act instanceof TYP.PtrType && t2Act instanceof TYP.PtrType)
		{
			TYP.PtrType t1Ptr = (TYP.PtrType)t1Act;
			TYP.PtrType t2Ptr = (TYP.PtrType)t2Act;

			if (t1Ptr.baseType == t1 && t2Ptr.baseType == t2)
				return true;

			return areEqu(t1Ptr.baseType, t2Ptr.baseType, visited);
		}

		// EQU:5
		if (t1Act instanceof TYP.StrType && t2Act instanceof TYP.StrType)
		{
			TYP.StrType t1Str = (TYP.StrType)t1Act;
			TYP.StrType t2Str = (TYP.StrType)t2Act;

			if (t1Str.compTypes.size() != t2Str.compTypes.size())
				return false;

			final int size = t1Str.compTypes.size();
			for (int i = 0; i < size; ++i)
				if (!areEqu(t1Str.compTypes.get(i), t2Str.compTypes.get(i), visited))
					return false;

			return true;
		}

		// EQU:6
		if (t1Act instanceof TYP.UniType && t2Act instanceof TYP.UniType)
		{
			TYP.UniType t1Uni = (TYP.UniType)t1Act;
			TYP.UniType t2Uni = (TYP.UniType)t2Act;

			if (t1Uni.compTypes.size() != t2Uni.compTypes.size())
				return false;

			final int size = t1Uni.compTypes.size();
			for (int i = 0; i < size; ++i)
				if (!areEqu(t1Uni.compTypes.get(i), t2Uni.compTypes.get(i), visited))
					return false;

			return true;
		}

		// EQU:7
		if (t1Act instanceof TYP.FunType && t2Act instanceof TYP.FunType)
		{
			TYP.FunType t1Fun = (TYP.FunType)t1Act;
			TYP.FunType t2Fun = (TYP.FunType)t2Act;
			
			if (t1Fun.parTypes.size() != t2Fun.parTypes.size())
				return false;

			final int size = t1Fun.parTypes.size();
			for (int i = 0; i < size; ++i)
				if (!areEqu(t1Fun.parTypes.get(i), t2Fun.parTypes.get(i), visited))
					return false;

			return areEqu(t1Fun.resType, t2Fun.resType, visited);
		}

		return false;
	}

	/**
	 * Checks if one type can be coerced into another.
	 * 
	 * @param from The source type.
	 * @param to The target type.
	 * @return True if the source type can be coerced into the target type, false otherwise.
	 */
	private static boolean canCoerce(TYP.Type from, TYP.Type to)
	{
		return canCoerce(from, to, new HashSet<>());
	}

	
	private static boolean canCoerce(TYP.Type from, TYP.Type to, Set<TYP.Type> visited)
	{	
		if (visited.contains(from) || visited.contains(to)) 
			return true;

		visited.add(from);
		visited.add(to);

		TYP.Type fromAct = from.actualType();
		TYP.Type toAct 	= to.actualType();

		// COE:1, COE:2, COE:3, COE:4
		if (fromAct == TYP.IntType.type && areEqu(to, TYP.IntType.type)) 	return true;
		if (fromAct == TYP.CharType.type && areEqu(to, TYP.CharType.type)) 	return true;
		if (fromAct == TYP.BoolType.type && areEqu(to, TYP.BoolType.type)) 	return true;
		if (fromAct == TYP.VoidType.type && areEqu(to, TYP.VoidType.type)) 	return true;

		// COE:6
		if (fromAct instanceof TYP.PtrType && ((TYP.PtrType)fromAct).baseType == TYP.VoidType.type)
			if (toAct instanceof TYP.PtrType)
				return true;
		
		// COE:5 
		if (fromAct instanceof TYP.PtrType && toAct instanceof TYP.PtrType)
			return canCoerce(((TYP.PtrType)fromAct).baseType, ((TYP.PtrType)toAct).baseType, visited);

		// COE:7
		if (fromAct instanceof TYP.ArrType && toAct instanceof TYP.ArrType)
		{
			TYP.ArrType t1Arr = (TYP.ArrType)fromAct;
			TYP.ArrType t2Arr = (TYP.ArrType)toAct;
			return t1Arr.numElems == t2Arr.numElems && canCoerce(t1Arr.elemType, t2Arr.elemType, visited);
		}

		// COE:8
		if (fromAct instanceof TYP.StrType && toAct instanceof TYP.StrType)
		{
			TYP.StrType fromStr = (TYP.StrType)fromAct;
			TYP.StrType toStr 	= (TYP.StrType)toAct;

			if (fromStr.compTypes.size() != toStr.compTypes.size())
				return false;

			final int size = fromStr.compTypes.size();
			for (int i = 0; i < size; ++i)
				if (!canCoerce(fromStr.compTypes.get(i), toStr.compTypes.get(i), visited))
					return false;

			return true;
		}

		// COE:9
		if (fromAct instanceof TYP.UniType && toAct instanceof TYP.UniType)
		{
			TYP.UniType fromUni = (TYP.UniType)fromAct;
			TYP.UniType toUni 	= (TYP.UniType)toAct;

			if (fromUni.compTypes.size() != toUni.compTypes.size())
				return false;

			final int size = fromUni.compTypes.size();
			for (int i = 0; i < size; ++i)
				if (!canCoerce(fromUni.compTypes.get(i), toUni.compTypes.get(i), visited))
					return false;

			return true;
		}

		// COE:10
		if (fromAct instanceof TYP.FunType && toAct instanceof TYP.FunType)
		{
			TYP.FunType fromFun = (TYP.FunType)fromAct;
			TYP.FunType toFun = (TYP.FunType)toAct;

			if (fromFun.parTypes.size() != toFun.parTypes.size())
				return false;

			final int size = fromFun.parTypes.size();
			for (int i = 0; i < size; ++i)
				if (!canCoerce(fromFun.parTypes.get(i), toFun.parTypes.get(i), visited))
					return false;

			return canCoerce(fromFun.resType, toFun.resType, visited);
		}

		// COE:11
		if (fromAct instanceof TYP.NameType && toAct instanceof TYP.NameType)
		{
			TYP.NameType fromName	= (TYP.NameType)fromAct;
			TYP.NameType toName 	= (TYP.NameType)toAct;

			return fromName.name.equals(toName.name) && areEqu(fromName.actualType(), toName.actualType(), visited);
		}

		return false;
	}

	/** (Unused but included to keep javadoc happy.) */
	private static final Predicate<TYP.Type> isVoid =
	(TYP.Type type) -> 	type.actualType() == TYP.VoidType.type;

	/** (Unused but included to keep javadoc happy.) */
	private static final Predicate<TYP.Type> isBasic =
		(TYP.Type type) -> 	areEqu(type, TYP.IntType.type) ||
							areEqu(type, TYP.CharType.type) ||
							areEqu(type, TYP.BoolType.type);
	/** (Unused but included to keep javadoc happy.) */
	private static final Predicate<TYP.Type> isBasicVoid = 
		(TYP.Type type) -> 	isBasic.test(type) || 
							areEqu(type, TYP.VoidType.type);

	/** (Unused but included to keep javadoc happy.) */
	private static final Predicate<TYP.Type> isBasicCheck = 
	(TYP.Type type) -> 	isBasic.test(type) || 
						type.actualType() instanceof TYP.PtrType ||
						type.actualType() instanceof TYP.FunType;				

	/** (Unused but included to keep javadoc happy.) */
	private static final Predicate<TYP.Type> isBasicVoidCheck = 
	(TYP.Type type) -> 	isBasicVoid.test(type) || 
						type.actualType() instanceof TYP.PtrType ||
						type.actualType() instanceof TYP.FunType;	

	/**
	 * Helper method for binary expressions.
	 * 
	 * @param binExpr The binary expression.
	 * @param t1 The type of the first operand.
	 * @param t2 The type of the second operand.
	 * @param type The expected type of the operands.
	 * @param op The operator.
	 */
	private static void helperBinExpr(AST.BinExpr binExpr, TYP.Type t1, TYP.Type t2, TYP.Type type, String op)
	{
		if (!areEqu(t1, type))
			throw new Report.Error(binExpr, "The left operand of binary '" + op + "' operator must be of type '" + type + "'.");
		if (!areEqu(t2, type))
			throw new Report.Error(binExpr, "The right operand of binary '" + op + "' operator must be of type '" + type + "'.");

		if (canCoerce(t2, t1))
			SemAn.ofType.put(binExpr, t1);
		else if (canCoerce(t1, t2))
			SemAn.ofType.put(binExpr, t2);
		else
			throw new Report.Error(binExpr, "Cannot coerce types'" + t1 + "' and '" + t2 + "'.");

		SemAn.isConst.put(binExpr, SemAn.isConst.get(binExpr.fstExpr) && SemAn.isConst.get(binExpr.sndExpr));
		SemAn.isAddr.put(binExpr, false);
	}

	/**
	 * Helper method for binary comparison expressions.
	 * 
	 * @param binExpr The binary expression.
	 * @param t1 The type of the first operand.
	 * @param t2 The type of the second operand.
	 * @param op The operator.
	 */
	private static void helperBinExprComp(AST.BinExpr binExpr, TYP.Type t1, TYP.Type t2, String op)
	{
		if (!isBasicCheck.test(t1))
			throw new Report.Error(binExpr, "The left operand of binary '" + op + "' operator must be of a simple type.");

		if (!isBasicCheck.test(t2))
			throw new Report.Error(binExpr, "The right operand of binary '" + op + "' operator must be of a simple type.");

		if (canCoerce(t2, t1) || canCoerce(t1, t2))
			SemAn.ofType.put(binExpr, TYP.BoolType.type);
		else
			throw new Report.Error(binExpr, "Cannot coerce types'" + t1 + "' and '" + t2 + "'.");

		SemAn.isConst.put(binExpr, SemAn.isConst.get(binExpr.fstExpr) && SemAn.isConst.get(binExpr.sndExpr));
		SemAn.isAddr.put(binExpr, false);
	}

	/**
	 * Detects recursive type definitions in the given type.
	 * 
	 * @param type The type to check for recursion.
	 * @param originalType The name of the original type being checked for recursion.
	 * @throws Report.Error if a self-referential or indirect recursive type definition is detected.
	 */
	private void detectRecursion(AST.Type type, String originalType) 
	{
		if (type instanceof AST.RecType) 
		{
			AST.RecType recType = (AST.RecType) type;
			for (AST.CompDefn comp : recType.comps) 
			{
				if (comp.type instanceof AST.NameType) 
				{
					String referencedType = ((AST.NameType) comp.type).name;
	
					if (referencedType.equals(originalType)) 
						throw new Report.Error(comp, "Self-referential type definition in '" + originalType + "'");
					
	
					// Check if referenced type leads back to the original type
					AST.TypDefn referencedDef = (AST.TypDefn)SemAn.defAt.get((AST.NameType)comp.type);
					if (referencedDef != null && currentScopeTypes.contains(referencedType)) 
						throw new Report.Error(comp, "Indirect recursive type definition detected involving '" 
							+ originalType + "' and '" + referencedType + "'");
					
					// Recursively check deeper references
					if (referencedDef != null) {
						detectRecursion(referencedDef.type, originalType);
					}
				}
			}
		}
	}

	/** (Unused but included to keep javadoc happy.) */
	private boolean hasMain;
	

	// ----- Trees -----
	@Override
	public TYP.Type visit(AST.Nodes<? extends AST.Node> nodes, Object arg) {
		for (final AST.Node node : nodes)
			node.accept(this, arg);

		if (!hasMain)
			 throw new Report.Error(nodes, "Program has no main function");

		return TYP.VoidType.type;
	}

	/** (Unused but included to keep javadoc happy.) */
	private Set<String> currentScopeTypes = new HashSet<>();

	// ----- Definitions -----	

	// TYP:2
	@Override
	public TYP.Type visit(AST.TypDefn typDefn, Object arg) 
	{
		if (!currentScopeTypes.add(typDefn.name)) 
            throw new Report.Error(typDefn, "Recursive type definition detected for type '" + typDefn.name + "'");

		try 
		{
			detectRecursion(typDefn.type, typDefn.name);

			typDefn.type.accept(this, arg);
		} finally
		{
			currentScopeTypes.remove(typDefn.name);
		}
	
		
		return TYP.VoidType.type;
	}

	// TYP:3
	@Override
	public TYP.Type visit(AST.VarDefn varDefn, Object arg) 
	{
		varDefn.type.accept(this, arg);

		if (areEqu(SemAn.isType.get(varDefn.type), TYP.VoidType.type))
			throw new Report.Error(varDefn, "Variable '" + varDefn.name + "' cannot be of type void.");

		SemAn.ofType.put(varDefn, SemAn.isType.get(varDefn.type));

		if (arg instanceof Boolean)
		{
			if ((boolean)arg == true)
				return SemAn.ofType.get(varDefn);
		}

		return TYP.VoidType.type;
	}

	// TYP:4
	@Override
	public TYP.Type visit(AST.DefFunDefn defFunDefn, Object arg) 
	{
		defFunDefn.type.accept(this, arg);

		List<TYP.Type> types = new LinkedList<TYP.Type>();
		for (AST.ParDefn parDefn : defFunDefn.pars)
		{
			parDefn.accept(this, arg);
			if (!isBasicCheck.test(SemAn.isType.get(parDefn.type)))
				throw new Report.Error(parDefn, "Invalid function parameter type '" + SemAn.isType.get(parDefn.type) + "'.");
			
			types.add(SemAn.isType.get(parDefn.type));

			SemAn.ofType.put(parDefn, SemAn.isType.get(parDefn.type));
		}

		TYP.Type resType = SemAn.isType.get(defFunDefn.type);
		SemAn.ofType.put(defFunDefn, new TYP.FunType(types, resType));

		if (arg instanceof Boolean)
		{
			if ((boolean)arg == true)
				return SemAn.ofType.get(defFunDefn);
		}

		for (AST.Stmt stmt : defFunDefn.stmts)
		{
			if (!isVoid.test(stmt.accept(this, resType)))
				throw new Report.Error(stmt, "Statment must be of type void");
		}

		if (!isBasicVoidCheck.test(resType))
			throw new Report.Error(defFunDefn, "Invalid function return type '" + resType + "'.");

		if (defFunDefn.name.equals("main") && defFunDefn.pars.size() == 0 && resType == TYP.IntType.type)
			hasMain = true;

		return TYP.VoidType.type;
	}

	// TYP:4
	@Override
	public TYP.Type visit(AST.ExtFunDefn extFunDefn, Object arg) 
	{
		extFunDefn.type.accept(this, arg);

		List<TYP.Type> types = new LinkedList<TYP.Type>();
		for (AST.ParDefn parDefn : extFunDefn.pars)
		{
			parDefn.accept(this, arg);
			if (!isBasicCheck.test(SemAn.isType.get(parDefn.type)))
				throw new Report.Error(parDefn, "Invalid function parameter type '" + SemAn.isType.get(parDefn.type) + "'.");
			
			types.add(SemAn.isType.get(parDefn.type));

			SemAn.ofType.put(parDefn, SemAn.isType.get(parDefn.type));
		}

		TYP.Type resType = SemAn.isType.get(extFunDefn.type);
		if (!isBasicVoidCheck.test(resType))
			throw new Report.Error(extFunDefn, "Invalid function return type '" + resType + "'.");

		SemAn.ofType.put(extFunDefn, new TYP.FunType(types, resType));

		if (arg instanceof Boolean)
		{
			if ((boolean)arg == true)
				return SemAn.ofType.get(extFunDefn);
		}

		return TYP.VoidType.type;
	}

	// // TYP4
	// @Override
	// public TYP.Type visit(AST.ParDefn parDefn, Object arg) 
	// {
	// 	parDefn.type.accept(this, arg);
		
	// 	SemAn.ofType.put(parDefn, SemAn.isType.get(parDefn.type));

	// 	try {
	// 		symbTable.ins(parDefn.name, parDefn);
	// 	} catch (SymbTable.CannotInsNameException e) {
	// 		throw new Report.Error(parDefn, e.getMessage());
	// 	}

	// 	return SemAn.ofType.get(parDefn);
	// }

	// // TYP4
	// @Override
	// public TYP.Type visit(AST.CompDefn compDefn, Object arg) 
	// {
	// 	compDefn.type.accept(this, arg);
		
	// 	SemAn.ofType.put(compDefn, SemAn.isType.get(compDefn.type));

	// 	return SemAn.ofType.get(compDefn);
	// }

	// ----- Statements -----

	// TYP:5
	@Override
	public TYP.Type visit(AST.AssignStmt assignStmt, Object arg) 
	{
		TYP.Type dstExprType = assignStmt.dstExpr.accept(this, arg);
		TYP.Type srcExprType = assignStmt.srcExpr.accept(this, arg);

		if (!isBasicCheck.test(dstExprType))
			throw new Report.Error(assignStmt, "Invalid assignment to '" + dstExprType + "'.");

		if (!isBasicCheck.test(srcExprType))
			throw new Report.Error(assignStmt, "Invalid assignment from '" + srcExprType + "'.");
		
		if (!canCoerce(srcExprType, dstExprType))
			throw new Report.Error(assignStmt, "Invalid assignment from '" + srcExprType + "' to '" + dstExprType + "'.");
		
		if (!SemAn.isAddr.get(assignStmt.dstExpr))
			throw new Report.Error(assignStmt, "The left-hand side of the assignment is not addressable.");
		
		return TYP.VoidType.type;
	}

	// TYP:6
	@Override
	public TYP.Type visit(AST.ReturnStmt returnStmt, Object arg)
	{
		if (arg == null)
			throw new Report.Error(returnStmt, "Function return type is not defined.");

		TYP.Type retExprType = returnStmt.retExpr.accept(this, arg);
		if (!isBasicCheck.test(retExprType))
			throw new Report.Error(returnStmt, "The return expression type '" + retExprType + "' is invalid");
		
		TYP.Type funType = (TYP.Type)arg;
		if (!isBasicCheck.test(funType))
			throw new Report.Error(returnStmt, "Function return type '" + funType + "' is invalid");

		if (!canCoerce(retExprType, funType))
			throw new Report.Error(returnStmt, "Cannot return type '" + retExprType + "' as '" + funType + "'.");

		return TYP.VoidType.type;
	}

	// TYP:41
	@Override
	public TYP.Type visit(AST.ExprStmt callStmt, Object arg) 
	{
		callStmt.expr.accept(this, arg);

		return TYP.VoidType.type;
	}

	// TYP:7
	@Override
	public TYP.Type visit(AST.WhileStmt whileStmt, Object arg) 
	{
		TYP.Type condExprType = whileStmt.condExpr.accept(this, arg);
		if (!areEqu(condExprType, TYP.BoolType.type))
			throw new Report.Error(whileStmt, "Condition expression must be of type 'bool'.");

		for (AST.Stmt stmt : whileStmt.stmts)
		{
			if (!isVoid.test(stmt.accept(this, arg)))
				throw new Report.Error(stmt, "Statment must be of type void");
		}

		return TYP.VoidType.type;
	}

	// TYP:8
	@Override
	public TYP.Type visit(AST.IfThenStmt ifThenStmt, Object arg) 
	{
		TYP.Type condExprType = ifThenStmt.condExpr.accept(this, arg);
		if (!areEqu(condExprType, TYP.BoolType.type))
			throw new Report.Error(ifThenStmt, "Condition expression must be of type 'bool'.");

		for (AST.Stmt stmt : ifThenStmt.thenStmt)
		{
			if (!isVoid.test(stmt.accept(this, arg)))
				throw new Report.Error(stmt, "Statment must be of type void");
		}	

		return TYP.VoidType.type;
	}

	// TYP:9
	@Override
	public TYP.Type visit(AST.IfThenElseStmt ifThenElseStmt, Object arg) 
	{
		TYP.Type condExprType = ifThenElseStmt.condExpr.accept(this, arg);
		if (!areEqu(condExprType, TYP.BoolType.type))
			throw new Report.Error(ifThenElseStmt, "Condition expression must be of type 'bool'.");

		for (AST.Stmt stmt : ifThenElseStmt.thenStmt)
		{
			if (!isVoid.test(stmt.accept(this, arg)))
				throw new Report.Error(stmt, "Statment must be of type void");
		}	
	
		for (AST.Stmt stmt : ifThenElseStmt.elseStmt)
		{
			if (!isVoid.test(stmt.accept(this, arg)))
				throw new Report.Error(stmt, "Statment must be of type void");
		}	

		return TYP.VoidType.type;
	}

	// TYP:10
	@Override
	public TYP.Type visit(AST.LetStmt letStmt, Object arg) 
	{
		for (AST.FullDefn defn : letStmt.defns)
		{
			if (!isVoid.test(defn.accept(this, arg)))
				throw new Report.Error(defn, "Definition must be of type void");
		}

		for (AST.Stmt stmt : letStmt.stmts)
		{
			stmt.accept(this, arg);
		}	
		

		return TYP.VoidType.type;
	}

	// // ----- Types -----

	// TYP:15
	@Override
	public TYP.Type visit(AST.PtrType ptrType, Object arg)
	{
		ptrType.baseType.accept(this, arg);
			
		if (areEqu(SemAn.isType.get(ptrType.baseType), TYP.VoidType.type))
			throw new Report.Error(ptrType, "Pointer type cannot be void.");

		return SemAn.isType.get(ptrType);
	}

	// TYP:16
	@Override
	public TYP.Type visit(AST.ArrType arrType, Object arg)
	{
		arrType.elemType.accept(this, arg);

		if (areEqu(SemAn.isType.get(arrType.elemType), TYP.VoidType.type))
			throw new Report.Error(arrType, "Array elements cannot be of type void.");

		Long numElems;
		try {
			numElems = Long.parseLong(arrType.numElems);
		} catch (NumberFormatException e) {
			throw new Report.Error(arrType, "Invalid array size: " + arrType.numElems);
		}

		// Validate the number of elements
		if (numElems < 0)
		    throw new Report.Error(arrType, "Array size must be a non-negative integer.");
		else if (numElems == 0)
			throw new Report.Error(arrType, "Array size cannot be zero.");

		return SemAn.isType.get(arrType);
	}

	// TYP:17
	@Override
	public TYP.Type visit(AST.StrType strType, Object arg) 
	{
		for (AST.CompDefn compDefn : strType.comps)
		{
			compDefn.accept(this, arg);
			if (areEqu(SemAn.isType.get(compDefn.type), TYP.VoidType.type))
				throw new Report.Error(strType, "Structure component '" + SemAn.isType.get(compDefn.type) + "' cannot be of type void.");

			SemAn.ofType.put(compDefn, SemAn.isType.get(compDefn.type));
		}

		return SemAn.isType.get(strType);
	}

	// TYP:18
	@Override
	public TYP.Type visit(AST.UniType uniType, Object arg) 
	{
		for (AST.CompDefn compDefn : uniType.comps)
		{
			compDefn.accept(this, arg);
			if (areEqu(SemAn.isType.get(compDefn.type), TYP.VoidType.type))
				throw new Report.Error(uniType, "Union component '" + SemAn.isType.get(compDefn.type) + "' cannot be of type void.");

			SemAn.ofType.put(compDefn, SemAn.isType.get(compDefn.type));
		}

		return SemAn.isType.get(uniType);
	}

	// TYP:19
	@Override
	public TYP.Type visit(AST.FunType funType, Object arg) 
	{
		funType.resType.accept(this, arg);

		for (AST.Type type : funType.parTypes)
		{
			type.accept(this, arg);
			if (!isBasicCheck.test(SemAn.isType.get(type)))
				throw new Report.Error(type, "Invalid function parameter type '" + SemAn.isType.get(type) + "'.");
		}

		TYP.Type resType = SemAn.isType.get(funType.resType);
		if (!isBasicVoidCheck.test(resType))
			throw new Report.Error(funType, "Invalid function return type '" + resType + "'.");

		return SemAn.isType.get(funType);
	}

	// // ----- Expressions -----

	// TYP:20, TYPE:21, TYP:22, TYP:23, TYP:24, TYP:25
	@Override
	public TYP.Type visit(AST.AtomExpr atomExpr, Object arg) 
	{
		switch (atomExpr.type) 
		{
			// TYP:20
			case INT:
			    try {
                	java.math.BigInteger value = new java.math.BigInteger(atomExpr.value);
                	java.math.BigInteger min = java.math.BigInteger.valueOf(Long.MIN_VALUE);
                	java.math.BigInteger max = java.math.BigInteger.valueOf(Long.MAX_VALUE);
               	 	if (value.compareTo(min) < 0 || value.compareTo(max) > 0)
                    	throw new Report.Error(atomExpr, "Integer literal out of 64-bit signed range.");
            	} catch (NumberFormatException e) {
                	throw new Report.Error(atomExpr, "Invalid integer literal.");
            	}

				SemAn.ofType.put(atomExpr, TYP.IntType.type);
				SemAn.isConst.put(atomExpr, true);
				SemAn.isAddr.put(atomExpr, false);
				break;

			// TYP:21
			case CHAR:
				SemAn.ofType.put(atomExpr, TYP.CharType.type);
				SemAn.isConst.put(atomExpr, true);
				SemAn.isAddr.put(atomExpr, false);
				break;
			
			// TYP:22
			case BOOL:
				SemAn.ofType.put(atomExpr, TYP.BoolType.type);
				SemAn.isConst.put(atomExpr, true);
				SemAn.isAddr.put(atomExpr, false);
				break;

			// TYP:24
			case STR:
				SemAn.ofType.put(atomExpr, new TYP.PtrType(TYP.CharType.type));
				SemAn.isConst.put(atomExpr, true);
				SemAn.isAddr.put(atomExpr, false);
				break;

			// TYP:25
			case PTR:
				try {
                	java.math.BigInteger value = new java.math.BigInteger(atomExpr.value);
                	java.math.BigInteger min = java.math.BigInteger.ZERO;
                	java.math.BigInteger max = new java.math.BigInteger("18446744073709551615");
               	 	if (value.compareTo(min) < 0 || value.compareTo(max) > 0)
                    	throw new Report.Error(atomExpr, "Pointer literal out of 64-bit range.");
            	} catch (NumberFormatException e) {
                	throw new Report.Error(atomExpr, "Invalid pointer literal.");
            	}

				SemAn.ofType.put(atomExpr, new TYP.PtrType(TYP.VoidType.type));
				SemAn.isConst.put(atomExpr, true);
				SemAn.isAddr.put(atomExpr, false);
				break;

			default:
				break;
		}

		return SemAn.ofType.get(atomExpr);
	}

	// TYP:26, TYP:27, TYP:35
	@Override
	public TYP.Type visit(AST.PfxExpr pfxExpr, Object arg) 
	{
		TYP.Type subExprType = pfxExpr.subExpr.accept(this, arg);

		switch (pfxExpr.oper) 
		{
			// TYP:26
			case ADD:			
				if (!areEqu(subExprType, TYP.IntType.type))
					throw new Report.Error(pfxExpr, "Unary '+' operator can only be applied to integers.");
				SemAn.ofType.put(pfxExpr, subExprType);
				SemAn.isConst.put(pfxExpr, SemAn.isConst.get(pfxExpr.subExpr));
				SemAn.isAddr.put(pfxExpr, false);
				break;
			
			// TYP:26
			case SUB:
				if (!areEqu(subExprType, TYP.IntType.type))
					throw new Report.Error(pfxExpr, "Unary '-' operator can only be applied to integers.");
				SemAn.ofType.put(pfxExpr, subExprType);
				SemAn.isConst.put(pfxExpr, SemAn.isConst.get(pfxExpr.subExpr));
				SemAn.isAddr.put(pfxExpr, false);
				break;

			// TYP:27
			case NOT:
				if (!areEqu(subExprType, TYP.BoolType.type))
					throw new Report.Error(pfxExpr, "Unary '!' operator can only be applied to booleans.");
				SemAn.ofType.put(pfxExpr, subExprType);
				SemAn.isConst.put(pfxExpr, SemAn.isConst.get(pfxExpr.subExpr));
				SemAn.isAddr.put(pfxExpr, false);
				break;

			// TYP:35
			case PTR:
				if (areEqu(subExprType, TYP.VoidType.type))
					throw new Report.Error(pfxExpr, "Cannot dereference a void pointer.");

				if (!SemAn.isAddr.get(pfxExpr.subExpr))
					throw new Report.Error(pfxExpr, "The pointer expression must be addressable.");

				SemAn.ofType.put(pfxExpr, new TYP.PtrType(subExprType));
				SemAn.isConst.put(pfxExpr, false);
				SemAn.isAddr.put(pfxExpr, false);
				break;
		
			default:
				break;
		}

		return SemAn.ofType.get(pfxExpr);
	}

	// TYP:28, TYP:29, TYP:30, TYP:31, TYP:32
	@Override
	public TYP.Type visit(AST.BinExpr binExpr, Object arg) 
	{
		TYP.Type fstExpr = binExpr.fstExpr.accept(this, arg);
		TYP.Type sndExpr = binExpr.sndExpr.accept(this, arg);

		switch (binExpr.oper) 
		{
			// TYP:28, TYP:29
			case AND:
				helperBinExpr(binExpr, fstExpr, sndExpr, TYP.BoolType.type, "&");
				break;

			// TYP:28, TYP:29
			case OR:
				helperBinExpr(binExpr, fstExpr, sndExpr, TYP.BoolType.type, "|");
				break;

			// TYP:30, TYP:31
			case MUL:
				helperBinExpr(binExpr, fstExpr, sndExpr, TYP.IntType.type, "*");
				break;

			// TYP:30, TYP:31
			case DIV:
				helperBinExpr(binExpr, fstExpr, sndExpr, TYP.IntType.type, "/");
				break;

			// TYP:30, TYP:31
			case MOD:
				helperBinExpr(binExpr, fstExpr, sndExpr, TYP.IntType.type, "%");
				break;

			// TYP:30, TYP:31
			case ADD:
				helperBinExpr(binExpr, fstExpr, sndExpr, TYP.IntType.type, "+");
				break;

			// TYP:30, TYP:31
			case SUB:
				helperBinExpr(binExpr, fstExpr, sndExpr, TYP.IntType.type, "-");
				break;

			// TYP:32
			case EQU:
				helperBinExprComp(binExpr, fstExpr, sndExpr, "==");
				break;

			// TYP:32
			case NEQ:
				helperBinExprComp(binExpr, fstExpr, sndExpr, "!=");
				break;

			// TYP:32
			case LTH:
				helperBinExprComp(binExpr, fstExpr, sndExpr, "<");
				break;

			// TYP:32
			case GTH:
				helperBinExprComp(binExpr, fstExpr, sndExpr, ">");
				break;

			// TYP:32
			case LEQ:
				helperBinExprComp(binExpr, fstExpr, sndExpr, "<=");
				break;

			// TYP:32
			case GEQ:
				helperBinExprComp(binExpr, fstExpr, sndExpr, ">=");
				break;

			default:
				break;
		}

		return SemAn.ofType.get(binExpr);
	}

	// TYP:33
	@Override
	public TYP.Type visit(AST.ArrExpr arrExpr, Object arg) 
	{
		TYP.Type arrExprType = arrExpr.arrExpr.accept(this, arg);
		if (!(arrExprType.actualType() instanceof TYP.ArrType))
			throw new Report.Error(arrExpr, "Array expression must be of type array.");

		arrExpr.idx.accept(this, arg);
		if (!areEqu(SemAn.ofType.get(arrExpr.idx), TYP.IntType.type))
			throw new Report.Error(arrExpr, "Array index must be of type 'int'.");
	
		if (!SemAn.isAddr.get(arrExpr.arrExpr))
			throw new Report.Error(arrExpr, "The array expression must be addressable.");
		
		SemAn.ofType.put(arrExpr, ((TYP.ArrType)arrExprType.actualType()).elemType);
		SemAn.isConst.put(arrExpr, false);
		SemAn.isAddr.put(arrExpr, true);

		return SemAn.ofType.get(arrExpr);
	}

	// TYP:34
	@Override
	public TYP.Type visit(AST.SfxExpr sfxExpr, Object arg) 
	{
		TYP.Type subExprType = sfxExpr.subExpr.accept(this, arg);

		switch (sfxExpr.oper) {
			// TYP:34
			case PTR:
				if (!(subExprType.actualType() instanceof TYP.PtrType))
					throw new Report.Error(sfxExpr, "Cannot dereference a non-pointer type.");

				TYP.Type baseType = ((TYP.PtrType) subExprType.actualType()).baseType;
				if (areEqu(baseType, TYP.VoidType.type))
					throw new Report.Error(sfxExpr, "Cannot dereference a void pointer.");

				if (SemAn.isConst.get(sfxExpr.subExpr))
					throw new Report.Error(sfxExpr, "The expression must not be const.");

				SemAn.ofType.put(sfxExpr, baseType);
				SemAn.isConst.put(sfxExpr, false);
				SemAn.isAddr.put(sfxExpr, true);
				break;

			default:
				throw new Report.Error(sfxExpr, "Unsupported suffix operator.");
		}

		return SemAn.ofType.get(sfxExpr);
	}

	// TYP:36, TYP:37
	@Override
	public TYP.Type visit(AST.CompExpr compExpr, Object arg) 
	{
		TYP.Type recExprType = compExpr.recExpr.accept(this, arg);
		TYP.Type actualType = recExprType.actualType();
	
		if (!(actualType instanceof TYP.StrType) && !(actualType instanceof TYP.UniType)) 
			throw new Report.Error(compExpr, "Expression must be of type structure or union.");
		if (!SemAn.isAddr.get(compExpr.recExpr))
			throw new Report.Error(compExpr, "Expression must be addressable.");

		TYP.RecType recType = (TYP.RecType)actualType;
		AST.Defn compDefn = recType.mapNames.get(compExpr.name);
		if (compDefn == null) 
			throw new Report.Error(compExpr, "Component '" + compExpr.name + "' not found in structure or union.");
		
		SemAn.mapPartDefn.put(compExpr, (AST.CompDefn)compDefn);

		SemAn.ofType.put(compExpr, SemAn.ofType.get(compDefn));
		SemAn.isConst.put(compExpr, false);
		SemAn.isAddr.put(compExpr, true);
	
		return SemAn.ofType.get(compExpr);
	}

	// TYP:38
	@Override
	public TYP.Type visit(AST.CallExpr callExpr, Object arg) 
	{
		TYP.Type funExpr = callExpr.funExpr.accept(this, arg);

		if (!(funExpr.actualType() instanceof TYP.FunType))
		{
			if (callExpr.funExpr instanceof AST.NameExpr) {
				AST.NameExpr nameExpr = (AST.NameExpr) callExpr.funExpr;
				AST.Defn defn = SemAn.defAt.get(nameExpr);
				if (defn instanceof AST.FunDefn) {
					AST.FunDefn funDefn = (AST.FunDefn) defn;
					TYP.Type returnType = SemAn.isType.get(defn.type);
					if (returnType == null)
						throw new Report.Error(callExpr, "Return type undefined in function '" + nameExpr.name + "'");
					
				}
			}
			throw new Report.Error(callExpr, "Call expression must refer to a function type.");
		}
		
		TYP.FunType funType = (TYP.FunType)funExpr.actualType();
		for (int i = 0; i < callExpr.argExprs.size(); ++i)
		{
			callExpr.argExprs.get(i).accept(this, arg);
			if (!canCoerce(funType.parTypes.get(i), SemAn.ofType.get(callExpr.argExprs.get(i))))
				throw new Report.Error(callExpr, "Cannot coerce argument type '" + SemAn.ofType.get(callExpr.argExprs.get(i)) + "' to function parameter type.");
		}
		
		SemAn.ofType.put(callExpr, funType.resType);
		SemAn.isConst.put(callExpr, false);
		SemAn.isAddr.put(callExpr, false);

		return SemAn.ofType.get(callExpr);
	}

	// TYP:39
	@Override
	public TYP.Type visit(AST.SizeExpr sizeExpr, Object arg) 
	{
		sizeExpr.type.accept(this, arg);
		
		if (areEqu(SemAn.isType.get(sizeExpr.type), TYP.VoidType.type))
			throw new Report.Error(sizeExpr, "Cannot get sizeof type 'void'.");

		SemAn.ofType.put(sizeExpr, TYP.IntType.type);
		SemAn.isConst.put(sizeExpr, true);
		SemAn.isAddr.put(sizeExpr, false);

		return TYP.IntType.type;
	}

	// TYP:40
	@Override
	public TYP.Type visit(AST.CastExpr castExpr, Object arg) 
	{
		castExpr.type.accept(this, arg);
		castExpr.expr.accept(this, arg);

		if (areEqu(SemAn.ofType.get(castExpr.expr), TYP.VoidType.type))
			throw new Report.Error(castExpr, "Casting a 'void' expression to type '" + SemAn.isType.get(castExpr.type) + "' is not allowed.");
	
		if (areEqu(SemAn.isType.get(castExpr.type), TYP.VoidType.type))
			throw new Report.Error(castExpr, "Casting an expression to type 'void' is not permitted.");

		SemAn.ofType.put(castExpr, SemAn.isType.get(castExpr.type));
		SemAn.isConst.put(castExpr, SemAn.isConst.get(castExpr.expr));
		SemAn.isAddr.put(castExpr, SemAn.isAddr.get(castExpr.expr));

		return SemAn.ofType.get(castExpr);
	}

	@Override
	public TYP.Type visit(AST.NameExpr nameExpr, Object arg) 
	{
		AST.Defn defn = SemAn.defAt.get(nameExpr);
		if (defn == null)
			throw new Report.Error(nameExpr, "Undefined name: " + nameExpr.name);

		
		TYP.Type type = SemAn.ofType.get(defn);

		if (type == null)
		{
			if (defn instanceof AST.FunDefn)
			{
				defn = (AST.FunDefn)defn;
				SemAn.isAddr.put(nameExpr, false);
			}
			else if (defn instanceof AST.VarDefn)
				defn = (AST.VarDefn)defn;
			type = (defn).accept(this, true);
			if (type == null)
				throw new Report.Error(nameExpr, "Type of name '" + nameExpr.name + "' is undefined.");

		}

		SemAn.ofType.put(nameExpr, type);
		SemAn.isConst.put(nameExpr, false);

		if (SemAn.isAddr.get(nameExpr) == null)
			SemAn.isAddr.put(nameExpr, true);

		return type;
	}
}
