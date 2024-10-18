/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.handlers.PlEsqlErrorListener;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.FunctionDefinition;
import org.elasticsearch.xpack.plesql.handlers.FunctionDefinitionHandler;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FunctionDefinitionHandlerTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private FunctionDefinitionHandler handler;

    @Before
    public void setup() {
        context = new ExecutionContext();
        executor = new ProcedureExecutor(context);
        handler = new FunctionDefinitionHandler(executor);
    }

    // Helper method to parse a function definition and return the necessary context
    private PlEsqlProcedureParser.Function_definitionContext parseFunction(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.function_definition();
    }

    // Helper method to parse a full procedure block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);

        parser.removeErrorListeners();  // Remove existing error listeners
        parser.addErrorListener(new PlEsqlErrorListener());  // Add your custom error listener

        return parser.procedure();
    }

    // Test 1: Define a function with no parameters and test its registration
    @Test
    public void testFunctionDefinitionWithoutParameters() {
        String functionQuery = "FUNCTION myFunction() BEGIN RETURN 10; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        handler.handle(functionContext);

        FunctionDefinition function = context.getFunction("myFunction");
        assertNotNull(function);
        assertEquals("myFunction", function.getName());
        assertEquals(0, function.getParameters().size());
        assertNotNull(function.getBody());
    }

    // Test 2: Define a function with one parameter and test its registration
    @Test
    public void testFunctionDefinitionWithOneParameter() {
        String functionQuery = "FUNCTION add(a INT) BEGIN RETURN a + 5; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        handler.handle(functionContext);

        FunctionDefinition function = context.getFunction("add");
        assertNotNull(function);
        assertEquals("add", function.getName());
        assertEquals(1, function.getParameters().size());
        assertEquals("a", function.getParameters().get(0));
        assertNotNull(function.getBody());
    }

    // Test 3: Define a function with multiple parameters and test its registration
    @Test
    public void testFunctionDefinitionWithMultipleParameters() {
        String functionQuery = "FUNCTION multiply(a INT, b INT, c INT) BEGIN RETURN a * b * c; END FUNCTION;";
        PlEsqlProcedureParser.Function_definitionContext functionContext = parseFunction(functionQuery);

        handler.handle(functionContext);

        FunctionDefinition function = context.getFunction("multiply");
        assertNotNull(function);
        assertEquals("multiply", function.getName());
        List<String> params = function.getParameters();
        assertEquals(3, params.size());
        assertEquals("a", params.get(0));
        assertEquals("b", params.get(1));
        assertEquals("c", params.get(2));
        assertNotNull(function.getBody());
    }

    // Test 4: Define a function within a procedure block and verify its execution
    @Test
    public void testFunctionDefinitionAndCallWithinBlock() {
        String blockQuery = """
            BEGIN
                DECLARE result INT;
                FUNCTION add(a INT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                SET result = add(3, 4);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that the function 'add' is registered
        FunctionDefinition function = context.getFunction("add");
        assertNotNull(function);
        assertEquals("add", function.getName());

        // Assert that 'result' is correctly assigned the value returned by 'add(3, 4)'
        assertNotNull("Variable 'result' should be declared.", context.getVariable("result"));
        Object resultValue = context.getVariable("result");
        assertTrue("Variable 'result' should be a number.", resultValue instanceof Number);
        int expectedResult = 7;
        int actualResult = ((Number) resultValue).intValue();
        assertEquals("Variable 'result' should be 7 after add function call.", expectedResult, actualResult);
    }

    // Test 5: Define a function with a return statement and execute it within a loop
    @Test
    public void testFunctionCallWithinLoop() {
        String blockQuery = """
            BEGIN
                DECLARE total INT, i INT;
                FUNCTION add(a INT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                SET total = 0;
                FOR i IN 1..3 LOOP
                    SET total = add(total, i);
                END LOOP;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that 'total' accumulates the sum correctly
        assertNotNull("Variable 'total' should be declared.", context.getVariable("total"));
        Object totalValue = context.getVariable("total");
        assertTrue("Variable 'total' should be a number.", totalValue instanceof Number);
        int expectedTotal = 6; // 0 + 1 + 2 + 3
        int actualTotal = ((Number) totalValue).intValue();
        assertEquals("Variable 'total' should be 6 after loop execution.", expectedTotal, actualTotal);
    }

    // Test 6: Define a function and execute it within an IF condition
    @Test
    public void testFunctionCallWithinIfCondition() {
        String blockQuery = """
            BEGIN
                DECLARE result INT;
                FUNCTION add(a INT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                SET result = add(5, 10);
                IF result > 10 THEN
                    SET result = add(result, 5);
                END IF;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that 'result' is updated correctly based on the IF condition
        assertNotNull("Variable 'result' should be declared.", context.getVariable("result"));
        Object resultValue = context.getVariable("result");
        assertTrue("Variable 'result' should be a number.", resultValue instanceof Number);
        int expectedResult = 20; // add(5,10) = 15 > 10, then add(15,5) = 20
        int actualResult = ((Number) resultValue).intValue();
        assertEquals("Variable 'result' should be 20 after IF condition.", expectedResult, actualResult);
    }

    // Test 7: Define multiple functions and execute them within a procedure block
    @Test
    public void testMultipleFunctionDefinitionsAndCalls() {
        String blockQuery = """
            BEGIN
                DECLARE sum INT, product INT;
                FUNCTION add(a INT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                FUNCTION multiply(a INT, b INT) BEGIN
                    RETURN a * b;
                END FUNCTION;
                SET sum = add(2, 3);
                SET product = multiply(sum, 4);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that both 'sum' and 'product' are correctly assigned
        assertNotNull("Variable 'sum' should be declared.", context.getVariable("sum"));
        Object sumValue = context.getVariable("sum");
        assertTrue("Variable 'sum' should be a number.", sumValue instanceof Number);
        int expectedSum = 5;
        int actualSum = ((Number) sumValue).intValue();
        assertEquals("Variable 'sum' should be 5 after add function call.", expectedSum, actualSum);

        assertNotNull("Variable 'product' should be declared.", context.getVariable("product"));
        Object productValue = context.getVariable("product");
        assertTrue("Variable 'product' should be a number.", productValue instanceof Number);
        int expectedProduct = 20; // 5 * 4
        int actualProduct = ((Number) productValue).intValue();
        assertEquals("Variable 'product' should be 20 after multiply function call.", expectedProduct, actualProduct);
    }

    // Test 8: Define a function with no return statement (should throw an error during execution)
    @Test(expected = RuntimeException.class)
    public void testFunctionDefinitionWithoutReturnThrowsError() {
        String blockQuery = """
            BEGIN
                DECLARE result INT;
                FUNCTION faultyFunction(a INT, b INT) BEGIN
                    SET result = a + b;
                END FUNCTION;
                SET result = faultyFunction(1, 2);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);
    }

    // Test 9: Define a function that calls another function within its body
    @Test
    public void testFunctionCallingAnotherFunction() {
        String blockQuery = """
            BEGIN
                DECLARE result INT;
                FUNCTION add(a INT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                FUNCTION addThreeNumbers(a INT, b INT, c INT) BEGIN
                    RETURN add(a, add(b, c));
                END FUNCTION;
                SET result = addThreeNumbers(1, 2, 3);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that 'result' is correctly assigned the sum of three numbers
        assertNotNull("Variable 'result' should be declared.", context.getVariable("result"));
        Object resultValue = context.getVariable("result");
        assertTrue("Variable 'result' should be a number.", resultValue instanceof Number);
        int expectedResult = 6; // addThreeNumbers(1,2,3) = add(1, add(2,3)) = 1 + 5 = 6
        int actualResult = ((Number) resultValue).intValue();
        assertEquals("Variable 'result' should be 6 after nested add function calls.", expectedResult, actualResult);
    }

    // Test 10: Define a function that returns another function's result and handle type coercion
    @Test
    public void testFunctionReturnWithTypeCoercion() {
        String blockQuery = """
            BEGIN
                DECLARE result FLOAT;
                FUNCTION add(a INT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                SET result = add(2, 3); -- Should handle INT to FLOAT conversion
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that 'result' is correctly assigned as FLOAT with value 5.0
        assertNotNull("Variable 'result' should be declared.", context.getVariable("result"));
        Object resultValue = context.getVariable("result");
        assertTrue("Variable 'result' should be a number.", resultValue instanceof Number);
        double expectedResult = 5.0;
        double actualResult = ((Number) resultValue).doubleValue();
        assertEquals("Variable 'result' should be 5.0 after add function call with type coercion.", expectedResult, actualResult, 0.000001);
    }

    // Test 11: Define a function with no parameters and execute it
    @Test
    public void testFunctionWithoutParameters() {
        String blockQuery = """
            BEGIN
                DECLARE greeting STRING;
                FUNCTION sayHello() BEGIN
                    RETURN 'Hello, World!';
                END FUNCTION;
                SET greeting = sayHello();
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that 'greeting' is correctly assigned the returned string
        assertNotNull("Variable 'greeting' should be declared.", context.getVariable("greeting"));
        Object greetingValue = context.getVariable("greeting");
        assertTrue("Variable 'greeting' should be a string.", greetingValue instanceof String);
        String expectedGreeting = "Hello, World!";
        String actualGreeting = (String) greetingValue;
        assertEquals("Variable 'greeting' should be 'Hello, World!'.", expectedGreeting, actualGreeting);
    }

    // Test 12: Define a function that returns a STRING and use it in a conditional
    @Test
    public void testFunctionReturnStringInCondition() {
        String blockQuery = """
            BEGIN
                DECLARE message STRING;
                FUNCTION getMessage(code INT) BEGIN
                    IF code = 1 THEN
                        RETURN 'Success';
                    ELSE
                        RETURN 'Failure';
                    END IF;
                END FUNCTION;
                SET message = getMessage(1);
                IF message = 'Success' THEN
                    SET message = 'Operation was successful.';
                ELSE
                    SET message = 'Operation failed.';
                END IF;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that 'message' is updated correctly based on the function's return value
        assertNotNull("Variable 'message' should be declared.", context.getVariable("message"));
        Object messageValue = context.getVariable("message");
        assertTrue("Variable 'message' should be a string.", messageValue instanceof String);
        String expectedMessage = "Operation was successful.";
        String actualMessage = (String) messageValue;
        assertEquals("Variable 'message' should reflect the successful operation.", expectedMessage, actualMessage);
    }

    // Test 13: Define a function with a recursive call (optional, if recursion is supported)
    @Test
    public void testRecursiveFunction() {
        String blockQuery = """
            BEGIN
                DECLARE factorialResult INT;
                FUNCTION factorial(n INT) BEGIN
                    IF n <= 1 THEN
                        RETURN 1;
                    ELSE
                        RETURN n * factorial(n - 1);
                    END IF;
                END FUNCTION;
                SET factorialResult = factorial(5); -- Expected: 120
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Assert that 'factorialResult' is correctly calculated
        assertNotNull("Variable 'factorialResult' should be declared.", context.getVariable("factorialResult"));
        Object factorialValue = context.getVariable("factorialResult");
        assertTrue("Variable 'factorialResult' should be a number.", factorialValue instanceof Number);
        int expectedFactorial = 120;
        int actualFactorial = ((Number) factorialValue).intValue();
        assertEquals("Variable 'factorialResult' should be 120 after recursive factorial calculation.", expectedFactorial, actualFactorial);
    }

    // Test 14: Invalid function definition (missing return statement) within a block
    @Test(expected = RuntimeException.class)
    public void testFunctionDefinitionWithoutReturnInBlockThrowsError() {
        String blockQuery = """
            BEGIN
                DECLARE result INT;
                FUNCTION faultyFunction(a INT, b INT) BEGIN
                    SET result = a + b;
                END FUNCTION;
                SET result = faultyFunction(1, 2);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);
    }
}
