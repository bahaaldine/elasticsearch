/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.plesql.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NumberBuiltInFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor dummyExecutor;
    private FunctionDefinitionHandler handler;
    private ThreadPool threadPool;
    private ProcedureExecutor executor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("array-test-pool");
        Client mockClient = null; // or a proper mock
        // Create a lexer with an empty source as a placeholder (adjust if needed)
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        // Initialize the FunctionDefinitionHandler as done in other test classes
        handler = new FunctionDefinitionHandler(executor);
        // Register the array built-in functions so that they are available for testing.
        NumberBuiltInFunctions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        // Properly terminate the thread pool to avoid thread leaks.
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testAbs() throws Exception {
        BuiltInFunctionDefinition absFn = context.getBuiltInFunction("ABS");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        absFn.execute(Arrays.asList(-123), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ABS failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(123.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testCeil() throws Exception {
        BuiltInFunctionDefinition ceilFn = context.getBuiltInFunction("CEIL");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        ceilFn.execute(Arrays.asList(3.2), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("CEIL failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(4.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testFloor() throws Exception {
        BuiltInFunctionDefinition floorFn = context.getBuiltInFunction("FLOOR");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        floorFn.execute(Arrays.asList(3.8), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("FLOOR failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(3.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testRoundWithoutScale() throws Exception {
        BuiltInFunctionDefinition roundFn = context.getBuiltInFunction("ROUND");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        roundFn.execute(Arrays.asList(3.6), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ROUND failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(4.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testRoundWithScale() throws Exception {
        BuiltInFunctionDefinition roundFn = context.getBuiltInFunction("ROUND");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        roundFn.execute(Arrays.asList(3.14159, 2), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ROUND with scale failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(3.14, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testPower() throws Exception {
        BuiltInFunctionDefinition powerFn = context.getBuiltInFunction("POWER");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        powerFn.execute(Arrays.asList(2, 3), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("POWER failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(8.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testSqrt() throws Exception {
        BuiltInFunctionDefinition sqrtFn = context.getBuiltInFunction("SQRT");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        sqrtFn.execute(Arrays.asList(9), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("SQRT failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(3.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testLogWithoutBase() throws Exception {
        BuiltInFunctionDefinition logFn = context.getBuiltInFunction("LOG");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        logFn.execute(Arrays.asList(Math.E), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("LOG failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(1.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testLogWithBase() throws Exception {
        BuiltInFunctionDefinition logFn = context.getBuiltInFunction("LOG");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        logFn.execute(Arrays.asList(100, 10), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("LOG with base failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(2.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testExp() throws Exception {
        BuiltInFunctionDefinition expFn = context.getBuiltInFunction("EXP");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        expFn.execute(Arrays.asList(1), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("EXP failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(Math.exp(1), ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testMod() throws Exception {
        BuiltInFunctionDefinition modFn = context.getBuiltInFunction("MOD");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        modFn.execute(Arrays.asList(10, 3), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("MOD failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(1.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testSignPositive() throws Exception {
        BuiltInFunctionDefinition signFn = context.getBuiltInFunction("SIGN");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        signFn.execute(Arrays.asList(10), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("SIGN (positive) failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(1, ((Number) resultHolder[0]).intValue());
    }

    @Test
    public void testSignNegative() throws Exception {
        BuiltInFunctionDefinition signFn = context.getBuiltInFunction("SIGN");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        signFn.execute(Arrays.asList(-5), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("SIGN (negative) failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(-1, ((Number) resultHolder[0]).intValue());
    }

    @Test
    public void testSignZero() throws Exception {
        BuiltInFunctionDefinition signFn = context.getBuiltInFunction("SIGN");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        signFn.execute(Arrays.asList(0), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("SIGN (zero) failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, ((Number) resultHolder[0]).intValue());
    }

    @Test
    public void testTruncWithoutScale() throws Exception {
        BuiltInFunctionDefinition truncFn = context.getBuiltInFunction("TRUNC");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        truncFn.execute(Arrays.asList(3.9), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("TRUNC without scale failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(3.0, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }

    @Test
    public void testTruncWithScale() throws Exception {
        BuiltInFunctionDefinition truncFn = context.getBuiltInFunction("TRUNC");
        CountDownLatch latch = new CountDownLatch(1);
        final Object[] resultHolder = new Object[1];
        truncFn.execute(Arrays.asList(3.14159, 2), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                resultHolder[0] = result;
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("TRUNC with scale failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        // Should truncate to 3.14 without rounding
        assertEquals(3.14, ((Number) resultHolder[0]).doubleValue(), 0.001);
    }
}
