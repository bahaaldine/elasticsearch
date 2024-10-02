/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.FunctionDefinition;
import org.elasticsearch.xpack.plesql.handlers.FunctionDefinitionHandler;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FunctionDefinitionHandlerTests {

    private ExecutionContext context;
    private FunctionDefinitionHandler handler;

    @Before
    public void setup() {
        context = new ExecutionContext();  // Assuming you have a real implementation of ExecutionContext
        handler = new FunctionDefinitionHandler(context);
    }

    // Helper method to parse a function definition and return the necessary context
    private PlEsqlProcedureParser.Function_definitionContext parseFunction(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.function_definition();
    }

    // Test 1: Define a function with no parameters
    @Test
    public void testFunctionDefinitionWithoutParameters() {
        String functionQuery = "FUNCTION myFunction() SET myVar = 10; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        handler.handle(functionContext);

        FunctionDefinition function = context.getFunction("myFunction");
        assertNotNull(function);
        assertEquals("myFunction", function.getName());
        assertEquals(0, function.getParameters().size());
    }

    // Test 2: Define a function with one parameter
    @Test
    public void testFunctionDefinitionWithOneParameter() {
        // Updated function query with typed parameter and simplified function body
        String functionQuery = "FUNCTION myFunction(param1 INT) SET myVar = 10; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        handler.handle(functionContext);

        FunctionDefinition function = context.getFunction("myFunction");
        assertNotNull(function);
        assertEquals("myFunction", function.getName());
        assertEquals(1, function.getParameters().size());
        assertEquals("param1", function.getParameters().get(0));  // Check that the parameter name is correct
    }

    // Test 3: Define a function with multiple parameters
    @Test
    public void testFunctionDefinitionWithMultipleParameters() {
        String functionQuery = "FUNCTION myFunction(param1 INT, param2 INT, param3 INT) SET myVar = 10; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        handler.handle(functionContext);

        FunctionDefinition function = context.getFunction("myFunction");
        assertNotNull(function);
        assertEquals("myFunction", function.getName());
        List<String> params = function.getParameters();
        assertEquals(3, params.size());
        assertEquals("param1", params.get(0));
        assertEquals("param2", params.get(1));
        assertEquals("param3", params.get(2));
    }

    // Test 4: Define a function with a statement block (basic syntax validation)
    @Test
    public void testFunctionDefinitionWithStatementBlock() {
        String functionQuery = "FUNCTION myFunction() SET myVar = 10; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        handler.handle(functionContext);

        FunctionDefinition function = context.getFunction("myFunction");
        assertNotNull(function);
        assertEquals("myFunction", function.getName());
        assertNotNull(function.getBody());
    }

    // Test 5: Invalid function definition (missing body)
    @Test(expected = RuntimeException.class)
    public void testInvalidFunctionDefinitionThrowsError() {
        // Deliberately missing parameter type to trigger an error
        String functionQuery = "FUNCTION myFunction(param1) SET myVar = 10;";  // Missing parameter type for param1
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        handler.handle(functionContext);  // Should throw a RuntimeException due to invalid function definition
    }
}
