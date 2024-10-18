/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.exceptions.BreakException;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

public class LoopStatementHandler {
    private ProcedureExecutor executor;

    public LoopStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    public void handle(PlEsqlProcedureParser.Loop_statementContext ctx) {
        if (ctx.FOR() != null) {
            // Handle FOR loop
            String loopVarName = ctx.ID().getText();
            Object startObj = executor.evaluateExpression(ctx.expression(0));
            Object endObj = executor.evaluateExpression(ctx.expression(1));

            if ( (startObj instanceof Integer) == false || (endObj instanceof Integer) == false ) {
                throw new RuntimeException("Loop range must be integer values.");
            }

            int start = (Integer) startObj;
            int end = (Integer) endObj;

            ExecutionContext currentContext = executor.getContext();
            boolean isAlreadyDeclared = currentContext.getVariables().containsKey(loopVarName);

            if ( isAlreadyDeclared == false ) {
                currentContext.declareVariable(loopVarName, "INT");
            }

            try {
                if (start <= end) {
                    for (int i = start; i <= end; i++) {
                        executor.getContext().setVariable(loopVarName, i);
                        executeLoopBody(ctx, currentContext);
                    }
                } else {
                    for (int i = start; i >= end; i--) {
                        executor.getContext().setVariable(loopVarName, i);
                        executeLoopBody(ctx, currentContext);
                    }
                }
            } catch (BreakException e) {
                // Exit the loop gracefully
            } catch (Exception e) {
                throw new RuntimeException("Error during loop execution: " + e.getMessage(), e);
            }

        } else if (ctx.WHILE() != null) {
            // Handle WHILE loop
            while (executor.evaluateCondition(ctx.condition())) {
                try {
                    executeLoopBody(ctx, executor.getContext());
                } catch (BreakException e) {
                    break; // Exit the loop when BREAK is encountered
                }
            }
        }
    }

    private void executeLoopBody(PlEsqlProcedureParser.Loop_statementContext ctx, ExecutionContext context) {
        for (PlEsqlProcedureParser.StatementContext stmtCtx : ctx.statement()) {
            executor.visit(stmtCtx);
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

    private Object handleOperator(Object left, Object right, int operatorType) {
        if (left instanceof Number && right instanceof Number) {
            double leftVal = ((Number) left).doubleValue();
            double rightVal = ((Number) right).doubleValue();

            switch (operatorType) {
                case PlEsqlProcedureParser.PLUS:
                    return leftVal + rightVal;
                case PlEsqlProcedureParser.MINUS:
                    return leftVal - rightVal;
                case PlEsqlProcedureParser.MULTIPLY:
                    return leftVal * rightVal;
                case PlEsqlProcedureParser.DIVIDE:
                    if (rightVal == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    return leftVal / rightVal;
                default:
                    throw new RuntimeException("Unknown operator: " + operatorType);
            }
        } else if (left instanceof String || right instanceof String) {
            if (operatorType == PlEsqlProcedureParser.PLUS) {
                return left.toString() + right.toString();
            } else {
                throw new RuntimeException("Cannot perform operation '" + operatorType + "' on String types.");
            }
        } else {
            throw new RuntimeException(
                "Unsupported operand types for operator '" + operatorType + "': " + left.getClass() + " and " + right.getClass()
            );
        }
    }
}
