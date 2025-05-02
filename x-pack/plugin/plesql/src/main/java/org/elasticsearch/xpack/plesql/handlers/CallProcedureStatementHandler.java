/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.List;

public class CallProcedureStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs an CallProcedureStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public CallProcedureStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    public void handleAsync(PlEsqlProcedureParser.Call_procedure_statementContext ctx, ActionListener<Object> listener) {
        String procedureName = ctx.ID().getText();
        List<PlEsqlProcedureParser.ExpressionContext> argContexts =
            ctx.argument_list() != null ? ctx.argument_list().expression() : new ArrayList<>();

        evaluateArgumentsAsync(argContexts, ActionListenerUtils.withLogging(new ActionListener<>() {
            @Override
            public void onResponse(List<Object> arguments) {
                executor.getProcedureAsync(procedureName, ActionListenerUtils.withLogging(new ActionListener<>() {
                    @Override
                    public void onResponse(FunctionDefinition procedure) {
                        if (procedure == null) {
                            listener.onFailure(new IllegalArgumentException("Procedure not found: " + procedureName));
                            return;
                        }

                        System.out.println("Procedure " + procedure.getName());

                        List<Parameter> paramNames = procedure.getParameters();
                        if (paramNames.size() != arguments.size()) {
                            listener.onFailure(
                                new IllegalArgumentException("Argument count mismatch: expected " +
                                    paramNames.size() + ", but got " + arguments.size()));
                            return;
                        }

                        ExecutionContext childContext = new ExecutionContext();
                        for (int i = 0; i < paramNames.size(); i++) {
                            childContext.declareVariable(paramNames.get(i).getName(), paramNames.get(i).getType());
                            childContext.setVariable(paramNames.get(i).getName(), arguments.get(i));
                        }

                        new ProcedureExecutor(childContext, executor.getThreadPool(), executor.getClient(), executor.getTokenStream())
                            .executeStatementsAsync(procedure.getBody(), 0, new ActionListener<Object>() {

                                @Override
                                public void onResponse(Object o) {
                                    listener.onResponse(o);
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
                }, this.getClass().getName(), "Get-Procedure"));
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        }, this.getClass().getName(), "Evaluate-Arguments"));
    }

    private void evaluateArgumentsAsync(List<PlEsqlProcedureParser.ExpressionContext> argContexts, ActionListener<List<Object>> listener) {
        List<Object> argValues = new ArrayList<>();
        evaluateArgumentAsync(argContexts, 0, argValues, listener);
    }

    private void evaluateArgumentAsync(List<PlEsqlProcedureParser.ExpressionContext> argContexts, int index, List<Object> argValues,
                                       ActionListener<List<Object>> listener) {
        if (index >= argContexts.size()) {
            listener.onResponse(argValues);
            return;
        }

        ActionListener<Object> argListener = new ActionListener<>() {
            @Override
            public void onResponse(Object value) {
                argValues.add(value);
                evaluateArgumentAsync(argContexts, index + 1, argValues, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> argLogger = ActionListenerUtils.withLogging(
            argListener, this.getClass().getName(), "Evaluate-Argument: " + argContexts.get(index)
        );

        executor.evaluateExpressionAsync(argContexts.get(index), argLogger);
    }
}
