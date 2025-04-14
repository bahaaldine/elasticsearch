/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.primitives.procedure;

import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.Parameter;

import java.util.List;

/**
 * Represents a procedure definition in PLESQL.
 * Procedures are like functions but do not return a value.
 * They have a name, a list of parameters (each with a mode, name, and type),
 * and a body (a list of statement contexts).
 */
public abstract class ProcedureDefinition extends FunctionDefinition {

    /**
     * Constructs a ProcedureDefinition.
     *
     * @param name       The name of the procedure.
     * @param parameters The list of parameters.
     * @param body       The procedure body as a list of statement contexts.
     */
    public ProcedureDefinition(String name, List<Parameter> parameters,
                               List<PlEsqlProcedureParser.StatementContext> body) {
        super(name, parameters, body);
    }

    /**
     * Executes the procedure.
     * Subclasses must implement this method to define the procedure’s behavior.
     * Procedures do not return a value.
     *
     * @param args the list of argument values.
     */
    protected abstract void executeProcedure(List<Object> args);

    /**
     * For procedures, the execute method calls executeProcedure and then returns null.
     *
     * @param args the list of argument values.
     * @return always returns null.
     */
    public Object execute(List<Object> args) {
        executeProcedure(args);
        return null;
    }
}
