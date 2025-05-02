/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionListener;

/**
 * Utility class for wrapping an ActionListener in logging calls.
 */
public final class ActionListenerUtils {

    private static final Logger LOGGER = LogManager.getLogger(ActionListenerUtils.class);

    private ActionListenerUtils() {
        // Prevent instantiation
    }

    /**
     * Wraps the given delegate ActionListener with logging for onResponse/onFailure.
     *
     * @param delegate  The real listener to be notified eventually
     * @param className
     * @param operation A short label for logs (e.g. "AssignmentHandler-eval" or "IfHandler-checkCondition")
     * @return A wrapped listener that logs the response/failure before calling delegate
     */
    public static <T> ActionListener<T> withLogging(ActionListener<T> delegate, String className, String operation) {
        return new ActionListener<>() {
            @Override
            public void onResponse(T response) {
                LOGGER.info("[{" + className + "}{" + operation + "}] onResponse: " + response);
                delegate.onResponse(response);
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.info("[{"+ operation +"}] onFailure: " + e.getMessage());
                delegate.onFailure(e);
            }
        };
    }
}
