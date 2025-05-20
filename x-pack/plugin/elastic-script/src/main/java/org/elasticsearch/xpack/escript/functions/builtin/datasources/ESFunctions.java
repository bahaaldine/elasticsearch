/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.datasources;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.context.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * Indicates that a class contains a collection of built-in functions that can be registered with Elastic Script.
 *
 * Used to distinguish utility-style registration classes (e.g., ESFunctions, MathFunctions)
 * from standalone function classes where each class represents a single function.
 *
 * Classes annotated with @FunctionCollection will be scanned at runtime and their
 * methods annotated with @FunctionSpec will be registered as available functions.
 *
 * Example usage:
 *
 * {@code
 * @FunctionCollection(
 *     category = "datasource",
 *     description = "Built-in functions for indexing, updating, and querying Elasticsearch documents"
 * )
 * public class ESFunctions {
 *     @FunctionSpec(...)
 *     public static void registerIndexDocument(...) { ... }
 * }
 * }
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATASOURCE,
    description = "Built-in datasource operations: index, get, update, refresh..."
)
public class ESFunctions  {
    private static final Logger LOGGER = LogManager.getLogger(ESFunctions.class);

    /**
     * Registers the INDEX_DOCUMENT function.
     *
     * <b>Function:</b> INDEX_DOCUMENT<br>
     * <b>Description:</b> Indexes a single document into the specified index. Returns metadata including
     * the document ID, index, and result status.<br>
     * <b>Inputs:</b>
     * <ul>
     *   <li>indexName (STRING): The name of the index to write to</li>
     *   <li>document (DOCUMENT): The document to index</li>
     * </ul>
     * <b>Output:</b> Map with keys "id", "index", and "result" describing the indexed document.
     */
    @FunctionSpec(
        name = "INDEX_DOCUMENT",
        description = "Indexes a single document into the specified index. " +
            "Returns metadata including the document ID, index, and result status.",
        parameters = {
            @FunctionParam(name = "indexName", type = "STRING", description = "The name of the index to write to"),
            @FunctionParam(name = "document", type = "DOCUMENT", description = "The document to index")
        },
        returnType = @FunctionReturn(
            type = "DOCUMENT",
            description = "Metadata of the indexed document including ID, index, and result status"
        ),
        examples = {"INDEX_DOCUMENT('my-index', {\"user\":\"kimchy\"})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerIndexDocumentFunction(ExecutionContext context, Client client) {
        LOGGER.info("Registering INDEX_DOCUMENT function");

        context.declareFunction("INDEX_DOCUMENT",
            List.of(
                new Parameter("indexName", "STRING", ParameterMode.IN),
                new Parameter("document", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INDEX_DOCUMENT", (args, listener) -> {
                String indexName = (String) args.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> doc = (Map<String, Object>) args.get(1);

                client.prepareIndex(indexName).setSource(doc).execute(ActionListener.wrap(
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
            })
        );
    }

    /**
     * Registers the INDEX_BULK function.
     *
     * <b>Function:</b> INDEX_BULK<br>
     * <b>Description:</b> Bulk indexes a list of documents into the specified index.
     * Returns number of items and the operation duration.<br>
     * <b>Inputs:</b>
     * <ul>
     *   <li>indexName (STRING): The name of the index to write to</li>
     *   <li>documents (ARRAY OF DOCUMENT): List of documents to index</li>
     * </ul>
     * <b>Output:</b> Map with keys "took" (duration in ms) and "items" (number of items indexed).
     */
    @FunctionSpec(
        name = "INDEX_BULK",
        description = "Bulk indexes a list of documents into the specified index. Returns number of items and the operation duration.",
        parameters = {
            @FunctionParam(name = "indexName", type = "STRING", description = "The name of the index to write to"),
            @FunctionParam(name = "documents", type = "ARRAY OF DOCUMENT", description = "List of documents to index")
        },
        returnType = @FunctionReturn(
            type = "DOCUMENT",
            description = "Summary including the number of indexed items and operation duration"
        ),
        examples = {"INDEX_BULK('my-index', [{\"x\":1}, {\"x\":2}])"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerIndexBulkFunction(ExecutionContext context, Client client) {
        LOGGER.info("Registering INDEX_BULK function");

        context.declareFunction("INDEX_BULK",
            List.of(
                new Parameter("indexName", "STRING", ParameterMode.IN),
                new Parameter("documents", "ARRAY OF DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INDEX_BULK", (args, listener) -> {
                String indexName = (String) args.get(0);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> docs = (List<Map<String, Object>>) args.get(1);

                var bulk = client.prepareBulk();
                for (Map<String, Object> doc : docs) {
                    bulk.add(client.prepareIndex(indexName).setSource(doc));
                }
                bulk.execute(ActionListener.wrap(
                    resp -> {
                        if (resp.hasFailures()) {
                            listener.onFailure(new RuntimeException("Bulk indexing errors: " + resp.buildFailureMessage()));
                        } else {
                            Map<String, Object> resultMap = Map.of(
                                "took", resp.getTook().getMillis(),
                                "items", resp.getItems().length
                            );
                            listener.onResponse(resultMap);
                        }
                    },
                    listener::onFailure
                ));
            })
        );
    }


    /**
     * Registers the UPDATE_DOCUMENT function.
     *
     * <b>Function:</b> UPDATE_DOCUMENT<br>
     * <b>Description:</b> Updates an existing document by ID in the specified index with partial fields from the given document.<br>
     * <b>Inputs:</b>
     * <ul>
     *   <li>indexName (STRING): The name of the index</li>
     *   <li>id (STRING): The ID of the document to update</li>
     *   <li>document (DOCUMENT): Partial document fields to update</li>
     * </ul>
     * <b>Output:</b> Map with keys "id", "index", and "result" describing the updated document.
     */
    @FunctionSpec(
        name = "UPDATE_DOCUMENT",
        description = "Updates an existing document by ID in the specified index with partial fields from the given document.",
        parameters = {
            @FunctionParam(name = "indexName", type = "STRING", description = "The name of the index"),
            @FunctionParam(name = "id", type = "STRING", description = "The ID of the document to update"),
            @FunctionParam(name = "document", type = "DOCUMENT", description = "Partial document fields to update")
        },
        returnType = @FunctionReturn(
            type = "DOCUMENT",
            description = "Metadata of the updated document including ID, index, and result status"
        ),
        examples = {"UPDATE_DOCUMENT('index', 'id123', {\"field\":\"value\"})"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerUpdateDocumentFunction(ExecutionContext context, Client client) {
        LOGGER.info("Registering UPDATE_DOCUMENT function");

        context.declareFunction("UPDATE_DOCUMENT",
            List.of(
                new Parameter("indexName", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN),
                new Parameter("document", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("UPDATE_DOCUMENT", (args, listener) -> {
                String indexName = (String) args.get(0);
                String id = (String) args.get(1);
                @SuppressWarnings("unchecked")
                Map<String, Object> doc = (Map<String, Object>) args.get(2);

                client.prepareUpdate(indexName, id).setDoc(doc).execute(ActionListener.wrap(
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
            })
        );
    }

    /**
     * Registers the GET_DOCUMENT function.
     *
     * <b>Function:</b> GET_DOCUMENT<br>
     * <b>Description:</b> Fetches a document by ID from the specified index. Returns the document if it exists, or null otherwise.<br>
     * <b>Inputs:</b>
     * <ul>
     *   <li>indexName (STRING): The name of the index</li>
     *   <li>id (STRING): The ID of the document to retrieve</li>
     * </ul>
     * <b>Output:</b> The document as a Map if found, or null.
     */
    @FunctionSpec(
        name = "GET_DOCUMENT",
        description = "Fetches a document by ID from the specified index. Returns the document if it exists, or null otherwise.",
        parameters = {
            @FunctionParam(name = "indexName", type = "STRING", description = "The name of the index"),
            @FunctionParam(name = "id", type = "STRING", description = "The ID of the document to retrieve")
        },
        returnType = @FunctionReturn(
            type = "DOCUMENT",
            description = "The retrieved document if found, otherwise null"
        ),
        examples = {"GET_DOCUMENT('index', 'id123')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerGetDocumentFunction(ExecutionContext context, Client client) {
        LOGGER.info("Registering GET_DOCUMENT function");

        context.declareFunction("GET_DOCUMENT",
            List.of(
                new Parameter("indexName", "STRING", ParameterMode.IN),
                new Parameter("id", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("GET_DOCUMENT", (args, listener) -> {
                String indexName = (String) args.get(0);
                String id = (String) args.get(1);
                client.prepareGet(indexName, id).execute(ActionListener.wrap(
                    resp -> {
                        if (resp.isExists()) {
                            listener.onResponse(resp.getSource());
                        } else {
                            listener.onResponse(null);
                        }
                    },
                    listener::onFailure
                ));
            })
        );
    }

    /**
     * Registers the REFRESH_INDEX function.
     *
     * <b>Function:</b> REFRESH_INDEX<br>
     * <b>Description:</b> Refreshes the specified index so that newly indexed documents become visible for search.<br>
     * <b>Inputs:</b>
     * <ul>
     *   <li>indexName (STRING): The name of the index to refresh</li>
     * </ul>
     * <b>Output:</b> Map with keys "shards", "successful", and "failed" indicating the refresh operation status.
     */
    @FunctionSpec(
        name = "REFRESH_INDEX",
        description = "Refreshes the specified index so that newly indexed documents become visible for search.",
        parameters = {
            @FunctionParam(name = "indexName", type = "STRING", description = "The name of the index to refresh")
        },
        returnType = @FunctionReturn(
            type = "DOCUMENT",
            description = "Status of the refresh operation including total, successful, and failed shards"
        ),
        examples = {"REFRESH_INDEX('my-index')"},
        category = FunctionCategory.DATASOURCE
    )
    public static void registerRefreshIndexFunction(ExecutionContext context, Client client) {
        LOGGER.info("Registering REFRESH_INDEX function");

        context.declareFunction("REFRESH_INDEX",
            List.of(
                new Parameter("indexName", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("REFRESH_INDEX", (args, listener) -> {
                String indexName = (String) args.get(0);

                client.admin().indices().prepareRefresh(indexName).execute(ActionListener.wrap(
                    resp -> {
                        Map<String, Object> resultMap = Map.of(
                            "shards", resp.getTotalShards(),
                            "successful", resp.getSuccessfulShards(),
                            "failed", resp.getFailedShards()
                        );
                        listener.onResponse(resultMap);
                    },
                    listener::onFailure
                ));
            })
        );
    }
}
