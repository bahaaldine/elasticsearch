/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
    public void handleAsync(ElasticScriptParser.Assignment_statementContext ctx,
                             ActionListener<Object> listener) {
        // Extract the variable reference (ID plus optional bracket expressions)
        ElasticScriptParser.VarRefContext varRef = ctx.varRef();
        String baseName = varRef.ID().getText();
        List<ElasticScriptParser.BracketExpressionContext> brackets = varRef.bracketExpression();
        ElasticScriptParser.ExpressionContext expression = ctx.expression();

        // Check that the base variable has been declared.
        if (executor.getContext().hasVariable(baseName) == false) {
            listener.onFailure(new RuntimeException("Variable '" + baseName + "' is not declared."));
            return;
        }

        // Listener to handle the evaluated RHS value
        ActionListener<Object> valueListener = ActionListener.wrap(value -> {
            try {
                // Coerce to the declared type
                Object coerced = coerceType(value, baseName);

                if (brackets.isEmpty()) {
                    // Simple assignment: var = value
                    executor.getContext().setVariable(baseName, coerced);

                    listener.onResponse(null);
                } else {
                    // Evaluate all bracket expressions sequentially
                    evaluateBracketKeys(brackets, 0, new java.util.ArrayList<>(), ActionListener.wrap(keys -> {
                        Object current = executor.getContext().getVariable(baseName);
                        if ( (current instanceof java.util.Map) == false ) {
                            listener.onFailure(new RuntimeException("Variable '" + baseName + "' is not a document."));
                            return;
                        }

                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> doc = (java.util.Map<String, Object>) current;

                        // Walk through all keys except the last
                        for (int i = 0; i < keys.size() - 1; i++) {
                            String key = keys.get(i);
                            Object nested = doc.get(key);
                            if (nested == null) {
                                nested = new java.util.LinkedHashMap<>();
                                doc.put(key, nested);
                            }
                            if ( (nested instanceof java.util.Map) == false ) {
                                listener.onFailure(new RuntimeException("Intermediate path is not a document at key: '" + key + "'"));
                                return;
                            }
                            if (nested instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> nestedMap = (java.util.Map<String, Object>) nested;
                                doc = nestedMap;
                            } else {
                                listener.onFailure(new RuntimeException("Intermediate path is not a document at key: '" + key + "'"));
                                return;
                            }
                        }

                        // Final assignment
                        doc.put(keys.get(keys.size() - 1), coerced);
                        listener.onResponse(null);
                    }, listener::onFailure));
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
        }, listener::onFailure);

        // Wrap with logging and kick off the RHS evaluation
        ActionListener<Object> loggedListener =
            ActionListenerUtils.withLogging(valueListener, this.getClass().getName(), "Assignment: " + baseName);
        executor.evaluateExpressionAsync(expression, loggedListener);
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

    private void evaluateBracketKeys(List<ElasticScriptParser.BracketExpressionContext> brackets, int index,
                                      List<String> keys, ActionListener<List<String>> listener) {
        if (index == brackets.size()) {
            listener.onResponse(keys);
            return;
        }
        ElasticScriptParser.BracketExpressionContext be = brackets.get(index);
        executor.evaluateExpressionAsync(be.expression(), ActionListener.wrap(result -> {
            keys.add(result == null ? null : result.toString());
            evaluateBracketKeys(brackets, index + 1, keys, listener);
        }, listener::onFailure));
    }
}
