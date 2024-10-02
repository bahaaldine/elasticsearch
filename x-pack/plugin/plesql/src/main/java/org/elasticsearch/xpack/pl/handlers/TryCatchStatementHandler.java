/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.pl.handlers;

import org.elasticsearch.xpack.pl.ProcedureExecutor;
import org.elasticsearch.xpack.pl.parser.PlEsqlProcedureParser;
import org.antlr.v4.runtime.Token;
import org.elasticsearch.xpack.pl.primitives.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

public class TryCatchStatementHandler {
    private ExecutionContext context;
    private ProcedureExecutor executor;

    public TryCatchStatementHandler(ExecutionContext context, ProcedureExecutor executor) {
        this.context = context;
        this.executor = executor;
    }

    public void handle(PlEsqlProcedureParser.Try_catch_statementContext ctx) {
        // List of all statements in the try_catch_statement
        List<PlEsqlProcedureParser.StatementContext> allStatements = ctx.statement();

        // Tokens: TRY, CATCH, FINALLY, ENDTRY
        Token tryToken = ctx.TRY().getSymbol();
        Token catchToken = ctx.CATCH() != null ? ctx.CATCH().getSymbol() : null;
        Token finallyToken = ctx.FINALLY() != null ? ctx.FINALLY().getSymbol() : null;
        Token endTryToken = ctx.ENDTRY().getSymbol();

        // Determine indices for each block
        int tryStartIndex = tryToken.getTokenIndex();
        int catchStartIndex = catchToken != null ? catchToken.getTokenIndex() : Integer.MAX_VALUE;
        int finallyStartIndex = finallyToken != null ? finallyToken.getTokenIndex() : Integer.MAX_VALUE;
        int endTryIndex = endTryToken.getTokenIndex();

        // Lists to store statements for each block
        List<PlEsqlProcedureParser.StatementContext> tryStatements = new ArrayList<>();
        List<PlEsqlProcedureParser.StatementContext> catchStatements = new ArrayList<>();
        List<PlEsqlProcedureParser.StatementContext> finallyStatements = new ArrayList<>();

        // Partition the statements into the appropriate blocks
        for (PlEsqlProcedureParser.StatementContext stmtCtx : allStatements) {
            int stmtTokenIndex = stmtCtx.getStart().getTokenIndex();

            if (stmtTokenIndex > tryStartIndex && stmtTokenIndex < catchStartIndex && stmtTokenIndex < finallyStartIndex
                    && stmtTokenIndex < endTryIndex) {
                // Statement belongs to TRY block
                tryStatements.add(stmtCtx);
            } else if (catchToken != null && stmtTokenIndex > catchStartIndex && stmtTokenIndex < finallyStartIndex
                    && stmtTokenIndex < endTryIndex) {
                // Statement belongs to CATCH block
                catchStatements.add(stmtCtx);
            } else if (finallyToken != null && stmtTokenIndex > finallyStartIndex && stmtTokenIndex < endTryIndex) {
                // Statement belongs to FINALLY block
                finallyStatements.add(stmtCtx);
            }
        }

        try {
            // Execute statements in TRY block
            for (PlEsqlProcedureParser.StatementContext stmtCtx : tryStatements) {
                executor.visit(stmtCtx);
            }
        } catch (Exception e) {
            // If exception occurs and CATCH block is present
            if (catchToken != null) {
                for (PlEsqlProcedureParser.StatementContext stmtCtx : catchStatements) {
                    executor.visit(stmtCtx);
                }
            } else {
                // If no CATCH block, rethrow the exception
                throw e;
            }
        } finally {
            // If FINALLY block is present
            if (finallyToken != null) {
                for (PlEsqlProcedureParser.StatementContext stmtCtx : finallyStatements) {
                    executor.visit(stmtCtx);
                }
            }
        }
    }
}
