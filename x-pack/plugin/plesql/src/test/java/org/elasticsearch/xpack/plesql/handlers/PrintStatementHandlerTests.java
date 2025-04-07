/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
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
import org.elasticsearch.xpack.plesql.utils.TestListAppender;
import org.junit.Test;

public class PrintStatementHandlerTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;
    private PrintStatementHandler printHandler;
    private TestListAppender testListAppender;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Set up a TestListAppender to capture log messages from PrintStatementHandler.
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration config = loggerContext.getConfiguration();
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%m").build();
        testListAppender = new TestListAppender("TestListAppender", null, layout);
        testListAppender.start();
        config.addAppender(testListAppender);
        // Attach the TestListAppender to the logger for PrintStatementHandler.
        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(PrintStatementHandler.class);
        logger.addAppender(testListAppender);
        logger.setAdditive(false);

        // Initialize the ExecutionContext and thread pool.
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // Use a mock if needed.
        // Create a dummy token stream from an empty source for executor initialization.
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        // Initialize the PrintStatementHandler (assuming its constructor takes a ProcedureExecutor).
        printHandler = new PrintStatementHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        // Remove the TestListAppender from the logger using the appender instance.
        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(PrintStatementHandler.class);
        logger.removeAppender(testListAppender);
        testListAppender.stop();
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testPrintStatementWithoutSeverity() throws InterruptedException {
        // Input without severity: PRINT 'Hello world';
        String input = "PRINT 'Hello world';";
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        PlEsqlProcedureParser.Print_statementContext printCtx = parser.print_statement();

        CountDownLatch latch = new CountDownLatch(1);
        printHandler.execute(printCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Print statement execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();

        List<String> messages = testListAppender.getMessages();
        boolean found = messages.stream().anyMatch(msg -> msg.contains("[PRINT]") && msg.contains("Hello world"));
        assertTrue("Expected log message not found. Captured messages: " + messages, found);
    }

    @Test
    public void testPrintStatementWithSeverity() throws InterruptedException {
        // Input with severity: PRINT 'Warning occurred', WARN;
        String input = "PRINT 'Warning occurred', WARN;";
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        PlEsqlProcedureParser.Print_statementContext printCtx = parser.print_statement();

        CountDownLatch latch = new CountDownLatch(1);
        printHandler.execute(printCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Print statement execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();

        List<String> messages = testListAppender.getMessages();
        boolean found = messages.stream().anyMatch(msg -> msg.contains("[PRINT]") && msg.contains("Warning occurred"));
        assertTrue("Expected warning log message not found. Captured messages: " + messages, found);
    }

    @Test
    public void testPrintStatementWithVariable() throws InterruptedException {
        // Set a variable in the global context.
        context.declareVariable("x", "STRING");
        context.setVariable("x", "variableValue");

        // Input: PRINT x;
        String input = "PRINT x;";
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        PlEsqlProcedureParser.Print_statementContext printCtx = parser.print_statement();

        CountDownLatch latch = new CountDownLatch(1);
        printHandler.execute(printCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Print statement execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();

        List<String> messages = testListAppender.getMessages();
        boolean found = messages.stream().anyMatch(msg -> msg.contains("[PRINT]") && msg.contains("variableValue"));
        assertTrue("Expected printed variable value not found. Captured messages: " + messages, found);
    }

    @Test
    public void testPrintStatementWithConcatenation() throws InterruptedException {
        // Set a variable in the global context.
        context.declareVariable("x", "STRING");
        context.setVariable("x", "variableValue");

        // Input: PRINT 'The value is: ' || x;
        String input = "PRINT 'The value is: ' || x;";
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        PlEsqlProcedureParser.Print_statementContext printCtx = parser.print_statement();

        CountDownLatch latch = new CountDownLatch(1);
        printHandler.execute(printCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Print statement execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();

        // Verify that the log output contains the concatenated result.
        // Expected: "[PRINT] The value is: variableValue"
        List<String> messages = testListAppender.getMessages();
        boolean found = messages.stream().anyMatch(
            msg -> msg.contains("[PRINT]") && msg.contains("The value is: ") && msg.contains("variableValue"));
        assertTrue("Expected concatenated log message not found. Captured messages: " + messages, found);
    }
}
