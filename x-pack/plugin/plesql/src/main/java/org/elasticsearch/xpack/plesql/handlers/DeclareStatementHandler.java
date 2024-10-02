/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

public class DeclareStatementHandler {
    private ExecutionContext context;

    public DeclareStatementHandler(ExecutionContext context) {
        this.context = context;
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

            System.out.println( "VAR TYPE:" + varType);

            // Check if the data type is unsupported
            if (isSupportedDataType(varType) == false) {
                throw new RuntimeException("Unsupported data type: " + varType);
            }

            Object initialValue = null;
            if (varCtx.expression() != null) {
                initialValue = evaluateExpression(varCtx.expression());
            }
            context.declareVariable(varName, varType);
            if (initialValue != null) {
                context.setVariable(varName, initialValue);
            }
        }
    }

    private Object evaluateExpression(PlEsqlProcedureParser.ExpressionContext ctx) {
        if (ctx.INT() != null) {
            return Integer.parseInt(ctx.INT().getText());
        } else if (ctx.FLOAT() != null) {
            return Double.parseDouble(ctx.FLOAT().getText());
        } else if (ctx.STRING() != null) {
            return ctx.STRING().getText().substring(1, ctx.STRING().getText().length() - 1);
        } else if (ctx.ID() != null && ctx.getChildCount() == 1) {
            return context.getVariable(ctx.ID().getText());
        } else if (ctx.op != null) {
            Object left = evaluateExpression(ctx.expression(0));
            Object right = evaluateExpression(ctx.expression(1));
            switch (ctx.op.getType()) {
                case PlEsqlProcedureParser.PLUS:
                    return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                case PlEsqlProcedureParser.MINUS:
                    return ((Number) left).doubleValue() - ((Number) right).doubleValue();
                case PlEsqlProcedureParser.MULTIPLY:
                    return ((Number) left).doubleValue() * ((Number) right).doubleValue();
                case PlEsqlProcedureParser.DIVIDE:
                    return ((Number) left).doubleValue() / ((Number) right).doubleValue();
                default:
                    throw new RuntimeException("Unknown operator: " + ctx.op.getText());
            }
        } else if (ctx.function_call() != null) {
            // TODO: Handle function calls if necessary
            return null;
        } else {
            throw new RuntimeException("Unsupported expression: " + ctx.getText());
        }
    }
}
