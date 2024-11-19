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

/**
 * The LoopStatementHandler class handles loop statements (WHILE, FOR) within the procedural SQL execution context.
 * It evaluates loop conditions and executes the loop body asynchronously.
 */
public class LoopStatementHandler {
    private final ProcedureExecutor executor;

    public LoopStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the loop statement asynchronously.
     *
     * @param ctx      The Loop_statementContext representing the loop statement.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void handleAsync(PlEsqlProcedureParser.Loop_statementContext ctx, ActionListener<Object> listener) {
        try {
            if (ctx.FOR() != null) {
                // Handle FOR loop asynchronously
                handleForLoopAsync(ctx, listener);
            } else if (ctx.WHILE() != null) {
                // Handle WHILE loop asynchronously
                handleWhileLoopAsync(ctx, listener);
            } else {
                listener.onResponse(null); // Unsupported loop type
            }
        } catch (Exception e) {
            listener.onFailure(new RuntimeException("Error during loop execution: " + e.getMessage(), e));
        }
    }

    /**
     * Handles a FOR loop asynchronously.
     */
    private void handleForLoopAsync(PlEsqlProcedureParser.Loop_statementContext ctx, ActionListener<Object> listener) {
        String loopVarName = ctx.ID().getText();

        // Evaluate start and end expressions asynchronously
        executor.evaluateExpressionAsync(ctx.expression(0), new ActionListener<Object>() {
            @Override
            public void onResponse(Object startObj) {
                executor.evaluateExpressionAsync(ctx.expression(1), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object endObj) {
                        if ( (startObj instanceof Integer) == false || (endObj instanceof Integer) == false ) {
                            listener.onFailure(new RuntimeException("Loop range must be integer values."));
                            return;
                        }

                        int start = (Integer) startObj;
                        int end = (Integer) endObj;

                        ExecutionContext currentContext = executor.getContext();
                        boolean isAlreadyDeclared = currentContext.getVariables().containsKey(loopVarName);

                        if ( isAlreadyDeclared == false ) {
                            currentContext.declareVariable(loopVarName, "INT");
                        }

                        executeForLoopAsync(loopVarName, start, end, ctx.statement(), listener);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Executes the FOR loop asynchronously.
     */
    private void executeForLoopAsync(String loopVarName, int start, int end, List<PlEsqlProcedureParser.StatementContext> statements,
                                     ActionListener<Object> listener) {
        // Determine the loop direction
        int step = (start <= end) ? 1 : -1;
        int condition = (start <= end) ? end : end;

        // Start the loop iterations asynchronously
        executeForLoopIterationAsync(loopVarName, start, condition, step, statements, listener);
    }

    /**
     * Executes each iteration of the FOR loop asynchronously.
     */
    private void executeForLoopIterationAsync(String loopVarName, int currentValue, int endValue, int step,
                                              List<PlEsqlProcedureParser.StatementContext> statements, ActionListener<Object> listener) {
        // Check loop termination condition
        if ((step > 0 && currentValue > endValue) || (step < 0 && currentValue < endValue)) {
            listener.onResponse(null); // Loop completed
            return;
        }

        executor.getContext().setVariable(loopVarName, currentValue);

        // Execute loop body statements
        executeStatementsAsync(statements, 0, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Proceed to the next iteration
                executeForLoopIterationAsync(loopVarName, currentValue + step, endValue, step, statements, listener);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof BreakException) {
                    listener.onResponse(null); // Break out of the loop
                } else if (e instanceof ReturnValue) {
                    listener.onFailure(e); // Propagate return value
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }

    /**
     * Handles a WHILE loop asynchronously.
     */
    private void handleWhileLoopAsync(PlEsqlProcedureParser.Loop_statementContext ctx, ActionListener<Object> listener) {
        executor.evaluateConditionAsync(ctx.condition(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object conditionResult) {
                if (conditionResult instanceof Boolean && (Boolean) conditionResult ) {
                    // Execute the loop body
                    executeStatementsAsync(ctx.statement(), 0, new ActionListener<Object>() {
                        @Override
                        public void onResponse(Object unused) {
                            // Repeat the loop
                            handleWhileLoopAsync(ctx, listener);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (e instanceof BreakException) {
                                listener.onResponse(null); // Exit the loop
                            } else if (e instanceof ReturnValue) {
                                listener.onFailure(e); // Propagate return value
                            } else {
                                listener.onFailure(e);
                            }
                        }
                    });
                } else {
                    listener.onResponse(null); // Loop condition is false, exit
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Executes a list of statements asynchronously.
     */
    private void executeStatementsAsync(List<PlEsqlProcedureParser.StatementContext> stmtCtxList, int index
        , ActionListener<Object> listener) {
        if (index >= stmtCtxList.size()) {
            listener.onResponse(null); // All statements executed
            return;
        }

        PlEsqlProcedureParser.StatementContext stmtCtx = stmtCtxList.get(index);
        // Visit the statement asynchronously
        executor.visitStatementAsync(stmtCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Proceed to the next statement
                executeStatementsAsync(stmtCtxList, index + 1, listener);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ReturnValue || e instanceof BreakException) {
                    listener.onFailure(e); // Propagate special exceptions
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }
}
