/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
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
        Client mockClient = null; // or mock(Client.class);
        PlEsqlProcedureLexer lexer =
            new PlEsqlProcedureLexer(CharStreams.fromString("")); // empty source
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
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
        String blockQuery = """
            PROCEDURE dummy_procedure (INOUT x NUMBER)
                BEGIN RETURN 100;
            END PROCEDURE;
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be 100", 100.0, returnValue.getValue());
    }

    // Test 2: RETURN with a string
    @Test
    public void testProcedureReturnString() throws InterruptedException {
        String blockQuery = "" +
            "PROCEDURE dummy_procedure (INOUT x NUMBER) +" +
                "BEGIN RETURN 'Hello, PL|ESQL!'; " +
            "END PROCEDURE";
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be 'Hello, PL|ESQL!'", "Hello, PL|ESQL!", returnValue.getValue());
    }

    // Test 3: RETURN with arithmetic operation
    @Test
    public void testProcedureReturnArithmeticOperation() throws InterruptedException {
        String blockQuery = "" +
            "PROCEDURE dummy_procedure (INOUT x NUMBER) +" +
                "BEGIN RETURN 50 + 25; " +
            "END PROCEDURE";
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be 75", 75.0, returnValue.getValue());
    }

    // Test 4: RETURN with boolean comparison
    @Test
    public void testProcedureReturnBoolean() throws InterruptedException {
        String blockQuery = "" +
            "PROCEDURE dummy_procedure (INOUT x NUMBER) +" +
                "BEGIN RETURN 10 > 5; " +
            "END PROCEDURE";
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be true", true, returnValue.getValue());
    }

    // Test 5: RETURN inside IF condition
    @Test
    public void testProcedureReturnInsideIfCondition() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_procedure (INOUT x NUMBER)
                BEGIN
                    IF 1 == 1 THEN
                        RETURN 'Condition met';
                    END IF;
                    RETURN 'Condition not met';
                END PROCEDURE
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be 'Condition met'", "Condition met", returnValue.getValue());
    }

    // Test 6: RETURN inside a LOOP
    @Test
    public void testProcedureReturnInsideLoop() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_procedure (INOUT x NUMBER)
                BEGIN
                    DECLARE i NUMBER;
                    FOR i IN 1..5 LOOP
                        IF i == 3 THEN
                            RETURN 'Loop exited at 3';
                        END IF;
                    END LOOP;
                    RETURN 'Loop completed';
                END PROCEDURE
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be 'Loop exited at 3'", "Loop exited at 3", returnValue.getValue());
    }

    // Test 7: RETURN with nested functions
    @Test
    public void testProcedureReturnWithNestedFunctionCall() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_procedure (INOUT x NUMBER)
                BEGIN
                    FUNCTION add(a NUMBER, b NUMBER) BEGIN
                        RETURN a + b;
                    END FUNCTION;
                    RETURN add(5, 10);
                END PROCEDURE
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be 15", 15.0, returnValue.getValue());
    }

    // Test 9: RETURN after a TRY-CATCH block
    @Test
    public void testProcedureReturnAfterTryCatch() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_procedure (INOUT x NUMBER)
                BEGIN
                    TRY
                        THROW 'Error';
                    CATCH
                        RETURN 'Error handled';
                    END TRY;
                    RETURN 'No error';
                END PROCEDURE
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be Error handled", "Error handled", returnValue.getValue());
    }

    // Additional Test: Return inside IF block
    @Test
    public void testReturnInsideIfBlock() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_procedure (INOUT x NUMBER)
                BEGIN
                    DECLARE myVar NUMBER;
                    IF 1 == 1 THEN
                        RETURN 42;
                    END IF;
                    SET myVar = 100; -- This should never be executed
                END PROCEDURE
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

        assertNotNull("Result should not be null", resultHolder[0]);
        assertTrue("Result should be a ReturnValue instance", resultHolder[0] instanceof ReturnValue);

        ReturnValue returnValue = (ReturnValue) resultHolder[0];
        assertEquals("Returned value should be 42", 42.0, returnValue.getValue());

        // Verify that 'myVar' was not set to 100
        assertNull("Variable 'myVar' should not be set after RETURN.", context.getVariable("myVar"));
    }
}
