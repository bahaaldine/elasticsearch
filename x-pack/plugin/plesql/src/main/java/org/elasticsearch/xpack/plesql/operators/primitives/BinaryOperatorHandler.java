/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.operators.primitives;

public interface BinaryOperatorHandler {
    /**
     * Determines whether this handler is applicable for the given operands.
     *
     * @param left  The left operand.
     * @param right The right operand.
     * @return true if applicable, false otherwise.
     */
    boolean isApplicable(Object left, Object right);

    /**
     * Applies the binary operator to the given operands.
     *
     * @param left  The left operand.
     * @param right The right operand.
     * @return The result of the operation.
     */
    Object apply(Object left, Object right);
}
