/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.symbol;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.plesql.functions.FunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SymbolTableTests extends ESTestCase {

    private ExecutionContext context;

    @Override
    public void setUp() throws Exception {
        // Use the default setUp from ESTestCase (the test cluster isn’t needed here)
        super.setUp();
        context = new ExecutionContext();
    }

    @Test
    public void testDeclareAndRetrieveVariable() {
        // Declare a variable and then set/get its value via the ExecutionContext.
        context.declareVariable("x", "NUMBER");
        context.setVariable("x", 42);
        Object val = context.getVariable("x");
        assertNotNull("Variable x should be declared", val);
        assertEquals("Variable x should be 42", 42, ((Number) val).intValue());
    }

    @Test
    public void testDeclareAndExecuteFunction() throws Exception {
        // Define a simple built-in function "add" that adds two numbers.
        List<Parameter> params = Arrays.asList(
            new Parameter("a", "NUMBER", ParameterMode.IN),
            new Parameter("b", "NUMBER", ParameterMode.IN)
        );
        // Create a synchronous built-in function wrapped in the new asynchronous API.
        FunctionDefinition addFunc = new BuiltInFunctionDefinition("add", (List<Object> args, ActionListener<Object> listener) -> {
            if (args.size() != 2) {
                listener.onFailure(new RuntimeException("add expects two arguments"));
            } else {
                double a = ((Number) args.get(0)).doubleValue();
                double b = ((Number) args.get(1)).doubleValue();
                listener.onResponse(a + b);
            }
        });
        // Declare the function in the context.
        context.declareFunction("add", addFunc);

        // Retrieve and test the function.
        FunctionDefinition retrieved = context.getFunction("add");
        assertNotNull("Function 'add' should be declared", retrieved);

        // Now execute the function asynchronously.
        CountDownLatch latch = new CountDownLatch(1);
        List<Object> arguments = Arrays.asList(3, 4);
        retrieved.execute(arguments, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // The synchronous function returns its result synchronously, but wrapped via execute.
                // Our implementation in BuiltInFunctionDefinition wraps it by calling listener.onResponse.
                if (result instanceof ReturnValue) {
                    result = ((ReturnValue) result).getValue();
                }
                assertEquals("Expected add(3, 4) to return 7", 7.0, ((Number) result).doubleValue(), 0.001);
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

    @Test
    public void testFunctionWithMixedParameterModes() throws Exception {
        // Define a function with mixed parameter modes.
        // For this test, we simulate a function that:
        //   - Has an IN parameter "a",
        //   - An OUT parameter "b" (which it computes as 2 * a),
        //   - And an INOUT parameter "c" (which it increments by a).
        List<Parameter> params = Arrays.asList(
            new Parameter("a", "NUMBER", ParameterMode.IN),
            new Parameter("b", "NUMBER", ParameterMode.OUT),
            new Parameter("c", "NUMBER", ParameterMode.INOUT)
        );
        FunctionDefinition mixFunc = new BuiltInFunctionDefinition("mixParams", (List<Object> args, ActionListener<Object> listener) -> {
            if (args.size() != 3) {
                listener.onFailure(new RuntimeException("mixParams expects three arguments"));
            } else {
                double a = ((Number) args.get(0)).doubleValue();
                double c = ((Number) args.get(2)).doubleValue();
                double newC = c + a;
                // Simulate an OUT parameter update by setting the variable in the context.
                context.setVariable("b", a * 2);
                context.setVariable("c", newC);
                listener.onResponse(a + newC);
            }
        });
        context.declareFunction("mixParams", mixFunc);
        // Declare variables for OUT/INOUT.
        context.declareVariable("b", "NUMBER");
        context.declareVariable("c", "NUMBER");
        context.setVariable("c", 10);

        CountDownLatch latch = new CountDownLatch(1);
        List<Object> arguments = Arrays.asList(5, null, 10);
        mixFunc.execute(arguments, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                // Expected: b becomes 5 * 2 = 10, c becomes 10 + 5 = 15, and function returns 5 + 15 = 20.
                if (result instanceof ReturnValue) {
                    result = ((ReturnValue) result).getValue();
                }
                assertEquals("Expected result is 20", 20.0, ((Number) result).doubleValue(), 0.001);
                Object bVal = context.getVariable("b");
                Object cVal = context.getVariable("c");
                assertNotNull("Variable b should be set", bVal);
                assertNotNull("Variable c should be set", cVal);
                assertEquals("b should be updated to 10", 10.0, ((Number) bVal).doubleValue(), 0.001);
                assertEquals("c should be updated to 15", 15.0, ((Number) cVal).doubleValue(), 0.001);
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
}
