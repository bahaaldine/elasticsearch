/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.exceptions.BreakException;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.elasticsearch.xpack.plesql.primitives.VariableDefinition;

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
            } else if (bodyResult instanceof ReturnValue) {
                listener.onResponse(bodyResult);
            } else {
                double nextVal = currentVal + step;
                executor.getThreadPool().generic().execute(() ->
                    executeRangeLoopIteration(loopVarName, nextVal, endVal, step, statements, listener)
                );
            }
        }, listener::onFailure));
    }

    /**
     * Handles asynchronous processing of a FOR-array loop.
     *
     * <p>This method evaluates the array expression provided in the loop context. If the array expression is a simple
     * identifier, it attempts to retrieve the declared variable's definition (using {@code getVariableDefinition})
     * to fetch the array's element type. If the definition is found and an element type is specified, that type is used.
     * Otherwise, the element type is inferred from the array's contents via {@code inferElementType}.
     *
     * <p>Once the appropriate element type is determined, the loop variable is declared with that type (if it hasn't
     * already been declared), and the loop is executed by invoking {@link #executeArrayLoopIteration(String, List, int, List, ActionListener)}
     * to process each element in the array.
     *
     * @param ctx the parse tree context of the FOR-array loop, containing the loop variable identifier,
     *            the array expression, and the loop body statements.
     * @param listener an asynchronous callback that is invoked upon completion or failure of the loop processing.
     *                 In case of failure, the listener receives a {@link RuntimeException} with the relevant error message.
     *
     * @throws RuntimeException if the evaluated array expression does not result in a {@link List}.
     */
    private void handleForArrayLoopAsync(PlEsqlProcedureParser.For_array_loopContext ctx, ActionListener<Object> listener) {
        String loopVarName = ctx.ID().getText();
        // Evaluate the array expression
        executor.evaluateExpressionAsync(ctx.array_loop_expression().expression(), ActionListener.wrap(arrayObj -> {
            if ( (arrayObj instanceof List) == false ) {
                listener.onFailure(new RuntimeException("Array loop expression must evaluate to a list."));
                return;
            }
            List<?> items = (List<?>) arrayObj;
            ExecutionContext context = executor.getContext();
            String elementType = "ANY";

            // Attempt to fetch the declared element type if the array expression is a single identifier.
            if (ctx.array_loop_expression().expression().getChildCount() == 1) {
                String arrayVarName = ctx.array_loop_expression().expression().getText();
                // Retrieve the VariableDefinition associated with the array variable
                VariableDefinition arrayVarDef = context.getVariableDefinition(arrayVarName);
                if (arrayVarDef != null && arrayVarDef.getElementType() != null) {
                    elementType = arrayVarDef.getElementType();
                } else {
                    // Fallback: infer type from array contents.
                    elementType = inferElementType(items);
                }
            } else {
                // If not a simple variable reference, fall back to inference.
                elementType = inferElementType(items);
            }
            // Declare the loop variable if not already declared using the determined type
            if ( context.getVariables().containsKey(loopVarName) == false ) {
                context.declareVariable(loopVarName, elementType);
            }

            executeArrayLoopIteration(loopVarName, items, 0, ctx.statement(), listener);
        }, listener::onFailure));
    }

    /**
     * Helper method to infer the element type from the array contents.
     *
     * @param items the list of items in the array
     * @return the inferred element type as a String
     */
    private String inferElementType(List<?> items) {
        String elementType = "ANY";
        if ( items.isEmpty() == false ) {
            Object firstElement = items.get(0);
            if (firstElement instanceof Number) {
                elementType = "NUMBER";
            } else if (firstElement instanceof String) {
                elementType = "STRING";
            } else if (firstElement instanceof List) {
                List<?> innerList = (List<?>) firstElement;
                if ( innerList.isEmpty() == false) {
                    Object inner = innerList.get(0);
                    if (inner instanceof Number) {
                        elementType = "ARRAY OF NUMBER";
                    } else if (inner instanceof String) {
                        elementType = "ARRAY OF STRING";
                    } else {
                        elementType = "ARRAY";
                    }
                } else {
                    elementType = "ARRAY";
                }
            } else if (firstElement instanceof java.util.Map) { // assuming DOCUMENT is represented as Map
                elementType = "DOCUMENT";
            }
        }
        return elementType;
    }

    /**
     * Recursively iterates over the elements of an array in a FOR-array loop.
     *
     * <p>This method sets the loop variable to the current element from the array and executes the loop body statements.
     * After the execution of the loop body:
     * <ul>
     *   <li>If a {@link BreakException} is encountered, the iteration terminates immediately.</li>
     *   <li>If not, the method recurses to process the next element in the array.</li>
     * </ul>
     *
     * @param loopVarName the name of the loop variable that will be assigned each element of the array.
     * @param items the list representing the array over which to iterate.
     * @param currentIndex the current index in the array that is being processed.
     * @param statements the list of statements constituting the body of the loop.
     * @param listener an asynchronous callback that is invoked when the loop completes processing all elements or if an error occurs.
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
            } else if (bodyResult instanceof ReturnValue) {
                listener.onResponse(bodyResult);
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
