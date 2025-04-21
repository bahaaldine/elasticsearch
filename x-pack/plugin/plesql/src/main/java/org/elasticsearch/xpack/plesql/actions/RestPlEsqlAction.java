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

import static org.elasticsearch.rest.RestRequest.Method.POST;

/**
 * Example RestPlEsqlAction that handles an asynchronous PL|ESQL request.
 */
public class RestPlEsqlAction extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestPlEsqlAction.class);

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
    public String getName() {
        return "pl_esql_execute_procedure";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        if (request.hasContentOrSourceParam() == false) {
            throw new IllegalArgumentException("Request body is required");
        }

        XContentParser parser = request.contentParser();
        String plEsqlQuery = null;

        XContentParser.Token token = parser.nextToken();
        while (token != null) {
            if ( token == XContentParser.Token.FIELD_NAME && "query".equals(parser.currentName()) ) {
                parser.nextToken();
                plEsqlQuery = parser.text();
                break;
            }
            token = parser.nextToken();
        }

        if (plEsqlQuery == null) {
            throw new IllegalArgumentException("Field [query] must be provided in the body");
        }

        // parse 'plEsqlQuery' from body, etc.
        final String finalQuery = plEsqlQuery;

        return channel -> {
            // call the async method
            plEsqlExecutor.executeProcedure(finalQuery, new ActionListener<>() {
                @Override
                public void onResponse(Object result) {
                    try {
                        // Always log with placeholders
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
