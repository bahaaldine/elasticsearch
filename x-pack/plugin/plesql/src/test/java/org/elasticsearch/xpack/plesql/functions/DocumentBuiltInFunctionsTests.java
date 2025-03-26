/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.functions.builtin.DocumentBuiltInFunctions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DocumentBuiltInFunctionsTests {

    private ExecutionContext context;

    @Before
    public void setup() {
        // Create a fresh global execution context.
        context = new ExecutionContext();
        // Register all built-in document functions.
        DocumentBuiltInFunctions.registerAll(context);
    }

    @Test
    public void testDocumentKeys() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_KEYS");
        Map<String, Object> doc = new HashMap<>();
        doc.put("key1", 1);
        doc.put("key2", 2);
        Object result = fn.execute(Arrays.asList(doc));
        List<?> keys = (List<?>) result;
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertEquals(2, keys.size());
    }

    @Test
    public void testDocumentValues() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_VALUES");
        Map<String, Object> doc = new HashMap<>();
        doc.put("key1", 1);
        doc.put("key2", 2);
        Object result = fn.execute(Arrays.asList(doc));
        List<?> values = (List<?>) result;
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertEquals(2, values.size());
    }

    @Test
    public void testDocumentGet() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_GET");
        Map<String, Object> doc = new HashMap<>();
        doc.put("name", "value");
        Object result = fn.execute(Arrays.asList(doc, "name"));
        assertEquals("value", result);
    }

    @Test
    public void testDocumentMerge() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_MERGE");
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("a", 1);
        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("b", 2);
        doc2.put("a", 3);
        Object result = fn.execute(Arrays.asList(doc1, doc2));
        Map<?, ?> merged = (Map<?, ?>) result;
        assertEquals(3, merged.get("a"));
        assertEquals(2, merged.get("b"));
        assertEquals(2, merged.size());
    }

    @Test
    public void testDocumentRemove() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_REMOVE");
        Map<String, Object> doc = new HashMap<>();
        doc.put("a", 1);
        doc.put("b", 2);
        Object result = fn.execute(Arrays.asList(doc, "a"));
        Map<?, ?> newDoc = (Map<?, ?>) result;
        assertFalse(newDoc.containsKey("a"));
        assertTrue(newDoc.containsKey("b"));
        assertEquals(1, newDoc.size());
    }

    @Test
    public void testDocumentContains() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("DOCUMENT_CONTAINS");
        Map<String, Object> doc = new HashMap<>();
        doc.put("foo", "bar");
        Object result = fn.execute(Arrays.asList(doc, "foo"));
        assertTrue((Boolean) result);
        result = fn.execute(Arrays.asList(doc, "baz"));
        assertFalse((Boolean) result);
    }
}
