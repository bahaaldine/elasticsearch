/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.actions;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * RestRunProcedureByIdAction that fetches a stored procedure by ID and executes it asynchronously.
 */
public class RestRunProcedureByIdAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestRunProcedureByIdAction.class);

    private final ElasticScriptExecutor elasticScriptExecutor;

    public RestRunProcedureByIdAction(ElasticScriptExecutor elasticScriptExecutor) {
        this.elasticScriptExecutor = elasticScriptExecutor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(POST, "/_query/escript/procedure/{procedure_id}/_execute").build()
        );
    }

    @Override
    public String getName() {
        return "pl_esql_run_procedure_by_id";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        String procedureId = request.param("procedure_id");
        if (procedureId == null || procedureId.isEmpty()) {
            throw new IllegalArgumentException("Procedure ID must be provided in the URL path");
        }

        LOGGER.info("Fetching procedure with ID: {}", procedureId);

        // Parse request body as a Map if present
        Map<String, Object> procedureArgs = Map.of();
        if (request.hasContentOrSourceParam()) {
            try (XContentParser parser = request.contentParser()) {
                procedureArgs = parser.map();
            }
        }

        Map<String, Object> finalProcedureArgs = procedureArgs; // because of lambda scope

        LOGGER.info("Procedure arguments {}", finalProcedureArgs);

        GetRequest getRequest = new GetRequest(".elastic_script_procedures", procedureId);

        return channel -> client.get(getRequest, new ActionListener<>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                if (getResponse.isExists() == false) {
                    try {
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject()
                            .field("error", "Procedure [" + procedureId + "] does not exist")
                            .endObject();
                        channel.sendResponse(new RestResponse(RestStatus.NOT_FOUND, builder));
                    } catch (IOException e) {
                        channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                    }
                    return;
                }

                Map<String, Object> source = getResponse.getSourceAsMap();
                Object procedureContentObj = source.get("procedure");
                if (procedureContentObj == null) {
                    try {
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject()
                            .field("error", "Procedure content missing for procedure [" + procedureId + "]")
                            .endObject();
                        channel.sendResponse(new RestResponse(RestStatus.NOT_FOUND, builder));
                    } catch (IOException e) {
                        channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                    }
                    return;
                }

                String procedureContent = procedureContentObj.toString();
                LOGGER.info("Executing procedure [{}]: {}", procedureId, procedureContent);

                elasticScriptExecutor.executeProcedure(procedureContent, finalProcedureArgs, new ActionListener<>() {
                    @Override
                    public void onResponse(Object result) {
                        try {
                            Object finalValue = (result instanceof ReturnValue) ? ((ReturnValue) result).getValue() : result;

                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();
                            builder.field("result", finalValue);
                            builder.endObject();

                            channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                        } catch (Exception e) {
                            channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
            }
        });
    }
}
