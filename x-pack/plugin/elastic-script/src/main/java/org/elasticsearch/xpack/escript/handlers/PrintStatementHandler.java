/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License 2.0.
 */
package org.elasticsearch.xpack.escript.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.evaluators.ExpressionEvaluator;

/**
 * Handler for PRINT statements.
 * Evaluates the expression asynchronously using ExpressionEvaluator.evaluateExpressionAsync,
 * then logs the resulting string with an optional severity.
 */
public class PrintStatementHandler {

    private final ProcedureExecutor executor;
    private static final Logger LOGGER = LogManager.getLogger(PrintStatementHandler.class);

    public PrintStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Executes a PRINT statement.
     *
     * The print statement is defined as:
     *   PRINT expression (COMMA severity)? SEMICOLON
     *
     * This method evaluates the expression asynchronously and then logs the result,
     * using the provided severity if available (defaulting to INFO otherwise).
     *
     * @param ctx the parse tree context for the print_statement.
     * @param listener the ActionListener to signal completion.
     */
    public void execute(ElasticScriptParser.Print_statementContext ctx, ActionListener<Object> listener) {
        // Use the expression() method provided by the parser – this should return only the expression node.
        ElasticScriptParser.ExpressionContext exprCtx = ctx.expression();

        new ExpressionEvaluator(executor)
            .evaluateExpressionAsync(exprCtx, new ActionListener<Object>() {
                @Override
                public void onResponse(Object result) {
                    String output = (result != null) ? result.toString() : "null";
                    String logMessage = "[PRINT] " + output;
                    if (ctx.severity() != null) {
                        String severity = ctx.severity().getText().toUpperCase();
                        switch (severity) {
                            case "DEBUG":
                                LOGGER.debug(logMessage);
                                break;
                            case "WARN":
                                LOGGER.warn(logMessage);
                                break;
                            case "ERROR":
                                LOGGER.error(logMessage);
                                break;
                            case "INFO":
                            default:
                                LOGGER.info(logMessage);
                                break;
                        }
                    } else {
                        LOGGER.info(logMessage);
                    }
                    listener.onResponse(null);
                }
                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
    }
}
