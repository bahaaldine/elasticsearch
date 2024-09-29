/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.pl.actions;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class PlEsqlQueryRequest extends ActionRequest {

    public PlEsqlQueryRequest() {
        super();
    }

    private String query;

    public PlEsqlQueryRequest(String query) {
        this.query = query;
    }

    public PlEsqlQueryRequest(StreamInput in) throws IOException {
        super(in);
        this.query = in.readString();
    }

    public String getQuery() {
        return query;
    }

    // Serialize the request
    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(query);
    }

    // TODO: Validate the request
    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
