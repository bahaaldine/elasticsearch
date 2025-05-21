/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.operators.string;

import org.elasticsearch.xpack.escript.operators.primitives.BinaryOperatorHandler;

/**
 * Handles string equality ('==').
 */
public class StringEqualityOperatorHandler implements BinaryOperatorHandler {

    @Override
    public boolean isApplicable(Object left, Object right) {
        return left instanceof String && right instanceof String;
    }

    @Override
    public Object apply(Object left, Object right) {
        // both left and right are strings (checked in isApplicable)
        return ((String) left).equals((String) right);
    }
}
