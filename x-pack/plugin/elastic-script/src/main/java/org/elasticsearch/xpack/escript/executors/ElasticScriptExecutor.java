/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in compliance
 * with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.escript.executors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.escript.functions.community.FunctionLoader;
import org.elasticsearch.xpack.escript.functions.builtin.datasources.ESFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datasources.EsqlBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DocumentBuiltInFunctions;
import org.elasticsearch.xpack.escript.handlers.ElasticScriptErrorListener;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DateBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.escript.procedure.ProcedureDefinition;
import org.elasticsearch.xpack.escript.utils.ActionListenerUtils;
import org.elasticsearch.xpack.escript.visitors.ProcedureDefinitionVisitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElasticScriptExecutor {

    private final ThreadPool threadPool;
    private final Client client;

    private static final Logger LOGGER = LogManager.getLogger(ElasticScriptExecutor.class);

    @Inject
    public ElasticScriptExecutor(ThreadPool threadPool, Client client) {
        this.threadPool = threadPool;
        this.client = client;
    }

    /**
     * Asynchronously executes a Elastic Script procedure.
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
     * @param procedureText The Elastic Script procedure text.
     * @param listener      The ActionListener to notify when execution is complete.
     */
    public void executeProcedure(String procedureText, Map<String, Object> args, ActionListener<Object> listener) {
        threadPool.generic().execute(() -> {
            try {
                // 1. Parse the procedure
                ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(procedureText));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                ElasticScriptParser parser = new ElasticScriptParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new ElasticScriptErrorListener());

                ElasticScriptParser.ProgramContext programContext = parser.program();

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
                                ElasticScriptLexer storedLexer = new ElasticScriptLexer(CharStreams.fromString(procedureContent));
                                CommonTokenStream storedTokens = new CommonTokenStream(storedLexer);
                                ElasticScriptParser storedParser = new ElasticScriptParser(storedTokens);
                                storedParser.removeErrorListeners();
                                storedParser.addErrorListener(new ElasticScriptErrorListener());
                                ElasticScriptParser.ProcedureContext procCtx = storedParser.procedure();

                                // Create fresh context and bind parameters
                                ExecutionContext executionContext = new ExecutionContext();
                                List<ElasticScriptParser.ParameterContext> parameterContexts =
                                    procCtx.parameter_list() != null ? procCtx.parameter_list().parameter() : List.of();

                                if (parameterContexts.isEmpty() == false) {
                                    // Parse call arguments directly from the call_procedure_statement
                                    List<ElasticScriptParser.ExpressionContext> callArgs =
                                        programContext.call_procedure_statement().argument_list() != null
                                            ? programContext.call_procedure_statement().argument_list().expression()
                                            : List.of();

                                    if (parameterContexts.size() != callArgs.size()) {
                                        listener.onFailure(
                                            new IllegalArgumentException("Mismatch between declared parameters and call arguments"));
                                        return;
                                    }

                                    for (int i = 0; i < parameterContexts.size(); i++) {
                                        var param = parameterContexts.get(i);
                                        String paramName = param.ID().getText();
                                        String paramType = param.datatype().getText().toUpperCase(java.util.Locale.ROOT);
                                        String rawValue = callArgs.get(i).getText();

                                        Object value;
                                        switch (paramType) {
                                            case "INT":
                                                value = Integer.valueOf(rawValue);
                                                break;
                                            case "NUMBER":
                                            case "FLOAT":
                                                value = Double.valueOf(rawValue);
                                                break;
                                            case "BOOLEAN":
                                                value = Boolean.parseBoolean(rawValue);
                                                break;
                                            case "STRING":
                                                value = rawValue.replaceAll("^['\\\"]|['\\\"]$", ""); // strip quotes
                                                break;
                                            case "ARRAY":
                                            case "ARRAYOFSTRING":
                                            case "ARRAYOFNUMBER":
                                            case "ARRAYOFDOCUMENT":
                                                LOGGER.info("Param name {} is an array of document", paramName);
                                                try (XContentParser parser =
                                                         org.elasticsearch.xcontent.XContentType.JSON.xContent().createParser(
                                                    org.elasticsearch.xcontent.NamedXContentRegistry.EMPTY,
                                                    org.elasticsearch.xcontent.DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                                                    new java.io.ByteArrayInputStream(
                                                        rawValue.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                                                ))) {
                                                    parser.nextToken();
                                                    value = parser.list();
                                                } catch (Exception e) {
                                                    throw new IllegalArgumentException(
                                                        "Failed to parse array input for parameter: " + paramName, e);
                                                }
                                                break;
                                            default:
                                                value = rawValue;
                                        }

                                        executionContext.declareVariable(paramName, paramType);
                                        executionContext.setVariable(paramName, value);
                                    }
                                }

                                // Visit inner procedure definitions
                                ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(executionContext);
                                for (ElasticScriptParser.StatementContext stmtCtx : procCtx.statement()) {
                                    if (stmtCtx.getChildCount() > 0 && "PROCEDURE".equalsIgnoreCase(stmtCtx.getChild(0).getText())) {
                                        procDefVisitor.visit(stmtCtx);
                                    }
                                }

                                ProcedureExecutor procedureExecutor =
                                    new ProcedureExecutor(executionContext, threadPool, client, storedTokens);
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
                                DocumentBuiltInFunctions.registerAll(executionContext);

                                FunctionLoader.loadCommunityFunctions(executionContext);

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
                    ElasticScriptParser.Delete_procedure_statementContext deleteContext = programContext.delete_procedure_statement();
                    String procedureId = deleteContext.ID().getText();
                    LOGGER.info("Deleting procedure {}", procedureId);
                    deleteProcedureAsync(procedureId, listener);
                } else if (programContext.create_procedure_statement() != null ) {
                    ElasticScriptParser.Create_procedure_statementContext createContext = programContext.create_procedure_statement();
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
    private boolean isProcedureCall(ElasticScriptParser.StatementContext stmt, ExecutionContext globalContext) {
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
    private String extractProcedureName(ElasticScriptParser.StatementContext stmt) {
        return stmt.function_call_statement().function_call().ID().getText();
    }

    /**
     * Extracts the procedure arguments from a statement assumed to be a procedure call.
     * This example simply evaluates the argument expressions as Doubles.
     *
     * @param stmt The statement context.
     * @return A list of evaluated argument values.
     */
    private List<Object> extractProcedureArguments(ElasticScriptParser.StatementContext stmt) {
        if (stmt.function_call_statement().function_call().argument_list() != null) {
            List<ElasticScriptParser.ExpressionContext> exprs =
                stmt.function_call_statement().function_call().argument_list().expression();
            List<Object> args = new ArrayList<>();
            for (ElasticScriptParser.ExpressionContext expr : exprs) {
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
        String indexName = ".elastic_script_procedures";

        LOGGER.info( "Storing procedure {}", procedureText );

        // Parse the procedure to extract parameters
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(procedureText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        ElasticScriptParser.ProcedureContext procCtx = parser.procedure();

        List<Map<String, Object>> parameters = new ArrayList<>();
        if (procCtx != null && procCtx.parameter_list() != null) {
            for (var paramCtx : procCtx.parameter_list().parameter()) {
                String paramName = paramCtx.ID().getText();
                String paramType = paramCtx.datatype().getText().toUpperCase();
                parameters.add(Map.of("name", paramName, "type", paramType));
            }
        }

        GetIndexRequest request = new GetIndexRequest(TimeValue.timeValueSeconds(30)).indices(indexName);
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
        client.prepareIndex(".elastic_script_procedures")
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
        client.prepareDelete(".elastic_script_procedures", id)
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
     * Asynchronously retrieves a stored procedure by ID from the .elastic_script_procedures index.
     *
     * @param id       The procedure document ID.
     * @param listener The ActionListener to notify with the procedure source or null if not found.
     */
    public void getProcedureAsync(String id, ActionListener<Map<String, Object>> listener) {
        client.prepareGet(".elastic_script_procedures", id)
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

