/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.utils;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;

import java.io.IOException;
import java.io.StringReader;

public class TestUtils {

    // Method to parse a block of PL|ES|QL code
    public static ElasticScriptParser.ProcedureContext parseProcedure(String query) {
        // Create a lexer and parser for the given input
        ElasticScriptLexer lexer = null;
        try {
            lexer = new ElasticScriptLexer(new ANTLRInputStream(new StringReader(query)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);

        // Remove any default error listeners if you want to customize error handling
        parser.removeErrorListeners();

        // Parse the input and return the procedure context
        return parser.procedure();
    }

    // Helper method to parse a BEGIN ... END procedure block.
    public static ElasticScriptParser.ProcedureContext parseBlock(String query) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        return parser.procedure();  // returns the procedure parse tree.
    }
}
