/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.features.NodeFeature;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.actions.RestGetProcedureAction;
import org.elasticsearch.xpack.escript.actions.RestRunEScriptAction;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;
import org.elasticsearch.xpack.escript.actions.ElasticScriptAction;
import org.elasticsearch.xpack.escript.actions.TransportElasticScriptAction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ElasticScriptPlugin extends Plugin implements ActionPlugin {
    private ThreadPool threadPool;
    private ElasticScriptExecutor elasticScriptExecutor;

    private static final Logger LOGGER = LogManager.getLogger(ElasticScriptPlugin.class);

    @Override
    public Collection<?> createComponents(PluginServices services) {
        this.threadPool = services.threadPool();
        Client client = services.client();

        // Initialize your components here
        // If ElasticScriptExecutor needs the ThreadPool or other services, pass them here
        elasticScriptExecutor = new ElasticScriptExecutor(threadPool, client);

        // Return components if any
        return Collections.emptyList();
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        NamedWriteableRegistry namedWriteableRegistry,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster,
        Predicate<NodeFeature> clusterSupportsFeature
    ) {
        return List.of(
            new RestRunEScriptAction(elasticScriptExecutor),
            new RestGetProcedureAction(elasticScriptExecutor)
        );
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return List.of(
            new ActionHandler<>(ElasticScriptAction.INSTANCE, TransportElasticScriptAction.class)
        );
    }
}
