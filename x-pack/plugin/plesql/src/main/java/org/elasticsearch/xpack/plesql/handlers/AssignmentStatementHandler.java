/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

public class AssignmentStatementHandler {
    private ExecutionContext context;

    public AssignmentStatementHandler(ExecutionContext context) {
        this.context = context;
    }

    public void handle(PlEsqlProcedureParser.Assignment_statementContext ctx) {
        String varName = ctx.ID().getText();
        Object value = evaluateExpression(ctx.expression());
        context.setVariable(varName, value);
    }

    private Object evaluateExpression(PlEsqlProcedureParser.ExpressionContext ctx) {
        if (ctx.INT() != null) {
            return Integer.parseInt(ctx.INT().getText());
        } else if (ctx.FLOAT() != null) {
            return Double.parseDouble(ctx.FLOAT().getText());
        } else if (ctx.STRING() != null) {
            // Remove surrounding quotes
            return ctx.STRING().getText().substring(1, ctx.STRING().getText().length() - 1);
        } else if (ctx.ID() != null && ctx.getChildCount() == 1) {
            return context.getVariable(ctx.ID().getText());
        } else if (ctx.op != null) {
            Object left = evaluateExpression(ctx.expression(0));
            Object right = evaluateExpression(ctx.expression(1));
            switch (ctx.op.getType()) {
                case PlEsqlProcedureParser.PLUS:
                    return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                case PlEsqlProcedureParser.MINUS:
                    return ((Number) left).doubleValue() - ((Number) right).doubleValue();
                case PlEsqlProcedureParser.MULTIPLY:
                    return ((Number) left).doubleValue() * ((Number) right).doubleValue();
                case PlEsqlProcedureParser.DIVIDE:
                    return ((Number) left).doubleValue() / ((Number) right).doubleValue();
                default:
                    throw new RuntimeException("Unknown operator: " + ctx.op.getText());
            }
        } else if (ctx.function_call() != null) {
            // TODO: Handle function calls if necessary
            return null; // Placeholder
        } else {
            throw new RuntimeException("Unsupported expression: " + ctx.getText());
        }
    }
}
