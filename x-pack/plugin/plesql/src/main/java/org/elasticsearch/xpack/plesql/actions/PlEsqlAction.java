/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.actions;


import org.elasticsearch.action.ActionType;

public class PlEsqlAction extends ActionType<PlEsqlQueryResponse> {

    public static final PlEsqlAction INSTANCE = new PlEsqlAction();
    public static final String NAME = "internal:query/plesql";

    public PlEsqlAction() { super(NAME); }
}
