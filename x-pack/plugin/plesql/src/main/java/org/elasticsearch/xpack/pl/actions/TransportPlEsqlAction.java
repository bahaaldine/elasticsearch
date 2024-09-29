/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.pl.actions;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.injection.guice.Inject;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.pl.PlEsqlExecutor;


public class TransportPlEsqlAction extends HandledTransportAction<PlEsqlQueryRequest, PlEsqlQueryResponse> {

    private final PlEsqlExecutor plEsqlExecutor;

    @Inject
    public TransportPlEsqlAction(TransportService transportService, ActionFilters actionFilters, PlEsqlExecutor plEsqlExecutor) {
        super(PlEsqlAction.NAME, transportService, actionFilters, PlEsqlQueryRequest::new, EsExecutors.DIRECT_EXECUTOR_SERVICE);
        this.plEsqlExecutor = plEsqlExecutor;
    }

    @Override
    protected void doExecute(Task task, PlEsqlQueryRequest request, ActionListener<PlEsqlQueryResponse> listener) {
        String result = plEsqlExecutor.executeProcedure(request.getQuery());
        PlEsqlQueryResponse response = new PlEsqlQueryResponse(result, RestStatus.OK);
        listener.onResponse(response);
    }
}
