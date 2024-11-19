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
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class ProcedureReturnStatementTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        executor = new ProcedureExecutor(context, threadPool);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a BEGIN ... END block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();  // Return the parsed block
    }

    // Test 1: Simple RETURN with integer
    @Test
    public void testProcedureReturnInteger() throws InterruptedException {
        String blockQuery = "BEGIN RETURN 100; END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals(100, resultHolder[0]);
    }

    // Test 2: RETURN with a string
    @Test
    public void testProcedureReturnString() throws InterruptedException {
        String blockQuery = "BEGIN RETURN 'Hello, PL|ESQL!'; END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals("Hello, PL|ESQL!", resultHolder[0]);
    }

    // Test 3: RETURN with arithmetic operation
    @Test
    public void testProcedureReturnArithmeticOperation() throws InterruptedException {
        String blockQuery = "BEGIN RETURN 50 + 25; END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals(75, resultHolder[0]);
    }

    // Test 4: RETURN with boolean comparison
    @Test
    public void testProcedureReturnBoolean() throws InterruptedException {
        String blockQuery = "BEGIN RETURN 10 > 5; END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals(true, resultHolder[0]);
    }

    // Test 5: RETURN inside IF condition
    @Test
    public void testProcedureReturnInsideIfCondition() throws InterruptedException {
        String blockQuery = """
                BEGIN
                    IF 1 = 1 THEN
                        RETURN 'Condition met';
                    END IF;
                    RETURN 'Condition not met';
                END
            """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals("Condition met", resultHolder[0]);
    }

    // Test 6: RETURN inside a LOOP
    @Test
    public void testProcedureReturnInsideLoop() throws InterruptedException {
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

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals("Loop exited at 3", resultHolder[0]);
    }

    // Test 7: RETURN with nested functions
    @Test
    public void testProcedureReturnWithNestedFunctionCall() throws InterruptedException {
        String blockQuery = """
                BEGIN
                    FUNCTION add(a INT, b INT) BEGIN
                        RETURN a + b;
                    END FUNCTION;
                    RETURN add(5, 10);
                END
            """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals(15, resultHolder[0]);
    }

    // Test 8: RETURN with ESQL query execution
    @Test
    public void testProcedureReturnWithEsqlQuery() throws InterruptedException {
        String blockQuery = """
                BEGIN
                    EXECUTE result = (ROW a=10 | KEEP a);
                    RETURN result;
                END
            """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Assuming the mock for ESQL query returns "Mock ESQL result"
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals("Mock ESQL result", resultHolder[0]);
    }

    // Test 9: RETURN after a TRY-CATCH block
    @Test
    public void testProcedureReturnAfterTryCatch() throws InterruptedException {
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

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals("Error handled", resultHolder[0]);
    }

    // Additional Test: Return inside IF block
    @Test
    public void testReturnInsideIfBlock() throws InterruptedException {
        String blockQuery = """
                BEGIN
                    DECLARE myVar INT;
                    IF 1 = 1 THEN
                        RETURN 42;
                    END IF;
                    SET myVar = 100; -- This should never be executed
                END
            """;

        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        final Object[] resultHolder = new Object[1];

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();

        assertNotNull(resultHolder[0]);
        assertEquals(42, resultHolder[0]);

        // Verify that 'myVar' was not set to 100
        assertNull("Variable 'myVar' should not be set after RETURN.", context.getVariable("myVar"));
    }
}
