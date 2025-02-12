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
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

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
                handleForLoopAsync(ctx, listener);
            } else if (ctx.WHILE() != null) {
                handleWhileLoopAsync(ctx, listener);
            } else {
                listener.onResponse(null); // Unsupported loop type
            }
        } catch (Exception e) {
            listener.onFailure(new RuntimeException("[Loop Statement Handler] Error during loop execution: " + e.getMessage(), e));
        }
    }

    /**
     * Handles a FOR loop asynchronously.
     */
    private void handleForLoopAsync(PlEsqlProcedureParser.Loop_statementContext ctx, ActionListener<Object> listener) {
        String loopVarName = ctx.ID().getText();

        ActionListener<Object> forLoopListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object startObj) {
                executor.evaluateExpressionAsync(ctx.expression(1), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object endObj) {
                        if ( (startObj instanceof Number) == false || (endObj instanceof Number) == false ) {
                            listener.onFailure(new RuntimeException("Loop range must be integer values."));
                            return;
                        }

                        int start = ((Number) startObj).intValue();
                        int end = (((Number) endObj).intValue());

                        ExecutionContext currentContext = executor.getContext();
                        boolean isAlreadyDeclared = currentContext.getVariables().containsKey(loopVarName);

                        if ( isAlreadyDeclared == false ) {
                            currentContext.declareVariable(loopVarName, "NUMBER");
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
        };

        ActionListener<Object> forLoopLogger = ActionListenerUtils.withLogging(forLoopListener, this.getClass().getName(),
                "For-Loop:" + ctx.getText());

        // Evaluate start and end expressions asynchronously
        executor.evaluateExpressionAsync(ctx.expression(0), forLoopLogger);
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

        executor.getContext().setVariable(loopVarName, Integer.valueOf(currentValue));

        ActionListener<Object> executeForLoopIterationListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object o) {
                if (o instanceof ReturnValue) {
                    listener.onResponse(o);
                } else {
                    // Proceed to the next iteration
                    executeForLoopIterationAsync(loopVarName, currentValue + step, endValue, step, statements, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof BreakException) {
                    listener.onResponse(null); // Break out of the loop
                } else if (e instanceof ReturnValue) {
                    listener.onResponse(e); // Propagate return value
                } else {
                    listener.onFailure(e);
                }
            }
        };

        ActionListener<Object> executeForLoopIterationLogger = ActionListenerUtils.withLogging(executeForLoopIterationListener,
            this.getClass().getName(),
                "For-Loop-Iteration-Execution: " + statements);

        // Execute loop body statements
        executeStatementsAsync(statements, 0, executeForLoopIterationLogger);
    }

    /**
     * Handles a WHILE loop asynchronously.
     */
    private void handleWhileLoopAsync(PlEsqlProcedureParser.Loop_statementContext ctx, ActionListener<Object> listener) {
        // Start the iteration chain
        doWhileIteration(ctx, listener);
    }

    private void doWhileIteration(PlEsqlProcedureParser.Loop_statementContext ctx, ActionListener<Object> listener) {

        ActionListener<Object> doWhileIterationListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object conditionResult) {
                if ( (conditionResult instanceof Boolean) == false ) {
                    listener.onFailure(new RuntimeException("WHILE condition must be boolean."));
                    return;
                }
                boolean loopShouldContinue = (Boolean) conditionResult;

                if ( loopShouldContinue == false) {
                    // Condition is false -> loop is done
                    listener.onResponse(null);
                    return;
                }

                // 2) Condition is true -> execute statements in the loop body
                executeStatementsAsync(ctx.statement(), 0, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object bodyResult) {
                        // If body returned a ReturnValue or something, propagate it
                        if (bodyResult instanceof ReturnValue) {
                            listener.onResponse(bodyResult);
                            return;
                        }
                        // 3) If no ReturnValue, schedule next iteration on the thread pool
                        executor.getThreadPool().generic().execute(() -> doWhileIteration(ctx, listener));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (e instanceof BreakException) {
                            // End the loop normally
                            listener.onResponse(null);
                        } else if (e instanceof ReturnValue) {
                            listener.onResponse(e);
                        } else {
                            listener.onFailure(e);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> doWhileIterationLogger = ActionListenerUtils.withLogging(doWhileIterationListener,
            this.getClass().getName(),
                "Do-While-Iteration:" + ctx.getText());

        executor.evaluateConditionAsync(ctx.condition(), doWhileIterationLogger);
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

        ActionListener<Object> executeLoopStatementListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                if (result instanceof ReturnValue) {
                    // If a statement returned a value, we must short-circuit
                    // and pass that ReturnValue back up the call chain
                    listener.onResponse(result);
                } else {
                    // Otherwise, proceed to the next statement
                    executeStatementsAsync(stmtCtxList, index + 1, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ReturnValue || e instanceof BreakException) {
                    listener.onFailure(e); // Propagate special exceptions
                } else {
                    listener.onFailure(e);
                }
            }
        };

        ActionListener<Object> executeLoopStatementLogger = ActionListenerUtils.withLogging(executeLoopStatementListener,
            this.getClass().getName(),
                "Execute-Loop-Statement:" + stmtCtx.getText());

        // Visit the statement asynchronously
        executor.visitStatementAsync(stmtCtx, executeLoopStatementLogger);
    }
}
