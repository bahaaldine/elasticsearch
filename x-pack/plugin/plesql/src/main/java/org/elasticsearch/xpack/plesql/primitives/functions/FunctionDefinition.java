/*
 * Copyright Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */
package org.elasticsearch.xpack.plesql.primitives.functions;

import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.util.List;

/**
 * The FunctionDefinition class represents a user-defined function within the PLESQL execution context.
 * It contains the function's name, a list of parameters (each with its name, type, and mode),
 * and the body statements.
 */
public abstract class FunctionDefinition {
    private final String name;
    private final List<Parameter> parameters;
    // The body is represented as a list of StatementContext objects.
    private final List<PlEsqlProcedureParser.StatementContext> body;

    /**
     * Constructs a FunctionDefinition with the specified name, parameters, and body.
     *
     * @param name       The name of the function.
     * @param parameters The list of Parameter objects.
     * @param body       The list of statements constituting the function body.
     */
    public FunctionDefinition(String name, List<Parameter> parameters, List<PlEsqlProcedureParser.StatementContext> body) {
        this.name = name;
        this.parameters = parameters;
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
     * Retrieves the list of parameters.
     *
     * @return A list of Parameter objects.
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * Retrieves the function body.
     *
     * @return A list of StatementContext objects representing the function body.
     */
    public List<PlEsqlProcedureParser.StatementContext> getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "FunctionDefinition{" +
            "name='" + name + '\'' +
            ", parameters=" + parameters +
            ", bodySize=" + (body != null ? body.size() : 0) +
            '}';
    }

    /**
     * Executes the function with the given argument values.
     *
     * @param args The list of argument values.
     * @return The result of executing the function.
     */
    public abstract Object execute(List<Object> args);
}
