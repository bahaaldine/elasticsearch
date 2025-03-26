/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.symbol;

import org.elasticsearch.xpack.plesql.primitives.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.VariableDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The SymbolTable class is responsible for managing variables and functions
 * within a given scope.
 */
public class SymbolTable {
    private final Map<String, VariableDefinition> variables = new HashMap<>();
    private final Map<String, FunctionDefinition> functions = new HashMap<>();

    // -------------------------
    // Variable Management
    // -------------------------

    /**
     * Declares a new variable with the specified name using a pre-built VariableDefinition.
     *
     * @param name   The variable name.
     * @param varDef The VariableDefinition instance.
     * @throws RuntimeException if the variable is already declared.
     */
    public void declareVariable(String name, VariableDefinition varDef) {
        if (variables.containsKey(name)) {
            throw new RuntimeException("Variable '" + name + "' is already declared.");
        }
        variables.put(name, varDef);
    }

    /**
     * Convenience overload: Declares a new variable with the specified name and type.
     * Internally, it creates a new VariableDefinition with a null initial value.
     *
     * @param name The variable name.
     * @param type The data type of the variable.
     */
    public void declareVariable(String name, String type) {
        declareVariable(name, new VariableDefinition(name, type, null));
    }

    /**
     * Convenience overload: Declares a new variable with the specified name, type,
     * and element type (for array variables).
     *
     * @param name        The variable name.
     * @param type        The data type of the variable.
     * @param elementType The data type of the elements (if an array).
     */
    public void declareVariable(String name, String type, String elementType) {
        declareVariable(name, new VariableDefinition(name, type, elementType));
    }

    /**
     * Sets or updates the value of a variable.
     *
     * @param name  The variable name.
     * @param value The value to assign.
     * @throws RuntimeException if the variable is not declared.
     */
    public void setVariable(String name, Object value) {
        VariableDefinition varDef = getVariableDefinition(name);
        if (varDef != null) {
            varDef.setValue(value);
        } else {
            throw new RuntimeException("Variable '" + name + "' is not declared.");
        }
    }

    /**
     * Retrieves the value of a variable.
     *
     * @param name The variable name.
     * @return The variable's value.
     * @throws RuntimeException if the variable is not declared.
     */
    public Object getVariable(String name) {
        VariableDefinition def = getVariableDefinition(name);
        if (def != null) {
            return def.getValue();
        }
        throw new RuntimeException("Variable '" + name + "' is not declared.");
    }

    /**
     * Checks whether a variable is declared.
     *
     * @param name The variable name.
     * @return true if declared; false otherwise.
     */
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    /**
     * Retrieves the VariableDefinition for a given variable.
     *
     * @param name The variable name.
     * @return The VariableDefinition, or null if not found.
     */
    public VariableDefinition getVariableDefinition(String name) {
        return variables.get(name);
    }

    /**
     * Returns an unmodifiable set of declared variable names.
     *
     * @return A Set of variable names.
     */
    public Set<String> getVariableNames() {
        return Collections.unmodifiableSet(variables.keySet());
    }

    // -------------------------
    // Function Management
    // -------------------------

    /**
     * Declares a new function.
     *
     * @param name    The function name.
     * @param fnDef   The FunctionDefinition instance.
     * @throws RuntimeException if the function is already declared.
     */
    public void declareFunction(String name, FunctionDefinition fnDef) {
        if (functions.containsKey(name)) {
            throw new RuntimeException("Function '" + name + "' is already declared.");
        }
        functions.put(name, fnDef);
    }

    /**
     * Retrieves the FunctionDefinition for a given function name.
     *
     * @param name The function name.
     * @return The FunctionDefinition.
     * @throws RuntimeException if the function is not declared.
     */
    public FunctionDefinition getFunction(String name) {
        if (functions.containsKey(name)) {
            return functions.get(name);
        }
        throw new RuntimeException("Function '" + name + "' is not declared.");
    }

    /**
     * Checks whether a function is declared.
     *
     * @param name The function name.
     * @return true if declared; false otherwise.
     */
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }

    /**
     * Returns an unmodifiable map of function names to FunctionDefinition objects.
     *
     * @return A Map of functions.
     */
    public Map<String, FunctionDefinition> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    /**
     * Clears all variables and functions from the current context.
     */
    public void clear() {
        variables.clear();
        functions.clear();
        System.out.println("Cleared all variables and functions from the current context.");
    }
}
