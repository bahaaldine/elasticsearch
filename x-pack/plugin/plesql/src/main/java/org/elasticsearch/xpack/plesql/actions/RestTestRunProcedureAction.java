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
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xpack.plesql.executors.PlEsqlExecutor;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * Example RestTestRunProcedureAction that handles an asynchronous PL|ESQL request.
 */
public class RestTestRunProcedureAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestTestRunProcedureAction.class);

    private final PlEsqlExecutor plEsqlExecutor;

    public RestTestRunProcedureAction(PlEsqlExecutor plEsqlExecutor) {
        this.plEsqlExecutor = plEsqlExecutor;
    }

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(POST, "/_query/plesql/test").build()
        );
    }

    @Override
    public String getName() {
        return "pl_esql_test_run_procedure";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (request.hasContentOrSourceParam() == false) {
            throw new IllegalArgumentException("Request body is required");
        }

        XContentParser parser = request.contentParser();
        String plEsqlQuery = null;
        Map<String, Object> args = null;

        XContentParser.Token token = parser.nextToken();
        while (token != null) {
            if (token == XContentParser.Token.FIELD_NAME) {
                String fieldName = parser.currentName();
                parser.nextToken();
                if ("query".equals(fieldName)) {
                    plEsqlQuery = parser.text();
                } else if ("args".equals(fieldName)) {
                    args = parser.map();
                }
            }
            token = parser.nextToken();
        }

        if (plEsqlQuery == null) {
            throw new IllegalArgumentException("Field [query] must be provided in the body");
        }

        if (args == null) {
            args = new HashMap<>();
        }

        final String finalQuery = plEsqlQuery;
        final Map<String, Object> finalArgs = args;
        LOGGER.info("Running procedure {}, with args {}", finalQuery, finalArgs);

        return channel -> {
            // call the async method
            plEsqlExecutor.executeProcedure(finalQuery, finalArgs, new ActionListener<>() {
                @Override
                public void onResponse(Object result) {
                    try {
                        LOGGER.debug("Object instance type: {}", result.getClass().getName());

                        // If 'result' is a ReturnValue, extract the .getValue() from it
                        Object finalValue = result;
                        if (result instanceof ReturnValue) {
                            LOGGER.debug("This is a ReturnValue, extracting getValue()");
                            finalValue = ((ReturnValue) result).getValue();
                        }

                        LOGGER.debug("Actual finalValue after extraction: {}", finalValue);

                        // Build the JSON response
                        XContentBuilder builder = XContentFactory.jsonBuilder();
                        builder.startObject();

                        // If it's some recognized type (String, List, Map, etc.), you can do direct field
                        // If you might have a custom object, you can fallback to .toString():
                        if (finalValue == null) {
                            // e.g. we can store null
                            builder.nullField("result");
                        } else if (finalValue instanceof String) {
                            builder.field("result", (String) finalValue);
                        } else if (finalValue instanceof Map) {
                            // If it’s a map of <String, Object>, XContentBuilder can handle it
                            builder.field("result", finalValue);
                        } else if (finalValue instanceof List) {
                            // If it’s a List of recognized subtypes (String, Map, etc.)
                            builder.field("result", finalValue);
                        } else {
                            // fallback: store toString
                            builder.field("result", finalValue.toString());
                        }

                        builder.endObject();

                        // Send success with the builder
                        channel.sendResponse(new RestResponse(RestStatus.OK, builder));

                    } catch (Exception e) {
                        channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // onFailure => internal error or parse error
                    channel.sendResponse(new RestResponse(RestStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                }
            });
        };
    }
}
