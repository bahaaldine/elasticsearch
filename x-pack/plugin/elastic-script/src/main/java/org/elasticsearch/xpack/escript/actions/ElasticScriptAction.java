/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.actions;


import org.elasticsearch.action.ActionType;

public class ElasticScriptAction extends ActionType<ElasticScriptQueryResponse> {

    public static final ElasticScriptAction INSTANCE = new ElasticScriptAction();
    public static final String NAME = "internal:query/escript";

    public ElasticScriptAction() { super(NAME); }
}
