/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the Elastic License 2.0;
 */

package org.elasticsearch.xpack.escript.functions.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines metadata for a parameter in a Elastic Script built-in function.
 * Used by agents and validation tools to reason about the function interface.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FunctionParam {
    /**
     * Name of the parameter as expected in the function signature.
     */
    String name();

    /**
     * Type of the parameter (e.g., STRING, NUMBER, DOCUMENT, ARRAY OF STRING).
     */
    String type();

    /**
     * Description of what the parameter represents.
     */
    String description();
}
