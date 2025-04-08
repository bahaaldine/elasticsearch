/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.elasticsearch.xpack.plesql.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The FunctionDefinitionHandler class is responsible for handling function definition statements
 * within the procedural SQL execution context. It parses the function name, parameters, and body,
 * creates a FunctionDefinition (using a list of Parameter objects), and registers it in the execution context.
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
            // Retrieve the function name.
            String functionName = ctx.ID().getText();

            // Check if the function is already defined.
            if (executor.getContext().hasFunction(functionName)) {
                listener.onFailure(new RuntimeException("Function '" + functionName + "' is already defined."));
                return;
            }

            // Build the list of parameters (each with mode, name, and type).
            List<Parameter> parameters = new ArrayList<>();
            if (ctx.parameter_list() != null) {
                for (PlEsqlProcedureParser.ParameterContext paramCtx : ctx.parameter_list().parameter()) {
                    ParameterMode mode = ParameterMode.IN; // default mode
                    String paramName;
                    String paramType;
                    // Assuming grammar: parameter : (IN | OUT | INOUT)? ID datatype ;
                    if (paramCtx.getChild(0).getText().equalsIgnoreCase("IN") ||
                        paramCtx.getChild(0).getText().equalsIgnoreCase("OUT") ||
                        paramCtx.getChild(0).getText().equalsIgnoreCase("INOUT")) {
                        // Parameter mode is provided.
                        String modeStr = paramCtx.getChild(0).getText().toUpperCase();
                        mode = ParameterMode.valueOf(modeStr);
                        paramName = paramCtx.getChild(1).getText();
                        paramType = paramCtx.getChild(2).getText().toUpperCase();
                    } else {
                        // No mode provided; default to IN.
                        paramName = paramCtx.getChild(0).getText();
                        paramType = paramCtx.getChild(1).getText().toUpperCase();
                    }
                    // Validate parameter data type.
                    if ( isSupportedDataType(paramType) == false ) {
                        listener.onFailure(new RuntimeException("Unsupported data type '" + paramType +
                            "' for parameter '" + paramName + "' in function '" + functionName + "'."));
                        return;
                    }
                    // Check for duplicate parameter names.
                    for (Parameter existing : parameters) {
                        if (existing.getName().equals(paramName)) {
                            listener.onFailure(new RuntimeException("Duplicate parameter name '" +
                                paramName + "' in function '" + functionName + "'."));
                            return;
                        }
                    }
                    parameters.add(new Parameter(paramName, paramType, mode));
                }
            }

            // Parse the function body.
            List<PlEsqlProcedureParser.StatementContext> functionBody = extractFunctionBody(ctx);

            // Create a FunctionDefinition using the new Parameter list.
            FunctionDefinition functionDefinition = new FunctionDefinition(functionName, parameters, functionBody) {
                @Override
                public Object execute(List<Object> args) {
                    // The execution for user-defined functions would go here.
                    // This method might be overridden by a visitor handling function execution.
                    return null;
                }
            };

            // Register the function in the execution context.
            executor.getContext().declareFunction(functionName, functionDefinition);

            listener.onResponse(null); // Function defined successfully.
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    /**
     * Executes the function with the given arguments asynchronously.
     *
     * If the function's body is empty, it calls the overridden execute() method directly.
     * Otherwise, it creates a new ExecutionContext for the function call, declares and assigns
     * parameters (handling IN, OUT, and INOUT modes), and then executes the function body
     * asynchronously. After execution, for each OUT and INOUT parameter, it checks if the child
     * context was updated; if not (for INOUT), it uses the value returned by execute().
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

        List<Parameter> parameters = function.getParameters();
        if (parameters.size() != arguments.size()) {
            listener.onFailure(new RuntimeException("Function '" + functionName + "' expects " + parameters.size() +
                " arguments, but " + arguments.size() + " were provided."));
            return;
        }

        // Create a new ExecutionContext for the function (child of the parent's context).
        ExecutionContext functionContext = new ExecutionContext(executor.getContext());

        // Declare and assign parameters in the function context.
        for (int i = 0; i < parameters.size(); i++) {
            Parameter param = parameters.get(i);
            String paramName = param.getName();
            String paramType = param.getType();
            ParameterMode mode = param.getMode();
            Object argValue = arguments.get(i);

            // Validate the argument type.
            if ( isArgumentTypeCompatible(paramType, argValue) == false ) {
                listener.onFailure(new RuntimeException("Type mismatch for parameter '" + paramName +
                    "'. Expected '" + paramType + "', but got '" +
                    (argValue != null ? argValue.getClass().getSimpleName() : "null") + "'."));
                return;
            }

            // For IN and OUT parameters, declare in the child context.
            if (mode == ParameterMode.IN || mode == ParameterMode.OUT) {
                functionContext.declareVariable(paramName, paramType);
                if (mode == ParameterMode.IN) {
                    functionContext.setVariable(paramName, argValue);
                }
            }
            // For INOUT parameters, skip declaration so that the parent's variable is used.
        }

        // If the function body is empty, directly call execute().
        if (function.getBody() == null || function.getBody().isEmpty()) {
            Object execResult = function.execute(arguments);
            // Propagate OUT and INOUT parameters.
            for (Parameter param : parameters) {
                if (param.getMode() == ParameterMode.OUT || param.getMode() == ParameterMode.INOUT) {
                    // For INOUT, since it wasn't declared in the child, the lookup delegates to the parent.
                    Object updatedValue = functionContext.getVariable(param.getName());
                    if (updatedValue == null) {
                        updatedValue = executor.getContext().getVariable(param.getName());
                    }
                    executor.getContext().setVariable(param.getName(), updatedValue);
                }
            }
            Object returnValue = execResult instanceof ReturnValue ? ((ReturnValue) execResult).getValue() : execResult;
            listener.onResponse(returnValue);
            return;
        }

        // For functions with a non-empty body, execute asynchronously.
        ProcedureExecutor functionExecutor = new ProcedureExecutor(
            functionContext,
            executor.getThreadPool(),
            null,  // No client needed.
            null   // No token stream needed.
        );

        ActionListener<Object> functionBodyListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Propagate OUT and INOUT parameters back to the parent's context.
                for (Parameter param : parameters) {
                    if (param.getMode() == ParameterMode.OUT || param.getMode() == ParameterMode.INOUT) {
                        Object updatedValue = functionContext.getVariable(param.getName());
                        if (updatedValue == null) {
                            updatedValue = executor.getContext().getVariable(param.getName());
                        }
                        executor.getContext().setVariable(param.getName(), updatedValue);
                    }
                }
                Object returnValue = result instanceof ReturnValue ? ((ReturnValue) result).getValue() : result;
                listener.onResponse(returnValue);
            }

            @Override
            public void onFailure(Exception e) {
                for (Parameter param : parameters) {
                    if (param.getMode() == ParameterMode.OUT || param.getMode() == ParameterMode.INOUT) {
                        Object updatedValue = functionContext.getVariable(param.getName());
                        if (updatedValue == null) {
                            updatedValue = executor.getContext().getVariable(param.getName());
                        }
                        executor.getContext().setVariable(param.getName(), updatedValue);
                    }
                }
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

        functionExecutor.executeStatementsAsync(function.getBody(), 0, functionBodyLogger);
    }

    /**
     * Extracts the list of statements constituting the function body.
     *
     * @param ctx The Function_definitionContext.
     * @return The list of StatementContext objects.
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
     *
     * @param dataType The data type as a string.
     * @return true if supported; false otherwise.
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
                return false;
        }
    }

    /**
     * Validates if the given value is compatible with the expected data type.
     *
     * @param dataType The expected data type.
     * @param value    The value to check.
     * @return true if the value is compatible; false otherwise.
     */
    private boolean isArgumentTypeCompatible(String dataType, Object value) {
        if (value == null) {
            return true;
        }
        switch (dataType.toUpperCase()) {
            case "NUMBER":
                return value instanceof Double || value instanceof Integer;
            case "STRING":
                return value instanceof String;
            case "DATE":
                return value instanceof java.util.Date;
            case "DOCUMENT":
                return value instanceof java.util.Map;
            case "ARRAY":
                return value instanceof java.util.List;
            default:
                return false;
        }
    }
}
