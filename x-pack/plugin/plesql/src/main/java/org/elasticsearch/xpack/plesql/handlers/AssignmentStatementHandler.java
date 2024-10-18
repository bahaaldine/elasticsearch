/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

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
    public void handle(PlEsqlProcedureParser.Assignment_statementContext ctx) {
        // Retrieve the variable name from the assignment statement
        String varName = ctx.ID().getText();

        // Check if the variable has been declared
        if ( executor.getContext().hasVariable(varName) == false ) {
            throw new RuntimeException("Variable '" + varName + "' is not declared.");
        }

        // Evaluate the expression on the right-hand side of the assignment
        Object value = executor.evaluateExpression(ctx.expression());

        // Assign the evaluated value to the variable, enforcing type compatibility
        try {
            executor.getContext().setVariable(varName, value);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error assigning value to variable '" + varName + "': " + e.getMessage(), e);
        }
    }
}
