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
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RestCreateProcedureAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestCreateProcedureAction.class);

    private final ElasticScriptExecutor elasticScriptExecutor;

    public RestCreateProcedureAction(ElasticScriptExecutor elasticScriptExecutor) {
        this.elasticScriptExecutor = elasticScriptExecutor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(RestRequest.Method.PUT, "/_query/escript/procedure/{procedure_id}").build()
        );
    }

    @Override
    public String getName() {
        return "pl_esql_create_procedure";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (request.hasContentOrSourceParam() == false) {
            throw new IllegalArgumentException("Request body is required");
        }

        XContentParser parser = request.contentParser();
        Map<String, Object> body = parser.map();

        String id = request.param("procedure_id");
        String procedure = (String) body.get("procedure");

        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Field [id] must be provided in the body");
        }
        if (procedure == null || procedure.isEmpty()) {
            throw new IllegalArgumentException("Field [procedure] must be provided in the body");
        }

        return channel -> elasticScriptExecutor.storeProcedureAsync(id, procedure, new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                try {
                    XContentBuilder builder = XContentFactory.jsonBuilder();
                    builder.startObject();
                    builder.field("acknowledged", true);
                    builder.endObject();
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
