/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.builtin.ArrayBuiltInFunctions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArrayBuiltInFunctionsTests {

    private ExecutionContext context;

    @Before
    public void setup() {
        // Create a fresh global ExecutionContext
        context = new ExecutionContext();
        // Register all built-in array functions
        ArrayBuiltInFunctions.registerAll(context);
    }

    @Test
    public void testArrayLength() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("ARRAY_LENGTH");
        // Test with an array of size 3
        Object result = fn.execute(Arrays.asList(Arrays.asList("a", "b", "c")));
        assertEquals(3, result);
    }

    @Test
    public void testArrayAppend() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("ARRAY_APPEND");
        List<String> array = Arrays.asList("a", "b");
        Object result = fn.execute(Arrays.asList(array, "c"));
        // The result should be a new list equal to ["a", "b", "c"]
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testArrayPrepend() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("ARRAY_PREPEND");
        List<String> array = Arrays.asList("b", "c");
        Object result = fn.execute(Arrays.asList(array, "a"));
        // Expected: ["a", "b", "c"]
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testArrayRemove() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("ARRAY_REMOVE");
        List<String> array = Arrays.asList("a", "b", "a", "c");
        Object result = fn.execute(Arrays.asList(array, "a"));
        // Expected: remove all occurrences of "a", resulting in ["b", "c"]
        assertEquals(Arrays.asList("b", "c"), result);
    }

    @Test
    public void testArrayContains() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("ARRAY_CONTAINS");
        List<String> array = Arrays.asList("a", "b", "c");
        Object result = fn.execute(Arrays.asList(array, "b"));
        assertTrue((Boolean) result);
        result = fn.execute(Arrays.asList(array, "x"));
        assertFalse((Boolean) result);
    }

    @Test
    public void testArrayDistinct() {
        BuiltInFunctionDefinition fn = context.getBuiltInFunction("ARRAY_DISTINCT");
        List<String> array = Arrays.asList("a", "b", "a", "c", "b");
        Object result = fn.execute(Arrays.asList(array));
        // Expected: first occurrence order preserved: ["a", "b", "c"]
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }
}
