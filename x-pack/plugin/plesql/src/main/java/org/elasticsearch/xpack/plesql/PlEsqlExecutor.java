/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

public class PlEsqlExecutor {
    public String executeProcedure(String procedureText) {
        // Create lexer and parser
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(procedureText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        // Add error listeners if needed
        parser.removeErrorListeners();
        parser.addErrorListener(new PlEsqlErrorListener());

        // Parse the procedure
        PlEsqlProcedureParser.ProcedureContext context = parser.procedure();

        // Create and use the visitor to execute the procedure
        ExecutionContext executionContext = new ExecutionContext();
        ProcedureExecutor executor = new ProcedureExecutor(executionContext);
        executor.visit(context);

        return "Procedure executed successlfully";
    }
}
