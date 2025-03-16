/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License 2.0.
 */
package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class LoopStatementHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // For tests, use null or a mocked client.
        // Create a dummy lexer to provide a token stream; the actual source is provided per test.
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a BEGIN ... END procedure block.
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();  // returns the procedure parse tree.
    }

    // Test 1: Simple FOR-range loop.
    @Test
    public void testSimpleForRangeLoop() throws InterruptedException {
        String blockQuery =
            "BEGIN DECLARE j NUMBER, i NUMBER; " +
                "FOR i IN 1..3 LOOP " +
                " SET j = i + 1; " +
                "END LOOP " +
                "END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("j should be declared", context.getVariable("j"));
                // For i = 3, j = 3 + 1 = 4.
                assertEquals(4.0, context.getVariable("j"));
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

    // Test 2: Simple WHILE loop.
    @Test
    public void testSimpleWhileLoop() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE i NUMBER = 1; WHILE i < 4 LOOP SET i = i + 1; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("i should be declared", context.getVariable("i"));
                // i increments until 4 is reached.
                assertEquals(4.0, context.getVariable("i"));
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

    // Test 3: Reverse FOR-range loop.
    @Test
    public void testReverseForRangeLoop() throws InterruptedException {
        String blockQuery = "BEGIN DECLARE j NUMBER = 0, i NUMBER; FOR i IN 5..3 LOOP SET j = j + i; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("j should be declared", context.getVariable("j"));
                // Sum of 5, 4, 3 equals 12.
                assertEquals(12.0, context.getVariable("j"));
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

    // Test 4: For-array loop (new rule). Iterates over an array literal.
    @Test
    public void testForArrayLoop() throws InterruptedException {
        // In this test, we declare an array and iterate over its elements.
        // We compute the sum of the elements.
        String blockQuery =
            "BEGIN " +
                "DECLARE sum NUMBER = 0; " +
                "DECLARE arr ARRAY = [10, 20, 30]; " +
                "FOR element IN arr LOOP " +
                " SET sum = sum + element; " +
                "END LOOP " +
                "RETURN sum; " +
                "END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // We expect the procedure to return 60.
                Object sumValue = context.getVariable("sum");
                assertNotNull("sum should be declared", sumValue);
                assertEquals(60.0, ((Number) sumValue).doubleValue(), 0.001);
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

    // Test 5: Nested FOR loop.
    @Test
    public void testNestedForLoop() throws InterruptedException {
        String blockQuery =
            "BEGIN " +
                "DECLARE i NUMBER; DECLARE j NUMBER; " +
                "FOR i IN 1..2 LOOP " +
                " FOR j IN 1..2 LOOP " +
                "  SET j = j + 1; " +
                " END LOOP " +
                "END LOOP " +
                "END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertNotNull("i should be declared", context.getVariable("i"));
                assertNotNull("j should be declared", context.getVariable("j"));
                // Outer loop runs twice so i should be 2.
                assertEquals(2.0, context.getVariable("i"));
                // Inner loop increments j: for each iteration, j becomes j+1.
                // With two iterations, if j started at 0 then j should be 2+1=3.
                assertEquals(3.0, context.getVariable("j"));
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
/*
    // Test 6: Infinite WHILE loop with break condition.
    @Test
    public void testInfiniteWhileLoopWithBreak() throws InterruptedException {
        String blockQuery =
            "BEGIN " +
                "DECLARE i NUMBER = 1; " +
                "WHILE 1 = 1 LOOP " +
                " SET i = i + 1; " +
                " IF i > 1000 THEN BREAK; END IF; " +
                "END LOOP " +
                "RETURN i; " +
                "END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Expect i to be 1001
                Object iValue = context.getVariable("i");
                assertNotNull("i should be declared", iValue);
                assertEquals(1001.0, ((Number) iValue).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }*/
}
