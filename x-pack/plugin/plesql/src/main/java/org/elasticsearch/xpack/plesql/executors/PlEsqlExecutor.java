/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.executors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.functions.builtin.datasources.ESFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datasources.EsqlBuiltInFunctions;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.handlers.ProcedureCallHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.DateBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.plesql.primitives.procedure.ProcedureDefinition;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;
import org.elasticsearch.xpack.plesql.visitors.ProcedureDefinitionVisitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlEsqlExecutor {

    private final ThreadPool threadPool;
    private final Client client;

    @Inject
    public PlEsqlExecutor(ThreadPool threadPool, Client client) {
        this.threadPool = threadPool;
        this.client = client;
    }

    /**
     * Asynchronously executes a PL|ESQL procedure.
     *
     * This method:
     *  1. Parses the input using the 'procedure' rule.
     *  2. Creates a fresh global ExecutionContext.
     *  3. Visits the procedure block to register any procedure definitions.
     *  4. Iterates over the statements to detect and synchronously process procedure calls
     *     (to update the global context).
     *  5. Removes those definition/call statements from the list.
     *  6. Creates a ProcedureExecutor with the updated global context and executes the remaining statements.
     *
     * @param procedureText The PLESQL procedure text.
     * @param listener      The ActionListener to notify when execution is complete.
     */
    public void executeProcedure(String procedureText, ActionListener<Object> listener) {
        threadPool.generic().execute(() -> {
            try {
                // 1. Parse the procedure block using the 'procedure' rule.
                PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(procedureText));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new PlEsqlErrorListener());
                PlEsqlProcedureParser.ProcedureContext procCtx = parser.procedure();

                // 2. Create a fresh global ExecutionContext.
                ExecutionContext executionContext = new ExecutionContext();

                // 3. Register built-in functions.
                StringBuiltInFunctions.registerAll(executionContext);
                NumberBuiltInFunctions.registerAll(executionContext);
                ArrayBuiltInFunctions.registerAll(executionContext);
                DateBuiltInFunctions.registerAll(executionContext);

                // 4. Visit the procedure block to find and register procedure definitions.
                ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(executionContext);
                for (PlEsqlProcedureParser.StatementContext stmtCtx : procCtx.statement()) {
                    // If the statement is a procedure definition, its first token is "PROCEDURE".
                    if (stmtCtx.getChildCount() > 0 && "PROCEDURE".equalsIgnoreCase(stmtCtx.getChild(0).getText())) {
                        procDefVisitor.visit(stmtCtx);
                    }
                }

                // 5. Preprocess the procedure block to update the context for any procedure calls.
                // Here we iterate through all statements and for any that are procedure calls,
                // we synchronously process them using the ProcedureCallHandler.
                ProcedureCallHandler procCallHandler = new ProcedureCallHandler(new ProcedureExecutor(executionContext,
                    threadPool, client, tokens));
                List<PlEsqlProcedureParser.StatementContext> executableStatements = new ArrayList<>();
                for (Iterator<PlEsqlProcedureParser.StatementContext> it = procCtx.statement().iterator(); it.hasNext(); ) {
                    PlEsqlProcedureParser.StatementContext stmt = it.next();
                    if (isProcedureCall(stmt, executionContext)) {
                        // Extract the procedure call details (procedure name, arguments) and process it.
                        // Here we call a synchronous version of the procedure call handler.
                        procCallHandler.executeProcedureCallAsync(
                            // This handler method should use your handler signature:
                            // (String procedureName, List<Object> arguments, ActionListener<Void> listener)
                            extractProcedureName(stmt),
                            extractProcedureArguments(stmt),
                            new ActionListener<Void>() {
                                @Override
                                public void onResponse(Void aVoid) {
                                    // Successfully updated the global context.
                                }
                                @Override
                                public void onFailure(Exception e) {
                                    // Log or handle error if needed.
                                }
                            }
                        );
                        // Optionally, do not include this statement in executableStatements.
                    } else {
                        executableStatements.add(stmt);
                    }
                }
                // Replace the statement list in procCtx with only executable statements.
                procCtx.statement().clear();
                procCtx.statement().addAll(executableStatements);

                // 6. Create a ProcedureExecutor with the updated global context.
                ProcedureExecutor procedureExecutor = new ProcedureExecutor(executionContext, threadPool, client, tokens);
                EsqlBuiltInFunctions.registerAll(executionContext, procedureExecutor, client);
                ESFunctions.registerGetDocumentFunction(executionContext, client);
                ESFunctions.registerUpdateDocumentFunction(executionContext, client);
                ESFunctions.registerIndexBulkFunction(executionContext, client);
                ESFunctions.registerIndexDocumentFunction(executionContext, client);
                ESFunctions.registerRefreshIndexFunction(executionContext, client);

                // 7. Set up a logging listener.
                ActionListener<Object> execListener = ActionListenerUtils.withLogging(listener,
                    this.getClass().getName(), "ExecutePLESQLProcedure-" + procedureText);
                // 8. Execute the procedure block asynchronously.
                procedureExecutor.visitProcedureAsync(procCtx, execListener);

            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    /**
     * Helper method to determine if a statement is a procedure call.
     * In this example, we assume procedure calls are written using function_call_statement,
     * and we check if the called name's definition is an instance of ProcedureDefinition.
     *
     * @param stmt          The statement context.
     * @param globalContext The global ExecutionContext.
     * @return true if the statement is a procedure call; false otherwise.
     */
    private boolean isProcedureCall(PlEsqlProcedureParser.StatementContext stmt, ExecutionContext globalContext) {
        if (stmt.function_call_statement() != null) {
            String callName = stmt.function_call_statement().function_call().ID().getText();
            try {
                return globalContext.getFunction(callName) instanceof ProcedureDefinition;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Extracts the procedure name from a statement assumed to be a procedure call.
     *
     * @param stmt The statement context.
     * @return The procedure name.
     */
    private String extractProcedureName(PlEsqlProcedureParser.StatementContext stmt) {
        return stmt.function_call_statement().function_call().ID().getText();
    }

    /**
     * Extracts the procedure arguments from a statement assumed to be a procedure call.
     * This example simply evaluates the argument expressions as Doubles.
     *
     * @param stmt The statement context.
     * @return A list of evaluated argument values.
     */
    private List<Object> extractProcedureArguments(PlEsqlProcedureParser.StatementContext stmt) {
        if (stmt.function_call_statement().function_call().argument_list() != null) {
            List<PlEsqlProcedureParser.ExpressionContext> exprs =
                stmt.function_call_statement().function_call().argument_list().expression();
            List<Object> args = new ArrayList<>();
            for (PlEsqlProcedureParser.ExpressionContext expr : exprs) {
                try {
                    args.add(Double.valueOf(expr.getText()));
                } catch (NumberFormatException e) {
                    args.add(expr.getText());
                }
            }
            return args;
        }
        return new ArrayList<>();
    }
}
