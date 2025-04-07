/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.utils;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;

import java.io.IOException;
import java.io.StringReader;

public class TestUtils {

    // Method to parse a block of PL|ES|QL code
    public static PlEsqlProcedureParser.ProcedureContext parseProcedure(String query) {
        // Create a lexer and parser for the given input
        PlEsqlProcedureLexer lexer = null;
        try {
            lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(new StringReader(query)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        // Remove any default error listeners if you want to customize error handling
        parser.removeErrorListeners();

        // Parse the input and return the procedure context
        return parser.procedure();
    }
}
