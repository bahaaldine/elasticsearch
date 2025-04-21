/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

/**
 * The ThrowStatementHandler class is responsible for handling THROW statements
 * within the procedural SQL execution context. It extracts the exception message
 * from a string literal and throws a RuntimeException to propagate the error.
 */
public class ThrowStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs a ThrowStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public ThrowStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the THROW statement asynchronously by extracting the exception message
     * from a string literal and invoking the listener's onFailure method.
     *
     * @param ctx      The Throw_statementContext representing the THROW statement.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void handleAsync(PlEsqlProcedureParser.Throw_statementContext ctx, ActionListener<Object> listener) {
        try {
            if (ctx.STRING() == null) {
                listener.onFailure(new RuntimeException("THROW statement requires a string message."));
                return;
            }

            // Extract the raw string with quotes
            String rawMessage = ctx.STRING().getText();

            // Remove the surrounding single quotes and unescape any escaped quotes
            String exceptionMessage = unescapeString(rawMessage.substring(1, rawMessage.length() - 1));

            // Invoke the listener's onFailure method with the exception
            listener.onFailure(new RuntimeException(exceptionMessage));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Utility method to unescape escaped single quotes in a string.
     *
     * @param input The input string with potential escaped characters.
     * @return The unescaped string.
     */
    private String unescapeString(String input) {
        if (input == null) {
            return null;
        }
        // Replace escaped single quotes with actual single quotes
        return input.replace("\\'", "'");
    }
}
