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
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.builtin.types.DocumentBuiltInFunctions;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class DocumentBuiltInFunctionsTests extends ESTestCase {

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
        DocumentBuiltInFunctions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        // Properly terminate the thread pool to avoid thread leaks.
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testDocumentKeys() throws InterruptedException {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_KEYS");
        Map<String, Object> doc = new HashMap<>();
        doc.put("key1", 1);
        doc.put("key2", 2);
        CountDownLatch latch = new CountDownLatch(1);
        fn.execute(Arrays.asList(doc), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                List<?> keys = (List<?>) result;
                assertTrue(keys.contains("key1"));
                assertTrue(keys.contains("key2"));
                assertEquals(2, keys.size());
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

    @Test
    public void testDocumentValues() throws InterruptedException {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_VALUES");
        Map<String, Object> doc = new HashMap<>();
        doc.put("key1", 1);
        doc.put("key2", 2);
        CountDownLatch latch = new CountDownLatch(1);
        fn.execute(Arrays.asList(doc), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                List<?> values = (List<?>) result;
                assertTrue(values.contains(1));
                assertTrue(values.contains(2));
                assertEquals(2, values.size());
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

    @Test
    public void testDocumentGet() throws InterruptedException {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_GET");
        Map<String, Object> doc = new HashMap<>();
        doc.put("name", "value");
        CountDownLatch latch = new CountDownLatch(1);
        fn.execute(Arrays.asList(doc, "name"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals("value", result);
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

    @Test
    public void testDocumentMerge() throws InterruptedException {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_MERGE");
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("a", 1);
        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("b", 2);
        doc2.put("a", 3);
        CountDownLatch latch = new CountDownLatch(1);
        fn.execute(Arrays.asList(doc1, doc2), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                Map<?, ?> merged = (Map<?, ?>) result;
                assertEquals(3, merged.get("a"));
                assertEquals(2, merged.get("b"));
                assertEquals(2, merged.size());
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

    @Test
    public void testDocumentRemove() throws InterruptedException {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_REMOVE");
        Map<String, Object> doc = new HashMap<>();
        doc.put("a", 1);
        doc.put("b", 2);
        CountDownLatch latch = new CountDownLatch(1);
        fn.execute(Arrays.asList(doc, "a"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                Map<?, ?> newDoc = (Map<?, ?>) result;
                assertFalse(newDoc.containsKey("a"));
                assertTrue(newDoc.containsKey("b"));
                assertEquals(1, newDoc.size());
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

    @Test
    public void testDocumentContains() throws InterruptedException {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_CONTAINS");
        Map<String, Object> doc = new HashMap<>();
        doc.put("foo", "bar");
        CountDownLatch latch = new CountDownLatch(1);
        fn.execute(Arrays.asList(doc, "foo"), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertTrue((Boolean) result);
                // Nested asynchronous call for a negative case.
                fn.execute(Arrays.asList(doc, "baz"), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object result) {
                        assertFalse((Boolean) result);
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Execution failed in nested call: " + e.getMessage());
                        latch.countDown();
                    }
                });
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
