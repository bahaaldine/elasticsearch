/*
 * Copyright Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.plesql.primitives;

import java.util.List;

/**
 * The VariableDefinition class represents a variable in the execution context.
 * It holds the variable's name, data type, and current value.
 */
public class VariableDefinition {
    private final String name;
    private final PLESQLDataType type;
    private Object value;
    private final String elementType;

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
        this.elementType = null;
    }

    /**
     * Constructs a VariableDefinition with the specified name, type, and element type.
     * <p>
     * For array types, the type string may be provided as "ARRAY", "ARRAY OF elementType"
     * or even without the expected space (e.g. "ARRAYOFSTRING"). In that case, the variable's
     * type will be set to ARRAY and the element type will be extracted from the type string
     * (unless an explicit elementType argument is provided).
     *
     * @param name         The name of the variable.
     * @param type         The data type of the variable (e.g. "NUMBER", "STRING", "ARRAY OF STRING").
     * @param elementType  (Optional) The element type for array variables; if null, it will be
     *                     extracted from the type string when applicable.
     * @throws IllegalArgumentException if the type string is invalid.
     */
    public VariableDefinition(String name, String type, String elementType) {
        this.name = name;
        String normalizedType = type.trim().toUpperCase();

        if (normalizedType.startsWith("ARRAY")) {
            // Fix missing space if type is like "ARRAYOFSTRING"
            if (normalizedType.startsWith("ARRAYOF") && normalizedType.startsWith("ARRAY OF") == false ) {
                normalizedType = "ARRAY OF " + normalizedType.substring("ARRAYOF".length());
            }
            // For plain "ARRAY", simply mark type as ARRAY and use provided elementType (could be null)
            if (normalizedType.equals("ARRAY")) {
                this.type = PLESQLDataType.ARRAY;
                this.elementType = elementType;
            } else if (normalizedType.startsWith("ARRAY OF ")) {
                this.type = PLESQLDataType.ARRAY;
                // If the caller did not supply an elementType, extract it from the type string.
                String extractedElementType = normalizedType.substring("ARRAY OF ".length()).trim();
                this.elementType = (elementType != null) ? elementType : extractedElementType;
            } else {
                throw new IllegalArgumentException("Invalid array type provided: " + type);
            }
        } else {
            try {
                this.type = PLESQLDataType.valueOf(normalizedType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid type provided: " + type, e);
            }
            this.elementType = elementType;
        }

        this.value = null;
    }

    /**
     * Retrieves the declared element type for this ARRAY variable.
     * <p>
     * This represents the data type of the elements contained in the array (e.g., NUMBER, STRING, or DATE).
     * For variables that are not of ARRAY type, this value may be {@code null}.
     *
     * @return the element type as a String, or {@code null} if not applicable.
     */
    public String getElementType() {
        return elementType;
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
            throw new RuntimeException("Type mismatch: Expected " + type + " for variable '" + name + "', " +
                "for value: " + value.getClass() + " .");
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
                return value instanceof java.util.Date;
            case ARRAY:
                return value instanceof List<?>;
            case DOCUMENT:
                return value instanceof java.util.Map;
            case ANY:
                return true;
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
