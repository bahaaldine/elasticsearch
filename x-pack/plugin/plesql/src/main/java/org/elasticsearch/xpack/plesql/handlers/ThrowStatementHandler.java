/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

public class ThrowStatementHandler {
    private ExecutionContext context;
    private ProcedureExecutor executor;

    public ThrowStatementHandler(ExecutionContext context, ProcedureExecutor executor) {
        this.context = context;
        this.executor = executor;
    }

    public void handle(PlEsqlProcedureParser.Throw_statementContext ctx) {
        String exceptionMessage = ctx.STRING().getText();
        exceptionMessage = exceptionMessage.substring(1, exceptionMessage.length() - 1);
        throw new RuntimeException(exceptionMessage);
    }
}
