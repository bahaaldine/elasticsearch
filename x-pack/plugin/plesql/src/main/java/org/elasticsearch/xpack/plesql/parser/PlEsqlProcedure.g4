grammar PlEsqlProcedure;

// Lexer Rules

BEGIN: 'BEGIN';
END: 'END';
EXECUTE: 'EXECUTE';
DECLARE: 'DECLARE';
SET: 'SET';
IF: 'IF';
ELSE: 'ELSE';
ELSEIF: 'ELSEIF';
ENDIF: 'END IF';
FOR: 'FOR';
IN: 'IN';
WHILE: 'WHILE';
LOOP: 'LOOP';
ENDLOOP: 'END LOOP';
TRY: 'TRY';
CATCH: 'CATCH';
FINALLY: 'FINALLY';
THROW: 'THROW';
ENDTRY: 'END TRY';
FUNCTION: 'FUNCTION';
END_FUNCTION: 'END FUNCTION';
THEN: 'THEN';

INT_TYPE: 'INT';
FLOAT_TYPE: 'FLOAT';
STRING_TYPE: 'STRING';
DATE_TYPE: 'DATE';

// Operators
PLUS: '+';
MINUS: '-';
MULTIPLY: '*';
DIVIDE: '/';
GREATER_THAN: '>';
LESS_THAN: '<';
NOT_EQUAL: '!=';
GREATER_EQUAL: '>=';
LESS_EQUAL: '<=';
PIPE: '|';
DOT_DOT: '..';
DOT: '.';
ASSIGN: '=';  // You can keep this if you prefer separate tokens for assignment and equality

LPAREN: '(';
RPAREN: ')';
COMMA: ',';
SEMICOLON: ';';
COLON: ':';

ID: [a-zA-Z_][a-zA-Z_0-9]*;

// Place FLOAT before INT
FLOAT: [0-9]+ '.' [0-9]+;
INT: [0-9]+;

STRING: '\'' ( ~('\'' | '\\') | '\\' . )* '\'';

// Comments
COMMENT
    : '--' ~[\r\n]*
    | '/*' .*? '*/'
    ;

// Whitespace
WS: [ \t\r\n]+ -> skip;

// Parser Rules

procedure
    : BEGIN statement+ END EOF
    ;

statement
    : throw_statement
    | execute_statement
    | declare_statement
    | assignment_statement
    | if_statement
    | loop_statement
    | try_catch_statement
    | function_definition
    | function_call_statement
    | SEMICOLON  // Allow empty statements
    ;

execute_statement
    : EXECUTE ESQL_QUERY
    ;

declare_statement
    : DECLARE variable_declaration_list SEMICOLON
    ;

variable_declaration_list
    : variable_declaration (COMMA variable_declaration)*
    ;

variable_declaration
    : ID datatype (ASSIGN expression)?
    ;

assignment_statement
    : SET ID ASSIGN expression SEMICOLON
    ;

if_statement
    : IF condition THEN statement+ (ELSEIF condition THEN statement+)* (ELSE statement+)? ENDIF
    ;

loop_statement
    : FOR ID IN expression DOT_DOT expression LOOP statement+ ENDLOOP
    | WHILE condition LOOP statement+ ENDLOOP
    ;

try_catch_statement
    : TRY statement+ (CATCH statement+)? (FINALLY statement+)? ENDTRY
    ;

throw_statement
    : THROW STRING SEMICOLON
    ;

function_definition
    : FUNCTION ID LPAREN (parameter_list)? RPAREN statement+ END_FUNCTION
    ;

function_call_statement
    : function_call SEMICOLON
    ;

function_call
    : ID LPAREN (argument_list)? RPAREN
    ;

parameter_list
    : parameter (COMMA parameter)*
    ;

parameter
    : ID datatype
    ;

argument_list
    : expression (COMMA expression)*
    ;

// ESQL_QUERY is now a lexer token
// esql_query
//     : LPAREN esql_query_body RPAREN SEMICOLON
//     ;

// esql_query_body
//     : (esql_query_clause)+
//     ;

// esql_query_clause
//     : (~RPAREN)+
//     ;

condition
    : expression comparison_operator expression
    ;

expression
    : expression op=MULTIPLY expression
    | expression op=DIVIDE expression
    | expression op=PLUS expression
    | expression op=MINUS expression
    | expression comparison_operator expression  // Added support for comparisons in expressions
    | LPAREN expression RPAREN
    | INT
    | FLOAT
    | STRING
    | ID
    | function_call
    ;

datatype
    : INT_TYPE
    | FLOAT_TYPE
    | STRING_TYPE
    | DATE_TYPE
    ;

comparison_operator
    : ASSIGN
    | NOT_EQUAL
    | LESS_THAN
    | GREATER_THAN
    | LESS_EQUAL
    | GREATER_EQUAL
    ;

// Lexer rule for ESQL_QUERY
ESQL_QUERY: LPAREN .*? RPAREN SEMICOLON;
