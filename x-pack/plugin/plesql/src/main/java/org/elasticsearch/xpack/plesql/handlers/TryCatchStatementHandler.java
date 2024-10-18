/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.util.ArrayList;
import java.util.List;

/**
 * The TryCatchHandler class is responsible for handling TRY-CATCH-FINALLY statements within the procedural SQL execution context.
 * It executes the TRY block, catches exceptions in the CATCH block, and executes the FINALLY block if present.
 */
public class TryCatchStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs a TryCatchHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public TryCatchStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the TRY-CATCH-FINALLY statement by executing the TRY block,
     * catching exceptions in the CATCH block, and executing the FINALLY block if present.
     *
     * @param ctx The Try_catch_statementContext representing the TRY-CATCH-FINALLY statement.
     */
    public void handle(PlEsqlProcedureParser.Try_catch_statementContext ctx) {
        List<PlEsqlProcedureParser.StatementContext> tryStatements = new ArrayList<>();
        List<PlEsqlProcedureParser.StatementContext> catchStatements = new ArrayList<>();
        List<PlEsqlProcedureParser.StatementContext> finallyStatements = new ArrayList<>();

        // Flags to track the current block
        boolean inTry = false;
        boolean inCatch = false;
        boolean inFinally = false;

        // Iterate through all children to partition statements
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);

            if (child instanceof TerminalNode) {
                TerminalNode terminal = (TerminalNode) child;
                String symbol = terminal.getSymbol().getText();

                if (symbol.equalsIgnoreCase("TRY")) {
                    inTry = true;
                    inCatch = false;
                    inFinally = false;
                    continue; // Skip the TRY keyword
                } else if (symbol.equalsIgnoreCase("CATCH")) {
                    inTry = false;
                    inCatch = true;
                    inFinally = false;
                    continue; // Skip the CATCH keyword
                } else if (symbol.equalsIgnoreCase("FINALLY")) {
                    inTry = false;
                    inCatch = false;
                    inFinally = true;
                    continue; // Skip the FINALLY keyword
                } else if (symbol.equalsIgnoreCase("ENDTRY")) {
                    inTry = false;
                    inCatch = false;
                    inFinally = false;
                    continue; // Skip the ENDTRY keyword
                }
            }

            if (child instanceof PlEsqlProcedureParser.StatementContext) {
                PlEsqlProcedureParser.StatementContext stmtCtx = (PlEsqlProcedureParser.StatementContext) child;
                if (inTry) {
                    tryStatements.add(stmtCtx);
                } else if (inCatch) {
                    catchStatements.add(stmtCtx);
                } else if (inFinally) {
                    finallyStatements.add(stmtCtx);
                } else {
                    // Statements outside of TRY/CATCH/FINALLY blocks should not occur
                    throw new RuntimeException("Statement found outside of TRY/CATCH/FINALLY blocks.");
                }
            }
        }

        try {
            // Execute TRY block
            executeStatements(tryStatements);
        } catch (RuntimeException e) { // Catch broader exceptions
            // Log the exception for debugging
            System.out.println("Exception caught in TRY block: " + e.getMessage());

            // Execute CATCH block if present
            if ( catchStatements.isEmpty() == false ) {
                executeStatements(catchStatements);
            } else {
                // Rethrow if no CATCH block to handle the exception
                throw e;
            }
        } finally {
            // Execute FINALLY block if present
            if ( finallyStatements.isEmpty() == false ) {
                executeStatements(finallyStatements);
            }
        }
    }

    /**
     * Executes a list of statements by visiting each statement context.
     *
     * @param stmtCtxList The list of StatementContext representing the statements to execute.
     */
    private void executeStatements(List<PlEsqlProcedureParser.StatementContext> stmtCtxList) {
        for (PlEsqlProcedureParser.StatementContext stmtCtx : stmtCtxList) {
            executor.visit(stmtCtx);
        }
    }
}
