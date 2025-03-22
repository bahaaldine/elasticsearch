/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.functions.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.functions.NumberBuiltInFunctions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class NumberBuiltInFunctionsTests {

    private ExecutionContext context;

    @Before
    public void setup() {
        // Create a fresh global execution context.
        context = new ExecutionContext();
        // Register all built-in numeric functions.
        NumberBuiltInFunctions.registerAll(context);
    }

    @Test
    public void testAbs() {
        BuiltInFunctionDefinition absFn = context.getBuiltInFunction("ABS");
        Object result = absFn.execute(Arrays.asList(-123));
        assertEquals(123.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testCeil() {
        BuiltInFunctionDefinition ceilFn = context.getBuiltInFunction("CEIL");
        Object result = ceilFn.execute(Arrays.asList(3.2));
        assertEquals(4.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testFloor() {
        BuiltInFunctionDefinition floorFn = context.getBuiltInFunction("FLOOR");
        Object result = floorFn.execute(Arrays.asList(3.8));
        assertEquals(3.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testRoundWithoutScale() {
        BuiltInFunctionDefinition roundFn = context.getBuiltInFunction("ROUND");
        Object result = roundFn.execute(Arrays.asList(3.6));
        assertEquals(4.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testRoundWithScale() {
        BuiltInFunctionDefinition roundFn = context.getBuiltInFunction("ROUND");
        Object result = roundFn.execute(Arrays.asList(3.14159, 2));
        assertEquals(3.14, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testPower() {
        BuiltInFunctionDefinition powerFn = context.getBuiltInFunction("POWER");
        Object result = powerFn.execute(Arrays.asList(2, 3));
        assertEquals(8.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testSqrt() {
        BuiltInFunctionDefinition sqrtFn = context.getBuiltInFunction("SQRT");
        Object result = sqrtFn.execute(Arrays.asList(9));
        assertEquals(3.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testLogWithoutBase() {
        BuiltInFunctionDefinition logFn = context.getBuiltInFunction("LOG");
        Object result = logFn.execute(Arrays.asList(Math.E));
        assertEquals(1.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testLogWithBase() {
        BuiltInFunctionDefinition logFn = context.getBuiltInFunction("LOG");
        Object result = logFn.execute(Arrays.asList(100, 10));
        assertEquals(2.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testExp() {
        BuiltInFunctionDefinition expFn = context.getBuiltInFunction("EXP");
        Object result = expFn.execute(Arrays.asList(1));
        assertEquals(Math.exp(1), ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testMod() {
        BuiltInFunctionDefinition modFn = context.getBuiltInFunction("MOD");
        Object result = modFn.execute(Arrays.asList(10, 3));
        assertEquals(1.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testSignPositive() {
        BuiltInFunctionDefinition signFn = context.getBuiltInFunction("SIGN");
        Object result = signFn.execute(Arrays.asList(10));
        assertEquals(1, result);
    }

    @Test
    public void testSignNegative() {
        BuiltInFunctionDefinition signFn = context.getBuiltInFunction("SIGN");
        Object result = signFn.execute(Arrays.asList(-5));
        assertEquals(-1, result);
    }

    @Test
    public void testSignZero() {
        BuiltInFunctionDefinition signFn = context.getBuiltInFunction("SIGN");
        Object result = signFn.execute(Arrays.asList(0));
        assertEquals(0, result);
    }

    @Test
    public void testTruncWithoutScale() {
        BuiltInFunctionDefinition truncFn = context.getBuiltInFunction("TRUNC");
        Object result = truncFn.execute(Arrays.asList(3.9));
        assertEquals(3.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void testTruncWithScale() {
        BuiltInFunctionDefinition truncFn = context.getBuiltInFunction("TRUNC");
        Object result = truncFn.execute(Arrays.asList(3.14159, 2));
        // Should truncate to 3.14 without rounding
        assertEquals(3.14, ((Number) result).doubleValue(), 0.001);
    }
}
