/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.operators.string;

import org.elasticsearch.xpack.plesql.operators.primitives.BinaryOperatorHandler;

/**
 * Handles string inequality {@literal <>}.
 */
public class StringInequalityOperatorHandler implements BinaryOperatorHandler {

    @Override
    public boolean isApplicable(Object left, Object right) {
        return left instanceof String && right instanceof String;
    }

    @Override
    public Object apply(Object left, Object right) {
        String leftStr = (String) left;
        String rightStr = (String) right;
        // Return true if strings are not equal
        return leftStr.equals(rightStr) == false;
    }
}
