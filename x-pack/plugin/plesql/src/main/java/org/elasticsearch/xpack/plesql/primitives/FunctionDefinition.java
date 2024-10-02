/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.primitives;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.util.List;

public class FunctionDefinition {
    private String name;
    private List<String> parameters;
    private List<PlEsqlProcedureParser.StatementContext> body;

    public FunctionDefinition(String name, List<String> parameters, List<PlEsqlProcedureParser.StatementContext> body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<PlEsqlProcedureParser.StatementContext> getBody() {
        return body;
    }
}
