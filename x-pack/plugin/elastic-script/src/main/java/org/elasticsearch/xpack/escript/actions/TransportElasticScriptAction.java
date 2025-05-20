/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.actions;

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
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;


public class TransportElasticScriptAction extends HandledTransportAction<ElasticScriptQueryRequest, ElasticScriptQueryResponse> {

    private static final Logger LOGGER = LogManager.getLogger(TransportElasticScriptAction.class);

    private final ElasticScriptExecutor elasticScriptExecutor;

    @Inject
    public TransportElasticScriptAction(TransportService transportService, ActionFilters actionFilters, ElasticScriptExecutor elasticScriptExecutor) {
        super(ElasticScriptAction.NAME, transportService, actionFilters, ElasticScriptQueryRequest::new, EsExecutors.DIRECT_EXECUTOR_SERVICE);
        this.elasticScriptExecutor = elasticScriptExecutor;
    }

    @Override
    protected void doExecute(Task task, ElasticScriptQueryRequest request, ActionListener<ElasticScriptQueryResponse> listener) {
        elasticScriptExecutor.executeProcedure(request.getQuery(), request.getArguments(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {

                LOGGER.debug("Return value from the Transport Elastic Script action");

                Object finalValue = result;
                if (result instanceof ReturnValue) {
                    finalValue = ((ReturnValue) result).getValue();
                }

                ElasticScriptQueryResponse response = new ElasticScriptQueryResponse(finalValue == null ? null : finalValue.toString(), RestStatus.OK);
                listener.onResponse(response);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }
}
