/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface FunctionSpec {
    String name();
    String description();
    FunctionParam[] parameters(); // Structured parameter definitions
    FunctionReturn returnType();  // Replaces String returnType()
    String[] examples();          // e.g., {"REVERSE_STRING('hello') -> 'olleh'"}
    FunctionCategory category();           // e.g., "string", "geo", "nlp", "security"
}
