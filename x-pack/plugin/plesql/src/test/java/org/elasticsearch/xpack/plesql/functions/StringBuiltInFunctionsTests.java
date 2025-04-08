/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.builtin.StringBuiltInFunctions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class StringBuiltInFunctionsTests {

    private ExecutionContext context;

    @Before
    public void setup() {
        // Create a fresh global execution context
        context = new ExecutionContext();
        // Register all built-in functions
        StringBuiltInFunctions.registerAll(context);
    }

    @Test
    public void testLength() {
        BuiltInFunctionDefinition lengthFn = context.getBuiltInFunction("LENGTH");
        Object result = lengthFn.execute(Arrays.asList("hello world"));
        // "hello world" is 11 characters long
        assertEquals(11, result);
    }

    @Test
    public void testSubstr() {
        BuiltInFunctionDefinition substrFn = context.getBuiltInFunction("SUBSTR");
        Object result = substrFn.execute(Arrays.asList("hello world", 7, 5));
        // Expected substring is "world"
        assertEquals("world", result);
    }

    @Test
    public void testUpper() {
        BuiltInFunctionDefinition upperFn = context.getBuiltInFunction("UPPER");
        Object result = upperFn.execute(Arrays.asList("hello"));
        assertEquals("HELLO", result);
    }

    @Test
    public void testLower() {
        BuiltInFunctionDefinition lowerFn = context.getBuiltInFunction("LOWER");
        Object result = lowerFn.execute(Arrays.asList("HELLO"));
        assertEquals("hello", result);
    }

    @Test
    public void testTrim() {
        BuiltInFunctionDefinition trimFn = context.getBuiltInFunction("TRIM");
        Object result = trimFn.execute(Arrays.asList("  hello  "));
        assertEquals("hello", result);
    }

    @Test
    public void testLTrim() {
        BuiltInFunctionDefinition ltrimFn = context.getBuiltInFunction("LTRIM");
        Object result = ltrimFn.execute(Arrays.asList("   hello"));
        assertEquals("hello", result);
    }

    @Test
    public void testRTrim() {
        BuiltInFunctionDefinition rtrimFn = context.getBuiltInFunction("RTRIM");
        Object result = rtrimFn.execute(Arrays.asList("hello   "));
        assertEquals("hello", result);
    }

    @Test
    public void testReplace() {
        BuiltInFunctionDefinition replaceFn = context.getBuiltInFunction("REPLACE");
        Object result = replaceFn.execute(Arrays.asList("hello world", "world", "PLESQL"));
        assertEquals("hello PLESQL", result);
    }

    @Test
    public void testInstr() {
        BuiltInFunctionDefinition instrFn = context.getBuiltInFunction("INSTR");
        Object result = instrFn.execute(Arrays.asList("hello world", "world"));
        // Expecting 7 (1-indexed)
        assertEquals(7, result);
    }

    @Test
    public void testLPad() {
        BuiltInFunctionDefinition lpadFn = context.getBuiltInFunction("LPAD");
        Object result = lpadFn.execute(Arrays.asList("hello", 10, "*"));
        assertEquals("*****hello", result);
    }

    @Test
    public void testRPad() {
        BuiltInFunctionDefinition rpadFn = context.getBuiltInFunction("RPAD");
        Object result = rpadFn.execute(Arrays.asList("hello", 10, "*"));
        assertEquals("hello*****", result);
    }

    @Test
    public void testSplit() {
        BuiltInFunctionDefinition splitFn = context.getBuiltInFunction("SPLIT");
        Object result = splitFn.execute(Arrays.asList("a,b,c", ","));
        // Expected to return a list with ["a", "b", "c"]
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testConcat() {
        BuiltInFunctionDefinition concatFn = context.getBuiltInFunction("||");
        Object result = concatFn.execute(Arrays.asList("hello", " ", "world"));
        assertEquals("hello world", result);
    }

    @Test
    public void testRegexpReplace() {
        BuiltInFunctionDefinition regexpReplaceFn = context.getBuiltInFunction("REGEXP_REPLACE");
        Object result = regexpReplaceFn.execute(Arrays.asList("abc123def", "[0-9]+", ""));
        assertEquals("abcdef", result);
    }

    @Test
    public void testRegexpSubstr() {
        BuiltInFunctionDefinition regexpSubstrFn = context.getBuiltInFunction("REGEXP_SUBSTR");
        Object result = regexpSubstrFn.execute(Arrays.asList("abc123def", "[0-9]+"));
        assertEquals("123", result);
    }

    @Test
    public void testReverse() {
        BuiltInFunctionDefinition reverseFn = context.getBuiltInFunction("REVERSE");
        Object result = reverseFn.execute(Arrays.asList("hello"));
        assertEquals("olleh", result);
    }

    @Test
    public void testInitCap() {
        BuiltInFunctionDefinition initcapFn = context.getBuiltInFunction("INITCAP");
        Object result = initcapFn.execute(Arrays.asList("hello world"));
        assertEquals("Hello World", result);
    }
}
