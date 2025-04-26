/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.actions;

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
import org.elasticsearch.xpack.plesql.executors.PlEsqlExecutor;

import java.io.IOException;
import java.util.List;

public class RestDeleteProcedureAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestDeleteProcedureAction.class);

    private final PlEsqlExecutor plEsqlExecutor;

    public RestDeleteProcedureAction(PlEsqlExecutor plEsqlExecutor) {
        this.plEsqlExecutor = plEsqlExecutor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(RestRequest.Method.DELETE, "/_query/plesql/procedure/{procedure_id}").build()
        );
    }

    @Override
    public String getName() {
        return "pl_esql_delete_procedure";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        LOGGER.info("Deleting procedure ... ");

        String id = request.param("procedure_id");
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Path parameter [procedure_id] is required");
        }

        LOGGER.info("Deleting procedure: " + id );

        return channel -> plEsqlExecutor.deleteProcedureAsync(id, new ActionListener<>() {
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
