/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.actions;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RestGetProcedureAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestGetProcedureAction.class);

    private final ElasticScriptExecutor elasticScriptExecutor;

    public RestGetProcedureAction(ElasticScriptExecutor elasticScriptExecutor) {
        this.elasticScriptExecutor = elasticScriptExecutor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(RestRequest.Method.GET, "/_query/escript/procedure/{procedure_id}").build()
        );
    }

    @Override
    public String getName() {
        return "pl_esql_get_procedure";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {
        String id = request.param("procedure_id");

        LOGGER.info("Getting procedure : {}", id);

        return channel -> elasticScriptExecutor.getProcedureAsync(id, new ActionListener<>() {
            @Override
            public void onResponse(Map<String, Object> result) {
                try {
                    if (result == null) {
                        channel.sendResponse(new RestResponse(RestStatus.NOT_FOUND, "Procedure not found: " + id));
                        return;
                    }
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("_id", id);
                    builder.field("_source", result);
                    builder.endObject();
                    LOGGER.info("Procedure {} found.", id);
                    channel.sendResponse(new RestResponse(RestStatus.OK, builder));
                } catch (IOException e) {
                    channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                }
            }

            @Override
            public void onFailure(Exception e) {
                channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
            }
        });
    }
}
