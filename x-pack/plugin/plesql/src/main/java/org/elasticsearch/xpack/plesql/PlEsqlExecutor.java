/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.functions.builtin.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.plesql.primitives.functions.builtin.DateBuiltInFunctions;
import org.elasticsearch.xpack.plesql.primitives.functions.builtin.NumberBuiltInFunctions;
import org.elasticsearch.xpack.plesql.primitives.functions.builtin.StringBuiltInFunctions;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

/**
 * Provides synchronous and asynchronous methods for executing PL|ESQL procedures.
 */
public class PlEsqlExecutor {

    private final ThreadPool threadPool;
    private final Client client;

    @Inject
    public PlEsqlExecutor(ThreadPool threadPool, Client client) {
        this.threadPool = threadPool;
        this.client = client;
    }

    /**
     * Asynchronous procedure execution.
     * Provides the "result" string to the listener once done.
     */
    public void executeProcedure(String procedureText, ActionListener<Object> listener) {
        threadPool.generic().execute(() -> {
            try {
                // 1) Parse inputt
                PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(procedureText));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

                parser.removeErrorListeners();
                parser.addErrorListener(new PlEsqlErrorListener());

                PlEsqlProcedureParser.ProcedureContext ctx = parser.procedure();

                // 2) Build executor
                ExecutionContext executionContext = new ExecutionContext();

                // Register built-in functions
                StringBuiltInFunctions.registerAll(executionContext);
                NumberBuiltInFunctions.registerAll(executionContext);
                ArrayBuiltInFunctions.registerAll(executionContext);
                DateBuiltInFunctions.registerAll(executionContext);

                ProcedureExecutor procedureExecutor = new ProcedureExecutor(executionContext, threadPool, client, tokens);

                ActionListener<Object> executeProcedureListener = new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object result) {
                        // Possibly the result can be a string or some other object
                        if (result == null) {
                            // You might interpret that as "no RETURN found" or "ok"
                            listener.onResponse("Procedure executed. (no return?)");
                        } else {
                            listener.onResponse(result);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onResponse(e);
                    }
                };

                ActionListener<Object> loggingListener =
                    ActionListenerUtils.withLogging(executeProcedureListener,
                        this.getClass().getName(), "ExecutePLESQLProcedure-" + procedureText);

                // 3) Visit the parse tree asynchronously
                procedureExecutor.visitProcedureAsync(ctx, loggingListener);

            } catch (Exception parseOrInitError) {
                // If an exception is thrown during parse or init
                listener.onFailure(parseOrInitError);
            }
        });
    }
}
