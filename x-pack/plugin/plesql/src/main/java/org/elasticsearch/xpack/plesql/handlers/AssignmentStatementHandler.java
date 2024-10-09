/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

public class AssignmentStatementHandler {
    private ExecutionContext context;
    private ProcedureExecutor executor;

    public AssignmentStatementHandler(ExecutionContext context, ProcedureExecutor executor) {
        this.context = context;
        this.executor = executor;
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
            String str = ctx.STRING().getText();
            return str.substring(1, str.length() - 1);
        } else if (ctx.ID() != null && ctx.getChildCount() == 1) {
            return context.getVariable(ctx.ID().getText());
        } else if (ctx.op != null) {
            Object left = evaluateExpression(ctx.expression(0));
            Object right = evaluateExpression(ctx.expression(1));

            // Handle arithmetic based on operand types
            if (left instanceof Integer && right instanceof Integer) {
                return handleIntegerArithmetic((Integer) left, (Integer) right, ctx.op.getType());
            } else {
                return handleFloatingPointArithmetic(((Number) left).doubleValue(), ((Number) right).doubleValue(), ctx.op.getType());
            }
        } else if (ctx.function_call() != null) {
            return executor.visitFunction_call(ctx.function_call());
        } else if (ctx.LPAREN() != null && ctx.RPAREN() != null) {
            return evaluateExpression(ctx.expression(0));
        } else {
            throw new RuntimeException("Unsupported expression: " + ctx.getText());
        }
    }

    // Helper methods to handle different arithmetic types
    private Object handleIntegerArithmetic(int left, int right, int operator) {
        switch (operator) {
            case PlEsqlProcedureParser.PLUS:
                return left + right;
            case PlEsqlProcedureParser.MINUS:
                return left - right;
            case PlEsqlProcedureParser.MULTIPLY:
                return left * right;
            case PlEsqlProcedureParser.DIVIDE:
                if (right == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                return left / right;  // This will return an integer result
            default:
                throw new RuntimeException("Unknown operator for integers: " + operator);
        }
    }

    private Object handleFloatingPointArithmetic(double left, double right, int operator) {
        switch (operator) {
            case PlEsqlProcedureParser.PLUS:
                return left + right;
            case PlEsqlProcedureParser.MINUS:
                return left - right;
            case PlEsqlProcedureParser.MULTIPLY:
                return left * right;
            case PlEsqlProcedureParser.DIVIDE:
                if (right == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                return left / right;
            default:
                throw new RuntimeException("Unknown operator for floating point: " + operator);
        }
    }
}
