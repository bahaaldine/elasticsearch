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

public class IfStatementHandler {
    private ExecutionContext context;
    private ProcedureExecutor executor;

    public IfStatementHandler(ExecutionContext context, ProcedureExecutor executor) {
        this.context = context;
        this.executor = executor;
    }

    public void handle(PlEsqlProcedureParser.If_statementContext ctx) {
        boolean conditionMatched = false;
        int conditionIndex = 0;
        int currentStatementIndex = 0;
        int totalStatements = ctx.statement().size();

        // TODO: maybe create clause specific classes?
        // IF clause handler
        if (evaluateCondition(ctx.condition(conditionIndex))) {
            conditionMatched = true;
            currentStatementIndex = executeStatements(ctx, currentStatementIndex, conditionIndex);
        }
        conditionIndex++;

        // ELSEIF clauses handler
        for (int i = 0; i < ctx.ELSEIF().size(); i++) {
            if (conditionMatched) {
                break;
            }
            if (evaluateCondition(ctx.condition(conditionIndex))) {
                conditionMatched = true;
                currentStatementIndex = executeStatements(ctx, currentStatementIndex, conditionIndex);
            }
            conditionIndex++;
        }

        // ELSE clause handler
        if (conditionMatched == false && ctx.ELSE() != null) {
            while (currentStatementIndex < totalStatements) {
                executor.visit(ctx.statement(currentStatementIndex));
                currentStatementIndex++;
            }
        }
    }

    private int executeStatements(PlEsqlProcedureParser.If_statementContext ctx, int currentStatementIndex, int conditionIndex) {
        int nextClauseTokenIndex = Integer.MAX_VALUE;

        if (conditionIndex + 1 < ctx.THEN().size()) {
            nextClauseTokenIndex = ctx.THEN(conditionIndex + 1).getSymbol().getTokenIndex();
        } else if (ctx.ELSE() != null) {
            nextClauseTokenIndex = ctx.ELSE().getSymbol().getTokenIndex();
        } else if (ctx.ENDIF() != null) {
            nextClauseTokenIndex = ctx.ENDIF().getSymbol().getTokenIndex();
        }

        int totalStatements = ctx.statement().size();

        while (currentStatementIndex < totalStatements) {
            PlEsqlProcedureParser.StatementContext stmtCtx = ctx.statement(currentStatementIndex);
            if (stmtCtx.getStart().getTokenIndex() >= nextClauseTokenIndex) {
                break;
            }
            executor.visit(stmtCtx);
            currentStatementIndex++;
        }
        return currentStatementIndex;
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
                return left.equals(right) == false;
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
            return context.getVariable(ctx.ID().getText());
        } else if (ctx.op != null) {
            Object left = evaluateExpression(ctx.expression(0));
            Object right = evaluateExpression(ctx.expression(1));
            switch (ctx.op.getType()) {
                case PlEsqlProcedureParser.PLUS:
                    return addValues(left, right);
                case PlEsqlProcedureParser.MINUS:
                    return subtractValues(left, right);
                case PlEsqlProcedureParser.MULTIPLY:
                    return multiplyValues(left, right);
                case PlEsqlProcedureParser.DIVIDE:
                    return divideValues(left, right);
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

    // arithmetic operations helper (add, sub, and multiply)
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
