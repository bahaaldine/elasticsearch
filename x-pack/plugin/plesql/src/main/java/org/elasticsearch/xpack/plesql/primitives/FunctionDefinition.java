/*
 * Copyright Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.plesql.primitives;

import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.util.List;

/**
 * The FunctionDefinition class represents a user-defined function within the execution context.
 * It contains the function's name, parameters, parameter types, and the body statements.
 */
public class FunctionDefinition {
    private final String name;
    private final List<String> parameters;
    private final List<String> parameterTypes;
    private final List<PlEsqlProcedureParser.StatementContext> body;

    /**
     * Constructs a FunctionDefinition with the specified name, parameters, parameter types, and body.
     *
     * @param name           The name of the function.
     * @param parameters     The list of parameter names.
     * @param parameterTypes The list of parameter data types.
     * @param body           The list of statements constituting the function body.
     */
    public FunctionDefinition(String name, List<String> parameters, List<String> parameterTypes,
                              List<PlEsqlProcedureParser.StatementContext> body) {
        this.name = name;
        this.parameters = parameters;
        this.parameterTypes = parameterTypes;
        this.body = body;
    }

    /**
     * Retrieves the name of the function.
     *
     * @return The function's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the list of parameter names.
     *
     * @return A list of parameter names.
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * Retrieves the list of parameter data types.
     *
     * @return A list of parameter data types.
     */
    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Retrieves the function's body statements.
     *
     * @return A list of StatementContext representing the function body.
     */
    public List<PlEsqlProcedureParser.StatementContext> getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "FunctionDefinition{" +
            "name='" + name + '\'' +
            ", parameters=" + parameters +
            ", parameterTypes=" + parameterTypes +
            ", bodySize=" + body.size() +
            '}';
    }
}
