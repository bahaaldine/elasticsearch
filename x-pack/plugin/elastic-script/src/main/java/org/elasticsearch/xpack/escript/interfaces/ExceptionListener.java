/*
 * Copyright Elasticsearch B.
 * under one or more contributor license agreements. Licensed under the Elastic
 * License 2.0; you may not use this file except in compliance with the Elastic
 * License 2.0.
 */

package org.elasticsearch.xpack.escript.interfaces;

/**
 * The ExceptionListener interface defines a contract for handling exceptions
 * that occur during the execution of procedural SQL statements.
 */
public interface ExceptionListener {
    /**
     * Handles the specified exception.
     *
     * @param e The exception to handle.
     */
    void onException(Exception e);
}
