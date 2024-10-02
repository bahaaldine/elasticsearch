/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */


package org.elasticsearch.xpack.plesql.handlers;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.FunctionDefinition;

public class FunctionDefinitionHandler {
    private ExecutionContext context;

    public FunctionDefinitionHandler(ExecutionContext context) {
        this.context = context;
    }

    public void handle(PlEsqlProcedureParser.Function_definitionContext ctx) {
        String functionName = ctx.ID().getText();

        // Validate that the function has a name
        if (functionName == null || functionName.isEmpty()) {
            throw new RuntimeException("Function name is missing.");
        }

        // Validate parameters and check for missing types
        List<String> parameters = new ArrayList<>();
        if (ctx.parameter_list() != null) {
            for (PlEsqlProcedureParser.ParameterContext paramCtx : ctx.parameter_list().parameter()) {
                if (paramCtx.ID() == null || paramCtx.datatype() == null) {
                    throw new RuntimeException("Parameter '" + paramCtx.ID().getText() + "' is missing a type.");
                }
                parameters.add(paramCtx.ID().getText());
            }
        }

        // Validate the function body (must contain at least one statement)
        if (ctx.statement() == null || ctx.statement().isEmpty()) {
            throw new RuntimeException("Function body is missing.");
        }

        // Define the function in the execution context
        FunctionDefinition function = new FunctionDefinition(functionName, parameters, ctx.statement());
        context.defineFunction(functionName, function);
    }
}
