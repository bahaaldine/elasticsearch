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
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.junit.Test;

import java.util.List;
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
        Client mockClient = null; // use a mock client if needed
        // Provide an empty source for the lexer; tokens will be re-parsed in helper methods
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        declareHandler = new DeclareStatementHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a declaration query
    private PlEsqlProcedureParser.Declare_statementContext parseDeclaration(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add custom error listener
        return parser.declare_statement();
    }

    // Existing Tests:

    // Test 1: Declare a NUMBER variable (used for int or float)
    @Test
    public void testDeclareIntVariable() throws InterruptedException {
        String declareQuery = "DECLARE myIntVar NUMBER;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
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

    // Test 2: Declare a STRING variable
    @Test
    public void testDeclareStringVariable() throws InterruptedException {
        String declareQuery = "DECLARE myStringVar STRING;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
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

    // Test 3: Declare multiple variables
    @Test
    public void testDeclareMultipleVariables() throws InterruptedException {
        String declareQuery = "DECLARE var1 NUMBER, var2 NUMBER, var3 STRING;";
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

    // Test 4: Declare a variable with an unsupported type (Expecting Parser Error)
    @Test
    public void testDeclareUnsupportedType() {
        String declareQuery = "DECLARE myVar UNSUPPORTED_TYPE;";
        try {
            PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
            fail("Expected a syntax error due to unsupported data type");
        } catch (RuntimeException e) {
            assertTrue("Exception message should indicate a syntax error.", e.getMessage().contains("Syntax error"));
        }
    }

    // Test 5: Declare a variable that's already declared
    @Test
    public void testDeclareExistingVariable() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(2);
        // First declaration
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Second declaration (should fail)
                declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        fail("Expected an exception due to variable already declared");
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        assertTrue("Exception message should indicate variable already declared.",
                            e.getMessage().contains("already declared"));
                        latch.countDown();
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

    // Test 6: Declare a variable with an initial value
    @Test
    public void testDeclareVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER = 10;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue(context.hasVariable("myVar"));
                assertEquals(10.0, ((Number) context.getVariable("myVar")).doubleValue(), 0.001);
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

    // Test 7: Declare a variable with invalid syntax
    @Test
    public void testDeclareVariableInvalidSyntax() {
        String declareQuery = "DECLARE NUMBER myVar;";  // Invalid syntax
        try {
            PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
            fail("Expected a syntax error due to invalid declaration syntax");
        } catch (RuntimeException e) {
            assertTrue("Exception message should indicate a syntax error.",
                e.getMessage().contains("Syntax error"));
        }
    }

    // New Array Tests:

    // Test 8: Declare an ARRAY variable of STRING without an initial value.
    @Test
    public void testDeclareArrayOfStringVariableWithoutInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF STRING;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
                // Expect the value to be null (or an empty list, depending on your design)
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 9: Declare an ARRAY variable of NUMBER with an initial value.
    @Test
    public void testDeclareArrayOfNumberVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF NUMBER = [10, 20, 30];";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
                Object value = context.getVariable("myArray");
                assertTrue("myArray should be a List.", value instanceof List);
                List<?> list = (List<?>) value;
                assertEquals(3, list.size());
                assertEquals(10.0, ((Number) list.get(0)).doubleValue(), 0.001);
                assertEquals(20.0, ((Number) list.get(1)).doubleValue(), 0.001);
                assertEquals(30.0, ((Number) list.get(2)).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration with initial value failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 10: Declare an ARRAY variable of STRING with an initial value.
    @Test
    public void testDeclareArrayOfStringVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF STRING = [\"a\", \"b\", \"c\"];";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
                Object value = context.getVariable("myArray");
                assertTrue("myArray should be a List.", value instanceof List);
                List<?> list = (List<?>) value;
                assertEquals(3, list.size());
                assertEquals("a", list.get(0));
                assertEquals("b", list.get(1));
                assertEquals("c", list.get(2));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration with initial value failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 11: Declare an ARRAY variable of DATE with an initial value.
    // (Assuming your DATE type is represented as a string literal in ISO format)
    @Test
    public void testDeclareArrayOfDateVariableWithInitialValue() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF DATE = [\"2020-01-01\", \"2020-12-31\"];";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
                Object value = context.getVariable("myArray");
                assertTrue("myArray should be a List.", value instanceof List);
                List<?> list = (List<?>) value;
                assertEquals(2, list.size());
                // Adjust these assertions depending on how DATE values are processed in your engine.
                assertEquals("2020-01-01", list.get(0).toString());
                assertEquals("2020-12-31", list.get(1).toString());
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration with initial date value failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 12: Declare an ARRAY variable with an unsupported element type.
    @Test
    public void testDeclareArrayVariableWithUnsupportedElementType() {
        // For example, "ARRAY OF UNSUPPORTED" should trigger an error.
        String declareQuery = "DECLARE myArray ARRAY OF UNSUPPORTED;";
        try {
            PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
            declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
                @Override
                public void onResponse(Object unused) {
                    fail("Expected an error for unsupported element type.");
                }
                @Override
                public void onFailure(Exception e) {
                    // Expected error message may vary; adjust as needed.
                    assertTrue(e.getMessage().contains("Unsupported data type"));
                }
            });
            fail("Expected a runtime error due to unsupported element type.");
        } catch (RuntimeException e) {
            // Expected to catch a syntax error here.
            assertTrue(e.getMessage().contains("Syntax error"));
        }
    }

    // Test 13: Declare a DOCUMENT variable with an empty initializer
    @Test
    public void testDeclareDocumentVariableWithEmptyInit() throws InterruptedException {
        String declareQuery = "DECLARE myVar DOCUMENT = {};";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myVar should be declared", context.hasVariable("myVar"));
                Object value = context.getVariable("myVar");
                assertNotNull("myVar value should not be null", value);
                assertTrue("myVar should be a Map", value instanceof java.util.Map);
                java.util.Map<?,?> map = (java.util.Map<?,?>) value;
                assertTrue("myVar map should be empty", map.isEmpty());
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Document declaration with empty initializer failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }
}
