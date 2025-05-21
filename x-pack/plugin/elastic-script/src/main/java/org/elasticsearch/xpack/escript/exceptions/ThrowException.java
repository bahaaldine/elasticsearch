/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.exceptions;

/**
 * Custom exception to handle THROW statements.
 */
public class ThrowException extends RuntimeException {
    /**
     * Constructs a ThrowException with the specified error message.
     *
     * @param message The error message.
     */
    public ThrowException(String message) {
        super(message);
    }
}
