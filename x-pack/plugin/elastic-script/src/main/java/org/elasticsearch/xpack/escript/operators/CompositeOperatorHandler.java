/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.operators;

import org.elasticsearch.xpack.escript.operators.primitives.BinaryOperatorHandler;

import java.util.List;

public class CompositeOperatorHandler implements BinaryOperatorHandler {
    private final List<BinaryOperatorHandler> handlers;

    public CompositeOperatorHandler(List<BinaryOperatorHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public boolean isApplicable(Object left, Object right) {
        for (BinaryOperatorHandler handler : handlers) {
            if (handler.isApplicable(left, right)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object apply(Object left, Object right) {
        for (BinaryOperatorHandler handler : handlers) {
            if (handler.isApplicable(left, right)) {
                return handler.apply(left, right);
            }
        }
        throw new RuntimeException("No applicable addition handler found for operands: " + left + ", " + right);
    }
}
