/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.FunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The FunctionDefinitionHandler class is responsible for handling function definition statements
 * within the procedural SQL execution context. It parses the function name, parameters, body,
 * and registers the function in the execution context for later invocation.
 */
public class FunctionDefinitionHandler {
    private final ProcedureExecutor executor;

    /**
     * Constructs a FunctionDefinitionHandler with the given ProcedureExecutor.
     *
     * @param executor The ProcedureExecutor instance responsible for executing procedures.
     */
    public FunctionDefinitionHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Handles the function definition statement asynchronously.
     *
     * @param ctx      The Function_definitionContext representing the function definition statement.
     * @param listener The ActionListener to handle asynchronous callbacks.
     */
    public void handleAsync(PlEsqlProcedureParser.Function_definitionContext ctx, ActionListener<Object> listener) {
        try {
            // Retrieve the function name
            String functionName = ctx.ID().getText();

            // Check if the function is already defined
            if (executor.getContext().hasFunction(functionName)) {
                listener.onFailure(new RuntimeException("Function '" + functionName + "' is already defined."));
                return;
            }

            // Parse parameters if any
            List<String> parameterNames = new ArrayList<>();
            List<String> parameterTypes = new ArrayList<>();
            if (ctx.parameter_list() != null) {
                for (PlEsqlProcedureParser.ParameterContext paramCtx : ctx.parameter_list().parameter()) {
                    String paramName = paramCtx.ID().getText();
                    String paramType = paramCtx.datatype().getText().toUpperCase();

                    // Validate parameter data type
                    if ( isSupportedDataType(paramType) == false ) {
                        listener.onFailure(new RuntimeException("Unsupported data type '" + paramType
                            + "' for parameter '" + paramName + "' in function '" + functionName + "'."));
                        return;
                    }

                    // Check for duplicate parameter names
                    if (parameterNames.contains(paramName)) {
                        listener.onFailure(new RuntimeException("Duplicate parameter name '"
                            + paramName + "' in function '" + functionName + "'."));
                        return;
                    }

                    parameterNames.add(paramName);
                    parameterTypes.add(paramType);
                }
            }

            // Parse the function body
            List<PlEsqlProcedureParser.StatementContext> functionBody = extractFunctionBody(ctx);

            // Create a FunctionDefinition object
            FunctionDefinition functionDefinition = new FunctionDefinition(functionName, parameterNames, parameterTypes, functionBody);

            // Register the function in the execution context
            executor.getContext().declareFunction(functionName, functionDefinition);

            listener.onResponse(null); // Function defined successfully
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Executes the function with the given arguments asynchronously.
     *
     * @param functionName The name of the function to execute.
     * @param arguments    The list of argument values.
     * @param listener     The ActionListener to handle asynchronous callbacks.
     */
    public void executeFunctionAsync(String functionName, List<Object> arguments, ActionListener<Object> listener) {
        FunctionDefinition function = executor.getContext().getFunction(functionName);
        if (function == null) {
            listener.onFailure(new RuntimeException("Function '" + functionName + "' is not defined."));
            return;
        }

        List<String> paramNames = function.getParameters();
        List<String> paramTypes = function.getParameterTypes();

        if (paramNames.size() != arguments.size()) {
            listener.onFailure(new RuntimeException("Function '" + functionName + "' expects " + paramNames.size() +
                " arguments, but " + arguments.size() + " were provided."));
            return;
        }

        // Create a new ExecutionContext for the function (child of the current context)
        ExecutionContext functionContext = new ExecutionContext(executor.getContext());

        // Declare and set parameters in the function context
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String paramType = paramTypes.get(i);
            Object argValue = arguments.get(i);

            // Validate argument type
            if ( isArgumentTypeCompatible(paramType, argValue) == false ) {
                listener.onFailure(new RuntimeException("Type mismatch for parameter '" + paramName
                    + "'. Expected '" + paramType + "', but got '"
                    + (argValue != null ? argValue.getClass().getSimpleName() : "null") + "'."));
                return;
            }

            functionContext.declareVariable(paramName, paramType);
            functionContext.setVariable(paramName, argValue);
        }

        // Create a new ProcedureExecutor that uses the functionContext.
        // Since getClient() and getTokenStream() are not available, we pass null.
        ProcedureExecutor functionExecutor = new ProcedureExecutor(
            functionContext,
            executor.getThreadPool(),
            null,
            null
        );

        ActionListener<Object> functionBodyListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // If the result is wrapped in a ReturnValue, unwrap it;
                // otherwise, assume the result is the intended return value (even if null)
                Object returnValue = result instanceof ReturnValue
                    ? ((ReturnValue) result).getValue()
                    : result;
                listener.onResponse(returnValue);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ReturnValue) {
                    Object returnValue = ((ReturnValue) e).getValue();
                    listener.onResponse(returnValue);
                } else {
                    listener.onFailure(e);
                }
            }
        };

        ActionListener<Object> functionBodyLogger = ActionListenerUtils.withLogging(
            functionBodyListener,
            this.getClass().getName(),
            "Function-Body: " + function.getBody()
        );

        // Execute the function body asynchronously using the new executor.
        functionExecutor.executeStatementsAsync(function.getBody(), 0, functionBodyLogger);
    }

    /**
     * Extracts the list of statements constituting the function body.
     */
    private List<PlEsqlProcedureParser.StatementContext> extractFunctionBody(PlEsqlProcedureParser.Function_definitionContext ctx) {
        List<PlEsqlProcedureParser.StatementContext> functionBody = ctx.statement();
        if (functionBody == null || functionBody.isEmpty()) {
            throw new RuntimeException("Function '" + ctx.ID().getText() + "' does not have a valid body.");
        }
        return functionBody;
    }

    /**
     * Determines if the provided data type is supported.
     * Updated to support NUMBER, STRING, DATE, DOCUMENT, and ARRAY.
     */
    private boolean isSupportedDataType(String dataType) {
        switch (dataType.toUpperCase()) {
            case "NUMBER":
            case "STRING":
            case "DATE":
            case "DOCUMENT":
            case "ARRAY":
                return true;
            default:
                return false;  // Unsupported data type
        }
    }

    /**
     * Validates if the argument value is compatible with the expected data type.
     * Updated to support NUMBER, STRING, DATE, DOCUMENT, and ARRAY.
     */
    private boolean isArgumentTypeCompatible(String dataType, Object value) {
        if (value == null) {
            return true; // Allow null assignments
        }
        switch (dataType.toUpperCase()) {
            case "NUMBER":
                return value instanceof Double || value instanceof Integer;
            case "STRING":
                return value instanceof String;
            case "DATE":
                return value instanceof java.util.Date;
            case "DOCUMENT":
                return value instanceof Map;
            case "ARRAY":
                return value instanceof List;
            default:
                return false;
        }
    }
}
