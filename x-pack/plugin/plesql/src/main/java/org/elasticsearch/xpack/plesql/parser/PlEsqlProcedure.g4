grammar PlEsqlProcedure;

// =======================
// Lexer Rules
// =======================

// Keywords
ELSEIF: 'ELSEIF';
ELSE: 'ELSE';
IF: 'IF';
THEN: 'THEN';
END: 'END';
BEGIN: 'BEGIN';
EXECUTE: 'EXECUTE';
DECLARE: 'DECLARE';
SET: 'SET';
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
RETURN: 'RETURN';
BREAK: 'BREAK';
PERSIST: 'PERSIST';
INTO: 'INTO';

// Data Types
INT_TYPE: 'INT';
FLOAT_TYPE: 'FLOAT';
STRING_TYPE: 'STRING';
DATE_TYPE: 'DATE';
NUMBER_TYPE: 'NUMBER';
DOCUMENT_TYPE: 'DOCUMENT';
ARRAY_TYPE: 'ARRAY';

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
OR: 'OR';
AND: 'AND';
EQUAL: '=';

// Range Operator
DOT_DOT: '..';

// Other Symbols
PIPE: '|';
DOT: '.';
LPAREN: '(';
RPAREN: ')';
COMMA: ',';
COLON: ':';
SEMICOLON: ';';

// Literals
FLOAT: [0-9]+ '.' [0-9]+;
INT: [0-9]+;
STRING
    : ('\'' ( ~('\'' | '\\') | '\\' . )* '\''
    | '"' ( ~('"' | '\\') | '\\' . )* '"')
    ;

// Identifier
ID: [a-zA-Z_][a-zA-Z_0-9]*;

// Comments and Whitespace
COMMENT
    : ( '--' ~[\r\n]* | '/*' .*? '*/' ) -> channel(HIDDEN)
    ;

WS : [ \t\r\n]+ -> channel(HIDDEN);

// --- Lexer rules for String built‑in functions ---
LENGTH: 'LENGTH';
SUBSTR: 'SUBSTR';
UPPER: 'UPPER';
LOWER: 'LOWER';
TRIM: 'TRIM';
LTRIM: 'LTRIM';
RTRIM: 'RTRIM';
REPLACE: 'REPLACE';
INSTR: 'INSTR';
LPAD: 'LPAD';
RPAD: 'RPAD';
SPLIT: 'SPLIT';
CONCAT: 'CONCAT';
REGEXP_REPLACE: 'REGEXP_REPLACE';
REGEXP_SUBSTR: 'REGEXP_SUBSTR';
REVERSE: 'REVERSE';
INITCAP: 'INITCAP';
LIKE: 'LIKE';

// --- Lexer Rules for Numeric Built‑In Functions ---

ABS: 'ABS';
CEIL: 'CEIL';
FLOOR: 'FLOOR';
ROUND: 'ROUND';
POWER: 'POWER';
SQRT: 'SQRT';
LOG: 'LOG';
EXP: 'EXP';
MOD: 'MOD';
SIGN: 'SIGN';
TRUNC: 'TRUNC';


// =======================
// Parser Rules
// =======================

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
    | return_statement
    | break_statement
    | expression_statement
    | SEMICOLON
    ;

break_statement
    : BREAK SEMICOLON
    ;

return_statement
    : RETURN expression SEMICOLON
    ;

expression_statement
    : expression SEMICOLON
    ;

execute_statement
    : EXECUTE variable_assignment LPAREN esql_query_content RPAREN (persist_clause)? SEMICOLON
    ;

variable_assignment
    : ID EQUAL
    ;

esql_query_content
    : ( . )*?  // Match any content non-greedily
    ;

declare_statement
    : DECLARE variable_declaration_list SEMICOLON
    ;

variable_declaration_list
    : variable_declaration (COMMA variable_declaration)*
    ;

variable_declaration
    : ID datatype (EQUAL expression)?
    ;

assignment_statement
    : SET ID EQUAL expression SEMICOLON
    ;

if_statement
    : IF condition THEN then_block+=statement+
        (elseif_block)*
        (ELSE else_block+=statement+)?
      END IF
      ;

elseif_block
    : ELSEIF condition THEN statement+
      ;

condition
    : expression
      ;

loop_statement
    : for_range_loop
    | for_array_loop
    | while_loop
    ;

for_range_loop
    : FOR ID IN range_loop_expression LOOP statement+ ENDLOOP
    ;

for_array_loop
    : FOR ID IN array_loop_expression LOOP statement+ ENDLOOP
    ;

while_loop
    : WHILE condition LOOP statement+ ENDLOOP
    ;

range_loop_expression
    : expression DOT_DOT expression
    ;

array_loop_expression
    : expression
    ;

try_catch_statement
    : TRY statement+ (CATCH statement+)? (FINALLY statement+)? ENDTRY
    ;

throw_statement
    : THROW STRING SEMICOLON
    ;

function_definition
    : FUNCTION ID LPAREN (parameter_list)? RPAREN BEGIN statement+ END FUNCTION
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

expression
    : logicalOrExpression
    ;

logicalOrExpression
    : logicalAndExpression (OR logicalAndExpression)*
    ;

logicalAndExpression
    : equalityExpression (AND equalityExpression)*
    ;

equalityExpression
    : relationalExpression ((EQUAL | NOT_EQUAL) relationalExpression)*
    ;

relationalExpression
    : additiveExpression ((LESS_THAN | GREATER_THAN | LESS_EQUAL | GREATER_EQUAL) additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpr ((MULTIPLY | DIVIDE) unaryExpr)*
    ;

unaryExpr
    : '-' unaryExpr
    | primaryExpression
    ;

arrayLiteral
    : '[' expressionList? ']'
    ;

expressionList
    : expression (COMMA expression)*
    ;

documentLiteral
    : '{' pairList? '}'
    ;

pairList
    : pair (COMMA pair)*
    ;

pair
    : (ID | STRING) COLON expression
    ;

primaryExpression
    : simplePrimaryExpression bracketExpression*
    ;

bracketExpression
    : '[' expression ']'
    ;

simplePrimaryExpression
    : LPAREN expression RPAREN
    | function_call
    | INT
    | FLOAT
    | STRING
    | arrayLiteral
    | documentLiteral
    | ID
    ;

datatype
    : INT_TYPE
    | FLOAT_TYPE
    | STRING_TYPE
    | DATE_TYPE
    | NUMBER_TYPE
    | array_datatype
    ;

array_datatype
    : ARRAY_TYPE 'OF' (NUMBER_TYPE | STRING_TYPE | DOCUMENT_TYPE | DATE_TYPE | ARRAY_TYPE )
    ;

persist_clause
    : PERSIST INTO ID
    ;
