/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

public class DeclareStatementHandler {
    private ProcedureExecutor executor;

    public DeclareStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    private boolean isSupportedDataType(String varType) {
        // Add supported types to this list
        switch (varType.toUpperCase()) {
            case "INT":
            case "FLOAT":
            case "STRING":
                return true;
            default:
                return false;  // Unsupported data type
        }
    }

    public void handleAsync(PlEsqlProcedureParser.Declare_statementContext ctx, ActionListener<Object> listener) {
        for (PlEsqlProcedureParser.Variable_declarationContext varCtx : ctx.variable_declaration_list().variable_declaration()) {
            String varName = varCtx.ID().getText();
            String varType = varCtx.datatype().getText();

            // Check if the data type is unsupported
            if (isSupportedDataType(varType) == false) {
                throw new RuntimeException("Unsupported data type: " + varType);
            }

            if (varCtx.expression() != null) {
                // Initialize variable with the expression's value
                executor.evaluateExpressionAsync(varCtx.expression(), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object value) {
                        executor.getContext().setVariable(varName, value);
                        listener.onResponse(value);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                });
            } else {
                listener.onResponse(null); // Variable declared without initialization
            }
        }
    }
}
