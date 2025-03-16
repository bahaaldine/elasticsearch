/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.exceptions.BreakException;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;

import java.util.List;

public class LoopStatementHandler {

    private final ProcedureExecutor executor;

    public LoopStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Dispatches loop handling based on the kind of loop parsed.
     */
    public void handleAsync(PlEsqlProcedureParser.Loop_statementContext ctx, ActionListener<Object> listener) {
        if (ctx.for_range_loop() != null) {
            handleForRangeLoopAsync(ctx.for_range_loop(), listener);
        } else if (ctx.for_array_loop() != null) {
            handleForArrayLoopAsync(ctx.for_array_loop(), listener);
        } else if (ctx.while_loop() != null) {
            handleWhileLoopAsync(ctx.while_loop(), listener);
        } else {
            listener.onResponse(null);
        }
    }

    /**
     * Processes a FOR-range loop.
     * Example: FOR i IN 1..3 LOOP ... END LOOP
     */
    private void handleForRangeLoopAsync(PlEsqlProcedureParser.For_range_loopContext ctx, ActionListener<Object> listener) {
        String loopVarName = ctx.ID().getText();
        // Evaluate start expression (first expression in range_loop_expression)
        executor.evaluateExpressionAsync(ctx.range_loop_expression().expression(0), ActionListener.wrap(startObj -> {
            if (startObj instanceof Number == false) {
                listener.onFailure(new RuntimeException("Start expression must be numeric."));
                return;
            }
            double startVal = ((Number) startObj).doubleValue();
            // Evaluate end expression (second expression in range_loop_expression)
            executor.evaluateExpressionAsync(ctx.range_loop_expression().expression(1), ActionListener.wrap(endObj -> {
                if (endObj instanceof Number == false) {
                    listener.onFailure(new RuntimeException("End expression must be numeric."));
                    return;
                }
                double endVal = ((Number) endObj).doubleValue();
                double step = (startVal <= endVal) ? 1.0 : -1.0;
                executeRangeLoopIteration(loopVarName, startVal, endVal, step, ctx.statement(), listener);
            }, listener::onFailure));
        }, listener::onFailure));
    }

    /**
     * Recursively executes one iteration of a FOR-range loop.
     */
    private void executeRangeLoopIteration(String loopVarName, double currentVal, double endVal, double step,
                                           List<PlEsqlProcedureParser.StatementContext> statements,
                                           ActionListener<Object> listener) {
        if ((step > 0 && currentVal > endVal) || (step < 0 && currentVal < endVal)) {
            listener.onResponse(null);
            return;
        }
        executor.getContext().setVariable(loopVarName, currentVal);
        executeStatementsAsync(statements, 0, ActionListener.wrap(bodyResult -> {
            if (bodyResult instanceof BreakException) {
                listener.onResponse(null);
            } else {
                double nextVal = currentVal + step;
                executor.getThreadPool().generic().execute(() ->
                    executeRangeLoopIteration(loopVarName, nextVal, endVal, step, statements, listener)
                );
            }
        }, listener::onFailure));
    }

    /**
     * Processes a FOR-array loop.
     * Example: FOR x IN [10, 20, 30] LOOP ... END LOOP
     */
    private void handleForArrayLoopAsync(PlEsqlProcedureParser.For_array_loopContext ctx, ActionListener<Object> listener) {
        String loopVarName = ctx.ID().getText();
        // Evaluate the array expression
        executor.evaluateExpressionAsync(ctx.array_loop_expression().expression(), ActionListener.wrap(arrayObj -> {
            if ((arrayObj instanceof List) == false) {
                listener.onFailure(new RuntimeException("Array loop expression must evaluate to a list."));
                return;
            }
            // Declare the loop variable if not already declared
            ExecutionContext context = executor.getContext();
            if (context.getVariables().containsKey(loopVarName) == false) {
                context.declareVariable(loopVarName, "ANY");
            }
            List<?> items = (List<?>) arrayObj;
            executeArrayLoopIteration(loopVarName, items, 0, ctx.statement(), listener);
        }, listener::onFailure));
    }

    /**
     * Recursively iterates over the array elements.
     */
    private void executeArrayLoopIteration(String loopVarName, List<?> items, int currentIndex,
                                           List<PlEsqlProcedureParser.StatementContext> statements,
                                           ActionListener<Object> listener) {
        if (currentIndex >= items.size()) {
            listener.onResponse(null);
            return;
        }
        // Set the loop variable to the current array element
        executor.getContext().setVariable(loopVarName, items.get(currentIndex));
        executeStatementsAsync(statements, 0, ActionListener.wrap(bodyResult -> {
            if (bodyResult instanceof BreakException) {
                listener.onResponse(null);
            } else {
                executor.getThreadPool().generic().execute(() ->
                    executeArrayLoopIteration(loopVarName, items, currentIndex + 1, statements, listener)
                );
            }
        }, listener::onFailure));
    }

    /**
     * Processes a WHILE loop.
     * Example: WHILE condition LOOP ... END LOOP
     */
    private void handleWhileLoopAsync(PlEsqlProcedureParser.While_loopContext ctx, ActionListener<Object> listener) {
        doWhileIteration(ctx, listener);
    }

    /**
     * Recursively evaluates a WHILE loop.
     */
    private void doWhileIteration(PlEsqlProcedureParser.While_loopContext ctx, ActionListener<Object> listener) {
        executor.evaluateConditionAsync(ctx.condition(), ActionListener.wrap(condResult -> {
            if ((condResult instanceof Boolean) == false) {
                listener.onFailure(new RuntimeException("WHILE condition must be boolean."));
                return;
            }
            boolean condition = ((Boolean) condResult).booleanValue();
            if (condition == false) {
                listener.onResponse(null);
                return;
            }
            executeStatementsAsync(ctx.statement(), 0, ActionListener.wrap(bodyResult -> {
                if (bodyResult instanceof BreakException) {
                    listener.onResponse(null);
                } else if (bodyResult instanceof ReturnValue) {
                    listener.onResponse(bodyResult);
                } else {
                    executor.getThreadPool().generic().execute(() ->
                        doWhileIteration(ctx, listener)
                    );
                }
            }, listener::onFailure));
        }, listener::onFailure));
    }

    /**
     * Executes a list of statements asynchronously.
     */
    private void executeStatementsAsync(List<PlEsqlProcedureParser.StatementContext> stmtCtxList,
                                        int index, ActionListener<Object> listener) {
        if (index >= stmtCtxList.size()) {
            listener.onResponse(null);
            return;
        }
        PlEsqlProcedureParser.StatementContext stmtCtx = stmtCtxList.get(index);
        executor.visitStatementAsync(stmtCtx, ActionListener.wrap(result -> {
            if (result instanceof ReturnValue) {
                listener.onResponse(result);
            } else {
                executeStatementsAsync(stmtCtxList, index + 1, listener);
            }
        }, listener::onFailure));
    }
}
