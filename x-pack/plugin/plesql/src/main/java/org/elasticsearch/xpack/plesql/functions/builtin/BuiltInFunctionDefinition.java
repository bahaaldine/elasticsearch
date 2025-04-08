/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin;

import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction;

import java.util.Collections;
import java.util.List;

public class BuiltInFunctionDefinition extends FunctionDefinition {

    private final BuiltInFunction function;
    private List<Parameter> parameters = Collections.emptyList();

    /**
     * Constructs a BuiltInFunctionDefinition with the given name and BuiltInFunction lambda.
     * Parameters, parameterTypes, and body are set to empty lists.
     *
     * @param name     the function name
     * @param function the lambda that implements this built-in function
     */
    public BuiltInFunctionDefinition(String name, BuiltInFunction function) {
        super(name, Collections.emptyList(), Collections.<PlEsqlProcedureParser.StatementContext>emptyList());
        this.function = function;
    }

    /**
     * Executes the built-in function by delegating to the stored BuiltInFunction lambda.
     *
     * @param args the list of argument values
     * @return the result of applying the built-in function
     */
    public Object execute(List<Object> args) {
        return function.apply(args);
    }

    /**
     * Updates the parameter list for this built‐in function definition.
     * <p>
     * This method replaces the existing (initially empty) parameter list with the given list of parameters.
     * It is used to update the function’s expected signature—for example, when registering a built‐in function
     * like TRIM that should accept one argument instead of none.
     *
     * @param parameters a list of {@link Parameter} objects defining the expected arguments for this function.
     */
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns the current parameter list for this function.
     *
     * @return a list of {@link Parameter} objects.
     */
    public List<Parameter> getParameters() {
        return parameters;
    }
}
