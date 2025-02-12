/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionType;
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
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.core.esql.action.ColumnInfo;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.utils.ActionListenerUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/*
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;*/

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

    private static final Logger LOGGER = LogManager.getLogger(ExecuteStatementHandler.class);

    private final ProcedureExecutor executor;
    private final Client    client;

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
            // Extract the raw ESQL query substring using the parse tree's token info
            String rawEsqlQuery = executor.getRawText(ctx.esql_query_content());

            // The name of the variable receiving the query result
            String variableName = ctx.variable_assignment().ID().getText();

            LOGGER.debug(() -> String.format("Raw ESQL query substring: [%s]", rawEsqlQuery));
            LOGGER.debug(() -> String.format("Result variable name: [%s]", variableName));

            // Optionally do variable substitution
            String substitutedQuery = substituteVariables(rawEsqlQuery);
            String finalSubstitutedQuery = substitutedQuery;
            LOGGER.debug(() -> String.format("After substitution, ESQL query: [%s]", finalSubstitutedQuery));
            LOGGER.debug("Executing ESQL query via EsqlQueryAction...");

            // 1) Extract the content and the variable name
            String executeQueryContent = ctx.esql_query_content().getText();
            String resultVariableName = ctx.variable_assignment().ID().getText();

            // For example, remove parentheses if your grammar encloses the content in parentheses
            if (executeQueryContent.length() > 2) {
                substitutedQuery = executeQueryContent.substring(1, executeQueryContent.length() - 2);
            }

            SearchRequest searchRequest = buildSearchRequest(finalSubstitutedQuery);

            LOGGER.debug("Search Query: [{}]", searchRequest);

            EsqlQueryRequestBuilder<? extends EsqlQueryRequest, ? extends EsqlQueryResponse> requestBuilder =
                EsqlQueryRequestBuilder.newRequestBuilder(client);

            requestBuilder.query(finalSubstitutedQuery);

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

                        // 2) Build an XContentBuilder to produce a JSON string
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject();
                        builder.field("rows", rowMaps);
                        builder.endObject();

                        // 3) Convert builder to a JSON string
                        String jsonString = Strings.toString(builder);

                        // Update the variable in the execution context with the query result
                        ExecutionContext exeContext = executor.getContext();
                        if ( exeContext.hasVariable(variableName) == false ) {
                            exeContext.declareVariable(variableName, "STRING");
                        }
                        exeContext.setVariable(variableName, jsonString);

                        // 4) Pass this JSON string back to the listener
                        listener.onResponse(jsonString);
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

            /*client.execute( requestBuilder.action(),  requestBuilder.request(), new ActionListener<EsqlQueryResponse>() {
                @Override
                public void onResponse(org.elasticsearch.xpack.esql.action.EsqlQueryResponse esqlQueryResponse) {
                    try {
                        SearchHit[] hits = searchResponse.getHits().getHits();
                        long totalHits = searchResponse.getHits().getTotalHits().value;

                        if (hits.length == 0) {
                            // No results, just log or store a small message
                            String noResultsMsg = String.format("No results! (Total hits: %d)", totalHits);
                            LOGGER.debug("Search Result: [{}]", noResultsMsg);

                            // Store it in the context
                            ExecutionContext exeContext = executor.getContext();
                            if (exeContext.hasVariable(resultVariableName) == false) {
                                exeContext.declareVariable(resultVariableName, "STRING");
                            }
                            exeContext.setVariable(resultVariableName, noResultsMsg);

                            listener.onResponse(null);
                            return;
                        }

                        // 1. Gather all top-level field names across all hits
                        //    So we can create columns for each field.
                        Set<String> allFieldNames = new LinkedHashSet<>();  // preserves insertion order
                        for (SearchHit hit : hits) {
                            Map<String, Object> sourceMap = hit.getSourceAsMap();
                            allFieldNames.addAll(sourceMap.keySet());
                        }

                        // Optionally, sort them alphabetically:
                        List<String> sortedFieldNames = new ArrayList<>(allFieldNames);
                        // sortedFieldNames.sort(String::compareTo);

                        // 2. Build a table in ASCII format
                        StringBuilder tableBuilder = new StringBuilder();
                        tableBuilder.append(String.format("Total hits: %d%n", totalHits));

                        // Table header
                        tableBuilder.append("-----------------------------------------------------------------\n");
                        // First column is "Doc ID", then each top-level field
                        tableBuilder.append(String.format("%-15s", "Document_ID"));  // e.g. 15 width
                        for (String fieldName : sortedFieldNames) {
                            // Each column header, truncated or spaced
                            tableBuilder.append(" | ").append(fieldName);
                        }
                        tableBuilder.append("\n");
                        tableBuilder.append("-----------------------------------------------------------------\n");

                        // 3. For each document, create one row
                        for (SearchHit hit : hits) {
                            String docId = hit.getId();
                            Map<String, Object> sourceMap = hit.getSourceAsMap();

                            // Print doc ID
                            tableBuilder.append(String.format("%-15s", docId));

                            // Then each top-level field
                            for (String fieldName : sortedFieldNames) {
                                Object val = sourceMap.get(fieldName);

                                if (val == null) {
                                    // no value => blank cell
                                    tableBuilder.append(" | ").append("");
                                } else if (val instanceof Map || val instanceof List) {
                                    // Nested object or array => convert to JSON
                                    String jsonVal = convertToJsonString(val);
                                    tableBuilder.append(" | ").append(jsonVal);
                                } else {
                                    // Plain value => just print string
                                    tableBuilder.append(" | ").append(val.toString());
                                }
                            }
                            tableBuilder.append("\n");
                        }

                        // This is your table
                        String tableOutput = tableBuilder.toString();
                        LOGGER.debug("Search Result:\n{}", tableOutput);

                        // 4. Store the table in your ExecutionContext
                        ExecutionContext exeContext = executor.getContext();
                        if ( exeContext.hasVariable(resultVariableName) == false ) {
                            exeContext.declareVariable(resultVariableName, "STRING");
                        }
                        exeContext.setVariable(resultVariableName, tableOutput);

                        // 5. Notify success
                        listener.onResponse(null);

                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                }


                @Override
                public void onResponse(EsqlQueryResponse esqlQueryResponse) {

                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });*/
        } catch (Exception ex) {
            listener.onFailure(ex);
        }
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
