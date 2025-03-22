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
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class TryCatchStatementHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private TryCatchStatementHandler handler;
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
        return parser.procedure();
    }

    // Test 1: Basic Try Block Execution Without Errors
    @Test
    public void testTryBlockExecution() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE j NUMBER; TRY SET j = 10; END TRY END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that the variable 'j' is set to 10
                assertNotNull("j should be declared in context.", context.getVariable("j"));
                assertEquals(10.0, context.getVariable("j"));
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

    // Test 2: Try-Catch Block Execution With an Error
    @Test
    public void testTryCatchBlockExecution() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE j NUMBER; TRY SET j = 10 / 0; CATCH SET j = 20; END TRY END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check if the CATCH block was executed
                Object jValue = context.getVariable("j");
                assertNotNull("Variable 'j' should be declared in the context.", jValue);
                assertEquals("The value of 'j' should be set to 20 by the CATCH block.", 20.0, jValue);
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

    // Test 3: Try-Catch-Finally Block Execution With an Error
    @Test
    public void testTryCatchFinallyBlockExecution() throws InterruptedException {
        String blockQuery = "BEGIN  DECLARE j NUMBER; TRY SET j = 10 / 0; CATCH SET j = 20; FINALLY SET j = 30; END TRY END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that the variable 'j' is set to 30 (from the FINALLY block)
                assertNotNull("j should be declared in context.", context.getVariable("j"));
                assertEquals(30.0, context.getVariable("j"));
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

    // Test 4: Try-Finally Block Execution Without Catch Block
    @Test
    public void testTryFinallyBlockExecution() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE j NUMBER; TRY SET j = 10; FINALLY SET j = 20; END TRY END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that the variable 'j' is set to 20 (from the FINALLY block)
                assertNotNull("j should be declared in context.", context.getVariable("j"));
                assertEquals(20.0, context.getVariable("j"));
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
