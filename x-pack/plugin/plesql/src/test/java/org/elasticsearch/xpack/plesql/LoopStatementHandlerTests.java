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
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LoopStatementHandlerTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;

    @Before
    public void setup() {
        context = new ExecutionContext();  // Real ExecutionContext
        threadPool = new TestThreadPool("test-thread-pool");
        executor = new ProcedureExecutor(context, threadPool);  // Use real ProcedureExecutor with thread pool
    }

    @After
    public void tearDown() throws InterruptedException {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
    }

    // Helper method to parse a BEGIN ... END block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();  // Return the parsed block
    }

    // Test 1: Simple FOR loop
    @Test
    public void testSimpleForLoop() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE j INT, i INT; FOR i IN 1..3 LOOP SET j = i + 1; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'j' is set to 4 at the end of the loop
                assertNotNull("j should be declared.", context.getVariable("j"));
                assertEquals(4, context.getVariable("j"));  // After incrementing 3 times (1, 2, 3 -> j = i + 1)
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

    // Test 2: Simple WHILE loop
    @Test
    public void testSimpleWhileLoop() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE i INT = 1; WHILE i < 4 LOOP SET i = i + 1; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'i' is set to 4 after the loop terminates
                assertNotNull("i should be declared.", context.getVariable("i"));
                assertEquals(4, context.getVariable("i"));  // Increment until i < 4 becomes false
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

    // Test 3: Reverse FOR loop with iterations
    @Test
    public void testReverseForLoop() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE j INT = 0, i INT; FOR i IN 5..3 LOOP SET j = j + i; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // The loop should iterate from 5 down to 3
                assertNotNull("j should be declared.", context.getVariable("j"));
                assertEquals(5 + 4 + 3, context.getVariable("j"));  // Sum of 5, 4, and 3
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

    // Test 4: WHILE loop with false initial condition
    @Test
    public void testWhileLoopFalseInitialCondition() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE i INT = 5; WHILE i < 4 LOOP SET i = i + 1; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // The loop should not run because the condition is false initially
                assertNotNull("i should be declared.", context.getVariable("i"));
                assertEquals(5, context.getVariable("i"));  // No loop iteration
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

    // Test 5: Nested FOR loop
    @Test
    public void testNestedForLoop() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE i INT; DECLARE j INT; FOR i IN 1..2 LOOP FOR j " +
            "IN 1..2 LOOP SET j = j + 1; END LOOP END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'i' and 'j' are properly updated
                assertNotNull("i should be declared.", context.getVariable("i"));
                assertNotNull("j should be declared.", context.getVariable("j"));
                assertEquals(2, context.getVariable("i"));  // 'i' will be 2 after the loop
                assertEquals(3, context.getVariable("j"));  // 'j' will be 3 after the loop (incremented twice in inner loop)
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

    // Test 6: Infinite WHILE loop (with break condition)
    @Test
    public void testInfiniteWhileLoop() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE i INT = 1; WHILE 1 = 1 LOOP SET i = i + 1; IF i > 1000 THEN BREAK; END IF; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // 'i' should not exceed 1000, and the loop should break when i > 1000
                assertNotNull("i should be declared.", context.getVariable("i"));
                assertEquals(1001, context.getVariable("i"));
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
