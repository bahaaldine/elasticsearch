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
import org.elasticsearch.xpack.plesql.primitives.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.elasticsearch.xpack.plesql.primitives.functions.Parameter;
import org.elasticsearch.xpack.plesql.primitives.functions.ParameterMode;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
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
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString("")); // empty source
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        handler = new FunctionDefinitionHandler(executor);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    // Helper method to parse a function definition.
    private PlEsqlProcedureParser.Function_definitionContext parseFunction(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new PlEsqlErrorListener());
        return parser.function_definition();
    }

    // Helper method to parse a procedure block.
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new PlEsqlErrorListener());
        return parser.procedure();
    }

    // Test 1: Function with no parameters.
    @Test
    public void testFunctionDefinitionWithoutParameters() throws InterruptedException {
        String functionQuery = "FUNCTION myFunction() BEGIN RETURN 10; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext funcCtx = parseFunction(functionQuery);

        CountDownLatch latch = new CountDownLatch(1);
        handler.handleAsync(funcCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                FunctionDefinition func = context.getFunction("myFunction");
                assertNotNull(func);
                assertEquals("myFunction", func.getName());
                assertEquals(0, func.getParameters().size());
                assertNotNull(func.getBody());
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

    // Test 2: Function with one parameter using explicit IN notation.
    @Test
    public void testFunctionDefinitionWithExplicitInParameter() throws InterruptedException {
        String functionQuery = "FUNCTION add(IN a NUMBER) BEGIN RETURN a + 5; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext funcCtx = parseFunction(functionQuery);

        CountDownLatch latch = new CountDownLatch(1);
        handler.handleAsync(funcCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                FunctionDefinition func = context.getFunction("add");
                assertNotNull(func);
                assertEquals("add", func.getName());
                assertEquals(1, func.getParameters().size());
                Parameter p = func.getParameters().get(0);
                assertEquals("a", p.getName());
                assertEquals("NUMBER", p.getType());
                assertEquals(ParameterMode.IN, p.getMode());
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

    // Test 3: Function with multiple parameters using explicit modes (IN, OUT, INOUT).
    @Test
    public void testFunctionDefinitionWithMixedExplicitParameterModes() throws InterruptedException {
        String functionQuery = "FUNCTION combine(IN a NUMBER, OUT b NUMBER, INOUT c NUMBER) " +
            "BEGIN RETURN a + c; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext funcCtx = parseFunction(functionQuery);

        CountDownLatch latch = new CountDownLatch(1);
        handler.handleAsync(funcCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                FunctionDefinition func = context.getFunction("combine");
                assertNotNull(func);
                assertEquals("combine", func.getName());
                List<Parameter> params = func.getParameters();
                assertEquals(3, params.size());

                assertEquals("a", params.get(0).getName());
                assertEquals("NUMBER", params.get(0).getType());
                assertEquals(ParameterMode.IN, params.get(0).getMode());

                assertEquals("b", params.get(1).getName());
                assertEquals("NUMBER", params.get(1).getType());
                assertEquals(ParameterMode.OUT, params.get(1).getMode());

                assertEquals("c", params.get(2).getName());
                assertEquals("NUMBER", params.get(2).getType());
                assertEquals(ParameterMode.INOUT, params.get(2).getMode());
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

    // Test 4: Function defined and called within a procedure block.
    @Test
    public void testFunctionDefinitionAndCallWithinBlock() throws InterruptedException {
        String blockQuery = """
            PROCEDURE myProcedure(IN a NUMBER, OUT b NUMBER, INOUT c NUMBER)
            BEGIN
                DECLARE result NUMBER;
                FUNCTION add(IN a NUMBER, IN b NUMBER) BEGIN RETURN a + b; END FUNCTION;
                SET result = add(3, 4);
            END PROCEDURE
        """;
        PlEsqlProcedureParser.ProcedureContext blockCtx = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                FunctionDefinition func = context.getFunction("add");
                assertNotNull(func);
                Object resultValue = context.getVariable("result");
                assertNotNull("Variable 'result' should be declared.", resultValue);
                assertTrue("Variable 'result' should be a number.", resultValue instanceof Number);
                assertEquals(7.0, ((Number) resultValue).doubleValue(), 0.001);
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

    // Test 5: Function call within a loop.
    @Test
    public void testFunctionCallWithinLoop() throws InterruptedException {
        String blockQuery = """
        PROCEDURE myProcedure(IN a NUMBER, OUT b NUMBER, INOUT c NUMBER)
        BEGIN
            DECLARE total NUMBER, i NUMBER;
            FUNCTION add(IN a NUMBER, IN b NUMBER) BEGIN RETURN a + b; END FUNCTION;
            SET total = 0;
            FOR i IN 1..3 LOOP
                SET total = add(total, i);
            END LOOP;
        END PROCEDURE
        """;
        PlEsqlProcedureParser.ProcedureContext blockCtx = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                Object totalValue = context.getVariable("total");
                assertNotNull("Variable 'total' should be declared.", totalValue);
                assertTrue("Variable 'total' should be a number.", totalValue instanceof Number);
                assertEquals(6, ((Number) totalValue).intValue());
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

    // Test 6: Function with no return statement should throw an error.
    @Test
    public void testFunctionDefinitionWithoutReturnThrowsError() throws InterruptedException {
        String blockQuery = """
            PROCEDURE myProcedure(IN a NUMBER, OUT b NUMBER, INOUT c NUMBER)
            BEGIN
                DECLARE result INT;
                FUNCTION faultyFunction(IN a INT, IN b INT) BEGIN SET result = a + b; END FUNCTION;
                SET result = faultyFunction(1, 2);
            END PROCEDURE
        """;
        PlEsqlProcedureParser.ProcedureContext blockCtx = parseBlock(blockQuery);
        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(blockCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                fail("Expected an exception due to missing return statement in function.");
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                // Expected exception.
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 7: Function with an OUT parameter.
    @Test
    public void testFunctionWithOutParameterPropagation() throws InterruptedException {
        // Define function: FUNCTION testOut(IN a NUMBER, OUT b NUMBER) BEGIN RETURN a + 5; END FUNCTION;
        // In dummy execution, simulate updating OUT parameter 'b' to a * 2.
        List<Parameter> parameters = Arrays.asList(
            new Parameter("a", "NUMBER", ParameterMode.IN),
            new Parameter("b", "NUMBER", ParameterMode.OUT)
        );
        FunctionDefinition testOutFunc = new FunctionDefinition("testOut", parameters, Collections.emptyList()) {
            @Override
            public Object execute(List<Object> args) {
                double a = ((Number) args.get(0)).doubleValue();
                // Simulate updating OUT parameter 'b' in the function's child context.
                executor.getContext().setVariable("b", a * 2);
                return new ReturnValue(a + 5);
            }
        };
        context.declareFunction("testOut", testOutFunc);
        context.declareVariable("b", "NUMBER");

        List<Object> arguments = Arrays.asList(10, null);
        CountDownLatch latch = new CountDownLatch(1);
        handler.executeFunctionAsync("testOut", arguments, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals(15.0, ((Number) result).doubleValue(), 0.001);
                Object bVal = context.getVariable("b");
                assertNotNull(bVal);
                assertEquals(20.0, ((Number) bVal).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Function execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 8: Function with an INOUT parameter.
    @Test
    public void testFunctionWithInOutParameterPropagation() throws InterruptedException {
        // Define function: FUNCTION increment(INOUT x NUMBER) BEGIN RETURN x + 10; END FUNCTION;
        // Dummy execution: simulate updating INOUT parameter 'x' to x + 10.
        List<Parameter> parameters = Collections.singletonList(
            new Parameter("x", "NUMBER", ParameterMode.INOUT)
        );
        FunctionDefinition incrementFunc = new FunctionDefinition("increment", parameters, Collections.emptyList()) {
            @Override
            public Object execute(List<Object> args) {
                double x = ((Number) args.get(0)).doubleValue();
                double newX = x + 10;
                executor.getContext().setVariable("x", newX);
                return new ReturnValue(newX);
            }
        };
        context.declareFunction("increment", incrementFunc);
        context.declareVariable("x", "NUMBER");
        context.setVariable("x", 5);

        List<Object> arguments = Collections.singletonList(5);
        CountDownLatch latch = new CountDownLatch(1);
        handler.executeFunctionAsync("increment", arguments, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertEquals(15.0, ((Number) result).doubleValue(), 0.001);
                Object xVal = context.getVariable("x");
                assertNotNull(xVal);
                assertEquals(15.0, ((Number) xVal).doubleValue(), 0.001);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Function execution failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    // Test 9: Function with mixed parameter modes (explicit IN, OUT, INOUT).
    @Test
    public void testFunctionWithMixedParameterModes() throws InterruptedException {
        // Define a function "mixParams" with:
        // - IN a NUMBER, OUT b NUMBER, INOUT c NUMBER.
        // The function will:
        //   - Set OUT parameter 'b' = a * 2.
        //   - Update INOUT parameter 'c' to c + a.
        //   - Return a + (c + a).
        String functionQuery = "FUNCTION mixParams(IN a NUMBER, OUT b NUMBER, INOUT c NUMBER) " +
            "BEGIN RETURN a + c; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext funcCtx = parseFunction(functionQuery);
        CountDownLatch latchDef = new CountDownLatch(1);
        // Register the function.
        handler.handleAsync(funcCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                FunctionDefinition func = context.getFunction("mixParams");
                assertNotNull(func);
                List<Parameter> params = func.getParameters();
                assertEquals(3, params.size());
                assertEquals("a", params.get(0).getName());
                assertEquals("NUMBER", params.get(0).getType());
                assertEquals(ParameterMode.IN, params.get(0).getMode());
                assertEquals("b", params.get(1).getName());
                assertEquals("NUMBER", params.get(1).getType());
                assertEquals(ParameterMode.OUT, params.get(1).getMode());
                assertEquals("c", params.get(2).getName());
                assertEquals("NUMBER", params.get(2).getType());
                assertEquals(ParameterMode.INOUT, params.get(2).getMode());
                latchDef.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Function definition failed: " + e.getMessage());
                latchDef.countDown();
            }
        });
        latchDef.await();

        // Overwrite the function definition with a dummy execute implementation that simulates:
        // - OUT parameter 'b' becomes a * 2.
        // - INOUT parameter 'c' becomes c + a.
        // - Return value is a + (c + a).
        FunctionDefinition mixParamsFunc = new FunctionDefinition("mixParams",
            context.getFunction("mixParams").getParameters(), Collections.emptyList()) {
            @Override
            public Object execute(List<Object> args) {
                double a = ((Number) args.get(0)).doubleValue();
                double c = ((Number) args.get(2)).doubleValue();
                double newC = c + a;
                executor.getContext().setVariable("b", a * 2);
                executor.getContext().setVariable("c", newC);
                return new ReturnValue(a + newC);
            }
        };
        // Overwrite the function in the context.
        context.overrideFunction("mixParams", mixParamsFunc);
        // Declare parent's variables for OUT/INOUT parameters.
        context.declareVariable("b", "NUMBER");
        context.declareVariable("c", "NUMBER");
        context.setVariable("c", 10);

        List<Object> arguments = Arrays.asList(5, null, 10);
        CountDownLatch latchExec = new CountDownLatch(1);
        handler.executeFunctionAsync("mixParams", arguments, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Expected:
                // OUT parameter 'b' should be 5 * 2 = 10,
                // INOUT parameter 'c' should be 10 + 5 = 15,
                // Return value should be 5 + 15 = 20.
                assertEquals(20.0, ((Number) result).doubleValue(), 0.001);
                Object bVal = context.getVariable("b");
                Object cVal = context.getVariable("c");
                assertNotNull(bVal);
                assertNotNull(cVal);
                assertEquals(10.0, ((Number) bVal).doubleValue(), 0.001);
                assertEquals(15.0, ((Number) cVal).doubleValue(), 0.001);
                latchExec.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Function execution failed: " + e.getMessage());
                latchExec.countDown();
            }
        });
        latchExec.await();
    }
}
