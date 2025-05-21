/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.operators.numeric;

import org.elasticsearch.xpack.escript.operators.primitives.BinaryOperatorHandler;

/**
 * Handles numeric equality ('==') by comparing double values.
 */
public class NumericEqualityOperatorHandler implements BinaryOperatorHandler {

    @Override
    public boolean isApplicable(Object left, Object right) {
        return left instanceof Number && right instanceof Number;
    }

    @Override
    public Object apply(Object left, Object right) {
        double leftVal = ((Number) left).doubleValue();
        double rightVal = ((Number) right).doubleValue();
        return leftVal == rightVal;
    }
}
