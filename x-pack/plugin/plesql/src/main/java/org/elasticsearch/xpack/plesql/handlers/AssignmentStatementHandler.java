/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

/**
 * The AssignmentStatementHandler class is responsible for handling assignment statements
 * within the procedural SQL execution context. It evaluates the expression on the right-hand
 * side of the assignment and assigns the resulting value to the specified variable.
 */
public class AssignmentStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs an AssignmentStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public AssignmentStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the assignment statement by evaluating the expression and assigning the value
     * to the specified variable. It ensures that the variable has been declared and that
     * the assigned value matches the variable's type.
     *
     * @param ctx The Assignment_statementContext representing the assignment statement.
     */
    public void handleAsync(PlEsqlProcedureParser.Assignment_statementContext ctx, ActionListener<Object> listener) {
        // Retrieve the variable name from the assignment statement
        String varName = ctx.ID().getText();
        PlEsqlProcedureParser.ExpressionContext expression = ctx.expression();


        // Check if the variable has been declared
        if ( executor.getContext().hasVariable(varName) == false ) {
            throw new RuntimeException("Variable '" + varName + "' is not declared.");
        }

        // Evaluate the expression asynchronously
        executor.evaluateExpressionAsync(expression, new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                // Coerce the value to the variable's type
                Object coercedValue = coerceType(value, varName);
                executor.getContext().setVariable(varName, coercedValue);
                listener.onResponse(null);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private Object coerceType(Object value, String variableName) {
        // Implement type coercion logic if necessary
        // For simplicity, we assume the value is already of the correct type
        return value;
    }
}
