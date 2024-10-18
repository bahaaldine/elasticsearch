/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.util.List;

/**
 * The IfStatementHandler class handles IF-ELSEIF-ELSE statements within the procedural SQL execution context.
 * It evaluates conditions and executes corresponding statement blocks.
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
     * Handles the IF-ELSEIF-ELSE statement by evaluating conditions and executing corresponding statement blocks.
     *
     * @param ctx The If_statementContext representing the IF-ELSEIF-ELSE statement.
     */
    public void handle(PlEsqlProcedureParser.If_statementContext ctx) {
        // Evaluate the main IF condition
        PlEsqlProcedureParser.ConditionContext mainCondition = ctx.condition();
        boolean conditionResult = executor.evaluateCondition(mainCondition);

        if (conditionResult) {
            // Execute the THEN block
            List<PlEsqlProcedureParser.StatementContext> thenStatements = ctx.then_block;
            executeStatements(thenStatements);
            return;
        }

        // Iterate through ELSEIF conditions
        List<PlEsqlProcedureParser.Elseif_blockContext> elseifBlocks = ctx.elseif_block();
        for (PlEsqlProcedureParser.Elseif_blockContext elseifBlock : elseifBlocks) {
            PlEsqlProcedureParser.ConditionContext elseifCondition = elseifBlock.condition();
            boolean elseifResult = executor.evaluateCondition(elseifCondition);
            if (elseifResult) {
                // Execute the corresponding ELSEIF THEN block
                List<PlEsqlProcedureParser.StatementContext> elseifStatements = elseifBlock.statement();
                executeStatements(elseifStatements);
                return;
            }
        }

        // Check for ELSE block
        if (ctx.else_block != null && ctx.else_block.isEmpty() == false) {
            // Execute the ELSE block
            List<PlEsqlProcedureParser.StatementContext> elseStatements = ctx.else_block;
            executeStatements(elseStatements);
        }
    }

    /**
     * Executes a list of statements.
     *
     * @param stmtCtxList The list of StatementContext representing the statements to execute.
     */
    private void executeStatements(List<PlEsqlProcedureParser.StatementContext> stmtCtxList) {
        for (PlEsqlProcedureParser.StatementContext stmtCtx : stmtCtxList) {
            executor.visit(stmtCtx);
        }
    }
}
