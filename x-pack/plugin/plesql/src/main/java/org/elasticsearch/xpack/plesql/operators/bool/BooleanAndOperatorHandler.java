/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.operators.bool;

import org.elasticsearch.xpack.plesql.operators.primitives.BinaryOperatorHandler;

/**
 * Handles boolean logical AND ('AND').
 */
public class BooleanAndOperatorHandler implements BinaryOperatorHandler {

    @Override
    public boolean isApplicable(Object left, Object right) {
        return left instanceof Boolean && right instanceof Boolean;
    }

    @Override
    public Object apply(Object left, Object right) {
        return ((Boolean) left) && ((Boolean) right);
    }
}
