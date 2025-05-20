/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the Elastic License 2.0;
 */


package org.elasticsearch.xpack.escript.functions.api;

public @interface FunctionReturn {
    String type();          // e.g., "STRING", "ARRAY OF DOCUMENT"
    String description();   // What the return value represents
}
