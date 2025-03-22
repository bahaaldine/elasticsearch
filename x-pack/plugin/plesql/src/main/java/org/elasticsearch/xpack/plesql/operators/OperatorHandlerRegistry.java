/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.operators;


import org.elasticsearch.xpack.plesql.operators.numeric.NumericAdditionOperatorHandler;
import org.elasticsearch.xpack.plesql.operators.numeric.NumericSubtractionOperatorHandler;
import org.elasticsearch.xpack.plesql.operators.primitives.BinaryOperatorHandler;
import org.elasticsearch.xpack.plesql.operators.string.StringConcatenationOperatorHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OperatorHandlerRegistry {
    private final Map<String, BinaryOperatorHandler> registry = new HashMap<>();

    public OperatorHandlerRegistry() {
        // For the "+" operator, we use a composite handler that first checks for string concatenation,
        // then numeric addition.
        registry.put("+", new CompositeAdditionOperatorHandler(Arrays.asList(
            new StringConcatenationOperatorHandler(),
            new NumericAdditionOperatorHandler()
        )));
        // For "-" operator, only numeric subtraction is supported.
        registry.put("-", new NumericSubtractionOperatorHandler());
        // You can register additional operators here (e.g., "*", "/", etc.).
    }

    public BinaryOperatorHandler getHandler(String operator) {
        BinaryOperatorHandler handler = registry.get(operator);
        if (handler == null) {
            throw new RuntimeException("Unsupported operator: " + operator);
        }
        return handler;
    }
}
