/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.junit.Before;
import org.junit.Test;

import static org.elasticsearch.xpack.plesql.TestUtils.parseProcedure;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class ProcedureReturnStatementTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;

    @Before
    public void setup() {
        context = new ExecutionContext();
        executor = new ProcedureExecutor(context);
    }

    // Helper method to parse a BEGIN ... END block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();  // Return the parsed block
    }

    // Test 1: Simple RETURN with integer
    @Test
    public void testProcedureReturnInteger() {
        String blockQuery = "BEGIN RETURN 100; END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);
        assertEquals(100, result);
    }

    // Test 2: RETURN with a string
    @Test
    public void testProcedureReturnString() {
        String blockQuery = "BEGIN RETURN 'Hello, PL|ESQL!'; END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);
        assertEquals("Hello, PL|ESQL!", result);
    }

    // Test 3: RETURN with arithmetic operation
    @Test
    public void testProcedureReturnArithmeticOperation() {
        String blockQuery = "BEGIN RETURN 50 + 25; END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);
        assertEquals(75, result);
    }

    // Test 4: RETURN with boolean comparison
    @Test
    public void testProcedureReturnBoolean() {
        String blockQuery = "BEGIN RETURN 10 > 5; END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);
        assertEquals(true, result);
    }

    // Test 5: RETURN inside IF condition
    @Test
    public void testProcedureReturnInsideIfCondition() {
        String blockQuery = """
            BEGIN
                IF 1 = 1 THEN
                    RETURN 'Condition met';
                END IF;
                RETURN 'Condition not met';
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);
        assertEquals("Condition met", result);
    }

    // Test: Return inside IF block
    @Test
    public void testReturnInsideIfBlock() {
        String blockQuery = """
            BEGIN
                DECLARE myVar INT;
                IF 1 = 1 THEN
                    RETURN 42;
                END IF;
                SET myVar = 100; -- This should never be executed
            END
        """;

        // Execute the procedure and catch the return value
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);

        // Validate that the correct return value was returned
        assertEquals(42, result);
    }

    // Test 6: RETURN inside a LOOP
    @Test
    public void testProcedureReturnInsideLoop() {
        String blockQuery = """
            BEGIN
                DECLARE i INT;
                FOR i IN 1..5 LOOP
                    IF i = 3 THEN
                        RETURN 'Loop exited at 3';
                    END IF;
                END LOOP;
                RETURN 'Loop completed';
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);
        assertEquals("Loop exited at 3", result);
    }

    // Test 7: RETURN with nested functions
    @Test
    public void testProcedureReturnWithNestedFunctionCall() {
        String blockQuery = """
            BEGIN
                FUNCTION add(a INT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                RETURN add(5, 10);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);
        assertEquals(15, result);
    }

    // Test 8: RETURN with ESQL query execution
    @Test
    public void testProcedureReturnWithEsqlQuery() {
        String blockQuery = """
            BEGIN
                EXECUTE result = (ROW a=10 | KEEP a);
                RETURN result;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        Object result = executor.getContext().getVariable("result");
        assertNotNull(result);
        assertEquals("Mock ESQL result", result);  // Assuming the mock for ESQL query is returning this
    }

    // Test 9: RETURN after a TRY-CATCH block
    @Test
    public void testProcedureReturnAfterTryCatch() {
        String blockQuery = """
            BEGIN
                TRY
                    THROW 'Error';
                CATCH
                    RETURN 'Error handled';
                END TRY;
                RETURN 'No error';
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        Object result = executor.visitProcedure(blockContext);

        assertNotNull(result);
        assertEquals("Error handled", result);
    }
}
