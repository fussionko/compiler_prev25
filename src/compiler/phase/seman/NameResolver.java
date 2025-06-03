package compiler.phase.seman;

import java.util.*;

import compiler.common.report.*;
import compiler.phase.abstr.*;

/**
 * Name resolver.
 * 
 * The name resolver connects each node of a abstract syntax tree where a name
 * is used with the node where it is defined. The only exceptions are struct and
 * union component names which are connected with their definitions by the type
 * resolver. The results of the name resolver are stored in
 * {@link compiler.phase.seman.SemAn#defAt}.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class NameResolver implements AST.FullVisitor<Object, NameResolver.Mode> {

	/** Constructs a new name resolver. */
	public NameResolver() {
	}

	/** Two passes of name resolving. */
	protected enum Mode {
		/** The first pass: declaring names. */
		DECLARE,
		/** The second pass: resolving names. */
		RESOLVE,
	}

	/** The symbol table. */
	private SymbTable symbTable = new SymbTable();

	// ----- Trees -----

	@Override
	public Object visit(AST.Nodes<? extends AST.Node> nodes, Mode mode) 
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

			symbTable.lock();
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
	
	@Override
	public Object visit(AST.TypDefn typDefn, Mode mode) 
	{
		switch (mode) {
			case DECLARE:
				try {
					symbTable.ins(typDefn.name, typDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(typDefn, e.getMessage());

				}
				break;
			
			case RESOLVE:
				typDefn.type.accept(this, mode);
				break;

			default:
				break;
		}

		return null;
	}

	@Override
	public Object visit(AST.VarDefn varDefn, Mode mode) 
	{
		switch (mode) {
			case DECLARE:
				try {
					symbTable.ins(varDefn.name, varDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(varDefn, e.getMessage());

				}
				break;

			case RESOLVE:
				varDefn.type.accept(this, mode);
				break;

			default:
				break;
		}

		return null;
	}

	@Override
	public Object visit(AST.DefFunDefn defFunDefn, Mode mode) 
	{
		switch (mode) {
			case DECLARE:
				try {
					symbTable.ins(defFunDefn.name, defFunDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(defFunDefn, e.getMessage());

				}
				break;

			case RESOLVE:
				defFunDefn.pars.accept(this, mode);
				defFunDefn.type.accept(this, mode);
				
				// Set new scope - Function definition (3)
				symbTable.newScope();
				defFunDefn.pars.accept(this, Mode.DECLARE);
				defFunDefn.stmts.accept(this, mode); 
				symbTable.oldScope();
				break;

			default:
				break;
		}

		return null;
	}

	@Override
	public Object visit(AST.ExtFunDefn extFunDefn, Mode mode) 
	{
		switch (mode) {
			case DECLARE:
				try {
					symbTable.ins(extFunDefn.name, extFunDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(extFunDefn, e.getMessage());

				}
				break;

			case RESOLVE:
				extFunDefn.pars.accept(this, mode);
				extFunDefn.type.accept(this, mode);
				
				// Set new scope - Function definition (3)
				symbTable.newScope();
				extFunDefn.pars.accept(this, Mode.DECLARE);
				symbTable.oldScope();
				break;

			default:
				break;
		}

		return null;
	}

	@Override
	public Object visit(AST.ParDefn parDefn, Mode mode) 
	{
		switch (mode) {
			case DECLARE:
				try {
					symbTable.ins(parDefn.name, parDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(parDefn, e.getMessage());

				}
				break;

			case RESOLVE:
				parDefn.type.accept(this, mode);
				break;
		
			default:
				break;
		}

		return null;
	}

	@Override
	public Object visit(AST.CompDefn compDefn, Mode mode) 
	{
		switch (mode) {
			case DECLARE:
				try {
					symbTable.ins(compDefn.name, compDefn);
				} catch (SymbTable.CannotInsNameException e) {
					throw new Report.Error(compDefn, e.getMessage());

				}
				break;

			case RESOLVE:
				compDefn.type.accept(this, mode);
				break;
		
			default:
				break;
		}
		
		return null;
	}

	// ----- Statements -----

	@Override
	public Object visit(AST.LetStmt letStmt, Mode mode) 
	{
		if (mode == Mode.RESOLVE)
		{
			// Set new scope - Let-in (2)
			symbTable.newScope();
			letStmt.defns.accept(this, Mode.DECLARE);
			letStmt.defns.accept(this, mode);
			letStmt.stmts.accept(this, mode);
			symbTable.oldScope();
		}

		return null;
	}

	// ----- Types -----

	@Override
	public Object visit(AST.NameType nameType, Mode mode) 
	{
		if (mode == Mode.RESOLVE)
		{
			try {						
				SemAn.defAt.put(nameType, symbTable.fnd(nameType.name));
			} catch (SymbTable.CannotFndNameException e) {
				throw new Report.Error(nameType, e.getMessage());
			}
		}

		return null;
	}

	// ----- Expressions -----

	@Override
	public Object visit(AST.NameExpr nameExpr, Mode mode) 
	{
		if (mode == Mode.RESOLVE)
		{
			try {		
				SemAn.defAt.put(nameExpr, symbTable.fnd(nameExpr.name));
			} catch (SymbTable.CannotFndNameException e) {
				throw new Report.Error(nameExpr, e.getMessage());
			}
		}

		return null;
	}
	
	// ===== SYMBOL TABLE =====

	/**
	 * A symbol table.
	 */
	public class SymbTable {

		/**
		 * A symbol table record denoting a definition of a name within a certain scope.
		 */
		private class ScopedDefn {

			/** The depth of the scope the definition belongs to. */
			public final int depth;

			/** The definition. */
			public final AST.Defn defn;

			/**
			 * Constructs a new record denoting a definition of a name within a certain
			 * scope.
			 * 
			 * @param depth The depth of the scope the definition belongs to.
			 * @param defn  The definition.
			 */
			public ScopedDefn(int depth, AST.Defn defn) {
				this.depth = depth;
				this.defn = defn;
			}

		}

		/**
		 * A mapping of names into lists of records denoting definitions at different
		 * scopes. At each moment during the lifetime of a symbol table, the definition
		 * list corresponding to a particular name contains all definitions that name
		 * within currently active scopes: the definition at the inner most scope is the
		 * first in the list and is visible, the other definitions are hidden.
		 */
		public final HashMap<String, LinkedList<ScopedDefn>> allDefnsOfAllNames;

		/**
		 * The list of scopes. Each scope is represented by a list of names defined
		 * within it.
		 */
		private final LinkedList<LinkedList<String>> scopes;

		/** The depth of the currently active scope. */
		private int currDepth;

		/** Whether the symbol table can no longer be modified or not. */
		private boolean lock;

		/**
		 * Constructs a new symbol table.
		 */
		public SymbTable() {
			allDefnsOfAllNames = new HashMap<String, LinkedList<ScopedDefn>>();
			scopes = new LinkedList<LinkedList<String>>();
			currDepth = 0;
			lock = false;
			newScope();
		}

		/**
		 * Returns the depth of the currently active scope.
		 * 
		 * @return The depth of the currently active scope.
		 */
		public int currDepth() {
			return currDepth;
		}

		/**
		 * Inserts a new definition of a name within the currently active scope or
		 * throws an exception if this name has already been defined within this scope.
		 * Once the symbol table is locked, any attempt to insert further definitions
		 * results in an internal error.
		 * 
		 * @param name The name.
		 * @param defn The definition.
		 * @throws CannotInsNameException Thrown if this name has already been defined
		 *                                within the currently active scope.
		 */
		public void ins(String name, AST.Defn defn) throws CannotInsNameException {
			if (lock)
				throw new Report.InternalError();

			LinkedList<ScopedDefn> allDefnsOfName = allDefnsOfAllNames.get(name);
			if (allDefnsOfName == null) {
				allDefnsOfName = new LinkedList<ScopedDefn>();
				allDefnsOfAllNames.put(name, allDefnsOfName);
			}

			// Check if it already defined at this scope
			if (!allDefnsOfName.isEmpty()) {
				ScopedDefn defnOfName = allDefnsOfName.getFirst();
				if (defnOfName.depth == currDepth)
					throw new CannotInsNameException(name);
			}

			allDefnsOfName.addFirst(new ScopedDefn(currDepth, defn));
			scopes.getFirst().addFirst(name);
		}

		/**
		 * Returns the currently visible definition of the specified name. If no
		 * definition of the name exists within these scopes, an exception is thrown.
		 * 
		 * @param name The name.
		 * @return The definition.
		 * @throws CannotFndNameException Thrown if the name is not defined within the
		 *                                currently active scope or any scope enclosing
		 *                                it.
		 */
		public AST.Defn fnd(String name) throws CannotFndNameException {
			LinkedList<ScopedDefn> allDefnsOfName = allDefnsOfAllNames.get(name);
			if (allDefnsOfName == null)
				throw new CannotFndNameException(name);

			if (allDefnsOfName.isEmpty())
				throw new CannotFndNameException(name);

			return allDefnsOfName.getFirst().defn;
		}

		/** Used for selecting the range of scopes. */
		public enum XScopeSelector {
			/** All live scopes. */
			ALL,
			/** Currently active scope. */
			ACT,
		}

		/**
		 * Constructs a new scope within the currently active scope. The newly
		 * constructed scope becomes the currently active scope.
		 */
		public void newScope() {
			if (lock)
				throw new Report.InternalError();

			currDepth++;
			scopes.addFirst(new LinkedList<String>());
		}

		/**
		 * Destroys the currently active scope by removing all definitions belonging to
		 * it from the symbol table. Makes the enclosing scope the currently active
		 * scope.
		 */
		public void oldScope() {
			if (lock)
				throw new Report.InternalError();

			if (currDepth == 0)
				throw new Report.InternalError();

			for (String name : scopes.getFirst()) {
				allDefnsOfAllNames.get(name).removeFirst();
			}
			scopes.removeFirst();
			currDepth--;
		}

		/**
		 * Prevents further modifications of this symbol table.
		 */
		public void lock() {
			lock = true;
		}

		/**
		 * An exception thrown when the name cannot be inserted into a symbol table.
		 */
		@SuppressWarnings("serial")
		public class CannotInsNameException extends Exception {

			/**
			 * Constructs a new exception.
			 * @param name Name
			 */
			public CannotInsNameException(String name)
			{
				super("'" + name + "' is already defined at [" + allDefnsOfAllNames.get(name).getFirst().defn.location() + "]");
			}
		}

		/**
		 * An exception thrown when the name cannot be found in the symbol table.
		 */
		@SuppressWarnings("serial")
		public class CannotFndNameException extends Exception {

			/**
			 * Constructs a new exception.
			 * @param name Name
			 */
			public CannotFndNameException(String name)
			{
				super("Cannot find declaration for '" + name + "'");
			}
		}

	}

}
