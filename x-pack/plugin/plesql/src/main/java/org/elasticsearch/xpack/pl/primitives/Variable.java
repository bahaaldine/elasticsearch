/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.pl.primitives;

public class Variable {
    private String name;
    private String type;
    private Object value;

    public Variable(String name, String type, Object value) {
        this.name = name;
        this.type = type.toUpperCase();
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        // Optionally, add type checking here
        if (!isCompatibleType(value)) {
            throw new RuntimeException("Type mismatch: Cannot assign value to variable '" + name + "' of type " + type);
        }
        this.value = value;
    }

    private boolean isCompatibleType(Object value) {
        if (value == null) {
            return true; // Allow null values
        }
        switch (type) {
            case "INT":
                return value instanceof Integer;
            case "FLOAT":
                return value instanceof Double || value instanceof Float;
            case "STRING":
                return value instanceof String;
            case "DATE":
                // Implement date type checking if needed
                return value instanceof java.util.Date;
            default:
                return true; // Unknown type, accept any value
        }
    }
}
