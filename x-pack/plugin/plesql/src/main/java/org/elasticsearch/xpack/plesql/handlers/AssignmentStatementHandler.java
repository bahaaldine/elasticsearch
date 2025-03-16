/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class AssignmentStatementHandler {
    private final ProcedureExecutor executor;

    public AssignmentStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles an assignment statement by evaluating the right-hand side expression,
     * coercing the result to match the declared type of the variable, and storing it.
     *
     * @param ctx The assignment statement context.
     * @param listener The listener to signal success/failure.
     */
    public void handleAsync(PlEsqlProcedureParser.Assignment_statementContext ctx, ActionListener<Object> listener) {
        // Retrieve the variable name
        String varName = ctx.ID().getText();
        PlEsqlProcedureParser.ExpressionContext expression = ctx.expression();

        // Check that the variable has been declared.
        if (executor.getContext().hasVariable(varName) == false) {
            throw new RuntimeException("Variable '" + varName + "' is not declared.");
        }

        ActionListener<Object> assignmentListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                try {
                    Object coercedValue = coerceType(value, varName);
                    executor.getContext().setVariable(varName, coercedValue);
                    listener.onResponse(null);
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> assignmentLogger =
            ActionListenerUtils.withLogging(assignmentListener, this.getClass().getName(), "AssignmentHandler: " + varName);

        executor.evaluateExpressionAsync(expression, assignmentLogger);
    }

    /**
     * Coerces the given value to the type declared for the variable.
     * For ARRAY types, if the evaluated value is a String, we try to parse it as a JSON array.
     *
     * @param value The evaluated value.
     * @param variableName The variable name.
     * @return The coerced value.
     */
    private Object coerceType(Object value, String variableName) {
        // Retrieve the declared type from the execution context.
        String varType = executor.getContext().getVariableType(variableName);
        if ("ARRAY".equalsIgnoreCase(varType)) {
            if (value instanceof java.util.List) {
                return value;
            } else if (value instanceof String) {
                String str = ((String) value).trim();
                try (XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                    .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                        new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)))) {
                    if (parser.nextToken() != XContentParser.Token.START_ARRAY) {
                        throw new RuntimeException("Expected a JSON array literal for ARRAY type");
                    }
                    return parser.list();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse JSON array for variable " + variableName, e);
                }
            } else {
                throw new RuntimeException("Cannot coerce value to ARRAY for variable " + variableName);
            }
        }
        // For other types, no coercion is performed.
        return value;
    }
}
