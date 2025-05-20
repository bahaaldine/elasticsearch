/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.escript.executors.ElasticScriptExecutor;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.handlers.CallProcedureStatementHandler;
import org.elasticsearch.xpack.escript.handlers.ElasticScriptErrorListener;
import org.elasticsearch.xpack.escript.visitors.ProcedureDefinitionVisitor;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DateBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.StringBuiltInFunctions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ElasticScriptExecutorProcedureTests {

    private ThreadPool threadPool;
    private ElasticScriptExecutor executor;

    @Before
    public void setUp() throws Exception {
        threadPool = new TestThreadPool("test-thread-pool");
        // For testing, we pass null for the client.
        executor = new ElasticScriptExecutor(threadPool, null);
    }

    @After
    public void tearDown() throws Exception {
        threadPool.shutdown();
    }

    // Helper method to parse a procedure definition (entire input is a procedure definition).
    private ElasticScriptParser.ProcedureContext parseProcedureDefinition(String procDefText) {
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(procDefText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ElasticScriptErrorListener());
        return parser.procedure();
    }

    // Helper method to create a fresh global ExecutionContext with built-in functions registered.
    private ExecutionContext newGlobalContext() {
        ExecutionContext context = new ExecutionContext();
        StringBuiltInFunctions.registerAll(context);
        NumberBuiltInFunctions.registerAll(context);
        ArrayBuiltInFunctions.registerAll(context);
        DateBuiltInFunctions.registerAll(context);
        return context;
    }

    // =========================
    // Test 1: Mixed Parameter Procedure
    // =========================
    @Test
    public void testProcedureCallUpdatesGlobalContext() throws InterruptedException {
        // Procedure with IN a, OUT b, INOUT c.
        String procDefText = ""
            + "PROCEDURE update_values(IN a NUMBER, OUT b NUMBER, INOUT c NUMBER) "
            + "BEGIN "
            + "   SET b = a * 2; "      // OUT b becomes 5 * 2 = 10.
            + "   SET c = c + a; "        // INOUT c becomes 10 + 5 = 15.
            + "END PROCEDURE;";

        ExecutionContext globalContext = newGlobalContext();
        ElasticScriptParser.ProcedureContext defCtx = parseProcedureDefinition(procDefText);
        // Register the procedure.
        ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(globalContext);
        procDefVisitor.visit(defCtx);
        assertNotNull(globalContext.getFunction("update_values"));

        // Set up global variables for OUT and INOUT.
        globalContext.declareVariable("b", "NUMBER");
        globalContext.declareVariable("c", "NUMBER");
        globalContext.setVariable("c", 10);

        ProcedureExecutor procExecutor = new ProcedureExecutor(globalContext, threadPool,
            null, new CommonTokenStream(new ElasticScriptLexer(CharStreams.fromString(procDefText))));
        CallProcedureStatementHandler procCallHandler = new CallProcedureStatementHandler(procExecutor);

        String callText = "CALL_PROCEDURE update_values(5.0, null, 10.0)";
        ElasticScriptLexer callLexer = new ElasticScriptLexer(CharStreams.fromString(callText));
        CommonTokenStream callTokens = new CommonTokenStream(callLexer);
        ElasticScriptParser callParser = new ElasticScriptParser(callTokens);
        ElasticScriptParser.Call_procedure_statementContext callCtx = callParser.call_procedure_statement();

        CountDownLatch latch = new CountDownLatch(1);
        procCallHandler.handleAsync(callCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object v) {
                Object bVal = globalContext.getVariable("b");
                Object cVal = globalContext.getVariable("c");
                assertNotNull(bVal);
                assertNotNull(cVal);
                assertEquals(10.0, Double.parseDouble(bVal.toString()), 0.001);
                assertEquals(15.0, Double.parseDouble(cVal.toString()), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Procedure call failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // =========================
    // Test 2: Procedure with Only OUT Parameter
    // =========================
    @Test
    public void testProcedureCallWithOnlyOutParameter() throws InterruptedException {
        // Procedure with one OUT parameter.
        String procDefText = ""
            + "PROCEDURE set_constant(OUT x NUMBER) "
            + "BEGIN "
            + "   SET x = 42; "
            + "END PROCEDURE;";

        ExecutionContext globalContext = newGlobalContext();
        ElasticScriptParser.ProcedureContext defCtx = parseProcedureDefinition(procDefText);
        ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(globalContext);
        procDefVisitor.visit(defCtx);
        assertNotNull(globalContext.getFunction("set_constant"));

        // Declare global variable for OUT parameter.
        globalContext.declareVariable("x", "NUMBER");

        // Create executor and call handler.
        ProcedureExecutor procExecutor = new ProcedureExecutor(globalContext, threadPool, null,
            new CommonTokenStream(new ElasticScriptLexer(CharStreams.fromString(procDefText))));
        CallProcedureStatementHandler procCallHandler = new CallProcedureStatementHandler(procExecutor);

        // For OUT parameter, pass null.
        String callText = "CALL_PROCEDURE set_constant(null)";
        ElasticScriptLexer callLexer = new ElasticScriptLexer(CharStreams.fromString(callText));
        CommonTokenStream callTokens = new CommonTokenStream(callLexer);
        ElasticScriptParser callParser = new ElasticScriptParser(callTokens);
        ElasticScriptParser.Call_procedure_statementContext callCtx = callParser.call_procedure_statement();
        CountDownLatch latch = new CountDownLatch(1);
        procCallHandler.handleAsync(callCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object v) {
                Object xVal = globalContext.getVariable("x");
                assertNotNull(xVal);
                assertEquals(42.0, Double.parseDouble(xVal.toString()), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Procedure call failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // =========================
    // Test 3: Procedure with Only INOUT Parameter
    // =========================
    @Test
    public void testProcedureCallWithOnlyInOutParameter() throws InterruptedException {
        // Procedure with one INOUT parameter.
        String procDefText = ""
            + "PROCEDURE increment(INOUT x NUMBER) "
            + "BEGIN "
            + "   SET x = x + 1; "
            + "END PROCEDURE;";

        ExecutionContext globalContext = newGlobalContext();
        ElasticScriptParser.ProcedureContext defCtx = parseProcedureDefinition(procDefText);
        ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(globalContext);
        procDefVisitor.visit(defCtx);
        assertNotNull(globalContext.getFunction("increment"));

        // Declare and initialize the INOUT variable.
        globalContext.declareVariable("x", "NUMBER");
        globalContext.setVariable("x", 5);

        // Create executor and call handler.
        ProcedureExecutor procExecutor = new ProcedureExecutor(globalContext, threadPool, null,
            new CommonTokenStream(new ElasticScriptLexer(CharStreams.fromString(procDefText))));
        CallProcedureStatementHandler procCallHandler = new CallProcedureStatementHandler(procExecutor);

        // For INOUT, pass the initial value.
        String callText = "CALL_PROCEDURE increment(5.0)";
        ElasticScriptLexer callLexer = new ElasticScriptLexer(CharStreams.fromString(callText));
        CommonTokenStream callTokens = new CommonTokenStream(callLexer);
        ElasticScriptParser callParser = new ElasticScriptParser(callTokens);
        ElasticScriptParser.Call_procedure_statementContext callCtx = callParser.call_procedure_statement();
        CountDownLatch latch = new CountDownLatch(1);
        procCallHandler.handleAsync(callCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object v) {
                Object xVal = globalContext.getVariable("x");
                assertNotNull(xVal);
                assertEquals(6.0, Double.parseDouble(xVal.toString()), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Procedure call failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // =========================
    // Test 4: Procedure Call with Mismatched Argument Count
    // =========================
    @Test
    public void testProcedureCallMismatchedArgumentCount() throws InterruptedException {
        // Define a procedure expecting 2 parameters.
        String procDefText = ""
            + "PROCEDURE dummy_proc(IN a NUMBER, IN b NUMBER) "
            + "BEGIN "
            + "   SET a = a; " // no-op
            + "END PROCEDURE;";

        ExecutionContext globalContext = newGlobalContext();
        ElasticScriptParser.ProcedureContext defCtx = parseProcedureDefinition(procDefText);
        ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(globalContext);
        procDefVisitor.visit(defCtx);
        assertNotNull(globalContext.getFunction("dummy_proc"));

        ProcedureExecutor procExecutor = new ProcedureExecutor(globalContext, threadPool, null,
            new CommonTokenStream(new ElasticScriptLexer(CharStreams.fromString(procDefText))));
        CallProcedureStatementHandler procCallHandler = new CallProcedureStatementHandler(procExecutor);

        // Call with one argument instead of two.
        String callText = "CALL_PROCEDURE dummy_proc(5.0)";
        ElasticScriptLexer callLexer = new ElasticScriptLexer(CharStreams.fromString(callText));
        CommonTokenStream callTokens = new CommonTokenStream(callLexer);
        ElasticScriptParser callParser = new ElasticScriptParser(callTokens);
        ElasticScriptParser.Call_procedure_statementContext callCtx = callParser.call_procedure_statement();
        CountDownLatch latch = new CountDownLatch(1);
        procCallHandler.handleAsync(callCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object v) {
                fail("Expected a mismatched argument count error.");
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                // Expected error.
                latch.countDown();
            }
        });
        latch.await();
    }

    // =========================
    // Test 5: Procedure Call with Invalid Argument Type
    // =========================
    @Test
    public void testProcedureCallInvalidArgumentType() throws InterruptedException {
        // Define a procedure expecting a NUMBER.
        String procDefText = ""
            + "PROCEDURE type_test(IN a NUMBER) "
            + "BEGIN "
            + "   SET a = a; " // no-op
            + "END PROCEDURE;";

        ExecutionContext globalContext = newGlobalContext();
        ElasticScriptParser.ProcedureContext defCtx = parseProcedureDefinition(procDefText);
        ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(globalContext);
        procDefVisitor.visit(defCtx);
        assertNotNull(globalContext.getFunction("type_test"));

        ProcedureExecutor procExecutor = new ProcedureExecutor(globalContext, threadPool, null,
            new CommonTokenStream(new ElasticScriptLexer(CharStreams.fromString(procDefText))));
        CallProcedureStatementHandler procCallHandler = new CallProcedureStatementHandler(procExecutor);

        // Call with a string instead of a number.
        String callText = "CALL_PROCEDURE type_test('not a number')";
        ElasticScriptLexer callLexer = new ElasticScriptLexer(CharStreams.fromString(callText));
        CommonTokenStream callTokens = new CommonTokenStream(callLexer);
        ElasticScriptParser callParser = new ElasticScriptParser(callTokens);
        ElasticScriptParser.Call_procedure_statementContext callCtx = callParser.call_procedure_statement();
        CountDownLatch latch = new CountDownLatch(1);
        procCallHandler.handleAsync(callCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object v) {
                fail("Expected a type mismatch error.");
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                // Expected error.
                latch.countDown();
            }
        });
        latch.await();
    }

    // =========================
    // Test 6: Procedure Call Error Propagation
    // =========================
    @Test
    public void testProcedureCallErrorPropagation() throws InterruptedException {
        // Define a procedure that updates an INOUT parameter then throws an error.
        String procDefText = ""
            + "PROCEDURE error_proc(INOUT x NUMBER) "
            + "BEGIN "
            + "   SET x = x + 5; "   // should update x (e.g., from 10 to 15)
            + "   THROW 'Forced error'; "  // simulate an error
            + "END PROCEDURE;";

        ExecutionContext globalContext = newGlobalContext();
        ElasticScriptParser.ProcedureContext defCtx = parseProcedureDefinition(procDefText);
        ProcedureDefinitionVisitor procDefVisitor = new ProcedureDefinitionVisitor(globalContext);
        procDefVisitor.visit(defCtx);
        assertNotNull(globalContext.getFunction("error_proc"));

        // Declare and initialize the INOUT variable.
        globalContext.declareVariable("x", "NUMBER");
        globalContext.setVariable("x", 10);

        ProcedureExecutor procExecutor = new ProcedureExecutor(globalContext, threadPool, null,
            new CommonTokenStream(new ElasticScriptLexer(CharStreams.fromString(procDefText))));
        CallProcedureStatementHandler procCallHandler = new CallProcedureStatementHandler(procExecutor);

        String callText = "CALL_PROCEDURE error_proc(10.0)";
        ElasticScriptLexer callLexer = new ElasticScriptLexer(CharStreams.fromString(callText));
        CommonTokenStream callTokens = new CommonTokenStream(callLexer);
        ElasticScriptParser callParser = new ElasticScriptParser(callTokens);
        ElasticScriptParser.Call_procedure_statementContext callCtx = callParser.call_procedure_statement();
        CountDownLatch latch = new CountDownLatch(1);
        procCallHandler.handleAsync(callCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object v) {
                fail("Expected an error to be thrown.");
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                // Even if there is an error, the updated value for x should be propagated.
                Object xVal = globalContext.getVariable("x");
                assertNotNull(xVal);
                double xNum = Double.parseDouble(xVal.toString());
                assertEquals(15.0, xNum, 0.001);
                latch.countDown();
            }
        });
        latch.await();
    }
}
