/*
 * Copyright Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.escript.exceptions;

/**
 * The BreakException is a custom exception used to control loop execution flow.
 * When a BREAK statement is encountered within a loop, this exception is thrown
 * to exit the loop gracefully.
 */
public class BreakException extends RuntimeException {
    /**
     * Constructs a BreakException with the specified message.
     *
     * @param message The detail message.
     */
    public BreakException(String message) {
        super(message);
    }
}
