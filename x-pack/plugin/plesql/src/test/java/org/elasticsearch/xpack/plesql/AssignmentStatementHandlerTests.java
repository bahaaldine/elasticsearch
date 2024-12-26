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
import org.elasticsearch.xpack.plesql.handlers.AssignmentStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Test;

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
        executor = new ProcedureExecutor(context, threadPool);
        assignmentHandler = new AssignmentStatementHandler(executor);
        declareHandler = new DeclareStatementHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a query and return the necessary context
    private PlEsqlProcedureParser.Assignment_statementContext parseAssignment(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.assignment_statement();
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

    // Test 1: Declare a variable and assign a simple integer value
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
                        // Verify that 'myVar' is set to 42
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

    // Test 2: Declare a variable and assign a simple float value
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
                        // Verify that 'myVar' is set to 42.5
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

    // Test 3: Declare a variable and assign a simple string value
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
                        // Verify that 'myVar' is set to 'hello'
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

    // Test 4: Declare a variable and assign the result of an arithmetic operation
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
                        // Verify that 'myVar' is set to 8.0
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
                        // Verify that 'myVar' is set to 42.0
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

        // Declare var1
        declareHandler.handleAsync(parseDeclaration(declareQuery1), new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Declare var2
                declareHandler.handleAsync(parseDeclaration(declareQuery2), new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        // Assign value to var1
                        assignmentHandler.handleAsync(parseAssignment(assignQuery1), new ActionListener<Object>() {
                            @Override
                            public void onResponse(Object unused) {
                                // Assign value of var1 to var2
                                assignmentHandler.handleAsync(parseAssignment(assignQuery2), new ActionListener<Object>() {
                                    @Override
                                    public void onResponse(Object unused) {
                                        // Verify that var2 was assigned the value of var1
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

    // New Test: Type mismatch error (assigning a string to a NUMBER variable)
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
                        assertTrue("Exception message should indicate type mismatch.",
                            e.getMessage().contains("Type mismatch"));
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

    // Test 9: Assigning result of division
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
                        // Verify that 'myVar' is set to 7.0
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
}
