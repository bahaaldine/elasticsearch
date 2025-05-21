/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions;

/**
 * Represents a function (or procedure) parameter with a name, a type, and a mode (IN, OUT, or INOUT).
 */
public class Parameter {
    private final String name;
    private final String type;
    private final ParameterMode mode;

    public Parameter(String name, String type, ParameterMode mode) {
        this.name = name;
        this.type = type;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public ParameterMode getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return mode + " " + name + " " + type;
    }
}
