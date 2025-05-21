/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.operators.string;

import org.elasticsearch.xpack.escript.operators.primitives.BinaryOperatorHandler;

/**
 * Handles string lexicographic greater-than-or-equal ('>=').
 */
public class StringGreaterThanOrEqualOperatorHandler implements BinaryOperatorHandler {

    @Override
    public boolean isApplicable(Object left, Object right) {
        return left instanceof String && right instanceof String;
    }

    @Override
    public Object apply(Object left, Object right) {
        return ((String) left).compareTo((String) right) >= 0;
    }
}
