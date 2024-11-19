/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.handlers.ExecuteStatementHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class ExecuteStatementHandlerTests extends ESTestCase {

    private ProcedureExecutor executor;
    private ExecutionContext context;
    private ExecuteStatementHandler handler;
    private ThreadPool threadPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        executor = new ProcedureExecutor(context, threadPool);
        handler = new ExecuteStatementHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a query block (BEGIN ... END)
    private PlEsqlProcedureParser.ProcedureContext parseQueryBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();
    }

    // Test 1: Basic ESQL execution test within a query block
    @Test
    public void testExecuteEsqlStatement() throws InterruptedException {
        String queryBlock = """
            BEGIN
                EXECUTE result_of_query=(ROW a=1, b='foo' | WHERE a > 0 | KEEP b);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Ensure the query result is stored in the context
                assertNotNull("The result should be stored in the context.", executor.getContext().getVariable("result_of_query"));
                assertEquals("Mock ESQL result", executor.getContext().getVariable("result_of_query"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 2: Variable substitution within ESQL query in a block
    @Test
    public void testExecuteEsqlWithVariableSubstitution() throws InterruptedException {
        // Declare a variable in the context
        context.declareVariable("threshold", "INT");
        context.setVariable("threshold", 10);

        String queryBlock = """
            BEGIN
                EXECUTE result_of_query=(ROW a=1, b="foo" | WHERE a > threshold | KEEP b);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Ensure variable substitution happens and result is stored
                assertNotNull("The result should be stored in the context.", executor.getContext().getVariable("result_of_query"));
                assertEquals("Mock ESQL result", executor.getContext().getVariable("result_of_query"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 3: Execute multiple ESQL queries in a query block
    @Test
    public void testExecuteMultipleEsqlQueries() throws InterruptedException {
        String queryBlock = """
        BEGIN
            EXECUTE my_result=(ROW a=1 | KEEP a);
            EXECUTE another_result=(ROW b=2 | KEEP b);
        END
    """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Ensure that results are stored in user-defined variables
                assertNotNull("The result of the first query should be stored in the context.",
                    executor.getContext().getVariable("my_result"));
                assertEquals("Mock ESQL result", executor.getContext().getVariable("my_result"));

                assertNotNull("The result of the second query should be stored in the context.",
                    executor.getContext().getVariable("another_result"));
                assertEquals("Mock ESQL result", executor.getContext().getVariable("another_result"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 4: Handling empty ESQL query in a block
    @Test
    public void testExecuteEmptyEsqlQuery() throws InterruptedException {
        String queryBlock = """
            BEGIN
                EXECUTE ();
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                fail("Expected a RuntimeException for an empty ESQL query");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                // Expected exception
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 5: ESQL execution with nested queries
    @Test
    public void testExecuteNestedEsqlQueries() throws InterruptedException {
        String queryBlock = """
            BEGIN
               EXECUTE result_of_query=(ROW a=1, b=ROW(c=2) | WHERE a > 0 | KEEP b);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Ensure the nested query result is stored in the context
                assertNotNull("The result should be stored in the context.", executor.getContext().getVariable("result_of_query"));
                assertEquals("Mock ESQL result", executor.getContext().getVariable("result_of_query"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 6: ESQL execution with invalid query structure (optional)
    // @Test
    // public void testExecuteInvalidEsqlQuery() throws InterruptedException {
    //     String queryBlock = """
    //         BEGIN
    //             EXECUTE (ROW a=1 b=2 | KEEP a);  -- Missing comma between fields
    //         END
    //     """;
    //     PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

    //     CountDownLatch latch = new CountDownLatch(1);

    //     executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
    //         @Override
    //         public void onResponse(Object unused) {
    //             fail("Expected a RuntimeException due to invalid query structure");
    //             latch.countDown();
    //         }

    //         @Override
    //         public void onFailure(Exception e) {
    //             // Expected exception
    //             latch.countDown();
    //         }
    //     });

    //     latch.await();
    // }

    // Test 7: ESQL execution with multiple variables
    @Test
    public void testExecuteEsqlWithMultipleVariables() throws InterruptedException {
        // Declare multiple variables in the context
        context.declareVariable("threshold", "INT");
        context.declareVariable("limit", "INT");
        context.setVariable("threshold", 5);
        context.setVariable("limit", 10);

        String queryBlock = """
            BEGIN
                EXECUTE result_of_query=(ROW a=1, b="foo" | WHERE a > threshold | LIMIT limit);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseQueryBlock(queryBlock);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Ensure multiple variable substitution happens and result is stored
                assertNotNull("The result should be stored in the context.", executor.getContext().getVariable("result_of_query"));
                assertEquals("Mock ESQL result", executor.getContext().getVariable("result_of_query"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }
}
