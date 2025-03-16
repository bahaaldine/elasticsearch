/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.PLESQLDataType;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DeclareStatementHandler {
    private final ProcedureExecutor executor;

    public DeclareStatementHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Checks whether the provided variable type string represents a supported data type.
     * Supported types include NUMBER, STRING, DATE, and ARRAY.
     * For ARRAY types, the syntax "ARRAY" or "ARRAY OF elementType" is allowed.
     * This method also handles cases where the input may lack the expected spaces (e.g. "ARRAYOFSTRING").
     *
     * @param varType the variable type string (e.g. "NUMBER", "ARRAY", "ARRAY OF STRING")
     * @return true if the type is supported, false otherwise.
     */
    private boolean isSupportedDataType(String varType) {
        // Normalize and trim the type string.
        String normalizedType = varType.trim().toUpperCase();

        // If the type starts with "ARRAY", check for the "OF" clause.
        if (normalizedType.startsWith("ARRAY")) {
            // If the token comes in as "ARRAYOF..." without a space, insert the space.
            if ( normalizedType.startsWith("ARRAYOF") ) {
                normalizedType = "ARRAY OF " + normalizedType.substring("ARRAYOF".length());
                String elementType = normalizedType.substring("ARRAY OF ".length()).trim();
                try {
                    // Verify that the element type is a valid base type.
                    PLESQLDataType.valueOf(elementType);
                    System.out.println("NORMALIZED TYPE : " + normalizedType );
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            return false;
        } else {
            try {
                PLESQLDataType type = PLESQLDataType.valueOf(normalizedType);
                // For non-array types, allow NUMBER, STRING, and DATE.
                return (type == PLESQLDataType.NUMBER || type == PLESQLDataType.STRING || type == PLESQLDataType.DATE);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    public void handleAsync(PlEsqlProcedureParser.Declare_statementContext ctx, ActionListener<Object> listener) {
        List<PlEsqlProcedureParser.Variable_declarationContext> varDecls =
            ctx.variable_declaration_list().variable_declaration();
        processVariableDeclarations(varDecls, 0, listener);
    }

    private void processVariableDeclarations(List<PlEsqlProcedureParser.Variable_declarationContext> varDecls,
                                             int index, ActionListener<Object> listener) {
        if (index >= varDecls.size()) {
            listener.onResponse(null); // All declarations succeeded
            return;
        }

        PlEsqlProcedureParser.Variable_declarationContext varCtx = varDecls.get(index);
        String varName = varCtx.ID().getText();
        String varType = varCtx.datatype().getText();
        String elementType = null;

        // Check if the datatype is supported.
        if ( isSupportedDataType(varType) == false ) {
            listener.onFailure(new RuntimeException("Unsupporteddddd data type: " + varType));
            return;
        }

        // If the type indicates an array, try to extract the element type.
        if (varType.toUpperCase().startsWith("ARRAY")) {
            // For a declaration like "ARRAY OF NUMBER", split on "OF" (case-insensitive)
            String[] parts = varType.split("(?i)OF");
            if (parts.length == 2) {
                elementType = parts[1].trim();
            } else {
                // Optionally default to ANY if no element type is specified.
                elementType = "ANY";
            }
        }

        // Now, declare the variable.
        try {
            // Assume ExecutionContext now has declareVariable(String, String, String)
            if (elementType != null) {
                executor.getContext().declareVariable(varName, varType, elementType);
            } else {
                executor.getContext().declareVariable(varName, varType);
            }
        } catch (Exception e) {
            listener.onFailure(e);
            return;
        }

        ActionListener<Object> variableDeclarationListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object value) {
                executor.getContext().setVariable(varName, value);
                processVariableDeclarations(varDecls, index + 1, listener);
            }

            @Override
            public void onFailure(Exception e) {
                if ("Null expression context".equals(e.getMessage())) {
                    executor.getContext().setVariable(varName, null);
                    processVariableDeclarations(varDecls, index + 1, listener);
                } else {
                    listener.onFailure(e);
                }
            }
        };

        ActionListener<Object> variableDeclarationLogger =
            ActionListenerUtils.withLogging(variableDeclarationListener,
                this.getClass().getName(),
                "Variable-Declaration: " + varName + " - " + varType +
                    (elementType != null ? " (Element Type: " + elementType + ")" : ""));

        // Process the initializer expression (if any)
        if (varCtx.expression() != null) {
            if (varType.toUpperCase().contains("ARRAY")) {
                // For ARRAY types, expect a JSON array literal.
                String exprText = varCtx.expression().getText().trim();
                try {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(exprText.getBytes(StandardCharsets.UTF_8));
                    try (XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                        .createParser(NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, inputStream)) {
                        if (parser.nextToken() != XContentParser.Token.START_ARRAY) {
                            throw new RuntimeException("Expected a JSON array literal for ARRAY type");
                        }
                        List<Object> arrayValue = parser.list();
                        // (Optionally, you might want to coerce each element to the declared elementType here.)
                        variableDeclarationLogger.onResponse(arrayValue);
                    }
                } catch (Exception e) {
                    variableDeclarationLogger.onFailure(e);
                }
            } else {
                executor.evaluateExpressionAsync(varCtx.expression(), variableDeclarationLogger);
            }
        } else {
            variableDeclarationLogger.onFailure(new RuntimeException("Null expression context"));
        }
    }
}
