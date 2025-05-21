/*
 * Copyright Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.escript.primitives;

/**
 * The ReturnValue class is a custom exception used to handle RETURN statements
 * within function executions. It carries the value to be returned by the function.
 */
public class ReturnValue extends RuntimeException {
    private final Object value;

    /**
     * Constructs a ReturnValue exception with the specified return value.
     *
     * @param value The value to be returned by the function.
     */
    public ReturnValue(Object value) {
        this.value = value;
    }

    /**
     * Retrieves the return value carried by this exception.
     *
     * @return The return value.
     */
    public Object getValue() {
        return value;
    }
}
