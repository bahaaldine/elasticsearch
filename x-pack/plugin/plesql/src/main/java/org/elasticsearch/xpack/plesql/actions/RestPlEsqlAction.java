/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.actions;

import org.elasticsearch.client.internal.node.NodeClient;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentParser.Token;
import org.elasticsearch.xpack.plesql.PlEsqlExecutor;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.POST;

public class RestPlEsqlAction extends BaseRestHandler {

    private final PlEsqlExecutor plEsqlExecutor;

    public RestPlEsqlAction(PlEsqlExecutor plEsqlExecutor) {
        this.plEsqlExecutor = plEsqlExecutor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(POST, "/_query/plesql").build()
        );
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (request.hasContentOrSourceParam() == false) {
            throw new IllegalArgumentException("Request body is required");
        }

        XContentParser parser = request.contentParser();
        String plEsqlQuery = null;

        Token token = parser.nextToken();
        while (token != null) {
            if (token == Token.FIELD_NAME && parser.currentName().equals("query")) {
                parser.nextToken(); // Move to value
                plEsqlQuery = parser.text(); // Extract the value of the query
                break;
            }
            token = parser.nextToken();
        }

        if (plEsqlQuery == null) {
            throw new IllegalArgumentException("Query must be provided in the body");
        }

        String finalPlEsqlQuery = plEsqlQuery;
        return channel -> {
            try {
                String result = plEsqlExecutor.executeProcedure(finalPlEsqlQuery);

                XContentBuilder builder = channel.newBuilder();
                builder.startObject();
                builder.field("result", result);
                builder.endObject();

                BytesReference content = BytesReference.bytes(builder);
                RestResponse response = new RestResponse(RestStatus.OK, String.valueOf(content));
                response.addHeader("Content-Type", "application/json");
                channel.sendResponse(response);
            } catch (IllegalArgumentException e) {
                BytesReference content = new BytesArray(e.getMessage());
                RestResponse response = new RestResponse(RestStatus.BAD_REQUEST, String.valueOf(content));
                response.addHeader("Content-Type", "text/plain");
                channel.sendResponse(response);
            } catch (Exception e) {
                BytesReference content = new BytesArray(e.getMessage());
                RestResponse response = new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, String.valueOf(content));
                response.addHeader("Content-Type", "text/plain");
                channel.sendResponse(response);
            }
        };
    }

    @Override
    public String getName() {
        return "pl_esql_execute_procedure";
    }
}
