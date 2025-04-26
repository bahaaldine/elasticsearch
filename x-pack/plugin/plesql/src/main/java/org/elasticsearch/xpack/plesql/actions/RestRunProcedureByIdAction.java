/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.actions;

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
import org.elasticsearch.xpack.plesql.executors.PlEsqlExecutor;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * RestRunProcedureByIdAction that fetches a stored procedure by ID and executes it asynchronously.
 */
public class RestRunProcedureByIdAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestRunProcedureByIdAction.class);

    private final PlEsqlExecutor plEsqlExecutor;

    public RestRunProcedureByIdAction(PlEsqlExecutor plEsqlExecutor) {
        this.plEsqlExecutor = plEsqlExecutor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(POST, "/_query/plesql/procedure/{procedure_id}/_execute").build()
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

        LOGGER.info("Fetching procedure with ID: " + procedureId);

        GetRequest getRequest = new GetRequest(".plesql_procedures", procedureId);

        return channel -> client.get(getRequest, new ActionListener<>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                if ( getResponse.isExists() == false ) {
                    LOGGER.warn("Procedure [{}] does not exist", procedureId);
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
                    LOGGER.warn("Procedure content missing for ID [{}]", procedureId);
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

                plEsqlExecutor.executeProcedure(procedureContent, new ActionListener<>() {
                    @Override
                    public void onResponse(Object result) {
                        try {
                            LOGGER.debug("Object instance type: {}", result.getClass().getName());

                            Object finalValue = result;
                            if (result instanceof ReturnValue) {
                                LOGGER.debug("This is a ReturnValue, extracting getValue()");
                                finalValue = ((ReturnValue) result).getValue();
                            }

                            LOGGER.debug("Actual finalValue after extraction: {}", finalValue);

                            XContentBuilder builder = XContentFactory.jsonBuilder();
                            builder.startObject();

                            if (finalValue == null) {
                                builder.nullField("result");
                            } else if (finalValue instanceof String) {
                                builder.field("result", (String) finalValue);
                            } else if (finalValue instanceof Map) {
                                builder.field("result", finalValue);
                            } else if (finalValue instanceof List) {
                                builder.field("result", finalValue);
                            } else {
                                builder.field("result", finalValue.toString());
                            }

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
                LOGGER.error("Failed to fetch procedure [{}]: {}", procedureId, e.getMessage());
                channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
            }
        });
    }
}
