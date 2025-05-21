/*
 * Copyright Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.escript.context;

import org.elasticsearch.xpack.escript.functions.FunctionDefinition;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.primitives.VariableDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ExecutionContext class manages the execution state, including variables and functions,
 * within a procedural SQL execution environment. It supports nested contexts to handle scopes
 * such as functions and loops.
 */
public class ExecutionContext {
    private final Map<String, VariableDefinition> variables;
    private final Map<String, FunctionDefinition> functions;
    private final ExecutionContext parentContext;  // Reference to parent context
    private Map<String, Object> procedureArguments;

    /**
     * Constructs a global ExecutionContext with no parent.
     */
    public ExecutionContext() {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.parentContext = null;
        this.procedureArguments = new HashMap<>();
    }

    /**
     * Constructs a nested ExecutionContext with the specified parent context.
     *
     * @param parentContext The parent ExecutionContext.
     */
    public ExecutionContext(ExecutionContext parentContext) {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.parentContext = parentContext;
        this.procedureArguments = new HashMap<>();
    }

    // -------------------------
    // Variable Management
    // -------------------------

    /**
     * Declares a new variable with the specified name and type in the current context.
     *
     * @param name The name of the variable.
     * @param type The data type of the variable.
     * @throws RuntimeException If the variable is already declared in the current context.
     */
    public void declareVariable(String name, String type) {
        if (variables.containsKey(name)) {
            throw new RuntimeException("Variable '" + name + "' is already declared in the current scope.");
        }
        variables.put(name, new VariableDefinition(name, type, null));
    }

    /**
     * Declares a new variable with the specified name, type, and element type in the current context.
     *
     * @param name The name of the variable.
     * @param type The data type of the variable.
     * @param elementType The declared type for the elements if this variable is an array.
     * @throws RuntimeException If the variable is already declared in the current context.
     */
    public void declareVariable(String name, String type, String elementType) {
        if (variables.containsKey(name)) {
            throw new RuntimeException("Variable '" + name + "' is already declared in the current scope.");
        }
        variables.put(name, new VariableDefinition(name, type, elementType));
    }

    /**
     * Sets or updates the value of a variable. Searches for the variable in the current
     * context and parent contexts recursively.
     *
     * @param name  The name of the variable.
     * @param value The value to assign to the variable.
     * @throws RuntimeException If the variable is not declared in any accessible context.
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
     * Retrieves the value of a variable. Searches for the variable in the current
     * context and parent contexts recursively.
     *
     * @param name The name of the variable.
     * @return The value of the variable.
     * @throws RuntimeException If the variable is not declared in any accessible context.
     */
    public Object getVariable(String name) {
        VariableDefinition varDef = getVariableDefinition(name);
        if (varDef != null) {
            return varDef.getValue();
        } else {
            throw new RuntimeException("Variable '" + name + "' is not declared.");
        }
    }

    /**
     * Retrieves the type of a declared variable as a String.
     * It calls toString() on the underlying type object.
     *
     * @param name The variable name.
     * @return The type as a String.
     * @throws RuntimeException If the variable is not declared.
     */
    public String getVariableType(String name) {
        VariableDefinition def = getVariableDefinition(name);
        if (def == null) {
            throw new RuntimeException("Variable '" + name + "' is not declared.");
        }
        // Explicitly return the string representation of the type.
        return def.getType().toString();
    }

    /**
     * Checks if a variable with the specified name exists in the current or any parent context.
     *
     * @param name The name of the variable.
     * @return true if the variable exists; false otherwise.
     */
    public boolean hasVariable(String name) {
        return getVariableDefinition(name) != null;
    }

    /**
     * Prints all declared variables and their current values in the execution context.
     */
    public void printAllVariables() {
        if (variables.isEmpty()) {
            return;
        }

        for (Map.Entry<String, VariableDefinition> entry : variables.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }

    /**
     * Retrieves the VariableDefinition for a given variable name by searching the current
     * and parent contexts recursively.
     *
     * @param name The name of the variable.
     * @return The VariableDefinition object if found; null otherwise.
     */
    public VariableDefinition getVariableDefinition(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else if (parentContext != null) {
            return parentContext.getVariableDefinition(name);
        } else {
            return null;
        }
    }

    // -------------------------
    // Function Management
    // -------------------------

    /**
     * Retrieves the FunctionDefinition associated with the specified function name.
     * Searches in the current context and parent contexts recursively.
     *
     * @param name The name of the function.
     * @return The FunctionDefinition object.
     * @throws RuntimeException If the function is not defined in any accessible context.
     */
    public FunctionDefinition getFunction(String name) {
        if (functions.containsKey(name)) {
            return functions.get(name);
        } else if (parentContext != null) {
            return parentContext.getFunction(name);
        } else {
            throw new RuntimeException("Function '" + name + "' is not defined.");
        }
    }

    /**
     * Retrieves the built-in function definition for the specified function name.
     * <p>
     * This method first retrieves a function using {@link #getFunction(String)}.
     * If the returned FunctionDefinition is an instance of BuiltInFunctionDefinition,
     * it is cast and returned. Otherwise, a RuntimeException is thrown.
     *
     * @param name the name of the built-in function to retrieve.
     * @return the BuiltInFunctionDefinition corresponding to the specified name.
     * @throws RuntimeException if the function exists but is not a built-in function.
     */
    public BuiltInFunctionDefinition getBuiltInFunction(String name) {
        FunctionDefinition fn = getFunction(name);
        if (fn instanceof BuiltInFunctionDefinition) {
            return (BuiltInFunctionDefinition) fn;
        } else {
            throw new RuntimeException("Function '" + name + "' is not a built-in function.");
        }
    }

    /**
     * Declares a new function in the global context. If the current context is nested,
     * the declaration is delegated to the parent context.
     *
     * @param name     The name of the function.
     * @param function The FunctionDefinition object.
     * @throws RuntimeException If the function is already defined in the global context.
     */
    public void declareFunction(String name, FunctionDefinition function) {
        if (parentContext == null) {
            if (functions.containsKey(name)) {
                throw new RuntimeException("Function '" + name + "' is already defined in the global context.");
            }
            functions.put(name, function);
        } else {
            parentContext.declareFunction(name, function);
        }
    }

    /**
     * Declares (or overrides) a function definition in the global execution context with a specified parameter list.
     * <p>
     * This method registers a new function with the given name and parameter list. If the current context is nested,
     * the declaration is delegated recursively to the parent context so that only the global context ultimately holds the definition.
     * <p>
     * If the provided FunctionDefinition is an instance of BuiltInFunctionDefinition, its parameter list is updated
     * using the provided parameters. Otherwise, the function is registered as-is.
     *
     * @param name       The name of the function to declare.
     * @param parameters A list of Parameter objects defining the function's parameter signature.
     *                   This should reflect the exact number of parameters the function expects.
     * @param function   The FunctionDefinition object that encapsulates the function's implementation.
     * @throws RuntimeException if a function with the given name is already defined in the global context.
     */
    public void declareFunction(String name, List<Parameter> parameters, FunctionDefinition function) {
        if (parentContext == null) {
            if (functions.containsKey(name)) {
                throw new RuntimeException("Function '" + name + "' is already defined in the global context.");
            }
            // If the function is a BuiltInFunctionDefinition, update its parameter list.
            if (function instanceof BuiltInFunctionDefinition) {
                ((BuiltInFunctionDefinition) function).setParameters(parameters);
            }
            functions.put(name, function);
        } else {
            parentContext.declareFunction(name, parameters, function);
        }
    }

    /**
     * Overrides an existing function definition (or declares it if not already present).
     * This is primarily useful for testing or updating built-in function definitions.
     *
     * @param name The function name.
     * @param function The new FunctionDefinition.
     */
    public void overrideFunction(String name, FunctionDefinition function) {
        if (parentContext != null) {
            parentContext.overrideFunction(name, function);
        } else {
            functions.put(name, function);
        }
    }

    /**
     * Checks if a function with the specified name exists in the current or any parent context.
     *
     * @param name The name of the function.
     * @return true if the function exists; false otherwise.
     */
    public boolean hasFunction(String name) {
        if (functions.containsKey(name)) {
            return true;
        } else if (parentContext != null) {
            return parentContext.hasFunction(name);
        } else {
            return false;
        }
    }

    /**
     * Retrieves an unmodifiable set of variable names available in the current context,
     * including those from parent contexts.
     *
     * @return A Set of variable names.
     */
    public Set<String> getVariableNames() {
        if (parentContext != null) {
            Set<String> parentVars = parentContext.getVariableNames();
            parentVars.addAll(variables.keySet());
            return parentVars;
        } else {
            return Collections.unmodifiableSet(variables.keySet());
        }
    }

    /**
     * Retrieves an unmodifiable view of the variables in the current context.
     *
     * @return A Map of variable names to VariableDefinition objects.
     */
    public Map<String, VariableDefinition> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * Retrieves an unmodifiable view of the functions in the current context.
     *
     * @return A Map of function names to FunctionDefinition objects.
     */
    public Map<String, FunctionDefinition> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    /**
     * Clears all variables and functions from the current context.
     * This does not affect any parent contexts.
     */
    public void clear() {
        variables.clear();
        functions.clear();
    }

    /**
     * Sets the arguments passed to the procedure.
     *
     * @param args a Map of argument names to their values.
     */
    public void setProcedureArguments(Map<String, Object> args) {
        this.procedureArguments.clear();
        if (args != null) {
            this.procedureArguments.putAll(args);
        }
    }

    /**
     * Retrieves the map of all procedure arguments.
     *
     * @return a Map of argument names to their values.
     */
    public Map<String, Object> getProcedureArguments() {
        return Collections.unmodifiableMap(procedureArguments);
    }

    /**
     * Retrieves a single argument by name.
     *
     * @param name the name of the argument.
     * @return the argument value.
     * @throws RuntimeException if the argument is not found.
     */
    public Object getArgumentValue(String name) {
        if (procedureArguments.containsKey(name)) {
            return procedureArguments.get(name);
        } else {
            throw new RuntimeException("Argument '" + name + "' is not defined for the procedure.");
        }
    }
}
