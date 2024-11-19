/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class DeclareStatementHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private DeclareStatementHandler declareHandler;
    private ThreadPool threadPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        executor = new ProcedureExecutor(context, threadPool);
        declareHandler = new DeclareStatementHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }


    // Helper method to parse a query for a declaration
    private PlEsqlProcedureParser.Declare_statementContext parseDeclaration(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.declare_statement();
    }

    // Test 1: Declare an INT variable
    @Test
    public void testDeclareIntVariable() throws InterruptedException {
        String declareQuery = "DECLARE myIntVar INT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);

        CountDownLatch latch = new CountDownLatch(1);

        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check if the variable is declared
                assertTrue(context.hasVariable("myIntVar"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 2: Declare a FLOAT variable
    @Test
    public void testDeclareFloatVariable() throws InterruptedException {
        String declareQuery = "DECLARE myFloatVar FLOAT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);

        CountDownLatch latch = new CountDownLatch(1);

        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check if the variable is declared
                assertTrue(context.hasVariable("myFloatVar"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 3: Declare a STRING variable
    @Test
    public void testDeclareStringVariable() throws InterruptedException {
        String declareQuery = "DECLARE myStringVar STRING;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);

        CountDownLatch latch = new CountDownLatch(1);

        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check if the variable is declared
                assertTrue(context.hasVariable("myStringVar"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 4: Declare multiple variables
    @Test
    public void testDeclareMultipleVariables() throws InterruptedException {
        String declareQuery = "DECLARE var1 INT, var2 FLOAT, var3 STRING;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);

        CountDownLatch latch = new CountDownLatch(1);

        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue(context.hasVariable("var1"));
                assertTrue(context.hasVariable("var2"));
                assertTrue(context.hasVariable("var3"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 5: Declare a variable with an unsupported type (Expecting Parser Error)
    @Test
    public void testDeclareUnsupportedType() {
        String declareQuery = "DECLARE myVar UNSUPPORTED_TYPE;";

        try {
            PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
            fail("Expected a syntax error due to unsupported data type");
        } catch (RuntimeException e) {
            // Verify that the exception message indicates a syntax error
            assertTrue("Exception message should indicate a syntax error.",
                e.getMessage().contains("Syntax error"));
        }
    }

    // Test 6: Declare a variable that's already declared
    @Test
    public void testDeclareExistingVariable() throws InterruptedException {
        String declareQuery = "DECLARE myVar INT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);

        CountDownLatch latch = new CountDownLatch(2);

        // First declaration
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Second declaration
                declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        fail("Expected an exception due to variable already declared");
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        try {
                            assertTrue("Exception message should indicate variable already declared.",
                                e.getMessage().contains("already declared"));
                            latch.countDown();
                        } catch (AssertionError ex) {
                            latch.countDown();
                            throw ex; // Re-throw to fail the test
                        }
                    }
                });
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("First declaration failed: " + e.getMessage());
                latch.countDown();
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 7: Declare a variable with an initial value (if supported)
    @Test
    public void testDeclareVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myVar INT = 10;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);

        CountDownLatch latch = new CountDownLatch(1);

        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check if the variable is declared and initialized
                assertTrue(context.hasVariable("myVar"));
                assertEquals(10, context.getVariable("myVar"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 8: Declare a variable with invalid syntax
    @Test
    public void testDeclareVariableInvalidSyntax() throws InterruptedException {
        String declareQuery = "DECLARE INT myVar;";  // Invalid syntax
        try {
            PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
            fail("Expected a syntax error due to unsupported data type");
        } catch (RuntimeException e) {
            // Verify that the exception message indicates a syntax error
            assertTrue("Exception message should indicate a syntax error.",
                e.getMessage().contains("Syntax error"));
        }
    }
}
