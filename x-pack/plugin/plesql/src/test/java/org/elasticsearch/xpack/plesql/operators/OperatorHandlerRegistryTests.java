/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.operators;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.plesql.operators.primitives.BinaryOperatorHandler;
import org.junit.Test;

import java.time.LocalDate;

public class OperatorHandlerRegistryTests extends ESTestCase {

    private OperatorHandlerRegistry registry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        registry = new OperatorHandlerRegistry();
    }

    @Test
    public void testStringConcatenationWithPlus() {
        BinaryOperatorHandler h = registry.getHandler("+");
        Object result = h.apply("Hello", "World");
        assertTrue(result instanceof String);
        assertEquals("HelloWorld", result);
    }

    @Test
    public void testStringEqualityAndInequality() {
        BinaryOperatorHandler eq = registry.getHandler("==");
        assertTrue(eq.isApplicable("a", "a"));
        assertFalse((Boolean) eq.apply("a", "b"));
        assertTrue((Boolean) eq.apply("foo", "foo"));

        BinaryOperatorHandler neq = registry.getHandler("<>");
        assertTrue(neq.isApplicable("a", "b"));
        assertTrue((Boolean) neq.apply("a", "b"));
        assertFalse((Boolean) neq.apply("foo", "foo"));
    }

    @Test
    public void testStringOrderingComparisons() {
        // '>' and '<' for strings
        BinaryOperatorHandler gt = registry.getHandler(">");
        assertTrue(gt.isApplicable("b", "a"));
        assertTrue((Boolean) gt.apply("b", "a"));
        assertFalse((Boolean) gt.apply("a", "b"));

        BinaryOperatorHandler lt = registry.getHandler("<");
        assertTrue((Boolean) lt.apply("a", "b"));
        assertFalse((Boolean) lt.apply("b", "a"));

        // '>=' and '<=' for strings
        BinaryOperatorHandler ge = registry.getHandler(">=");
        assertTrue((Boolean) ge.apply("b", "a"));
        assertTrue((Boolean) ge.apply("a", "a"));
        assertFalse((Boolean) ge.apply("a", "b"));

        BinaryOperatorHandler le = registry.getHandler("<=");
        assertTrue((Boolean) le.apply("a", "b"));
        assertTrue((Boolean) le.apply("a", "a"));
        assertFalse((Boolean) le.apply("b", "a"));
    }

    @Test
    public void testNumericArithmetic() {
        BinaryOperatorHandler plus = registry.getHandler("+");
        assertEquals(5.0, ((Number) plus.apply(2, 3)).doubleValue(), 1e-9);

        BinaryOperatorHandler minus = registry.getHandler("-");
        assertEquals(6.0, ((Number) minus.apply(10, 4)).doubleValue(), 1e-9);

        BinaryOperatorHandler mul = registry.getHandler("*");
        assertEquals(12.0, ((Number) mul.apply(3, 4)).doubleValue(), 1e-9);

        BinaryOperatorHandler div = registry.getHandler("/");
        assertEquals(0.5, ((Number) div.apply(2, 4)).doubleValue(), 1e-9);

        BinaryOperatorHandler mod = registry.getHandler("%");
        assertEquals(1.0, ((Number) mod.apply(10, 3)).doubleValue(), 1e-9);
    }

    @Test
    public void testNumericComparisons() {
        BinaryOperatorHandler gt = registry.getHandler(">");
        assertTrue((Boolean) gt.apply(5, 3));
        assertFalse((Boolean) gt.apply(3, 5));

        BinaryOperatorHandler lt = registry.getHandler("<");
        assertTrue((Boolean) lt.apply(3, 5));
        assertFalse((Boolean) lt.apply(5, 3));

        BinaryOperatorHandler ge = registry.getHandler(">=");
        assertTrue((Boolean) ge.apply(3, 3));
        assertTrue((Boolean) ge.apply(4, 3));
        assertFalse((Boolean) ge.apply(2, 3));

        BinaryOperatorHandler le = registry.getHandler("<=");
        assertTrue((Boolean) le.apply(3, 3));
        assertTrue((Boolean) le.apply(2, 3));
        assertFalse((Boolean) le.apply(4, 3));
    }

    @Test
    public void testLogicalOperators() {
        BinaryOperatorHandler land = registry.getHandler("AND");
        assertTrue((Boolean) land.apply(true, true));
        assertFalse((Boolean) land.apply(true, false));

        BinaryOperatorHandler lor = registry.getHandler("OR");
        assertTrue((Boolean) lor.apply(true, false));
        assertFalse((Boolean) lor.apply(false, false));
    }

    @Test
    public void testDateComparisons() {
        LocalDate d1 = LocalDate.of(2021, 1, 1);
        LocalDate d2 = LocalDate.of(2022, 6, 15);

        BinaryOperatorHandler eq = registry.getHandler("==");
        assertTrue((Boolean) eq.apply(d1, d1));
        assertFalse((Boolean) eq.apply(d1, d2));

        BinaryOperatorHandler neq = registry.getHandler("<>");
        assertTrue((Boolean) neq.apply(d1, d2));
        assertFalse((Boolean) neq.apply(d1, d1));

        BinaryOperatorHandler gt = registry.getHandler(">");
        assertTrue((Boolean) gt.apply(d2, d1));
        assertFalse((Boolean) gt.apply(d1, d2));

        BinaryOperatorHandler lt = registry.getHandler("<");
        assertTrue((Boolean) lt.apply(d1, d2));
        assertFalse((Boolean) lt.apply(d2, d1));

        BinaryOperatorHandler ge = registry.getHandler(">=");
        assertTrue((Boolean) ge.apply(d1, d1));
        assertTrue((Boolean) ge.apply(d2, d1));
        assertFalse((Boolean) ge.apply(d1, d2));

        BinaryOperatorHandler le = registry.getHandler("<=");
        assertTrue((Boolean) le.apply(d1, d1));
        assertTrue((Boolean) le.apply(d1, d2));
        assertFalse((Boolean) le.apply(d2, d1));
    }

    @Test(expected = RuntimeException.class)
    public void testUnsupportedOperatorThrows() {
        registry.getHandler("@");
    }
}
