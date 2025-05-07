/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.executors.PlEsqlExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

public class DeleteProcedureStatementHandler {

    private static final Logger LOGGER = LogManager.getLogger(DeleteProcedureStatementHandler.class);

    private final ExecutionContext context;
    private final PlEsqlExecutor executor;

    public DeleteProcedureStatementHandler(ExecutionContext context, PlEsqlExecutor executor) {
        this.context = context;
        this.executor = executor;
    }

    public void handleAsync(PlEsqlProcedureParser.Delete_procedure_statementContext ctx, ActionListener<Object> listener) {
        TerminalNode procIdNode = ctx.ID();

        LOGGER.info("Deleting procedure {}", procIdNode);
        if (procIdNode == null) {
            listener.onFailure(new IllegalArgumentException("Procedure ID is missing"));
            return;
        }

        String procedureId = procIdNode.getText();
        executor.deleteProcedureAsync(procedureId, listener);
    }
}
