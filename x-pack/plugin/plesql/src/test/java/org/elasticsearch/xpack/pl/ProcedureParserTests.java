/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.pl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.pl.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.pl.parser.PlEsqlProcedureParser;

public class ProcedureParserTests extends ESTestCase {

    public void testProcedureParsing() {
        String procedure = """
            BEGIN
                EXECUTE (FROM retro_arcade_purchases
                       | WHERE user_id > 100
                       | SORT date DESC);
                SET some_value = 100;
                DECLARE user_id INT;
            END;
        """;

        // Create a lexer and parser for the input procedure
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(procedure));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        // Parse the procedure and visit the nodes
        PlEsqlProcedureParser.ProcedureContext context = parser.procedure();
        assertNotNull(context);
        System.out.println("Procedure parsed successfully: " + context.toStringTree(parser));
    }
}
