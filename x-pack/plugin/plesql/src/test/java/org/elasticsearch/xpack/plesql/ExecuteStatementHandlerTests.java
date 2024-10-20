/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.handlers.ExecuteStatementHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecuteStatementHandlerTests {

    private ProcedureExecutor executor;
    private ExecutionContext context;
    private ExecuteStatementHandler handler;

    @Before
    public void setup() {
        context = new ExecutionContext();
        executor = new ProcedureExecutor(context);
        handler = new ExecuteStatementHandler(executor);
    }

    // Helper method to parse a query block (BEGIN ... END)
    private PlEsqlProcedureParser.ProcedureContext parseQueryBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();
    }

    // Test 1: Basic ESQL execution test within a query block
    @Test
    public void testExecuteEsqlStatement() {
        String queryBlock = """
            BEGIN
                EXECUTE result_of_query=(ROW a=1, b='foo' | WHERE a > 0 | KEEP b);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        executor.visitProcedure(blockContext);

        // Ensure the query result is stored in the context
        assertNotNull("The result should be stored in the context.", executor.getContext().getVariable("result_of_query"));
        assertEquals("Mock ESQL result", executor.getContext().getVariable("result_of_query"));
    }

    // Test 2: Variable substitution within ESQL query in a block
    @Test
    public void testExecuteEsqlWithVariableSubstitution() {
        // Declare a variable in the context
        executor.getContext().declareVariable("threshold", "INT");
        executor.getContext().setVariable("threshold", 10);

        String queryBlock = """
            BEGIN
                EXECUTE result_of_query=(ROW a=1, b="foo" | WHERE a > threshold | KEEP b);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        executor.visitProcedure(blockContext);

        // Ensure variable substitution happens and result is stored
        assertNotNull("The result should be stored in the context.", executor.getContext().getVariable("result_of_query"));
        assertEquals("Mock ESQL result", executor.getContext().getVariable("result_of_query"));
    }

    // Test 3: Execute multiple ESQL queries in a query block
    @Test
    public void testExecuteMultipleEsqlQueries() {
        String queryBlock = """
        BEGIN
            EXECUTE my_result=(ROW a=1 | KEEP a);
            EXECUTE another_result=(ROW b=2 | KEEP b);
        END
    """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        executor.visitProcedure(blockContext);

        // Ensure that results are stored in user-defined variables
        assertNotNull("The result of the first query should be stored in the context.",
            executor.getContext().getVariable("my_result"));
        assertEquals("Mock ESQL result", executor.getContext().getVariable("my_result"));

        assertNotNull("The result of the second query should be stored in the context.",
            executor.getContext().getVariable("another_result"));
        assertEquals("Mock ESQL result", executor.getContext().getVariable("another_result"));
    }

    // Test 4: Handling empty ESQL query in a block
    @Test(expected = RuntimeException.class)
    public void testExecuteEmptyEsqlQuery() {
        String queryBlock = """
            BEGIN
                EXECUTE ();
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        // This should throw a RuntimeException for an empty ESQL query
        executor.visitProcedure(blockContext);
    }

    // Test 5: ESQL execution with nested queries
    @Test
    public void testExecuteNestedEsqlQueries() {
        String queryBlock = """
            BEGIN
               EXECUTE result_of_query=(ROW a=1, b=ROW(c=2) | WHERE a > 0 | KEEP b);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        executor.visitProcedure(blockContext);

        // Ensure the nested query result is stored in the context
        assertNotNull("The result should be stored in the context.", executor.getContext().getVariable("result_of_query"));
        assertEquals("Mock ESQL result", executor.getContext().getVariable("result_of_query"));
    }

    // Test 6: ESQL execution with invalid query structure
//    @Test(expected = RuntimeException.class)
//    public void testExecuteInvalidEsqlQuery() {
//        String queryBlock = """
//            BEGIN
//                EXECUTE (ROW a=1 b=2 | KEEP a);  -- Missing comma between fields
//            END
//        """;
//        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);
//
//        // Expecting a RuntimeException due to invalid query structure
//        executor.visitProcedure(blockContext);
//    }

    // Test 7: ESQL execution with multiple variables
    @Test
    public void testExecuteEsqlWithMultipleVariables() {
        // Declare multiple variables in the context
        executor.getContext().declareVariable("threshold", "INT");
        executor.getContext().declareVariable("limit", "INT");
        executor.getContext().setVariable("threshold", 5);
        executor.getContext().setVariable("limit", 10);

        String queryBlock = """
            BEGIN
                EXECUTE result_of_query=(ROW a=1, b="foo" | WHERE a > threshold | LIMIT limit);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        executor.visitProcedure(blockContext);

        // Ensure multiple variable substitution happens and result is stored
        assertNotNull("The result should be stored in the context.", executor.getContext().getVariable("result_of_query"));
        assertEquals("Mock ESQL result", executor.getContext().getVariable("result_of_query"));
    }
}
