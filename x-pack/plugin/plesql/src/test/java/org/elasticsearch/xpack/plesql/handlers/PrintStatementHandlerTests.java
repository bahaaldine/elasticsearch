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
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.plesql.utils.TestListAppender;
import org.elasticsearch.xpack.plesql.utils.TestUtils;
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
        // *** REGISTER BUILT-IN FUNCTIONS ***
        // Register string built-in functions so that UPPER, LOWER, etc. are defined.
        StringBuiltInFunctions.registerAll(context);

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

    @Test
    public void testForArrayLoopPrintDocumentField() throws InterruptedException {
        // This procedure declares an array of 10 documents and prints the 'name' field from each.
        String blockQuery =
            "PROCEDURE printDocFieldTest(INOUT dummy NUMBER) " +
                "BEGIN " +
                "  DECLARE arr ARRAY OF DOCUMENT = " +
                "    [" +
                "      {\"name\": \"Alice\", \"age\": 30}, " +
                "      {\"name\": \"Bob\", \"age\": 25}, " +
                "      {\"name\": \"Charlie\", \"age\": 40}, " +
                "      {\"name\": \"David\", \"age\": 35}, " +
                "      {\"name\": \"Eve\", \"age\": 28}, " +
                "      {\"name\": \"Frank\", \"age\": 45}, " +
                "      {\"name\": \"Grace\", \"age\": 32}, " +
                "      {\"name\": \"Heidi\", \"age\": 29}, " +
                "      {\"name\": \"Ivan\", \"age\": 38}, " +
                "      {\"name\": \"Judy\", \"age\": 26} " +
                "    ]; " +
                "  FOR doc IN arr LOOP " +
                "    PRINT doc['name'], INFO; " +
                "  END LOOP " +
                "END PROCEDURE";

        // Parse the procedure block.
        PlEsqlProcedureParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);

        // Execute the procedure block asynchronously.
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();

        // After execution, check that the PRINT statements were processed correctly.
        // The TestListAppender (configured in your test setup) should capture log messages for each document's "name".
        List<String> messages = testListAppender.getMessages();
        String[] expectedNames = {"Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy"};
        for (String name : expectedNames) {
            boolean found = messages.stream().anyMatch(msg -> msg.contains("[PRINT]") && msg.contains(name));
            assertTrue("Expected log message to contain '" + name + "'. Captured messages: " + messages, found);
        }
    }

    @Test
    public void testLargeDatasetProcessingWithBuiltInFunctions() throws InterruptedException {
        // This procedure declares an array of 10 document records and uses built-in functions
        // to process and print the results. In this example:
        // - For each document, it prints:
        //      UPPER(TRIM(doc['name'])) || ": " || LENGTH(TRIM(doc['value']))
        // For example, if the first document is {"name": " John Doe ", "value": " data1 "},
        // then UPPER(TRIM(" John Doe ")) gives "JOHN DOE" and LENGTH(TRIM(" data1 ")) gives 5,
        // so the printed output should be: "[PRINT] JOHN DOE: 5"

        String blockQuery =
            "PROCEDURE processLargeDataset() " +
                "BEGIN " +
                "  DECLARE arr ARRAY OF DOCUMENT = [ " +
                "    {\"name\": \" John Doe \", \"value\": \" data1 \"}, " +
                "    {\"name\": \" Alice \", \"value\": \" data2 \"}, " +
                "    {\"name\": \" Bob \", \"value\": \" data3 \"}, " +
                "    {\"name\": \" Charlie \", \"value\": \" data4 \"}, " +
                "    {\"name\": \" David \", \"value\": \" data5 \"}, " +
                "    {\"name\": \" Eve \", \"value\": \" data6 \"}, " +
                "    {\"name\": \" Frank \", \"value\": \" data7 \"}, " +
                "    {\"name\": \" Grace \", \"value\": \" data8 \"}, " +
                "    {\"name\": \" Heidi \", \"value\": \" data9 \"}, " +
                "    {\"name\": \" Ivan \", \"value\": \" data10 \"} " +
                "  ]; " +
                "  FOR doc IN arr LOOP " +
                "    PRINT UPPER(TRIM(doc['name'])) || ': ' || LENGTH(TRIM(doc['value'])), INFO; " +
                "  END LOOP; " +
                "  RETURN 1; " +
                "END PROCEDURE";

        // Use your existing TestUtils.parseBlock() method (or similar) to parse the procedure.
        PlEsqlProcedureParser.ProcedureContext blockContext = TestUtils.parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Procedure execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();

        // Verify that the log output (captured by TestListAppender) contains the expected messages.
        // We expect for each record a message in the form "[PRINT] <NAME>: <LENGTH>".
        // For our first document, after trimming, we expect:
        //   UPPER(" John Doe ") → "JOHN DOE"
        //   TRIM(" data1 ") → "data1" → LENGTH is 5
        List<String> messages = testListAppender.getMessages();

        // Define expected outputs for each document.
        // Adjust the expected numbers if your implementation of TRIM and LENGTH differs.
        String[] expectedOutputs = new String[] {
            "JOHN DOE: 5",
            "ALICE: 5",
            "BOB: 5",
            "CHARLIE: 5",
            "DAVID: 5",
            "EVE: 5",
            "FRANK: 5",
            "GRACE: 5",
            "HEIDI: 5",
            "IVAN: 6"  // Example: " data10 " trimmed → "data10" has 6 characters.
        };

        for (String expected : expectedOutputs) {
            boolean found = messages.stream().anyMatch(msg -> msg.contains("[PRINT]") && msg.contains(expected));
            assertTrue("Expected log message containing '" + expected + "' not found. Captured messages: " + messages, found);
        }
    }
}
