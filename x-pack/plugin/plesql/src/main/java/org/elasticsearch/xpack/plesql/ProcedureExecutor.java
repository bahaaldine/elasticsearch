/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql;

import org.elasticsearch.xpack.plesql.handlers.AssignmentStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.plesql.handlers.IfStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.LoopStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.TryCatchStatementHandler;
import org.elasticsearch.xpack.plesql.interfaces.ExceptionListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureBaseVisitor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.FunctionDefinition;

public class ProcedureExecutor extends PlEsqlProcedureBaseVisitor<Object> {
    private ExecutionContext context = new ExecutionContext();

    // Instantiate handlers
    private AssignmentStatementHandler assignmentHandler;
    private DeclareStatementHandler declareHandler;
    private IfStatementHandler ifHandler;
    private LoopStatementHandler loopHandler;
    private FunctionDefinitionHandler functionDefHandler;
    private TryCatchStatementHandler tryCatchHandler;

    @SuppressWarnings("this-escape")
    public ProcedureExecutor(ExecutionContext context, ExceptionListener listener) {
        this.context = context;
        this.assignmentHandler = new AssignmentStatementHandler(context, this);
        this.declareHandler = new DeclareStatementHandler(context);
        this.ifHandler = new IfStatementHandler(context, this);
        this.loopHandler = new LoopStatementHandler(context, this);
        this.functionDefHandler = new FunctionDefinitionHandler(context);
        this.tryCatchHandler = new TryCatchStatementHandler(context, this);
    }

    @Override
    public Object visitProcedure(PlEsqlProcedureParser.ProcedureContext ctx) {
        // Create a new scope for the procedure block
        for (PlEsqlProcedureParser.StatementContext stmtCtx : ctx.statement()) {
            visit(stmtCtx);  // Process each statement
        }
        return null;
    }

    @Override
    public Object visitDeclare_statement(PlEsqlProcedureParser.Declare_statementContext ctx) {
        declareHandler.handle(ctx);
        return null;
    }

    @Override
    public Object visitAssignment_statement(PlEsqlProcedureParser.Assignment_statementContext ctx) {
        assignmentHandler.handle(ctx);
        return null;
    }

    @Override
    public Object visitIf_statement(PlEsqlProcedureParser.If_statementContext ctx) {
        ifHandler.handle(ctx);
        return null;
    }

    @Override
    public Object visitLoop_statement(PlEsqlProcedureParser.Loop_statementContext ctx) {
        loopHandler.handle(ctx);
        return null;
    }

    @Override
    public Object visitFunction_definition(PlEsqlProcedureParser.Function_definitionContext ctx) {
        functionDefHandler.handle(ctx);
        return null;
    }

    @Override
    public Object visitTry_catch_statement(PlEsqlProcedureParser.Try_catch_statementContext ctx) {
        // Use the tryCatchHandler to handle the try-catch block
        tryCatchHandler.handle(ctx);
        return null;
    }

    @Override
    public Object visitFunction_call(PlEsqlProcedureParser.Function_callContext ctx) {
        String functionName = ctx.ID().getText();
        FunctionDefinition function = context.getFunction(functionName);

        // Create a new execution context for the function
        ExecutionContext previousContext = context;
        context = new ExecutionContext();

        // Pass arguments to parameters
        if (ctx.argument_list() != null) {
            int paramCount = function.getParameters().size();
            int argCount = ctx.argument_list().expression().size();
            if (argCount != paramCount) {
                throw new RuntimeException("Function '" + functionName + "' expects " + paramCount + " arguments.");
            }

            for (int i = 0; i < paramCount; i++) {
                String paramName = function.getParameters().get(i);
                Object argValue = evaluateExpression(ctx.argument_list().expression(i));
                context.declareVariable(paramName, "UNKNOWN"); // You can specify the type if available
                context.setVariable(paramName, argValue);
            }
        }

        // Execute the function body
        for (PlEsqlProcedureParser.StatementContext stmtCtx : function.getBody()) {
            visit(stmtCtx);
        }

        // Optionally, get return value

        // Restore the previous context
        context = previousContext;

        return null;
    }

    @Override
    public Object visitExecute_statement(PlEsqlProcedureParser.Execute_statementContext ctx) {
        String esqlQuery = ctx.ESQL_QUERY().getText();
        esqlQuery = esqlQuery.substring(1, esqlQuery.length() - 2); // Remove surrounding parentheses and semicolon

        // Optionally, replace variable placeholders with actual values
        esqlQuery = substituteVariables(esqlQuery);

        Object result = executeEsqlQuery(esqlQuery);

        // Store or process the result as needed
        context.setVariable("result_of_query", result);
        return null;
    }

    @Override
    public Object visitFunction_call_statement(PlEsqlProcedureParser.Function_call_statementContext ctx) {
        return visitFunction_call(ctx.function_call());
    }


    // Helper methods

    private String substituteVariables(String esqlQuery) {
        // Simple substitution logic
        for (String varName : context.getVariableNames()) {
            Object value = context.getVariable(varName);
            esqlQuery = esqlQuery.replace(varName, value.toString());
        }
        return esqlQuery;
    }

    private Object executeEsqlQuery(String esqlQuery) {
        // Implement the logic to execute the ES|QL query using Elasticsearch APIs
        // For now, we'll just print the query and return a placeholder result
        System.out.println("Executing ESQL query: " + esqlQuery);
        // TODO: Integrate with Elasticsearch ESQL execution API
        return null; // Return actual query result
    }

    private Object evaluateExpression(PlEsqlProcedureParser.ExpressionContext ctx) {
        // Same as in handlers
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
            return visitFunction_call(ctx.function_call());
        } else {
            throw new RuntimeException("Unsupported expression: " + ctx.getText());
        }
    }
}
