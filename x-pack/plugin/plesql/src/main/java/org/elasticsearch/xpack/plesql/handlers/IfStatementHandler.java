/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */


package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.exceptions.BreakException;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.util.List;

/**
 * The IfStatementHandler class handles IF-ELSEIF-ELSE statements within the procedural SQL execution context.
 * It evaluates conditions and executes corresponding statement blocks asynchronously.
 */
public class IfStatementHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs an IfStatementHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public IfStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the IF-ELSEIF-ELSE statement by evaluating conditions and executing corresponding statement blocks asynchronously.
     *
     * @param ctx      The If_statementContext representing the IF-ELSEIF-ELSE statement.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void handleAsync(PlEsqlProcedureParser.If_statementContext ctx, ActionListener<Object> listener) {
        ActionListener<Object> ifStatementListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object conditionResult) {
                if ( conditionResult instanceof Boolean && (Boolean) conditionResult ) {
                    // Execute the THEN block
                    List<PlEsqlProcedureParser.StatementContext> thenStatements = ctx.then_block;
                    executeStatementsAsync(thenStatements, 0, listener);
                } else {
                    // Proceed to ELSEIF blocks
                    handleElseIfBlocksAsync(ctx, 0, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> ifStatementLogger = ActionListenerUtils.withLogging(ifStatementListener, this.getClass().getName(),
            "If-Statement " + ctx.condition());

        // Start by evaluating the main IF condition asynchronously
        executor.evaluateConditionAsync(ctx.condition(), ifStatementLogger);
    }

    /**
     * Handles the ELSEIF blocks asynchronously.
     *
     * @param ctx      The If_statementContext containing the ELSEIF blocks.
     * @param index    The current index of the ELSEIF block being processed.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    private void handleElseIfBlocksAsync(PlEsqlProcedureParser.If_statementContext ctx, int index, ActionListener<Object> listener) {
        List<PlEsqlProcedureParser.Elseif_blockContext> elseifBlocks = ctx.elseif_block();

        if (index >= elseifBlocks.size()) {
            // No more ELSEIF blocks, proceed to ELSE block if it exists
            if ( ctx.else_block != null && ctx.else_block.isEmpty() == false ) {
                // Execute the ELSE block
                List<PlEsqlProcedureParser.StatementContext> elseStatements = ctx.else_block;
                executeStatementsAsync(elseStatements, 0, listener);
            } else {
                // No ELSE block, execution ends here
                listener.onResponse(null);
            }
            return;
        }

        PlEsqlProcedureParser.Elseif_blockContext elseifBlock = elseifBlocks.get(index);

        ActionListener<Object> elseIfStatementListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object elseifResult) {
                if ( elseifResult instanceof Boolean && (Boolean) elseifResult ) {
                    // Execute the corresponding ELSEIF THEN block
                    List<PlEsqlProcedureParser.StatementContext> elseifStatements = elseifBlock.statement();
                    executeStatementsAsync(elseifStatements, 0, listener);
                } else {
                    // Proceed to the next ELSEIF block
                    handleElseIfBlocksAsync(ctx, index + 1, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        ActionListener<Object> elseIfStatementLogger = ActionListenerUtils.withLogging(elseIfStatementListener, this.getClass().getName(),
            "Else-If-Statement: " + elseifBlock.condition());

        // Evaluate the ELSEIF condition asynchronously
        executor.evaluateConditionAsync(elseifBlock.condition(), elseIfStatementLogger);
    }

    /**
     * Executes a list of statements asynchronously.
     *
     * @param stmtCtxList The list of StatementContext representing the statements to execute.
     * @param index       The current index of the statement being executed.
     * @param listener    The ActionListener to handle asynchronous callbacks.
     */
    private void executeStatementsAsync(List<PlEsqlProcedureParser.StatementContext> stmtCtxList, int index,
                                        ActionListener<Object> listener) {
        if (index >= stmtCtxList.size()) {
            listener.onResponse(null); // All statements executed
            return;
        }

        PlEsqlProcedureParser.StatementContext stmtCtx = stmtCtxList.get(index);

        ActionListener<Object> ifConditionStatementExecutionListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object o) {
                if (o instanceof ReturnValue) {
                    listener.onResponse(o);
                } else {
                    executeStatementsAsync(stmtCtxList, index + 1, listener);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ReturnValue) {
                    // Propagate ReturnValue exception to signal function return
                    listener.onFailure(e);
                } else if (e instanceof BreakException) {
                    // Propagate BreakException to signal loop break
                    listener.onFailure(e);
                } else {
                    listener.onFailure(e);
                }
            }
        };

        ActionListener<Object> ifConditionStatementExecutionLogger = ActionListenerUtils.withLogging(ifConditionStatementExecutionListener,
            this.getClass().getName(),
            "If-Condition-Statement-Execution: " + stmtCtx.getText());

        // Visit the statement asynchronously
        executor.visitStatementAsync(stmtCtx, ifConditionStatementExecutionLogger);
    }
}
