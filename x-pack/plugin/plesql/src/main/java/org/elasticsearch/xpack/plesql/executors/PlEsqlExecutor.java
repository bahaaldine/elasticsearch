/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.executors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.functions.builtin.datasources.ESFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datasources.EsqlBuiltInFunctions;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.DateBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.plesql.procedure.ProcedureDefinition;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;
import org.elasticsearch.xpack.plesql.visitors.ProcedureDefinitionVisitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlEsqlExecutor {

    private final ThreadPool threadPool;
    private final Client client;

    private static final Logger LOGGER = LogManager.getLogger(PlEsqlExecutor.class);

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
    public void executeProcedure(String procedureText, Map<String, Object> args, ActionListener<Object> listener) {
        threadPool.generic().execute(() -> {
            try {
                // 1. Parse the procedure
                PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(procedureText));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new PlEsqlErrorListener());

                PlEsqlProcedureParser.ProgramContext programContext = parser.program();

                if (programContext.call_procedure_statement() != null) {
                    getProcedureAsync(programContext.call_procedure_statement().ID().getText(), new ActionListener<Map<String, Object>>() {
                        @Override
                        public void onResponse(Map<String, Object> stringObjectMap) {
                            Object procedureContentObj = stringObjectMap.get("procedure");
                            if (procedureContentObj == null) {
                                listener.onFailure(new IllegalArgumentException("Procedure content is missing"));
                                return;
                            }

                            String procedureContent = procedureContentObj.toString();
                            LOGGER.info("Executing stored procedure [{}]: {}",
                                programContext.call_procedure_statement().ID().getText(), procedureContent);

                            try {
                                PlEsqlProcedureLexer storedLexer = new PlEsqlProcedureLexer(CharStreams.fromString(procedureContent));
                                CommonTokenStream storedTokens = new CommonTokenStream(storedLexer);
                                PlEsqlProcedureParser storedParser = new PlEsqlProcedureParser(storedTokens);
                                storedParser.removeErrorListeners();
                                storedParser.addErrorListener(new PlEsqlErrorListener());
                                PlEsqlProcedureParser.ProcedureContext procCtx = storedParser.procedure();

                                // Create fresh context and bind parameters
                                ExecutionContext executionContext = new ExecutionContext();
                                List<PlEsqlProcedureParser.ParameterContext> parameterContexts =
                                    procCtx.parameter_list() != null ? procCtx.parameter_list().parameter() : List.of();

                                if (parameterContexts.isEmpty() == false) {
                                    if (args == null) {
                                        listener.onFailure(
                                            new IllegalArgumentException("Procedure expects arguments but none were provided"));
                                        return;
                                    }
                                    if (parameterContexts.size() != ((Map<?, ?>) args.get("params")).size()) {
                                        listener.onFailure(
                                            new IllegalArgumentException("Mismatch between declared parameters and provided arguments"));
                                        return;
                                    }

                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> params = (Map<String, Object>) args.get("params");

                                    for (var param : parameterContexts) {
                                        String paramName = param.ID().getText();
                                        String paramType = param.datatype().getText().toUpperCase(java.util.Locale.ROOT);
                                        Object value = params.get(paramName);
                                        executionContext.declareVariable(paramName, paramType);
                                        executionContext.setVariable(paramName, value);
                                    }
                                }

                                // Visit inner procedure definitions
                                ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(executionContext);
                                for (PlEsqlProcedureParser.StatementContext stmtCtx : procCtx.statement()) {
                                    if (stmtCtx.getChildCount() > 0 && "PROCEDURE".equalsIgnoreCase(stmtCtx.getChild(0).getText())) {
                                        procDefVisitor.visit(stmtCtx);
                                    }
                                }

                                ProcedureExecutor procedureExecutor = new ProcedureExecutor(executionContext, threadPool, client, storedTokens);
                                EsqlBuiltInFunctions.registerAll(executionContext, procedureExecutor, client);
                                ESFunctions.registerGetDocumentFunction(executionContext, client);
                                ESFunctions.registerUpdateDocumentFunction(executionContext, client);
                                ESFunctions.registerIndexBulkFunction(executionContext, client);
                                ESFunctions.registerIndexDocumentFunction(executionContext, client);
                                ESFunctions.registerRefreshIndexFunction(executionContext, client);
                                StringBuiltInFunctions.registerAll(executionContext);
                                NumberBuiltInFunctions.registerAll(executionContext);
                                ArrayBuiltInFunctions.registerAll(executionContext);
                                DateBuiltInFunctions.registerAll(executionContext);

                                ActionListener<Object> execListener = ActionListenerUtils.withLogging(listener,
                                    this.getClass().getName(), "ExecuteStoredProcedure-" + procedureContent);

                                procedureExecutor.visitProcedureAsync(procCtx, execListener);
                            } catch (Exception e) {
                                listener.onFailure(e);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    });
                } else if (programContext.delete_procedure_statement() != null) {
                    PlEsqlProcedureParser.Delete_procedure_statementContext deleteContext = programContext.delete_procedure_statement();
                    String procedureId = deleteContext.ID().getText();
                    LOGGER.info("Deleting procedure {}", procedureId);
                    deleteProcedureAsync(procedureId, listener);
                } else if (programContext.create_procedure_statement() != null ) {
                    PlEsqlProcedureParser.Create_procedure_statementContext createContext = programContext.create_procedure_statement();
                    String procedureId = createContext.procedure().ID().getText();

                    Token start = createContext.procedure().getStart();
                    Token stop = createContext.procedure().getStop();
                    String rawProcedureText = tokens.getText(start, stop);

                    LOGGER.info("Storing procedure {}", procedureId);
                    storeProcedureAsync( procedureId, rawProcedureText, listener );
                } else {
                    listener.onFailure(new IllegalArgumentException("Unsupported top-level statement"));
                }
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

    /**
     * Asynchronously stores the procedure text into a dedicated Elasticsearch index.
     *
     * @param id            The document ID to use for the procedure.
     * @param procedureText The procedure text to store.
     * @param listener      The ActionListener to notify on completion or error.
     */

    public void storeProcedureAsync(String id, String procedureText, ActionListener<Object> listener) throws IOException {
        String indexName = ".plesql_procedures";

        LOGGER.info( "Storing procedure {}", procedureText );

        // Parse the procedure to extract parameters
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(procedureText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new PlEsqlErrorListener());
        PlEsqlProcedureParser.ProcedureContext procCtx = parser.procedure();

        List<Map<String, Object>> parameters = new ArrayList<>();
        if (procCtx != null && procCtx.parameter_list() != null) {
            for (var paramCtx : procCtx.parameter_list().parameter()) {
                String paramName = paramCtx.ID().getText();
                String paramType = paramCtx.datatype().getText().toUpperCase();
                parameters.add(Map.of("name", paramName, "type", paramType));
            }
        }

        GetIndexRequest request = new GetIndexRequest().indices(indexName);
        client.admin().indices().getIndex(request, ActionListener.wrap(
            getIndexResponse -> {
                indexProcedureDocument(id, procedureText, parameters, listener);
            },
            error -> {
                if (ExceptionsHelper.unwrapCause(error) instanceof IndexNotFoundException) {
                    LOGGER.info( "Index {} does not exist, creating it ...", indexName );
                    CreateIndexRequest createRequest = new CreateIndexRequest(indexName);
                    client.admin().indices().create(createRequest, ActionListener.wrap(
                        createResponse -> indexProcedureDocument(id, procedureText, parameters, listener),
                        listener::onFailure
                    ));
                } else {
                    listener.onFailure(error);
                }
            }
        ));
    }

    private void indexProcedureDocument(String id, String procedureText, List<Map<String,
        Object>> parameters, ActionListener<Object> listener) {
        client.prepareIndex(".plesql_procedures")
            .setId(id)
            .setSource(Map.of(
                "procedure", procedureText,
                "parameters", parameters
            ))
            .execute(ActionListener.wrap(
                resp -> {
                    Map<String, Object> resultMap = Map.of(
                        "id", resp.getId(),
                        "index", resp.getIndex(),
                        "result", resp.getResult().getLowercase()
                    );
                    listener.onResponse(resultMap);
                },
                listener::onFailure
            ));
    }

    public void deleteProcedureAsync(String id, ActionListener<Object> acknowledged) {
        client.prepareDelete(".plesql_procedures", id)
            .execute(ActionListener.wrap(
                resp -> {
                    Map<String, Object> resultMap = Map.of(
                        "id", resp.getId(),
                        "index", resp.getIndex(),
                        "result", resp.getResult().getLowercase()
                    );
                    acknowledged.onResponse(resultMap);
                },
                acknowledged::onFailure
            ));
    }

    /**
     * Asynchronously retrieves a stored procedure by ID from the .plesql_procedures index.
     *
     * @param id       The procedure document ID.
     * @param listener The ActionListener to notify with the procedure source or null if not found.
     */
    public void getProcedureAsync(String id, ActionListener<Map<String, Object>> listener) {
        client.prepareGet(".plesql_procedures", id)
            .execute(ActionListener.wrap(
                resp -> {
                    if (resp.isExists()) {
                        listener.onResponse(resp.getSource());
                    } else {
                        listener.onResponse(null);
                    }
                },
                listener::onFailure
            ));
    }
}

