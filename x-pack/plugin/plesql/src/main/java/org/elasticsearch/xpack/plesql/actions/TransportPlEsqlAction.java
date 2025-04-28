/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.actions;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.plesql.executors.PlEsqlExecutor;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;


public class TransportPlEsqlAction extends HandledTransportAction<PlEsqlQueryRequest, PlEsqlQueryResponse> {

    private static final Logger LOGGER = LogManager.getLogger(TransportPlEsqlAction.class);

    private final PlEsqlExecutor plEsqlExecutor;

    @Inject
    public TransportPlEsqlAction(TransportService transportService, ActionFilters actionFilters, PlEsqlExecutor plEsqlExecutor) {
        super(PlEsqlAction.NAME, transportService, actionFilters, PlEsqlQueryRequest::new, EsExecutors.DIRECT_EXECUTOR_SERVICE);
        this.plEsqlExecutor = plEsqlExecutor;
    }

    @Override
    protected void doExecute(Task task, PlEsqlQueryRequest request, ActionListener<PlEsqlQueryResponse> listener) {
        plEsqlExecutor.executeProcedure(request.getQuery(), request.getArguments(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {

                LOGGER.debug("Return value from the Transport PLESQL action");

                Object finalValue = result;
                if (result instanceof ReturnValue) {
                    finalValue = ((ReturnValue) result).getValue();
                }

                PlEsqlQueryResponse response = new PlEsqlQueryResponse(finalValue == null ? null : finalValue.toString(), RestStatus.OK);
                listener.onResponse(response);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }
}
