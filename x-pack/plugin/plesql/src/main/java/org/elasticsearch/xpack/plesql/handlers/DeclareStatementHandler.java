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

import java.util.List;

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
        List<PlEsqlProcedureParser.Variable_declarationContext> varDecls = ctx.variable_declaration_list().variable_declaration();
        processVariableDeclarations(varDecls, 0, listener);
    }

    private void processVariableDeclarations(List<PlEsqlProcedureParser.Variable_declarationContext> varDecls, int index,
                                             ActionListener<Object> listener) {
        if (index >= varDecls.size()) {
            listener.onResponse(null); // All declarations succeeded
            return;
        }

        PlEsqlProcedureParser.Variable_declarationContext varCtx = varDecls.get(index);
        String varName = varCtx.ID().getText();
        String varType = varCtx.datatype().getText();

        // Check if the data type is unsupported
        if ( isSupportedDataType(varType) == false ) {
            listener.onFailure(new RuntimeException("Unsupported data type: " + varType));
            return;
        }

        try {
            executor.getContext().declareVariable(varName, varType);
        } catch (Exception e) {
            listener.onFailure(e);
            return;
        }

        // Initialize variable with the expression's value
        executor.evaluateExpressionAsync(varCtx.expression(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                executor.getContext().setVariable(varName, value);
                // Proceed to the next variable declaration
                processVariableDeclarations(varDecls, index + 1, listener);
            }

            @Override
            public void onFailure(Exception e) {
                if (e.getMessage().equals("Null expression context"))  {
                    executor.getContext().setVariable(varName, null);
                    // Proceed to the next variable declaration
                    processVariableDeclarations(varDecls, index + 1, listener);
                } else {
                    listener.onFailure(e);
                }
            }
        });
    }
}
