/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

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

    public void handle(PlEsqlProcedureParser.Declare_statementContext ctx) {
        for (PlEsqlProcedureParser.Variable_declarationContext varCtx : ctx.variable_declaration_list().variable_declaration()) {
            String varName = varCtx.ID().getText();
            String varType = varCtx.datatype().getText();

            // Check if the data type is unsupported
            if (isSupportedDataType(varType) == false) {
                throw new RuntimeException("Unsupported data type: " + varType);
            }

            Object initialValue = null;
            if (varCtx.expression() != null) {
                initialValue = executor.evaluateExpression(varCtx.expression());
            }

            executor.getContext().declareVariable(varName, varType);
            if (initialValue != null) {
                executor.getContext().setVariable(varName, initialValue);
            }
        }
    }
}
