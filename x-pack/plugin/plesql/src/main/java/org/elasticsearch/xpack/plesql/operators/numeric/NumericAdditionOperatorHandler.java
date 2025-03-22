/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.operators.numeric;

import org.elasticsearch.xpack.plesql.operators.primitives.BinaryOperatorHandler;

public class NumericAdditionOperatorHandler implements BinaryOperatorHandler {
    @Override
    public boolean isApplicable(Object left, Object right) {
        return left instanceof Number && right instanceof Number;
    }

    @Override
    public Object apply(Object left, Object right) {
        double leftVal = ((Number) left).doubleValue();
        double rightVal = ((Number) right).doubleValue();
        return leftVal + rightVal;
    }
}
