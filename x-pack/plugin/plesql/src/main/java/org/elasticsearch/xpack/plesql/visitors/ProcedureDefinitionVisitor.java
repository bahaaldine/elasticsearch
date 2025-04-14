/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.visitors;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.primitives.procedure.ProcedureDefinition;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureBaseVisitor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor for procedure definitions.
 * Processes a procedure definition of the form:
 *
 *   PROCEDURE update_values(IN a NUMBER, OUT b NUMBER, INOUT c NUMBER)
 *   BEGIN
 *       -- procedure body
 *   END PROCEDURE;
 *
 * and registers it in the provided ExecutionContext.
 */
public class ProcedureDefinitionVisitor extends PlEsqlProcedureBaseVisitor<Object> {

    private final ExecutionContext context;

    public ProcedureDefinitionVisitor(ExecutionContext context) {
        this.context = context;
    }

    @Override
    public Object visitProcedure(PlEsqlProcedureParser.ProcedureContext ctx) {
        // Get the procedure name.
        String procName = ctx.ID().getText();

        // Ensure the procedure is not already defined.
        if (context.hasFunction(procName)) {
            throw new RuntimeException("Procedure '" + procName + "' is already defined.");
        }

        // Build the parameter list.
        List<Parameter> parameters = new ArrayList<>();
        if (ctx.parameter_list() != null) {
            for (PlEsqlProcedureParser.ParameterContext pCtx : ctx.parameter_list().parameter()) {
                ParameterMode mode = ParameterMode.IN; // Default mode
                String paramName;
                String paramType;
                // Grammar: parameter : (IN | OUT | INOUT)? ID datatype ;
                if (pCtx.getChild(0).getText().equalsIgnoreCase("IN") ||
                    pCtx.getChild(0).getText().equalsIgnoreCase("OUT") ||
                    pCtx.getChild(0).getText().equalsIgnoreCase("INOUT")) {
                    mode = ParameterMode.valueOf(pCtx.getChild(0).getText().toUpperCase());
                    paramName = pCtx.getChild(1).getText();
                    paramType = pCtx.getChild(2).getText().toUpperCase();
                } else {
                    paramName = pCtx.getChild(0).getText();
                    paramType = pCtx.getChild(1).getText().toUpperCase();
                }
                parameters.add(new Parameter(paramName, paramType, mode));
            }
        }

        // Retrieve the procedure body (list of statements).
        List<PlEsqlProcedureParser.StatementContext> body = ctx.statement();
        if (body == null || body.isEmpty()) {
            throw new RuntimeException("Procedure '" + procName + "' has an empty body.");
        }

        // Create a ProcedureDefinition instance.
        ProcedureDefinition procDef = new ProcedureDefinition(procName, parameters, body) {
            @Override
            public void execute(List<Object> args, ActionListener<Object> listener) {

            }

            @Override
            protected void executeProcedure(List<Object> args) {
                // For now, simply delegate execution to a ProcedureExecutor.
                // In your full implementation, you would execute each statement in the body.
                System.out.println("Executing procedure " + procName + " with arguments: " + args);
                // For example:
                // new ProcedureExecutor(childContext, threadPool, client, tokens).executeStatementsAsync(body, 0, someListener);
            }
        };

        // Register the procedure in the ExecutionContext.
        context.declareFunction(procName, procDef);

        // Return null (procedure definitions don't produce a runtime value).
        return null;
    }
}
