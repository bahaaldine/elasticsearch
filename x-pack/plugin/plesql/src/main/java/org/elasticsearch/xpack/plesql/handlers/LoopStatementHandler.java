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

public class LoopStatementHandler {
    private ExecutionContext context;
    private ProcedureExecutor executor;

    public LoopStatementHandler(ExecutionContext context, ProcedureExecutor executor) {
        this.context = context;
        this.executor = executor;
    }

    public void handle(PlEsqlProcedureParser.Loop_statementContext ctx) {
        if (ctx.FOR() != null) {
            // Handle FOR loop
            String varName = ctx.ID().getText();
            Object startValue = evaluateExpression(ctx.expression(0));
            Object endValue = evaluateExpression(ctx.expression(1));

            if (!(startValue instanceof Number) || !(endValue instanceof Number)) {
                throw new RuntimeException("FOR loop bounds must be numeric.");
            }

            int start = (Integer) startValue;
            int end = (Integer) endValue;

            if (start <= end) {
                // Ascending order
                for (int i = start; i <= end; i++) {
                    context.setVariable(varName, i);
                    for (PlEsqlProcedureParser.StatementContext stmtCtx : ctx.statement()) {
                        executor.visit(stmtCtx);
                    }
                }
            } else {
                // Descending order
                for (int i = start; i >= end; i--) {
                    context.setVariable(varName, i);
                    for (PlEsqlProcedureParser.StatementContext stmtCtx : ctx.statement()) {
                        executor.visit(stmtCtx);
                    }
                }
            }

            // Remove the loop variable after the loop finishes
            //context.getVariables().remove(varName);
        } else if (ctx.WHILE() != null) {
            // Handle WHILE loop
            while (evaluateCondition(ctx.condition())) {
                for (PlEsqlProcedureParser.StatementContext stmtCtx : ctx.statement()) {
                    executor.visit(stmtCtx);
                }
            }
        }
    }

    private boolean evaluateCondition(PlEsqlProcedureParser.ConditionContext ctx) {
        Object left = evaluateExpression(ctx.expression(0));
        Object right = evaluateExpression(ctx.expression(1));
        String operator = ctx.comparison_operator().getText();

        switch (operator) {
            case "=":
            case "==":
                return left.equals(right);
            case "!=":
                return !left.equals(right);
            case "<":
                return compareValues(left, right) < 0;
            case ">":
                return compareValues(left, right) > 0;
            case "<=":
                return compareValues(left, right) <= 0;
            case ">=":
                return compareValues(left, right) >= 0;
            default:
                throw new RuntimeException("Unknown comparison operator: " + operator);
        }
    }

    private int compareValues(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();
            return Double.compare(leftVal, rightVal);
        } else if (left instanceof String && right instanceof String) {
            return ((String) left).compareTo((String) right);
        } else {
            throw new RuntimeException("Cannot compare values of types: " + left.getClass() + " and " + right.getClass());
        }
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
            // Just return the value as is
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
                    if (((Number) right).doubleValue() == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    return ((Number) left).doubleValue() / ((Number) right).doubleValue();
                default:
                    throw new RuntimeException("Unknown operator: " + ctx.op.getText());
            }
        } else if (ctx.function_call() != null) {
            return executor.visitFunction_call(ctx.function_call());
        } else if (ctx.LPAREN() != null && ctx.RPAREN() != null) {
            return evaluateExpression(ctx.expression(0));
        } else {
            throw new RuntimeException("Unsupported expression: " + ctx.getText());
        }
    }

    // Arithmetic operations helper methods
    private Object addValues(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            if (left instanceof Double || right instanceof Double) {
                return ((Number) left).doubleValue() + ((Number) right).doubleValue();
            } else {
                return ((Number) left).intValue() + ((Number) right).intValue();
            }
        } else if (left instanceof String || right instanceof String) {
            return left.toString() + right.toString();
        } else {
            throw new RuntimeException("Cannot add values of types: " + left.getClass() + " and " + right.getClass());
        }
    }

    private Object subtractValues(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            if (left instanceof Double || right instanceof Double) {
                return ((Number) left).doubleValue() - ((Number) right).doubleValue();
            } else {
                return ((Number) left).intValue() - ((Number) right).intValue();
            }
        } else {
            throw new RuntimeException("Cannot subtract values of types: " + left.getClass() + " and " + right.getClass());
        }
    }

    private Object multiplyValues(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            if (left instanceof Double || right instanceof Double) {
                return ((Number) left).doubleValue() * ((Number) right).doubleValue();
            } else {
                return ((Number) left).intValue() * ((Number) right).intValue();
            }
        } else {
            throw new RuntimeException("Cannot multiply values of types: " + left.getClass() + " and " + right.getClass());
        }
    }

    private Object divideValues(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            if (((Number) right).doubleValue() == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return ((Number) left).doubleValue() / ((Number) right).doubleValue();
        } else {
            throw new RuntimeException("Cannot divide values of types: " + left.getClass() + " and " + right.getClass());
        }
    }
}
