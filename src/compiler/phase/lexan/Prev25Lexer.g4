lexer grammar Prev25Lexer;

@header {
	package compiler.phase.lexan;

	import compiler.common.report.*;
}

@members {
    @Override
	public LexAn.LocLogToken nextToken() {
		return (LexAn.LocLogToken) super.nextToken();
	}
}


INTCONST	: DIGIT+;

CHARCONST	: '\'' (~[\u0000-\u001f\\\u0027\u007f] | HEX | '\\\\' | '\\\'') '\'';

STRING 		: '"' (~[\u0000-\u001f\\\u0022\u007f] | HEX | '\\\\' | '\\"')+ '"';

// Operators and Symbols
AMP        : '&';
PIPE       : '|';
EQEQ       : '==';
NOTEQ      : '!=';
LT         : '<';
GT         : '>';
LTEQ       : '<=';
GTEQ       : '>=';
MUL        : '*';
DIV        : '/';
MOD        : '%';
PLUS       : '+';
MINUS      : '-';
EXCL       : '!';
DOT        : '.';
CARET      : '^';
EQUAL      : '=';
COLON      : ':';
COMMA      : ',';
LBRACE     : '{';
RBRACE     : '}';
LPAREN     : '(';
RPAREN     : ')';
LBRACK     : '[';
RBRACK     : ']';

// Keywords
BOOL       : 'bool';
CHAR       : 'char';
DO         : 'do';
ELSE       : 'else';
END        : 'end';
FALSE      : 'false';
FUN        : 'fun';
IF         : 'if';
IN         : 'in';
INT        : 'int';
LET        : 'let';
NULL       : 'null';
RETURN     : 'return';
SIZEOF     : 'sizeof';
THEN       : 'then';
TRUE       : 'true';
TYP        : 'typ';
VAR        : 'var';
VOID       : 'void';
WHILE      : 'while';

ID			: (LETTER | '_')(LETTER | DIGIT | '_')*;
//INVALID_ID	: DIGIT*(LETTER | '_')(LETTER | DIGIT | '_')*;

COMMENT  	: '#' ~[\r\n]* -> skip;

WHITESPACE	: [ \n\r\t]+ -> skip;

SYMBOL		: (
		AMP | PIPE | EQEQ | NOTEQ | LT | GT | LTEQ | GTEQ | MUL | DIV | MOD | PLUS | 
		MINUS | EXCL | DOT | CARET | EQUAL | COLON | COMMA | LBRACE | RBRACE | LPAREN | 
		RPAREN | LBRACK | RBRACK 
	);

KEYWORD 	: (
		BOOL | CHAR | DO | ELSE | END | FALSE | FUN | IF | IN | INT | LET | 
		NULL | RETURN | SIZEOF | THEN | TRUE | TYP | VAR | VOID  | WHILE
	);


fragment DIGIT 				: [0-9];
fragment LETTER				: [a-zA-Z];
fragment HEX				: '\\0x'[0-9A-F][0-9A-F];
