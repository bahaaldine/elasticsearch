/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IfStatementHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private PlEsqlProcedureParser parser;
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
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String blockQuery) {
        CharStream input = CharStreams.fromString(blockQuery);
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new PlEsqlErrorListener());

        return parser.procedure();
    }

    // Test 1: Simple IF statement with a true condition
    @Test
    public void testSimpleIfTrueCondition() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE myVar NUMBER; IF 1 = 1 THEN SET myVar = 10; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("myVar should be declared.", context.getVariable("myVar"));
                assertEquals(10.0, context.getVariable("myVar"));
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

    // Test 2: Simple IF statement with a false condition
    @Test
    public void testSimpleIfFalseCondition() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE myVar NUMBER; IF 1 = 2 THEN SET myVar = 10; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'myVar' is not set in the context
                assertNull("myVar should not be set.", context.getVariable("myVar"));
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

    // Test 3: IF-ELSE statement with false IF and true ELSE
    @Test
    public void testIfElseStatement() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE myVar NUMBER; IF 1 = 2 THEN SET myVar = 10; ELSE SET myVar = 20; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'myVar' is set to 20 (from ELSE branch)
                assertEquals(20.0, context.getVariable("myVar"));
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

    // Test 4: IF-ELSEIF-ELSE statement
    @Test
    public void testIfElseIfElseStatement() throws InterruptedException {
        String blockQuery =
            "BEGIN " +
                "DECLARE myVar NUMBER; " +
                "IF 1 = 2 THEN " +
                    "SET myVar = 10; " +
                "ELSEIF 1 = 1 THEN " +
                    "SET myVar = 20; " +
                "ELSE " +
                    "SET myVar = 30; " +
                "END IF; " +
            "END";

        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'myVar' is set to 20 (from ELSEIF branch)
                assertEquals(20.0, context.getVariable("myVar"));
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

    // Test 5: Arithmetic operations in IF condition
    @Test
    public void testArithmeticInIfCondition() throws Exception {
        String blockQuery = "BEGIN DECLARE myVar NUMBER; IF 5 + 5 = 10 THEN SET myVar = 10; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CompletableFuture<Void> future = new CompletableFuture<>();

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                try {
                    Object varValue = context.getVariable("myVar");
                    System.out.println("Retrieved 'myVar' value: " + varValue);
                    assertNotNull("Variable 'myVar' should have been set.", varValue);
                    assertEquals("Variable 'myVar' should be set to 10.", 10.0, varValue);
                    future.complete(null);
                } catch (AssertionError e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                future.completeExceptionally(new AssertionError("Execution failed: " + e.getMessage()));
            }
        });

        // Await the CompletableFuture with a timeout to prevent indefinite blocking
        future.get(5, TimeUnit.SECONDS);
    }

    // Test 6: Nested IF statement
    @Test
    public void testNestedIfStatement() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE myVar NUMBER; IF 1 = 1 THEN IF 2 = 2 THEN SET myVar = 10; END IF END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'myVar' is set to 10 in the context
                assertEquals(10.0, context.getVariable("myVar"));
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

    // Test 7: IF statement with comparison operators
    @Test
    public void testIfStatementWithComparisonOperators() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE myVar NUMBER; IF 5 > 3 THEN SET myVar = 10; END IF END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'myVar' is set to 10 in the context
                assertEquals(10.0, context.getVariable("myVar"));
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
