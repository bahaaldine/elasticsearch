/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.FunctionDefinition;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FunctionDefinitionHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private FunctionDefinitionHandler handler;
    private ThreadPool threadPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // or mock(Client.class);
        PlEsqlProcedureLexer lexer =
            new PlEsqlProcedureLexer(CharStreams.fromString("")); // empty source
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        handler = new FunctionDefinitionHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a function definition and return the necessary context
    private PlEsqlProcedureParser.Function_definitionContext parseFunction(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.function_definition();
    }

    // Helper method to parse a full procedure block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.procedure();
    }

    // Test 1: Define a function with no parameters and test its registration
    @Test
    public void testFunctionDefinitionWithoutParameters() throws InterruptedException {
        String functionQuery = "FUNCTION myFunction() BEGIN RETURN 10; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        CountDownLatch latch = new CountDownLatch(1);

        handler.handleAsync(functionContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                FunctionDefinition function = context.getFunction("myFunction");
                assertNotNull(function);
                assertEquals("myFunction", function.getName());
                assertEquals(0, function.getParameters().size());
                assertNotNull(function.getBody());
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Function definition failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 2: Define a function with one parameter and test its registration
    @Test
    public void testFunctionDefinitionWithOneParameter() throws InterruptedException {
        String functionQuery = "FUNCTION add(a NUMBER) BEGIN RETURN a + 5; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        CountDownLatch latch = new CountDownLatch(1);

        handler.handleAsync(functionContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                FunctionDefinition function = context.getFunction("add");
                assertNotNull(function);
                assertEquals("add", function.getName());
                assertEquals(1, function.getParameters().size());
                assertEquals("a", function.getParameters().get(0));
                assertNotNull(function.getBody());
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Function definition failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 3: Define a function with multiple parameters and test its registration
    @Test
    public void testFunctionDefinitionWithMultipleParameters() throws InterruptedException {
        String functionQuery = "FUNCTION multiply(a NUMBER, b NUMBER, c NUMBER) BEGIN RETURN a * b * c; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        CountDownLatch latch = new CountDownLatch(1);

        handler.handleAsync(functionContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                FunctionDefinition function = context.getFunction("multiply");
                assertNotNull(function);
                assertEquals("multiply", function.getName());
                List<String> params = function.getParameters();
                assertEquals(3, params.size());
                assertEquals("a", params.get(0));
                assertEquals("b", params.get(1));
                assertEquals("c", params.get(2));
                assertNotNull(function.getBody());
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Function definition failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 4: Define a function within a procedure block and verify its execution
    @Test
    public void testFunctionDefinitionAndCallWithinBlock() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE result NUMBER;
                FUNCTION add(a NUMBER, b NUMBER) BEGIN
                    RETURN a + b;
                END FUNCTION;
                SET result = add(3, 4);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Assert that the function 'add' is registered
                FunctionDefinition function = context.getFunction("add");
                assertNotNull(function);
                assertEquals("add", function.getName());

                // Assert that 'result' is correctly assigned the value returned by 'add(3, 4)'
                assertNotNull("Variable 'result' should be declared.", context.getVariable("result"));
                Object resultValue = context.getVariable("result");
                assertTrue("Variable 'result' should be a number.", resultValue instanceof Number);
                int expectedResult = 7;
                int actualResult = ((Number) resultValue).intValue();
                assertEquals("Variable 'result' should be 7 after add function call.", expectedResult, actualResult);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Procedure execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Continue updating other test methods similarly...

    // Example of Test 5 updated
    // Test 5: Define a function with a return statement and execute it within a loop
    @Test
    public void testFunctionCallWithinLoop() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE total NUMBER, i NUMBER;
                FUNCTION add(a NUMBER, b NUMBER) BEGIN
                    RETURN a + b;
                END FUNCTION;
                SET total = 0;
                FOR i IN 1..3 LOOP
                    SET total = add(total, i);
                END LOOP;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Assert that 'total' accumulates the sum correctly
                assertNotNull("Variable 'total' should be declared.", context.getVariable("total"));
                Object totalValue = context.getVariable("total");
                assertTrue("Variable 'total' should be a number.", totalValue instanceof Number);
                int expectedTotal = 6; // 0 + 1 + 2 + 3
                int actualTotal = ((Number) totalValue).intValue();
                assertEquals("Variable 'total' should be 6 after loop execution.", expectedTotal, actualTotal);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Procedure execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Update the rest of the tests following the same pattern.

    // Note: For tests that expect exceptions, you need to adjust how you handle exceptions since JUnit's @Test(expected = ...)
    // doesn't work well with asynchronous code.

    // For example, Test 8: Define a function with no return statement (should throw an error during execution)
    @Test
    public void testFunctionDefinitionWithoutReturnThrowsError() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE result INT;
                FUNCTION faultyFunction(a INT, b INT) BEGIN
                    SET result = a + b;
                END FUNCTION;
                SET result = faultyFunction(1, 2);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                fail("Expected an exception due to missing return statement in function.");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                // Expected exception
                latch.countDown();
            }
        });

        latch.await();
    }
}
