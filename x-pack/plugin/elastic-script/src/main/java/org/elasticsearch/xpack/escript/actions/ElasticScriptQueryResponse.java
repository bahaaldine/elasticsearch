/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */


package org.elasticsearch.xpack.escript.actions;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

public class ElasticScriptQueryResponse extends ActionResponse {
    private String result;
    private RestStatus status;

    public ElasticScriptQueryResponse(StreamInput in) throws IOException {
        super();
        this.result = in.readString();
        this.status = RestStatus.readFrom(in);
    }

    public ElasticScriptQueryResponse(String result, RestStatus status) {
        this.result = result;
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public RestStatus getStatus() {
        return status;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(result);
        RestStatus.writeTo(out, status);
    }
}
