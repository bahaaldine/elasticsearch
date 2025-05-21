/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.escript.handlers.ElasticScriptErrorListener;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ArrayBuiltInFunctionsTests extends ESTestCase {

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
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        // Initialize the FunctionDefinitionHandler as done in other test classes
        handler = new FunctionDefinitionHandler(executor);
        // Register the array built-in functions so that they are available for testing.
        ArrayBuiltInFunctions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        // Properly terminate the thread pool to avoid thread leaks.
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a BEGIN ... END block
    private ElasticScriptParser.ProcedureContext parseBlock(String blockQuery) {
        CharStream input = CharStreams.fromString(blockQuery);
        ElasticScriptLexer lexer = new ElasticScriptLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());

        return parser.procedure();
    }

    @Test
    public void testArrayLength() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3, 4);
        CountDownLatch latch = new CountDownLatch(1);
        // "ARRAY_LENGTH" built-in function is registered and should return the size of the array.
        handler.executeFunctionAsync("ARRAY_LENGTH", Arrays.asList(array), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("Array length should match", array.size(), ((Number) result).intValue());
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ARRAY_LENGTH function execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testArrayAppend() throws Exception {
        List<Object> array = new ArrayList<>(Arrays.asList(1, 2, 3));
        CountDownLatch latch = new CountDownLatch(1);
        // Append 4 to array.
        handler.executeFunctionAsync("ARRAY_APPEND", Arrays.asList(array, 4), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                assertTrue("Result of ARRAY_APPEND must be a List", result instanceof List);
                List<?> newArray = (List<?>) result;
                assertEquals("New array size should be 4", 4, newArray.size());
                assertEquals("Appended element should be 4", 4, newArray.get(3));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ARRAY_APPEND function execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testArrayPrepend() throws Exception {
        List<Object> array = new ArrayList<>(Arrays.asList(2, 3, 4));
        CountDownLatch latch = new CountDownLatch(1);
        // Prepend 1 to the array.
        handler.executeFunctionAsync("ARRAY_PREPEND", Arrays.asList(array, 1), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                assertTrue("Result of ARRAY_PREPEND must be a List", result instanceof List);
                List<?> newArray = (List<?>) result;
                assertEquals("New array size should be 4", 4, newArray.size());
                assertEquals("Prepend element should be 1", 1, newArray.get(0));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ARRAY_PREPEND function execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testArrayRemove() throws Exception {
        List<Object> array = new ArrayList<>(Arrays.asList(1, 2, 3, 2, 4));
        CountDownLatch latch = new CountDownLatch(1);
        // Remove all occurrences of element 2.
        handler.executeFunctionAsync("ARRAY_REMOVE", Arrays.asList(array, 2), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                assertTrue("Result of ARRAY_REMOVE must be a List", result instanceof List);
                List<?> newArray = (List<?>) result;
                // After removing all occurrences of 2: expected list [1,3,4]
                assertEquals("New array size should be 3", 3, newArray.size());
                assertFalse("New array should not contain 2", newArray.contains(2));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ARRAY_REMOVE function execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testArrayContains() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 3);
        CountDownLatch latch = new CountDownLatch(1);
        // Test if the array contains 2.
        handler.executeFunctionAsync("ARRAY_CONTAINS", Arrays.asList(array, 2), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                assertTrue("Result of ARRAY_CONTAINS must be a Boolean", result instanceof Boolean);
                assertTrue("Array should contain 2", (Boolean) result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ARRAY_CONTAINS function execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testArrayDistinct() throws Exception {
        List<Object> array = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        CountDownLatch latch = new CountDownLatch(1);
        handler.executeFunctionAsync("ARRAY_DISTINCT", Arrays.asList(array), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                assertTrue("Result of ARRAY_DISTINCT must be a List", result instanceof List);
                List<?> distinctArray = (List<?>) result;
                // Expected distinct elements are [1, 2, 3, 4]
                assertEquals("Distinct array should contain 4 elements", 4, distinctArray.size());
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("ARRAY_DISTINCT function execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }
}
