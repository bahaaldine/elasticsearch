/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.resolve.ResolveIndexAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xpack.core.esql.action.EsqlQueryRequest;
import org.elasticsearch.xpack.core.esql.action.EsqlQueryRequestBuilder;
import org.elasticsearch.xpack.core.esql.action.EsqlQueryResponse;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.core.esql.action.ColumnInfo;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;
import org.elasticsearch.xpack.plesql.utils.PersistUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles EXECUTE statements asynchronously using a standard Elasticsearch SearchRequest,
 * rather than an ESQL-specific action.
 *
 * Example usage:
 *   EXECUTE resultVariable=(some DSL ???)
 * or
 *   EXECUTE resultVariable=(INDEX:my-index, Q:match_all, SIZE:10);
 *
 * Adjust the grammar parsing and the search-building logic to match your actual DSL or placeholders.
 */
public class ExecuteStatementHandler {

    private static Logger LOGGER;
    static {
        try {
            LOGGER = LogManager.getLogger(ExecuteStatementHandler.class);
        } catch (Exception e) {
            // Log the error using System.err, or assign a no-op logger
            System.err.println("Failed to initialize logger in ExecuteStatementHandler: " + e);
            LOGGER = null; // or a fallback logger if available
        }
    }

    private final ProcedureExecutor executor;
    private final Client client;

    /**
     * @param executor The ProcedureExecutor that holds the ExecutionContext, etc.
     * @param client   The ES Client to run searches on.
     */
    public ExecuteStatementHandler(ProcedureExecutor executor, Client client) {
        this.executor = executor;
        this.client = client;
    }

    /**
     * Converts the given columns and row-iterator into a list of maps,
     * each map representing one row with columnName -> value.
     */
    public static List<Map<String, Object>> rowsAsMaps(
        List<ColumnInfo> columns,
        Iterable<Iterable<Object>> rowIter
    ) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Iterable<Object> rowValues : rowIter) {
            // Convert rowValues to a List for easy indexing
            List<Object> rowList = new ArrayList<>();
            rowValues.forEach(rowList::add);

            // Build a map of columnName -> value
            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                String colName = columns.get(i).name();
                Object val = i < rowList.size() ? rowList.get(i) : null;
                rowMap.put(colName, val);
            }
            result.add(rowMap);
        }
        return result;
    }

    /**
     * Called when we see an EXECUTE statement in the parse tree:
     *   EXECUTE resultVariable=(some expression)
     */
    @SuppressWarnings("unchecked")
    public void handleAsync(PlEsqlProcedureParser.Execute_statementContext ctx, ActionListener<Object> listener) {
        try {
            // Extract raw query and variable name
            String rawEsqlQuery = executor.getRawText(ctx.esql_query_content());
                String variableName = ctx.variable_assignment().ID().getText();
                LOGGER.info(() -> String.format("Raw ESQL query substring: [%s] and variable [%s]", rawEsqlQuery, variableName));

                // Variable substitution
                String substitutedQuery = substituteVariables(rawEsqlQuery);
                String finalSubstitutedQuery = substitutedQuery;
                LOGGER.info(() -> String.format("After substitution, ESQL query: [%s]", finalSubstitutedQuery));

                // Extract the content and the variable name
                SearchRequest searchRequest = buildSearchRequest(finalSubstitutedQuery);
                LOGGER.info("Search Query: [{}]", searchRequest);

                EsqlQueryRequestBuilder<? extends EsqlQueryRequest, ? extends EsqlQueryResponse> requestBuilder =
                    EsqlQueryRequestBuilder.newRequestBuilder(client);
                requestBuilder.query(finalSubstitutedQuery);

                String finalSubstitutedQuery1 = substitutedQuery;
                ActionListener<EsqlQueryResponse> executeESQLStatementListener = new ActionListener<EsqlQueryResponse>() {

                    @Override
                    public void onResponse(EsqlQueryResponse esqlQueryResponse) {
                        try {
                            // 1) Convert columns+rows to a List<Map<String,Object>>:
                            //    e.g. the `rowsAsMaps(...)` method from earlier
                            List<Map<String, Object>> rowMaps = rowsAsMaps(
                                (List<ColumnInfo>) esqlQueryResponse.response().columns(),
                                esqlQueryResponse.response().rows()
                            );

                            // Convert the result (rowMaps) into JSON with consistent field types.
                            List<Map<String, Object>> normalizedRows = new ArrayList<>();
                            for (Map<String, Object> row : rowMaps) {
                                Map<String, Object> normalized = new LinkedHashMap<>();
                                for (Map.Entry<String, Object> entry : row.entrySet()) {
                                    Object value = entry.getValue();
                                    // Convert nested objects/arrays to JSON string, or force to string.
                                    if (value instanceof Map || value instanceof List) {
                                        normalized.put(entry.getKey(), convertToJsonString(value));
                                    } else {
                                        normalized.put(entry.getKey(), value);
                                    }
                                }
                                normalizedRows.add(normalized);
                            }

                            // 2) Build an XContentBuilder to produce a JSON string
                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            builder.field("rows", normalizedRows);
                            builder.endObject();
                            String jsonString = Strings.toString(builder);
                            LOGGER.debug("ESQL JSON Documents: {}", jsonString);

                            // Retrieve the persist clause (if any)
                            String persistIndexName = null;
                            if (ctx.persist_clause() != null) {
                                persistIndexName = ctx.persist_clause().ID().getText();
                                LOGGER.debug("Persist clause provided: [{}]", persistIndexName);
                            } else {
                                LOGGER.debug("No persist clause provided. Result will remain transient.");
                            }

                            // If a persist clause is provided, index the result into that index
                            if (persistIndexName != null) {
                                // Extract the source index from the query (e.g., "FROM retro_arcade_games" yields "retro_arcade_games")
                                String sourceIndex = extractSourceIndex(finalSubstitutedQuery1);
                                LOGGER.debug("Extracted source index: [{}]", sourceIndex);

                                ActionListener<Void> persistIndexListener = new ActionListener<Void>() {
                                    @Override
                                    public void onResponse(Void aVoid) {
                                        ExecutionContext exeContext = executor.getContext();
                                        if ( exeContext.hasVariable(variableName) == false ) {
                                            exeContext.declareVariable(variableName, "ARRAY");
                                        }
                                        exeContext.setVariable(variableName, normalizedRows);
                                        listener.onResponse(null);
                                    }
                                    @Override
                                    public void onFailure(Exception e) {
                                        listener.onFailure(e);
                                    }
                                };


                                ActionListener<Void> persistIndexLogger = ActionListenerUtils.withLogging(persistIndexListener,
                                    this.getClass().getName(),
                                    "Persist-Index-Statement: " + persistIndexName);

                                persistResults(sourceIndex, persistIndexName, rowMaps, persistIndexLogger);
                            } else {
                                // Update the variable in the execution context with the query result
                                ExecutionContext exeContext = executor.getContext();
                                if ( exeContext.hasVariable(variableName) == false ) {
                                    exeContext.declareVariable(variableName, "ARRAY");
                                }
                                exeContext.setVariable(variableName, normalizedRows);

                                // 4) Pass this JSON string back to the listener
                                listener.onResponse(null);
                            }

                        } catch (Exception e) {
                            listener.onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }
                };

                ActionListener<EsqlQueryResponse> executeESQLStatementLogger = ActionListenerUtils.withLogging(executeESQLStatementListener,
                    this.getClass().getName(),
                    "Execute-ESQL-Statement: " + requestBuilder.request());

                client.<EsqlQueryRequest, EsqlQueryResponse>execute(
                    (ActionType<EsqlQueryResponse>) requestBuilder.action(),
                    requestBuilder.request(),
                    executeESQLStatementLogger
                    );
            } catch (Exception ex) {
                listener.onFailure(ex);
            }
    }

    // --- Asynchronous Persist Workflow ---

    /**
     * Persists the given documents into the target index by:
     * 1) Getting the mapping from the source index.
     * 2) Checking whether the target index exists.
     * 3) Creating the target index if needed.
     * 4) Bulk indexing the documents.
     */
    private void persistResults(String sourceIndex, String targetIndex, List<Map<String, Object>> documents,
                                ActionListener<Void> listener) {
        // 1) Get source mapping asynchronously.
        GetMappingsRequest getMappingsRequest = new GetMappingsRequest().indices(sourceIndex);
        client.admin().indices().getMappings(getMappingsRequest, new ActionListener<GetMappingsResponse>() {
            @Override
            public void onResponse(GetMappingsResponse getMappingsResponse) {
                try {
                    Map<String, Object> sourceMapping = getMappingsResponse.mappings().get(sourceIndex).sourceAsMap();
                    LOGGER.debug("Source mapping: {}", sourceMapping);
                    // 2) Check if target index exists asynchronously.
                    String[] indices = {targetIndex};
                    ResolveIndexAction.Request getIndexRequest = new ResolveIndexAction.Request(indices);
                    client.admin().indices().resolveIndex(getIndexRequest, new ActionListener<ResolveIndexAction.Response>() {
                        @Override
                        public void onResponse(ResolveIndexAction.Response exists) {
                            LOGGER.debug("Target index [{}] already exists.", targetIndex);
                            // If index exists, simply proceed to bulk indexing.
                            PersistUtil.bulkIndexDocuments(client, targetIndex, documents, listener);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            LOGGER.debug("Index {} has not been created yet", targetIndex);
                            // 3) Create target index asynchronously using the source mapping.
                            CreateIndexRequest createIndexRequest = new CreateIndexRequest(targetIndex);
                            createIndexRequest.mapping(sourceMapping);
                            client.admin().indices().create(createIndexRequest, new ActionListener<CreateIndexResponse>() {
                                @Override
                                public void onResponse(CreateIndexResponse createIndexResponse) {
                                    LOGGER.debug("Target index [{}] created successfully.", targetIndex);
                                    // 4) Bulk index documents.
                                    PersistUtil.bulkIndexDocuments(client, targetIndex, documents, listener);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    listener.onFailure(e);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    listener.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    // --- Helper Methods ---

    /**
     * Uses a simple regex to extract the source index from an ESQL query.
     * For example, "FROM retro_arcade_games | LIMIT 2" returns "retro_arcade_games".
     */
    private String extractSourceIndex(String esqlQuery) {
        Pattern pattern = Pattern.compile("FROM\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(esqlQuery);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("Unable to extract source index from query: " + esqlQuery);
    }

    /**
     * Example method: parse your "executeQueryContent" to a SearchRequest.
     * In a real scenario, you might interpret "executeQueryContent" as a small
     * DSL snippet, or parse index name, or do something more advanced.
     */
    private SearchRequest buildSearchRequest(String content) {
        // e.g. if content is "INDEX:my-index, Q:match_all, SIZE:10" -> parse it

        String indexName = "retro_arcade_games"; // default
        // Possibly parse out a 'Q:...' substring and interpret
        // For now, we do a trivial match_all
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.simpleQueryStringQuery(content));

        SearchRequest request = new SearchRequest(indexName);
        request.source(sourceBuilder);
        return request;
    }

    /**
     * Substitutes placeholders like ":myVar" with actual values from the Executor's context.
     */
    private String substituteVariables(String original) {
        for (String varName : executor.getContext().getVariableNames()) {
            Object val = executor.getContext().getVariable(varName);
            // If placeholders are like ':var', do:
            original = original.replace(":" + varName, String.valueOf(val));
        }
        return original;
    }


    /**
     * Utility to convert a Map or List to a JSON string. You can expand
     * error handling or limit recursion, etc.
     */
    private String convertToJsonString(Object obj) {
        try {
            // If you have XContent or Jackson available:
            //   return Strings.toString(XContentFactory.jsonBuilder().value(obj));
            // Alternatively, do a simple approach with Jackson or your own library:

            // Example: If you have Jackson:
            //   ObjectMapper mapper = new ObjectMapper();
            //   return mapper.writeValueAsString(obj);

            // For demonstration, we do a naive approach:
            return obj.toString(); // fallback
        } catch (Exception e) {
            return "<error converting to JSON>";
        }
    }
}
