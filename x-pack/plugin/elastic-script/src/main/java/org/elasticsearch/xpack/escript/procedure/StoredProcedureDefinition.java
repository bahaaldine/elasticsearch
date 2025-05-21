/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.procedure;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.util.List;

public class StoredProcedureDefinition extends ProcedureDefinition {

    public StoredProcedureDefinition(String name, List<Parameter> parameters,
                                     List<ElasticScriptParser.StatementContext> body) {
        super(name, parameters, body);
    }

    @Override
    public void execute(List<Object> args, ActionListener<Object> listener) {

    }

    @Override
    protected void executeProcedure(List<Object> args) {
        // For now, do nothing — you can wire real execution later
    }
}
