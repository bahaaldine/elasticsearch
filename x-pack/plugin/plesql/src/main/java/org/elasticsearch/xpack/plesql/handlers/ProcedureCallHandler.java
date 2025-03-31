/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the
 * Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.procedure.ProcedureDefinition;
import org.elasticsearch.xpack.plesql.primitives.functions.Parameter;
import org.elasticsearch.xpack.plesql.primitives.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.util.List;

public class ProcedureCallHandler {

    private final ProcedureExecutor executor;

    public ProcedureCallHandler(ProcedureExecutor executor) {
        this.executor = executor;
    }

    /**
     * Executes a procedure call asynchronously.
     * Procedures do not return a value; instead, the focus is on propagating updated
     * OUT and INOUT parameter values back to the caller’s (global) context.
     *
     * @param procedureName The name of the procedure to execute.
     * @param arguments     The list of argument values.
     * @param listener      The ActionListener to notify upon completion.
     */
    public void executeProcedureCallAsync(String procedureName, List<Object> arguments, ActionListener<Void> listener) {
        // Retrieve the procedure definition (assumed to be stored as a ProcedureDefinition).
        ProcedureDefinition procDef = (ProcedureDefinition) executor.getContext().getFunction(procedureName);
        if (procDef == null) {
            listener.onFailure(new RuntimeException("Procedure '" + procedureName + "' is not defined."));
            return;
        }

        List<Parameter> parameters = procDef.getParameters();
        if (parameters.size() != arguments.size()) {
            listener.onFailure(new RuntimeException("Procedure '" + procedureName + "' expects " + parameters.size() +
                " arguments, but " + arguments.size() + " were provided."));
            return;
        }

        // Create a child ExecutionContext for the procedure call.
        // For INOUT parameters, we intentionally do not declare them here so that the parent's variable is used.
        ExecutionContext procContext = new ExecutionContext(executor.getContext());

        // Declare and assign IN and OUT parameters in the child context.
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

            // For IN and OUT parameters, declare them in the child context.
            if (mode == ParameterMode.IN || mode == ParameterMode.OUT) {
                procContext.declareVariable(paramName, paramType);
                if (mode == ParameterMode.IN) {
                    procContext.setVariable(paramName, argValue);
                }
                // OUT parameters remain uninitialized.
            }
            // For INOUT parameters, we do not declare them in the child,
            // so that the lookup falls back to the parent's variable (pass-by-reference).
        }

        // If the procedure body is empty, call execute() directly.
        if (procDef.getBody() == null || procDef.getBody().isEmpty()) {
            procDef.execute(arguments);
            propagateParameters(procContext, parameters);
            listener.onResponse(null);
            return;
        }

        // Otherwise, execute the procedure body asynchronously.
        ProcedureExecutor procExecutor = new ProcedureExecutor(
            procContext,
            executor.getThreadPool(),
            null,  // No client needed.
            null   // No token stream needed.
        );

        ActionListener<Object> procBodyListener = new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                propagateParameters(procContext, parameters);
                listener.onResponse(null);
            }
            @Override
            public void onFailure(Exception e) {
                propagateParameters(procContext, parameters);
                listener.onFailure(e);
            }
        };

        ActionListener<Object> procBodyLogger = ActionListenerUtils.withLogging(
            procBodyListener,
            this.getClass().getName(),
            "Procedure-Body: " + procDef.getBody()
        );

        procExecutor.executeStatementsAsync(procDef.getBody(), 0, procBodyLogger);
    }

    /**
     * Propagates updated OUT and INOUT parameters from the procedure's child context back into the parent's context.
     *
     * @param childContext The child ExecutionContext used for procedure execution.
     * @param parameters   The list of procedure parameters.
     */
    private void propagateParameters(ExecutionContext childContext, List<Parameter> parameters) {
        for (Parameter param : parameters) {
            if (param.getMode() == ParameterMode.OUT || param.getMode() == ParameterMode.INOUT) {
                Object updatedValue = childContext.getVariable(param.getName());
                if (updatedValue == null) {
                    updatedValue = executor.getContext().getVariable(param.getName());
                }
                executor.getContext().setVariable(param.getName(), updatedValue);
            }
        }
    }

    /**
     * Helper method to validate whether a given value is compatible with the expected data type.
     *
     * @param dataType The expected data type (as a string).
     * @param value    The value to check.
     * @return true if the value is compatible; false otherwise.
     */
    private boolean isArgumentTypeCompatible(String dataType, Object value) {
        if (value == null) {
            return true;
        }
        switch (dataType.toUpperCase()) {
            case "NUMBER":
                return value instanceof Number;
            case "STRING":
                return value instanceof String;
            // Extend for other types such as DATE, DOCUMENT, ARRAY, etc.
            default:
                return false;
        }
    }
}
