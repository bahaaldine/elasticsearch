/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.primitives;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExecutionContext {
    private Map<String, Variable> variables = new HashMap<>();
    private Map<String, FunctionDefinition> functions = new HashMap<>();

    // Declare a variable with a type
    public void declareVariable(String name, String type) {
        if (variables.containsKey(name)) {
            throw new RuntimeException("Variable '" + name + "' is already declared.");
        }
        variables.put(name, new Variable(name, type, null));
    }

    // Set or update the value of a variable
    public void setVariable(String name, Object value) {
        Variable variable = variables.get(name);
        if (variable == null) {
            throw new RuntimeException("In Set Variable --- Variable '" + name + "' is not declared.");
        }
        // TODO: perform type checking here
        variable.setValue(value);
    }

    // Get the value of a variable
    public Object getVariable(String name) {
        Variable variable = variables.get(name);
        if (variable == null) {
            throw new RuntimeException("Variable '" + name + "' is not declared.");
        }
        return variable.getValue();
    }

    public void printAllVariables() {
        System.out.println("Current variables in the context: " + variables);
    }

    // Define a function
    public void defineFunction(String name, FunctionDefinition function) {
        if (functions.containsKey(name)) {
            throw new RuntimeException("Function '" + name + "' is already defined.");
        }
        functions.put(name, function);
    }

    // Get a function definition
    public FunctionDefinition getFunction(String name) {
        FunctionDefinition function = functions.get(name);
        if (function == null) {
            throw new RuntimeException("Function '" + name + "' is not defined.");
        }
        return function;
    }

    // Get all variable names
    public Set<String> getVariableNames() {
        return variables.keySet();
    }

    // Get the variables map
    public Map<String, Variable> getVariables() {
        return variables;
    }

    // Get the functions map
    public Map<String, FunctionDefinition> getFunctions() {
        return functions;
    }

    // Optional: Clear all variables and functions (useful for resetting context)
    public void clear() {
        variables.clear();
        functions.clear();
    }
}
