/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class IfStatementHandlerTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private PlEsqlProcedureParser parser;


    @Before
    public void setup() {
        context = new ExecutionContext();  // Real ExecutionContext
        executor = new ProcedureExecutor(context);  // Use real ProcedureExecutor
    }

    // Helper method to parse a BEGIN ... END block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String blockQuery) {
        CharStream input = CharStreams.fromString(blockQuery);
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();
    }

    // Helper method to parse an IF statement
    private PlEsqlProcedureParser.If_statementContext parseIfStatement(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.if_statement();
    }

    // Test 1: Simple IF statement with a true condition
    @Test
    public void testSimpleIfTrueCondition() {
        String blockQuery = "BEGIN DECLARE myVar INT; IF 1 = 1 THEN SET myVar = 10; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        executor.visitProcedure(blockContext);
        assertNotNull("myVar should be declared.", context.getVariable("myVar"));
        assertEquals(10, context.getVariable("myVar"));
    }

    // Test 2: Simple IF statement with a false condition
    @Test
    public void testSimpleIfFalseCondition() {
        // Setup an IF statement with a false condition: IF 1 = 2 THEN SET myVar = 10; ENDIF;
        String blockQuery = "BEGIN DECLARE myVar INT; IF 1 = 2 THEN SET myVar = 10; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        executor.visitProcedure(blockContext);

        // Check that 'myVar' is not set in the context
        assertNull(context.getVariable("myVar"));
    }

    // Test 3: IF-ELSE statement with false IF and true ELSE
    @Test
    public void testIfElseStatement() {
        // Setup an IF-ELSE statement: IF 1 = 2 THEN SET myVar = 10; ELSE SET myVar = 20; ENDIF;
        String blockQuery = "BEGIN DECLARE myVar INT; IF 1 = 2 THEN SET myVar = 10; ELSE SET myVar = 20; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        executor.visitProcedure(blockContext);

        // Check that 'myVar' is set to 20 (from ELSE branch)
        assertEquals(20, context.getVariable("myVar"));
    }

    @Test
    public void testParseIfElseIfElse() {
        String blockQuery = "BEGIN DECLARE myVar INT; IF 1 = 2 THEN SET myVar = 10; " +
            "ELSEIF 1 = 1 THEN SET myVar = 20; ELSE SET myVar = 30; END IF END";

        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        // Use a parse tree walker or print the tree for inspection
        System.out.println(blockContext.toStringTree(parser));
    }

    private void printTokens(String input) {
        CharStream charStream = CharStreams.fromString(input);
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        for (Token token : tokens.getTokens()) {
            System.out.println(token.getText() + " : " + PlEsqlProcedureLexer.VOCABULARY.getSymbolicName(token.getType()));
        }
    }

    // Test 4: IF-ELSEIF-ELSE statement
    @Test
    public void testIfElseIfElseStatement() {
        // Setup an IF-ELSEIF-ELSE statement: IF 1 = 2 THEN SET myVar = 10; ELSEIF 1 = 1 THEN SET myVar = 20; ELSE SET myVar = 30; END IF; END;
        String blockQuery = "BEGIN DECLARE myVar INT; IF 1 = 2 THEN SET myVar = 10; " +
            "ELSEIF 1 = 1 THEN SET myVar = 20; ELSE SET myVar = 30; END IF; END;";

        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'myVar' is set to 20 (from ELSEIF branch)
        assertEquals(20, context.getVariable("myVar"));
    }

    // Test 5: Arithmetic operations in IF condition
    @Test
    public void testArithmeticInIfCondition() {
        String blockQuery = "BEGIN DECLARE myVar INT; IF 5 + 5 = 10 THEN SET myVar = 10; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'myVar' is set to 10
        assertEquals(10, context.getVariable("myVar"));
    }

    // Test 6: Nested IF statement
    @Test
    public void testNestedIfStatement() {
        // Setup a nested IF statement: IF 1 = 1 THEN IF 2 = 2 THEN SET myVar = 10; ENDIF; ENDIF;
        String blockQuery = "BEGIN DECLARE myVar INT; IF 1 = 1 THEN IF 2 = 2 THEN SET myVar = 10; END IF END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'myVar' is set to 10 in the context
        assertEquals(10, context.getVariable("myVar"));
    }

    // Test 7: IF statement with comparison operators
    @Test
    public void testIfStatementWithComparisonOperators() {
        // Simplified comparison expression
        String blockQuery = "BEGIN DECLARE myVar INT; IF 5 > 3 THEN SET myVar = 10; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'myVar' is set to 10 in the context
        assertEquals(10, context.getVariable("myVar"));
    }
}
