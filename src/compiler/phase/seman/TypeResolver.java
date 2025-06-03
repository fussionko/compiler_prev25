package compiler.phase.seman;

import java.util.*;

import compiler.common.report.*;
import compiler.phase.abstr.*;

/**
 * Type resolver.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class TypeResolver implements AST.FullVisitor<TYP.Type, TypeResolver.Mode> {

	/** Two passes of type resolving. */
	protected enum Mode {
		/** The first pass: declaring types. */
		DECLARE,
		/** The second pass: resolving types. */
		RESOLVE,
	}

	/** Constructs a new name resolver. */
	public TypeResolver() {
	}

	// ----- Trees -----
	
	@Override
	public TYP.Type visit(AST.Nodes<? extends AST.Node> nodes, Mode mode) 
	{
		// First call
		if (mode == null)
		{
			// Declare
			for (final AST.Node node : nodes)
				node.accept(this, Mode.DECLARE);

			// Resolve
			for (final AST.Node node : nodes)
				node.accept(this, Mode.RESOLVE);
		}
		else 
		{	
			// Run as mode trough all the nodes
			for (final AST.Node node : nodes)
				node.accept(this, mode);
		}

		return null;
	}

	// ----- Definitions -----
	
	// TYP2
	@Override
	public TYP.Type visit(AST.TypDefn typDefn, Mode mode) 
	{
		switch (mode) {
			case DECLARE:
				SemAn.isType.put(typDefn, new TYP.NameType(typDefn.name));
				break;

			case RESOLVE:
				typDefn.type.accept(this, mode);

				TYP.NameType nameTypeRef = (TYP.NameType)SemAn.isType.get(typDefn);
				if (nameTypeRef == null)
					throw new Report.Error(typDefn, "Type definition not declared: " + typDefn.name);
				
				TYP.Type resolvedType = SemAn.isType.get(typDefn.type);
    			if (resolvedType == null)
    			    throw new Report.Error(typDefn, "Type of '" + typDefn.name + "' could not be resolved.");

				nameTypeRef.setActType(SemAn.isType.get(typDefn.type));
				break;

			default:
				break;
		}

		return SemAn.isType.get(typDefn);
	}

	// ----- Types ----

	// TYP11, TYP12, TYP13, TYP14
	@Override
	public TYP.Type visit(AST.AtomType atomType, Mode mode) 
	{
		if (mode == Mode.RESOLVE)
		{
			switch (atomType.type) 
			{
				case INT:
					SemAn.isType.put(atomType, TYP.IntType.type);
					break;

				case CHAR:
					SemAn.isType.put(atomType, TYP.CharType.type);
					break;

				case BOOL:
					SemAn.isType.put(atomType, TYP.BoolType.type);
					break;

				case VOID:
					SemAn.isType.put(atomType, TYP.VoidType.type);
					break;

				default:
					break;
			}
		}

		return SemAn.isType.get(atomType);
	}


	// TYP15
	@Override
	public TYP.Type visit(AST.PtrType ptrType, Mode mode)
	{
		if (mode == Mode.RESOLVE)
		{
			ptrType.baseType.accept(this, mode);
			
			SemAn.isType.put(ptrType, new TYP.PtrType(SemAn.isType.get(ptrType.baseType)));
		}
			
		return SemAn.isType.get(ptrType);
	}

	// TYP16
	@Override
	public TYP.Type visit(AST.ArrType arrType, Mode mode)
	{
		if (mode == Mode.RESOLVE)
		{
			arrType.elemType.accept(this, mode);

			Long numElems;
			try {
			    numElems = Long.parseLong(arrType.numElems);
			} catch (NumberFormatException e) {
			    throw new Report.Error(arrType, "Invalid array size: " + arrType.numElems);
			}

			SemAn.isType.put(arrType, new TYP.ArrType(SemAn.isType.get(arrType.elemType), numElems));
		}	

		return SemAn.isType.get(arrType);
	}

	// TYP17
	@Override
	public TYP.Type visit(AST.StrType strType, Mode mode) 
	{
		if (mode == Mode.RESOLVE)
		{
			strType.comps.accept(this, mode);

			HashMap<String, AST.Defn> mapCompNames = new HashMap<>();

			// Get parameters
			List<TYP.Type> compTypes = new LinkedList<TYP.Type>();
			for (AST.CompDefn compDefn : strType.comps)
			{
				compTypes.add(SemAn.isType.get(compDefn.type));
				mapCompNames.put(compDefn.name, compDefn);
			}
			
			TYP.StrType typStrType = new TYP.StrType(compTypes);
			typStrType.mapNames = mapCompNames;
			SemAn.isType.put(strType, typStrType);
		}

		return SemAn.isType.get(strType);
	}

	// TYP18
	@Override
	public TYP.Type visit(AST.UniType uniType, Mode mode) 
	{
		if (mode == Mode.RESOLVE)
		{
			uniType.comps.accept(this, mode);

			HashMap<String, AST.Defn> mapCompNames = new HashMap<>();

			// Get components
			List<TYP.Type> compTypes = new LinkedList<TYP.Type>();
			for (AST.CompDefn compDefn : uniType.comps)
			{
				compTypes.add(SemAn.isType.get(compDefn.type));
				mapCompNames.put(compDefn.name, compDefn);
			}
			

			TYP.UniType typUniType = new TYP.UniType(compTypes);
			typUniType.mapNames = mapCompNames;
			SemAn.isType.put(uniType, typUniType);
		}
			
		return SemAn.isType.get(uniType);
	}

	// TYP19
	@Override
	public TYP.Type visit(AST.FunType funType, Mode mode) 
	{
		if (mode == Mode.RESOLVE)
		{
			funType.parTypes.accept(this, mode);
			funType.resType.accept(this, mode);

			// Get resolved parameters
			List<TYP.Type> types = new LinkedList<TYP.Type>();
			for (AST.Type type : funType.parTypes)
				types.add(SemAn.isType.get(type));
			
			SemAn.isType.put(funType, new TYP.FunType(types, SemAn.isType.get(funType.resType)));
		}

		return SemAn.isType.get(funType);
	}

	// TYP2
	@Override
	public TYP.Type visit(AST.NameType nameType, Mode mode) 
	{
		if (mode == Mode.RESOLVE) 
		{	
			AST.Defn defn = SemAn.defAt.get(nameType);
			if (defn == null) 
				throw new Report.Error(nameType, "Type '" + nameType.name + "' is not declared.");

			TYP.Type resolvedType = SemAn.isType.get(defn);
			if (resolvedType == null) 
				throw new Report.Error(nameType, "Type '" + nameType.name + "' is not defined.");

			// Store the resolved type
			SemAn.isType.put(nameType, resolvedType);
		}

		return SemAn.isType.get(nameType);
	}
}
