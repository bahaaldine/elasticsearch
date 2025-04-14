/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.util.ArrayList;
import java.util.List;

/**
 * A definition for asynchronous built‑in functions.
 * It stores the function name, its parameters, and an AsyncBuiltInFunction implementation,
 * and signals completion via an ActionListener.
 */
public class AsyncBuiltInFunctionDefinition extends FunctionDefinition {

    private final AsyncBuiltInFunction asyncFunction;

    /**
     * Constructs an AsyncBuiltInFunctionDefinition.
     *
     * @param name       The function name.
     * @param parameters The expected list of parameters.
     * @param asyncFunction The asynchronous function implementation.
     */
    public AsyncBuiltInFunctionDefinition(String name, List<Parameter> parameters, AsyncBuiltInFunction asyncFunction) {
        // Pass an empty list for the function body.
        super(name, parameters, new ArrayList<PlEsqlProcedureParser.StatementContext>());
        this.asyncFunction = asyncFunction;
    }

    @Override
    public void execute(List<Object> args, ActionListener<Object> listener) {
        // Directly delegate to the asynchronous function implementation.
        asyncFunction.apply(args, listener);
    }
}
