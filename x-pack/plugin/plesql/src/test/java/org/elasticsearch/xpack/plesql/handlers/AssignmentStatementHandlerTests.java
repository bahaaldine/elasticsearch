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
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AssignmentStatementHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;
    private AssignmentStatementHandler assignmentHandler;
    private DeclareStatementHandler declareHandler;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // use a mock client if needed
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        assignmentHandler = new AssignmentStatementHandler(executor);
        declareHandler = new DeclareStatementHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse an assignment statement
    private PlEsqlProcedureParser.Assignment_statementContext parseAssignment(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new PlEsqlErrorListener());
        return parser.assignment_statement();
    }

    // Helper method to parse a declaration statement
    private PlEsqlProcedureParser.Declare_statementContext parseDeclaration(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new PlEsqlErrorListener());
        return parser.declare_statement();
    }

    // Test 1: Declare a NUMBER variable and assign an integer
    @Test
    public void testDeclareAndAssignInteger() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER;";
        String assignQuery = "SET myVar = 42;";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        assertEquals(42.0, context.getVariable("myVar"));
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 2: Declare a NUMBER variable and assign a float value
    @Test
    public void testDeclareAndAssignFloat() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER;";
        String assignQuery = "SET myVar = 42.5;";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        assertEquals(42.5, context.getVariable("myVar"));
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 3: Declare a STRING variable and assign a string value
    @Test
    public void testDeclareAndAssignString() throws InterruptedException {
        String declareQuery = "DECLARE myVar STRING;";
        String assignQuery = "SET myVar = 'hello';";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        assertEquals("hello", context.getVariable("myVar"));
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 4: Declare a NUMBER variable and assign the result of an arithmetic operation
    @Test
    public void testDeclareAndAssignArithmeticOperation() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER;";
        String assignQuery = "SET myVar = 5 + 3;";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        assertEquals(8.0, context.getVariable("myVar"));
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 5: Declare a variable and assign the result of a multiplication operation
    @Test
    public void testDeclareAndAssignMultiplication() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER;";
        String assignQuery = "SET myVar = 6 * 7;";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        assertEquals(42.0, context.getVariable("myVar"));
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 6: Variable reference assignment (assigning one variable to another)
    @Test
    public void testVariableReferenceAssignment() throws InterruptedException {
        String declareQuery1 = "DECLARE var1 NUMBER;";
        String declareQuery2 = "DECLARE var2 NUMBER;";
        String assignQuery1 = "SET var1 = 10;";
        String assignQuery2 = "SET var2 = var1;";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery1), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                declareHandler.handleAsync(parseDeclaration(declareQuery2), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        assignmentHandler.handleAsync(parseAssignment(assignQuery1), new ActionListener<Object>() {
                            @Override
                            public void onResponse(Object unused) {
                                assignmentHandler.handleAsync(parseAssignment(assignQuery2), new ActionListener<Object>() {
                                    @Override
                                    public void onResponse(Object unused) {
                                        assertEquals(10.0, context.getVariable("var2"));
                                        latch.countDown();
                                    }
                                    @Override
                                    public void onFailure(Exception e) {
                                        fail("Assignment of var2 failed: " + e.getMessage());
                                        latch.countDown();
                                    }
                                });
                            }
                            @Override
                            public void onFailure(Exception e) {
                                fail("Assignment of var1 failed: " + e.getMessage());
                                latch.countDown();
                            }
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Declaration of var2 failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration of var1 failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 7: Type mismatch error for NUMBER assignment (assigning a string to a NUMBER variable)
    @Test
    public void testNumberAssignmentTypeMismatch() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER;";
        String assignQuery = "SET myVar = 'NotANumber';";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        fail("Expected a RuntimeException due to type mismatch.");
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        assertTrue(e.getMessage().contains("Type mismatch"));
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 8: Declare a NUMBER variable and assign the result of a division operation
    @Test
    public void testDeclareAndAssignDivision() throws InterruptedException {
        String declareQuery = "DECLARE myVar NUMBER;";
        String assignQuery = "SET myVar = 42 / 6;";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        assertEquals(7.0, context.getVariable("myVar"));
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // NEW Test 9: Declare an ARRAY variable (without initial value)
    @Test
    public void testDeclareArrayVariable() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF STRING;";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertTrue("myArray should be declared.", context.hasVariable("myArray"));
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

    // NEW Test 10: Declare an ARRAY variable with an initial value via SET (using a JSON string literal)
    @Test
    public void testDeclareAndAssignArrayViaSet() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF NUMBER;";
        // Use a quoted JSON array so that the evaluator returns a String
        String assignQuery = "SET myArray = [100,200,300];";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        Object value = context.getVariable("myArray");
                        assertTrue("myArray should be a List.", value instanceof List);
                        List<?> list = (List<?>) value;
                        assertEquals(3, list.size());
                        assertEquals(100.0, ((Number) list.get(0)).doubleValue(), 0.001);
                        assertEquals(200.0, ((Number) list.get(1)).doubleValue(), 0.001);
                        assertEquals(300.0, ((Number) list.get(2)).doubleValue(), 0.001);
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Array assignment via SET failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // NEW Test 11: Type mismatch error for ARRAY assignment (assigning a non-array value)
    @Test
    public void testArrayAssignmentTypeMismatch() {
        // This is an invalid array assignment literal which should cause a syntax error.
        String assignQuery = "SET myArray = not an array;";
        try {
            // Attempt to parse the assignment.
            PlEsqlProcedureParser.Assignment_statementContext assignCtx = parseAssignment(assignQuery);
            fail("Expected a syntax error due to invalid array assignment literal.");
        } catch (RuntimeException e) {
            // Assert that the error message indicates a syntax error.
            assertTrue("Exception message should indicate a syntax error.",
                e.getMessage().contains("Syntax error"));
        }
    }

    // Test 12: Declare an ARRAY variable and assign an array of strings
    @Test
    public void testDeclareAndAssignArrayOfStrings() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF STRING;";
        String assignQuery = "SET myArray = [\"hello\", \"world\"];";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        Object value = context.getVariable("myArray");
                        assertTrue("myArray should be a List.", value instanceof List);
                        List<?> list = (List<?>) value;
                        assertEquals(2, list.size());
                        assertEquals("hello", list.get(0));
                        assertEquals("world", list.get(1));
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Array assignment via SET for strings failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 13: Declare an ARRAY variable and assign an empty array
    @Test
    public void testDeclareAndAssignEmptyArray() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF NUMBER;";
        String assignQuery = "SET myArray = [];";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        Object value = context.getVariable("myArray");
                        assertTrue("myArray should be a List.", value instanceof List);
                        List<?> list = (List<?>) value;
                        assertEquals(0, list.size());
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Empty array assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

/*    // Test 14: Declare an ARRAY variable and assign a nested array
    @Test
    public void testDeclareAndAssignNestedArray() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF ARRAY OF NUMBER;";
        String assignQuery = "SET myArray = [[1,2],[3,4]];";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        Object value = context.getVariable("myArray");
                        assertTrue("myArray should be a List.", value instanceof List);
                        List<?> list = (List<?>) value;
                        assertEquals(2, list.size());
                        for (Object item : list) {
                            assertTrue("Each item should be a List", item instanceof List);
                        }
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Nested array assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }*/

    // Test 15: Declare an ARRAY variable and assign a mixed array
    @Test
    public void testDeclareAndAssignMixedArray() throws InterruptedException {
        String declareQuery = "DECLARE myArray ARRAY OF DOCUMENT;";
        String assignQuery = "SET myArray = [42, 'text', [1, 2]];";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        Object value = context.getVariable("myArray");
                        assertTrue("myArray should be a List.", value instanceof List);
                        List<?> list = (List<?>) value;
                        assertEquals(3, list.size());
                        assertEquals(42.0, ((Number) list.get(0)).doubleValue(), 0.001);
                        assertEquals("text", list.get(1));
                        assertTrue("Third element should be a List.", list.get(2) instanceof List);
                        List<?> nested = (List<?>) list.get(2);
                        assertEquals(2, nested.size());
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Mixed array assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Array declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }
    // NEW Test 16: Declare a DOCUMENT variable and assign a document literal
    @Test
    public void testDeclareAndAssignDocumentLiteral() throws InterruptedException {
        String declareQuery = "DECLARE myDoc DOCUMENT;";
        String assignQuery = "SET myDoc = {\"title\":\"My Test\",\"rating\":5};";
        CountDownLatch latch = new CountDownLatch(1);
        declareHandler.handleAsync(parseDeclaration(declareQuery), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assignmentHandler.handleAsync(parseAssignment(assignQuery), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        Object value = context.getVariable("myDoc");
                        assertNotNull("myDoc should not be null", value);
                        assertTrue("myDoc should be a Map", value instanceof java.util.Map);
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
                        assertEquals("My Test", map.get("title"));
                        assertEquals(5.0, ((Number) map.get("rating")).doubleValue(), 0.001);
                        latch.countDown();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        fail("Document literal assignment failed: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                fail("Document declaration failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }
}
