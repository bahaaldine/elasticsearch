/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.util.ArrayList;
import java.util.List;

/**
 * The TryCatchStatementHandler class is responsible for handling TRY-CATCH-FINALLY statements asynchronously.
 */
public class TryCatchStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs a TryCatchStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public TryCatchStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the TRY-CATCH-FINALLY statement asynchronously.
     *
     * @param ctx      The Try_catch_statementContext representing the TRY-CATCH-FINALLY statement.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void handleAsync(PlEsqlProcedureParser.Try_catch_statementContext ctx, ActionListener<Object> listener) {
        // Partition the statements into TRY, CATCH, and FINALLY blocks
        List<PlEsqlProcedureParser.StatementContext> tryStatements = new ArrayList<>();
        List<PlEsqlProcedureParser.StatementContext> catchStatements = new ArrayList<>();
        List<PlEsqlProcedureParser.StatementContext> finallyStatements = new ArrayList<>();

        partitionStatements(ctx, tryStatements, catchStatements, finallyStatements);

        // Execute TRY block asynchronously
        executeStatementsAsync(tryStatements, 0, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // TRY block completed successfully
                executeFinallyBlock(finallyStatements, listener);
            }

            @Override
            public void onFailure(Exception e) {
                // Exception occurred in TRY block
                if ( catchStatements.isEmpty() == false ) {
                    // Execute CATCH block
                    executeStatementsAsync(catchStatements, 0, new ActionListener<Object>() {
                        @Override
                        public void onResponse(Object unused) {
                            // CATCH block completed
                            executeFinallyBlock(finallyStatements, listener);
                        }

                        @Override
                        public void onFailure(Exception ex) {
                            // Exception in CATCH block
                            executeFinallyBlock(finallyStatements, new ActionListener<Object>() {
                                @Override
                                public void onResponse(Object unused) {
                                    listener.onFailure(ex);
                                }

                                @Override
                                public void onFailure(Exception exc) {
                                    listener.onFailure(exc);
                                }
                            });
                        }
                    });
                } else {
                    // No CATCH block; execute FINALLY and rethrow exception
                    executeFinallyBlock(finallyStatements, new ActionListener<Object>() {
                        @Override
                        public void onResponse(Object aVoid) {
                            listener.onFailure(e);
                        }

                        @Override
                        public void onFailure(Exception ex) {
                            listener.onFailure(ex);
                        }
                    });
                }
            }
        });
    }

    /**
     * Partitions the statements into TRY, CATCH, and FINALLY blocks.
     */
    private void partitionStatements(PlEsqlProcedureParser.Try_catch_statementContext ctx,
                                     List<PlEsqlProcedureParser.StatementContext> tryStatements,
                                     List<PlEsqlProcedureParser.StatementContext> catchStatements,
                                     List<PlEsqlProcedureParser.StatementContext> finallyStatements) {
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
    }

    /**
     * Executes a list of statements asynchronously.
     */
    private void executeStatementsAsync(List<PlEsqlProcedureParser.StatementContext> stmtCtxList, int index,
                                        ActionListener<Object> listener) {
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
                listener.onFailure(e);
            }
        });
    }

    /**
     * Executes the FINALLY block asynchronously.
     */
    private void executeFinallyBlock(List<PlEsqlProcedureParser.StatementContext> finallyStatements, ActionListener<Object> listener) {
        if ( finallyStatements.isEmpty() == false ) {
            executeStatementsAsync(finallyStatements, 0, new ActionListener<Object>() {
                @Override
                public void onResponse(Object unused) {
                    listener.onResponse(null);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        } else {
            listener.onResponse(null); // No FINALLY block
        }
    }
}
