/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.operators;

import org.elasticsearch.xpack.escript.operators.bool.BooleanAndOperatorHandler;
import org.elasticsearch.xpack.escript.operators.bool.BooleanOrOperatorHandler;
import org.elasticsearch.xpack.escript.operators.date.DateEqualityOperatorHandler;
import org.elasticsearch.xpack.escript.operators.date.DateGreaterThanOperatorHandler;
import org.elasticsearch.xpack.escript.operators.date.DateGreaterThanOrEqualOperatorHandler;
import org.elasticsearch.xpack.escript.operators.date.DateInequalityOperatorHandler;
import org.elasticsearch.xpack.escript.operators.date.DateLessThanOperatorHandler;
import org.elasticsearch.xpack.escript.operators.date.DateLessThanOrEqualOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericAdditionOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericDivisionOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericGreaterThanOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericGreaterThanOrEqualOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericLessThanOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericLessThanOrEqualOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericModuloOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericMultiplicationOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericSubtractionOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericEqualityOperatorHandler;
import org.elasticsearch.xpack.escript.operators.numeric.NumericInequalityOperatorHandler;
import org.elasticsearch.xpack.escript.operators.primitives.BinaryOperatorHandler;
import org.elasticsearch.xpack.escript.operators.string.StringConcatenationOperatorHandler;
import org.elasticsearch.xpack.escript.operators.string.StringEqualityOperatorHandler;
import org.elasticsearch.xpack.escript.operators.string.StringGreaterThanOperatorHandler;
import org.elasticsearch.xpack.escript.operators.string.StringGreaterThanOrEqualOperatorHandler;
import org.elasticsearch.xpack.escript.operators.string.StringInequalityOperatorHandler;
import org.elasticsearch.xpack.escript.operators.string.StringLessThanOperatorHandler;
import org.elasticsearch.xpack.escript.operators.string.StringLessThanOrEqualOperatorHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OperatorHandlerRegistry {
    private final Map<String, BinaryOperatorHandler> registry = new HashMap<>();

    public OperatorHandlerRegistry() {
        registry.put(
            "+",
            new CompositeOperatorHandler(Arrays.asList(
                new StringConcatenationOperatorHandler(),
                new NumericAdditionOperatorHandler()
            ))
        );

        // '-' → numeric subtract
        registry.put("-", new NumericSubtractionOperatorHandler());

        // '==' → string or numeric equals
        registry.put("==", new CompositeOperatorHandler(Arrays.asList(
            new DateEqualityOperatorHandler(),
            new StringEqualityOperatorHandler(),
            new NumericEqualityOperatorHandler()
        )));

        // '<>' → string or numeric not‐equals
        registry.put("<>", new CompositeOperatorHandler(Arrays.asList(
            new DateInequalityOperatorHandler(),
            new StringInequalityOperatorHandler(),
            new NumericInequalityOperatorHandler()
        )));


        // '*' → numeric multiply
        registry.put("*", new NumericMultiplicationOperatorHandler());
        // '/' → numeric divide
        registry.put("/", new NumericDivisionOperatorHandler());
        // '%' → numeric modulo
        registry.put("%", new NumericModuloOperatorHandler());

        // '>'  → string or numeric greater‑than
        registry.put(">", new CompositeOperatorHandler(Arrays.asList(
            new DateGreaterThanOperatorHandler(),
            new StringGreaterThanOperatorHandler(),
            new NumericGreaterThanOperatorHandler()
        )));

        // '<'  → string or numeric less‑than
        registry.put("<", new CompositeOperatorHandler(Arrays.asList(
            new DateLessThanOperatorHandler(),
            new StringLessThanOperatorHandler(),
            new NumericLessThanOperatorHandler()
        )));

        // '>=' → string or numeric greater‑than‑or‑equal
        registry.put(">=", new CompositeOperatorHandler(Arrays.asList(
            new DateGreaterThanOrEqualOperatorHandler(),
            new StringGreaterThanOrEqualOperatorHandler(),
            new NumericGreaterThanOrEqualOperatorHandler()
        )));

        // '<=' → string or numeric less‑than‑or‑equal
        registry.put("<=", new CompositeOperatorHandler(Arrays.asList(
            new DateLessThanOrEqualOperatorHandler(),
            new StringLessThanOrEqualOperatorHandler(),
            new NumericLessThanOrEqualOperatorHandler()
        )));

        // Boolean logical operators
        registry.put("AND", new BooleanAndOperatorHandler());
        registry.put("OR",  new BooleanOrOperatorHandler());
    }

    public BinaryOperatorHandler getHandler(String operator) {
        BinaryOperatorHandler handler = registry.get(operator);
        if (handler == null) {
            throw new RuntimeException("Unsupported operator: " + operator);
        }
        return handler;
    }
}
