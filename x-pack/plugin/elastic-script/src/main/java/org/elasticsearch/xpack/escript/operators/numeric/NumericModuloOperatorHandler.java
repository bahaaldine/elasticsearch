/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.operators.numeric;

import org.elasticsearch.xpack.escript.operators.primitives.BinaryOperatorHandler;

/**
 * Handles numeric modulo ('%').
 */
public class NumericModuloOperatorHandler implements BinaryOperatorHandler {

    @Override
    public boolean isApplicable(Object left, Object right) {
        return left instanceof Number && right instanceof Number;
    }

    @Override
    public Object apply(Object left, Object right) {
        double l = ((Number) left).doubleValue();
        double r = ((Number) right).doubleValue();
        return l % r;
    }
}
