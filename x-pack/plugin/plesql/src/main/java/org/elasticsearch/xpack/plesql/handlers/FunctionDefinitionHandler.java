/*
 * Copyright Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.FunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;

import java.util.ArrayList;
import java.util.List;

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
     * Handles the function definition statement by parsing the function name, parameters, and body,
     * then registering the function in the execution context.
     *
     * @param ctx The Function_definitionContext representing the function definition statement.
     */
    public void handle(PlEsqlProcedureParser.Function_definitionContext ctx) {
        // Retrieve the function name
        String functionName = ctx.ID().getText();

        // Check if the function is already defined
        if (executor.getContext().hasFunction(functionName)) {
            throw new RuntimeException("Function '" + functionName + "' is already defined.");
        }

        // Parse parameters if any
        List<String> parameterNames = new ArrayList<>();
        List<String> parameterTypes = new ArrayList<>();
        if (ctx.parameter_list() != null) {
            for (PlEsqlProcedureParser.ParameterContext paramCtx : ctx.parameter_list().parameter()) {
                String paramName = paramCtx.ID().getText();
                String paramType = paramCtx.datatype().getText().toUpperCase();

                // Validate parameter data type
                if ( isSupportedDataType(paramType) == false) {
                    throw new RuntimeException("Unsupported data type '" + paramType + "' for parameter '"
                        + paramName + "' in function '" + functionName + "'.");
                }

                // Check for duplicate parameter names
                if (parameterNames.contains(paramName)) {
                    throw new RuntimeException("Duplicate parameter name '" + paramName + "' in function '" + functionName + "'.");
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

    }

    /**
     * Extracts the list of statements constituting the function body from the Function_definitionContext.
     *
     * @param ctx The Function_definitionContext.
     * @return A list of StatementContext representing the function body.
     */
    private List<PlEsqlProcedureParser.StatementContext> extractFunctionBody(PlEsqlProcedureParser.Function_definitionContext ctx) {
        // In your grammar, the function body is directly defined as a series of statements between BEGIN and END FUNCTION
        // Therefore, you can directly retrieve the list of statements using ctx.statement()

        List<PlEsqlProcedureParser.StatementContext> functionBody = ctx.statement();
        if (functionBody == null || functionBody.isEmpty()) {
            throw new RuntimeException("Function '" + ctx.ID().getText() + "' does not have a valid body.");
        }

        return functionBody;
    }

    /**
     * Determines if the provided data type is supported for function parameters and return types.
     *
     * @param dataType The data type as a String.
     * @return true if the data type is supported; false otherwise.
     */
    private boolean isSupportedDataType(String dataType) {
        // Add supported types to this list
        switch (dataType.toUpperCase()) {
            case "INT":
            case "FLOAT":
            case "STRING":
            case "DATE":
                return true;
            default:
                return false;  // Unsupported data type
        }
    }

    /**
     * Executes the function with the given arguments.
     *
     * @param functionName The name of the function to execute.
     * @param arguments    The list of argument values.
     * @return The result of the function execution.
     */
    public Object executeFunction(String functionName, List<Object> arguments) {
        FunctionDefinition function = executor.getContext().getFunction(functionName);
        if (function == null) {
            throw new RuntimeException("Function '" + functionName + "' is not defined.");
        }

        List<String> paramNames = function.getParameters();
        List<String> paramTypes = function.getParameterTypes();

        if (paramNames.size() != arguments.size()) {
            throw new RuntimeException("Function '" + functionName + "' expects " + paramNames.size() + " arguments, but "
                + arguments.size() + " were provided.");
        }

        // Create a new ExecutionContext for the function
        ExecutionContext functionContext = new ExecutionContext(executor.getContext());

        // Declare and set parameters in the function context
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            String paramType = paramTypes.get(i);
            Object argValue = arguments.get(i);

            // Validate argument type
            if ( isArgumentTypeCompatible(paramType, argValue) == false ) {
                throw new RuntimeException("Type mismatch for parameter '" + paramName + "'. Expected '" + paramType + "', but got '"
                    + argValue.getClass().getSimpleName() + "'.");
            }

            functionContext.declareVariable(paramName, paramType);
            functionContext.setVariable(paramName, argValue);
        }

        // Temporarily set the executor's context to the function context
        ExecutionContext originalContext = executor.getContext();
        executor.setContext(functionContext);

        try {
            // Execute the function body
            for (PlEsqlProcedureParser.StatementContext stmtCtx : function.getBody()) {
                executor.visitStatement(stmtCtx);
            }
        } catch (ReturnValue rv) {
            // Retrieve the return value from the exception
            return rv.getValue();
        } finally {
            // Restore the original context
            executor.setContext(originalContext);
        }

        // If no RETURN statement was encountered, return null or throw an error
        throw new RuntimeException("Function '" + functionName + "' did not return a value.");
    }

    /**
     * Validates if the argument value is compatible with the expected data type.
     *
     * @param dataType The expected data type as a String.
     * @param value    The argument value.
     * @return true if compatible; false otherwise.
     */
    private boolean isArgumentTypeCompatible(String dataType, Object value) {
        if (value == null) {
            return true; // Allow null assignments
        }

        switch (dataType.toUpperCase()) {
            case "INT":
                return value instanceof Integer;
            case "FLOAT":
                return value instanceof Float || value instanceof Double || value instanceof Integer;
            case "STRING":
                return value instanceof String;
            case "DATE":
                return value instanceof java.util.Date;
            default:
                return false;
        }
    }
}
