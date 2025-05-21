/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.builtin.datasources.EsqlBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DocumentBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;

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

    public void handleAsync(ElasticScriptParser.Call_procedure_statementContext ctx, ActionListener<Object> listener) {
        String procedureName = ctx.ID().getText();
        List<ElasticScriptParser.ExpressionContext> argContexts =
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

                        StringBuiltInFunctions.registerAll(childContext);
                        NumberBuiltInFunctions.registerAll(childContext);
                        ArrayBuiltInFunctions.registerAll(childContext);
                        DocumentBuiltInFunctions.registerAll(childContext);
                        EsqlBuiltInFunctions.registerAll(childContext,executor,executor.getClient());

                        new ProcedureExecutor(childContext, executor.getThreadPool(), executor.getClient(), executor.getTokenStream())
                            .executeStatementsAsync(procedure.getBody(), 0, new ActionListener<Object>() {

                                @Override
                                public void onResponse(Object o) {
                                    if ( o instanceof ReturnValue ) {
                                        listener.onResponse(((ReturnValue) o).getValue());
                                    } else {
                                        listener.onResponse(o);
                                    }
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

    private void evaluateArgumentsAsync(List<ElasticScriptParser.ExpressionContext> argContexts, ActionListener<List<Object>> listener) {
        List<Object> argValues = new ArrayList<>();
        evaluateArgumentAsync(argContexts, 0, argValues, listener);
    }

    private void evaluateArgumentAsync(List<ElasticScriptParser.ExpressionContext> argContexts, int index, List<Object> argValues,
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
