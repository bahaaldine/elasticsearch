/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin.datasources;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

import java.util.List;
import java.util.Map;

public class ESFunctions  {
    private static final Logger LOGGER = LogManager.getLogger(ESFunctions.class);

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

