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
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.handlers.AssignmentStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.DeclareStatementHandler;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AssignmentStatementHandlerTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private AssignmentStatementHandler assignmentHandler;
    private DeclareStatementHandler declareHandler;
    private ThreadPool threadPool;

    @Before
    public void setup() {
        context = new ExecutionContext();
        // Use TestThreadPool for unit tests
        threadPool = new TestThreadPool("test-thread-pool");
        executor = new ProcedureExecutor(context, threadPool);
        assignmentHandler = new AssignmentStatementHandler(executor);
        declareHandler = new DeclareStatementHandler(executor);
    }

    @After
    public void tearDown() throws InterruptedException {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
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
        String declareQuery = "DECLARE myVar INT;";
        PlEsqlProcedureParser.Declare_statementContext declareContext = parseDeclaration(declareQuery);

        CountDownLatch latch = new CountDownLatch(1);

        declareHandler.handleAsync(declareContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                String assignQuery = "SET myVar = 42;";
                PlEsqlProcedureParser.Assignment_statementContext assignContext = parseAssignment(assignQuery);

                assignmentHandler.handleAsync(assignContext, new ActionListener<Object>() {
                    @Override
                    public void onResponse(Object unused) {
                        assertEquals(42, context.getVariable("myVar"));
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

    // ... (other test methods remain the same, except for possible minor adjustments)

    // Remember to update the other test methods similarly, ensuring they use the CountDownLatch and handle exceptions appropriately
}
