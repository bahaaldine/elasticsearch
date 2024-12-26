/*
 * Copyright Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.plesql.primitives;

/**
 * The VariableDefinition class represents a variable in the execution context.
 * It holds the variable's name, data type, and current value.
 */
public class VariableDefinition {
    private final String name;
    private final PLESQLDataType type;
    private Object value;

    /**
     * Constructs a VariableDefinition with the specified name, type, and initial value.
     *
     * @param name  The name of the variable.
     * @param type  The data type of the variable.
     * @param value The initial value of the variable.
     */
    public VariableDefinition(String name, String type, Object value) {
        this.name = name;
        try {
            this.type = PLESQLDataType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid type provided: " + type, e);
        }
        this.value = value;
    }

    /**
     * Retrieves the name of the variable.
     *
     * @return The variable's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the data type of the variable.
     *
     * @return The variable's data type.
     */
    public PLESQLDataType getType() {
        return type;
    }

    /**
     * Retrieves the current value of the variable.
     *
     * @return The variable's value.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets a new value for the variable after validating type compatibility.
     *
     * @param value The new value to assign.
     * @throws RuntimeException If the value's type does not match the variable's data type.
     */
    public void setValue(Object value) {
        if ( isTypeCompatible(value) == false ) {
            throw new RuntimeException("Type mismatch: Expected " + type + " for variable '" + name + "'.");
        }
        if (value instanceof Number) {
            // If value is a Number (Integer, Float, Double, etc.), convert directly
            this.value = ((Number) value).doubleValue();
        } else {
            this.value = value;
        }
    }

    /**
     * Checks if the given value is compatible with the variable's type.
     *
     * @param value The value to check.
     * @return true if compatible, false otherwise.
     */
    public boolean isTypeCompatible(Object value) {
        if (value == null) {
            return true; // Allow null assignments
        }

        switch (type) {
            case NUMBER:
                return value instanceof Double || value instanceof Integer;
            case STRING:
                return value instanceof String;
            case DATE:
                return value instanceof java.util.Date; // Adjust as per your date handling
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "VariableDefinition{" +
            "name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", value=" + value +
            '}';
    }
}
