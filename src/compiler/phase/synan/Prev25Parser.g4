parser grammar Prev25Parser;

@header {
	package compiler.phase.synan;	
	
	import java.util.*;
	import compiler.common.report.*;
	import compiler.phase.lexan.*;
	import compiler.phase.abstr.*;
}

@members {

	private Location loc(Token tok) { return new Location((LexAn.LocLogToken)tok); }
	private Location loc(Token     tok1, Token     tok2) { return new Location((LexAn.LocLogToken)tok1, (LexAn.LocLogToken)tok2); }
	private Location loc(Token     tok1, Locatable loc2) { return new Location((LexAn.LocLogToken)tok1, loc2); }
	private Location loc(Locatable loc1, Token     tok2) { return new Location(loc1, (LexAn.LocLogToken)tok2); }
	private Location loc(Locatable loc1, Locatable loc2) { return new Location(loc1, loc2); }
}

options{
    tokenVocab=Prev25Lexer;
}

// Helpers

parameterList 
	returns [List<AST.ParDefn> ast]	
	: { $ast = new ArrayList<AST.ParDefn>(); }
	| parameter
		{ $ast = new ArrayList<AST.ParDefn>(); $ast.addLast($parameter.ast); }
	| pl = parameterList COMMA parameter
		{ $ast = $pl.ast; $ast.addLast($parameter.ast); }
	; 
parameter
	returns [AST.ParDefn ast]
	: ID COLON type
		{ $ast = new AST.ParDefn(loc($ID, $type.stop), $ID.text, $type.ast); } 
	;

recordList 	
	returns [List<AST.CompDefn> ast]
	: record
		{ $ast = new ArrayList<AST.CompDefn>(); $ast.addLast($record.ast); }
	| rl = recordList COMMA record
		{ $ast = $rl.ast; $ast.addLast($record.ast); }
	; 
record
	returns [AST.CompDefn ast]
	: ID COLON type
		{ $ast = new AST.CompDefn(loc($ID, $type.stop), $ID.text, $type.ast); } 
	;

stmtList
	returns [List<AST.Stmt> ast]
	: { $ast = new ArrayList<AST.Stmt>(); } 
	| stmts
	  { $ast = $stmts.ast; }
	;

//-----------------------------------------------------------------------------------------------

source
	returns [AST.Nodes<AST.FullDefn> ast]
	:	defs EOF
		{ $ast = new AST.Nodes<AST.FullDefn>($defs.ast); }
	;

defs
	returns [List<AST.FullDefn> ast]
	:	def 
		{ $ast = new ArrayList<AST.FullDefn>(); $ast.addLast($def.ast); }
	|	d = defs def
		{ $ast = $d.ast; $ast.addLast($def.ast); }
	;

def 
	returns [AST.FullDefn ast]
	: TYP ID EQUAL type
		{ $ast = new AST.TypDefn(loc($TYP, $type.stop), $ID.text, $type.ast); }
	| VAR ID COLON type 
		{ $ast = new AST.VarDefn(loc($VAR, $type.stop), $ID.text, $type.ast); }
	| FUN ID LPAREN parameterList RPAREN COLON type
		{ $ast = new AST.ExtFunDefn(loc($FUN, $type.stop), $ID.text, $parameterList.ast, $type.ast); }
	| FUN ID LPAREN parameterList RPAREN COLON type EQUAL stmts
		{ $ast = new AST.DefFunDefn(loc($FUN, $stmts.stop), $ID.text, $parameterList.ast, $type.ast, $stmts.ast); }
	;

stmts
	returns [List<AST.Stmt> ast]
	: stmt
		{ $ast = new ArrayList<AST.Stmt>(); $ast.addLast($stmt.ast); } 
	| s = stmts COMMA stmt 
		{ $ast = $s.ast; $ast.addLast($stmt.ast); }
	;

stmt
	returns [AST.Stmt ast]
	: expr
		{ $ast = new AST.ExprStmt(loc($expr.start, $expr.stop), $expr.ast); }
	| e1 = expr EQUAL e2 = expr
		{ $ast = new AST.AssignStmt(loc($e1.start, $e2.stop), $e1.ast, $e2.ast); }
	| RETURN expr 
		{ $ast = new AST.ReturnStmt(loc($RETURN, $expr.stop), $expr.ast); }
	| WHILE expr DO stmtList END
		{ $ast = new AST.WhileStmt(loc($WHILE, $END), $expr.ast, $stmtList.ast); }
	| IF expr THEN stmtList END
		{ $ast = new AST.IfThenStmt(loc($IF, $END), $expr.ast, $stmtList.ast); }
	| IF expr THEN t = stmtList ELSE e = stmtList END
		{ $ast = new AST.IfThenElseStmt(loc($IF, $END), $expr.ast, $t.ast, $e.ast); }
	| LET defs IN stmts END
		{ $ast = new AST.LetStmt(loc($LET, $END), $defs.ast, $stmts.ast); }
	;

parTypes
	returns [List<AST.Type> ast]
	: { $ast = new ArrayList<AST.Type>(); } 
	| type
		{ $ast = new ArrayList<AST.Type>(); $ast.addLast($type.ast); } 
	| pt = parTypes COMMA type
		{ $ast = $pt.ast; $ast.addLast($type.ast); } 
	;
type
	returns [AST.Type ast]
	: LPAREN parTypes RPAREN COLON type 
		{ $ast = new AST.FunType(loc($LPAREN, $type.stop), $parTypes.ast, $type.ast); }
	| LBRACK INTCONST RBRACK type
		{ $ast = new AST.ArrType(loc($LBRACK, $type.stop), $type.ast, $INTCONST.text); }
	| CARET type
		{ $ast = new AST.PtrType(loc($CARET, $type.stop), $type.ast); }
	| LT recordList GT
		{ $ast = new AST.StrType(loc($LT, $GT), $recordList.ast); }
	| LBRACE recordList RBRACE
		{ $ast = new AST.UniType(loc($LBRACE, $RBRACE), $recordList.ast); }
	| INT
		{ $ast = new AST.AtomType(loc($INT), AST.AtomType.Type.INT); }
	| CHAR
		{ $ast = new AST.AtomType(loc($CHAR), AST.AtomType.Type.CHAR); }
	| BOOL
		{ $ast = new AST.AtomType(loc($BOOL), AST.AtomType.Type.BOOL); }
	| VOID
		{ $ast = new AST.AtomType(loc($VOID), AST.AtomType.Type.VOID); }
	| ID
		{ $ast = new AST.NameType(loc($ID), $ID.text); }
	;
	
argExprs
	returns [List<AST.Expr> ast]
	: { $ast = new ArrayList<AST.Expr>(); } 
	| expr
		{ $ast = new ArrayList<AST.Expr>(); $ast.addLast($expr.ast); } 
	| ae = argExprs COMMA expr
		{ $ast = $ae.ast; $ast.addLast($expr.ast); } 
	;

expr
    returns [AST.Expr ast]
    : logicalOrExpr
		{ $ast = $logicalOrExpr.ast; }
    ;

logicalOrExpr
    returns [AST.Expr ast]
    : e1 = logicalOrExpr PIPE e2 = logicalAndExpr 
        { $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.OR, $e1.ast, $e2.ast); }
	| logicalAndExpr
		{ $ast = $logicalAndExpr.ast; }
    ;

logicalAndExpr
    returns [AST.Expr ast]
    : e1 = logicalAndExpr AMP e2 = compExpr 
        { $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.AND, $e1.ast, $e2.ast); }
	| compExpr
		{ $ast = $compExpr.ast; }
    ;

compExpr
    returns [AST.Expr ast]
	: e1 = addExpr EQEQ e2 = addExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.EQU, $e1.ast, $e2.ast); }
	| e1 = addExpr NOTEQ e2 = addExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.NEQ, $e1.ast, $e2.ast); }
	| e1 = addExpr LT e2 = addExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.LTH, $e1.ast, $e2.ast); }
	| e1 = addExpr GT e2 = addExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.GTH, $e1.ast, $e2.ast); }
	| e1 = addExpr LTEQ e2 = addExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.LEQ, $e1.ast, $e2.ast); }
	| e1 = addExpr GTEQ e2 = addExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.GEQ, $e1.ast, $e2.ast); }
	| addExpr
		{ $ast = $addExpr.ast; }
    ;

addExpr
    returns [AST.Expr ast]
	: e1 = addExpr PLUS e2 = mulExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.ADD, $e1.ast, $e2.ast); }
	| e1 = addExpr MINUS e2 = mulExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.SUB, $e1.ast, $e2.ast); }
	| mulExpr
		{ $ast = $mulExpr.ast; }
    ;

mulExpr
    returns [AST.Expr ast]
	: e1 = mulExpr MUL e2 = prefixExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.MUL, $e1.ast, $e2.ast); }
	| e1 = mulExpr DIV e2 = prefixExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.DIV, $e1.ast, $e2.ast); }
	| e1 = mulExpr MOD e2 = prefixExpr
		{ $ast = new AST.BinExpr(loc($e1.start, $e2.stop), AST.BinExpr.Oper.MOD, $e1.ast, $e2.ast); }
	| prefixExpr
		{ $ast = $prefixExpr.ast; }
    ;

prefixExpr
    returns [AST.Expr ast]
	: PLUS prefixExpr
		{ $ast = new AST.PfxExpr(loc($PLUS, $prefixExpr.stop), AST.PfxExpr.Oper.ADD, $prefixExpr.ast); }
	| MINUS prefixExpr
		{ $ast = new AST.PfxExpr(loc($MINUS, $prefixExpr.stop), AST.PfxExpr.Oper.SUB, $prefixExpr.ast); }
	| EXCL prefixExpr
		{ $ast = new AST.PfxExpr(loc($EXCL, $prefixExpr.stop), AST.PfxExpr.Oper.NOT, $prefixExpr.ast); }
	| CARET prefixExpr
	 	{ $ast = new AST.PfxExpr(loc($CARET, $prefixExpr.stop), AST.PfxExpr.Oper.PTR, $prefixExpr.ast); }
	| suffixExpr
		{ $ast = $suffixExpr.ast; }
    ;

suffixExpr
    returns [AST.Expr ast]
	: se = suffixExpr LPAREN argExprs RPAREN
		{ $ast = new AST.CallExpr(loc($se.start, $RPAREN), $se.ast, $argExprs.ast); }
	| se = suffixExpr LBRACK e = expr RBRACK
		{ $ast = new AST.ArrExpr(loc($se.start, $RBRACK), $se.ast, $e.ast); }
	| se = suffixExpr CARET
		{ $ast = new AST.SfxExpr(loc($se.start, $CARET), AST.SfxExpr.Oper.PTR, $se.ast); }
	| se = suffixExpr DOT ID
		{ $ast = new AST.CompExpr(loc($se.start, $ID), $se.ast, $ID.text); }
	| primaryExpr
		{ $ast = $primaryExpr.ast; }
    ;

primaryExpr
    returns [AST.Expr ast]
	: INTCONST
		{ $ast = new AST.AtomExpr(loc($INTCONST), AST.AtomExpr.Type.INT, $INTCONST.text); }
	| CHARCONST
		{ $ast = new AST.AtomExpr(loc($CHARCONST), AST.AtomExpr.Type.CHAR, $CHARCONST.text); }
	| STRING
		{ $ast = new AST.AtomExpr(loc($STRING), AST.AtomExpr.Type.STR, $STRING.text); }
	| TRUE
		{ $ast = new AST.AtomExpr(loc($TRUE), AST.AtomExpr.Type.BOOL, $TRUE.text); }
	| FALSE
		{ $ast = new AST.AtomExpr(loc($FALSE), AST.AtomExpr.Type.BOOL, $FALSE.text); }
	| NULL
		{ $ast = new AST.AtomExpr(loc($NULL), AST.AtomExpr.Type.PTR, "0"); }
	| ID
		{ $ast = new AST.NameExpr(loc($ID), $ID.text); }
  	| LPAREN expr RPAREN
		{ $ast = $expr.ast; $ast.relocate(loc($LPAREN, $RPAREN));  }
	| SIZEOF type
		{ $ast = new AST.SizeExpr(loc($SIZEOF, $type.stop), $type.ast); }
	| LBRACE expr COLON type RBRACE
		{ $ast = new AST.CastExpr(loc($LBRACE, $RBRACE), $type.ast, $expr.ast); }
    ;
