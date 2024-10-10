/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.handlers.TryCatchStatementHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TryCatchStatementHandlerTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private TryCatchStatementHandler handler;

    @Before
    public void setup() {
        context = new ExecutionContext();
        executor = new ProcedureExecutor(context, null);
        handler = new TryCatchStatementHandler(context, executor);
    }

    // Helper method to parse a TRY-CATCH block
    private PlEsqlProcedureParser.Try_catch_statementContext parseTryCatch(String query) {
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(query);
        // Assuming the last statement is the TRY-CATCH
        return blockContext.statement(blockContext.statement().size() - 1).try_catch_statement();
    }

    // Helper method to parse a BEGIN ... END block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();
    }

    // Test 1: Basic Try Block Execution Without Errors
    @Test
    public void testTryBlockExecution() {
        String blockQuery = "BEGIN DECLARE j INT; TRY SET j = 10; END TRY END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        executor.visitProcedure(blockContext);

        // Check that the variable 'j' is set to 10
        assertNotNull("j should be declared in context.", context.getVariable("j"));
        assertEquals(10, context.getVariable("j"));
    }

    // Test 2: Try-Catch Block Execution With an Error
    @Test
    public void testTryCatchBlockExecution() {
        String blockQuery = "BEGIN DECLARE j INT; TRY SET j = 10 / 0; CATCH SET j = 20; END TRY END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        executor.visitProcedure(blockContext);

        // Check if the CATCH block was executed
        Object jValue = context.getVariable("j");
        assertNotNull("Variable 'j' should be declared in the context.", jValue);
        assertEquals("The value of 'j' should be set to 20 by the CATCH block.", 20, jValue);
    }

    // Test 3: Try-Catch-Finally Block Execution With an Error
    @Test
    public void testTryCatchFinallyBlockExecution() {
        String blockQuery = "BEGIN  DECLARE j INT; TRY SET j = 10 / 0; CATCH SET j = 20; FINALLY SET j = 30; END TRY END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        executor.visitProcedure(blockContext);

        // Check that the variable 'j' is set to 30 (from the FINALLY block)
        assertNotNull("j should be declared in context.", context.getVariable("j"));
        assertEquals(30, context.getVariable("j"));
    }

    // Test 4: Try-Finally Block Execution Without Catch Block
    @Test
    public void testTryFinallyBlockExecution() {
        String blockQuery = "BEGIN DECLARE j INT; TRY SET j = 10; FINALLY SET j = 20; END TRY END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        executor.visitProcedure(blockContext);


        // Check that the variable 'j' is set to 20 (from the FINALLY block)
        assertNotNull("j should be declared in context.", context.getVariable("j"));
        assertEquals(20, context.getVariable("j"));
    }
}
