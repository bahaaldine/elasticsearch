/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.VariableDefinition;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class DeclareStatementHandlerTests {
    private ExecutionContext context;
    private ProcedureExecutor executor;
    private DeclareStatementHandler handler;

    @Before
    public void setUp() {
        context = new ExecutionContext();
        executor = new ProcedureExecutor(context);
        handler = new DeclareStatementHandler(executor);
    }

    // Helper method to parse PLESQL queries
    private PlEsqlProcedureParser.Declare_statementContext parseQuery(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.declare_statement();
    }

    @Test
    public void testSimpleDeclaration() {
        String query = "DECLARE user_id INT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseQuery(query);
        handler.handle(declareContext);

        assertTrue(context.getVariableNames().contains("user_id"));
        VariableDefinition userId = context.getVariables().get("user_id");
        assertEquals("INT", userId.getType());
        assertNull(userId.getValue());
    }

    @Test
    public void testDeclarationWithInitialization() {
        String query = "DECLARE user_id INT = 100;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseQuery(query);
        handler.handle(declareContext);

        assertTrue(context.getVariableNames().contains("user_id"));
        VariableDefinition userId = context.getVariables().get("user_id");
        assertEquals("INT", userId.getType());
        assertEquals(100, userId.getValue());
    }

    @Test
    public void testMultipleDeclarations() {
        String query = "DECLARE user_id INT, total FLOAT = 50.5;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseQuery(query);
        handler.handle(declareContext);

        assertTrue(context.getVariableNames().contains("user_id"));
        assertTrue(context.getVariableNames().contains("total"));

        VariableDefinition userId = context.getVariables().get("user_id");
        VariableDefinition total = context.getVariables().get("total");

        assertEquals("INT", userId.getType());
        assertNull(userId.getValue());

        assertEquals("FLOAT", total.getType());
        assertEquals(50.5, total.getValue());
    }

    @Test(expected = RuntimeException.class)
    public void testVariableRedeclarationThrowsError() {
        String query = "DECLARE user_id INT; DECLARE user_id INT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseQuery(query);
        handler.handle(declareContext);
        // Should throw RuntimeException for redeclaration
        handler.handle(declareContext);
    }

    @Test(expected = RuntimeException.class)
    public void testUnsupportedDataTypeThrowsError() {
        String query = "DECLARE unsupported_type CHAR;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseQuery(query);
        handler.handle(declareContext);
    }
}

