/*
 * Copyright Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.plesql.primitives;

import org.elasticsearch.xpack.plesql.primitives.functions.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.functions.interfaces.BuiltInFunction;

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

    /**
     * Constructs a global ExecutionContext with no parent.
     */
    public ExecutionContext() {
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.parentContext = null;
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
    }

    /**
     * Retrieves the parent ExecutionContext.
     *
     * @return The parent ExecutionContext, or null if this is the global context.
     */
    public ExecutionContext getParentContext() {
        return this.parentContext;
    }

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
            Object value = varDef.getValue();
            return value;
        } else {
            throw new RuntimeException("Variable '" + name + "' is not declared.");
        }
    }

    public String getVariableType(String name) {
        VariableDefinition def = getVariableDefinition(name);
        if (def == null) {
            throw new RuntimeException("Variable '" + name + "' is not declared.");
        }
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
     * If no variables are declared, it will print "No variables declared."
     *
     * Example output:
     * <pre>
     * All Variables:
     * x = 5
     * y = 10.0
     * z = null
     * </pre>
     */
    public void printAllVariables() {
        if (variables.isEmpty()) {
            System.out.println("No variables declared.");
            return;
        }

        System.out.println("All Variables:");
        for (Map.Entry<String, VariableDefinition> entry : variables.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }

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
     * If the returned {@code FunctionDefinition} is an instance of {@code BuiltInFunctionDefinition},
     * it is cast and returned. Otherwise, a {@code RuntimeException} is thrown, indicating that the
     * specified function is not implemented as a built-in function.
     *
     * @param name the name of the built-in function to retrieve.
     * @return the {@code BuiltInFunctionDefinition} corresponding to the specified name.
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
     * Declares a new function in the global context. If the current context is not global,
     * delegates the declaration to the parent context.
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
     * Convenience overload: Declares a built-in function by wrapping it in a FunctionDefinition.
     *
     * @param name          The name of the function (e.g., "UPPER", "LENGTH", etc.).
     * @param builtInLambda A BuiltInFunction lambda to be invoked for this function.
     */
    public void declareFunction(String name, BuiltInFunction builtInLambda) {
        FunctionDefinition def = new FunctionDefinition(
            name,
            Collections.emptyList(),    // parameterNames: built-in functions have no declared parameters here
            Collections.emptyList(),    // parameterTypes: built-in functions have no declared parameter types here
            Collections.emptyList()     // function body: built-in functions have no body since they're executed via lambda
        ) {
            public Object execute(List<Object> args) {
                return builtInLambda.apply(args);
            }
        };
        declareFunction(name, def);
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
     * Retrieves all variable names accessible in the current context, including those from parent contexts.
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
     * This does not affect parent contexts.
     */
    public void clear() {
        variables.clear();
        functions.clear();
        System.out.println("Cleared all variables and functions from the current context.");
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
}
