/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under
 * the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */
package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.utils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IntegrationTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;

    @Before
    public void setup() {
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // or mock(Client.class);
        PlEsqlProcedureLexer lexer =
            new PlEsqlProcedureLexer(CharStreams.fromString("")); // empty source
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
    }

    @After
    public void tearDown() throws InterruptedException {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
    }

    // Helper method to parse a block of PL|ES|QL code
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        return TestUtils.parseProcedure(query);
    }

    // Test 1: Basic integration test combining multiple handlers
    @Test
    public void testBasicIntegration() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE x INT, y FLOAT, i INT;
                SET x = 5;
                SET y = 10.0;
                IF x = 5 THEN
                    SET x = x * 2;  -- x becomes 10
                END IF;
                FOR i IN 1..3 LOOP
                    SET y = y + i;  -- y accumulates values
                END LOOP;
                TRY
                    SET x = x / (y - 16.0); -- x = 10 / (y - 16.0)
                CATCH
                    SET x = -1;  -- Set x to -1 in case of exception
                END TRY;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Expected value of y: 10.0 + 1 + 2 + 3 = 16.0
                // Attempting x = 10 / (16.0 - 16.0) => Division by zero, x should be -1

                // Check that 'x' is -1 (set in the CATCH block)
                assertNotNull("Variable 'x' should be declared.", context.getVariable("x"));
                assertTrue("Variable 'x' should be of type Integer.", context.getVariable("x") instanceof Integer);
                assertEquals("Variable 'x' should be -1 after the TRY-CATCH block.", -1, context.getVariable("x"));

                // Check that 'y' is 16.0
                assertNotNull("Variable 'y' should be declared.", context.getVariable("y"));
                assertTrue("Variable 'y' should be of type Double.", context.getVariable("y") instanceof Double);
                assertEquals("Variable 'y' should be 16.0 after the loop.", 16.0, (Double) context.getVariable("y"), 0.0001);

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 2: Nested control flow test
    @Test
    public void testNestedControlFlow() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE a INT = 1, b INT = 2, c INT = 3, i INT;
                IF a < b THEN
                    FOR i IN 1..3 LOOP
                        SET a = a + i;
                        IF a >= 5 THEN
                            SET b = b * i;
                        END IF;
                    END LOOP;
                END IF;
                TRY
                    SET c = b / (a - 7);
                CATCH
                    SET c = 100;
                END TRY;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Expected values:
                // a: 1 + 1 + 2 + 3 = 7
                // b: b * i when a >= 5 (b = 2 * 3 = 6)
                // c: Division by zero occurs (a - 7 = 0), so c = 100

                // Check that 'a' is 7
                assertNotNull("Variable 'a' should be declared.", context.getVariable("a"));
                assertEquals("Variable 'a' should be 7 after the loop.", 7, context.getVariable("a"));

                // Check that 'b' is 6
                assertNotNull("Variable 'b' should be declared.", context.getVariable("b"));
                assertEquals("Variable 'b' should be 6 after the loop.", 6, context.getVariable("b"));

                // Check that 'c' is 100
                assertNotNull("Variable 'c' should be declared.", context.getVariable("c"));
                assertEquals("Variable 'c' should be 100 after the TRY-CATCH block.", 100, context.getVariable("c"));

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 3: Function calls within control flow
    @Test
    public void testFunctionCallsWithinControlFlow() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE result FLOAT, i INT;
                FUNCTION add(a FLOAT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                SET result = add(1, 2); -- result = 3
                IF result > 2 THEN
                    FOR i IN 1..2 LOOP
                        SET result = add(result, i); -- result accumulates values
                    END LOOP;
                END IF;
                TRY
                    SET result = add(result, 5) / (result - 10); -- result = (11) / (result - 10)
                CATCH
                    SET result = -100;
                END TRY;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                /*
                Calculations:
                - result = add(1, 2) => 3
                - Since result > 2, enter IF
                - Loop i from 1 to 2:
                  - i=1: result = add(3,1) => 4
                  - i=2: result = add(4,2) => 6
                - TRY:
                  - result = add(6,5) / (6 - 10) => 11 / (-4) => -2.75
                  - No exception occurs
                */

                // Check that 'result' is approximately -2.75
                assertNotNull("Variable 'result' should be declared.", context.getVariable("result"));
                Object resultValue = context.getVariable("result");
                assertTrue("Variable 'result' should be a number.", resultValue instanceof Number);
                double expectedResult = -2.75;
                double actualResult = ((Number) resultValue).doubleValue();
                assertEquals("Variable 'result' should be approximately -2.75 after calculations.", expectedResult, actualResult, 0.000001);

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 4: Edge cases, including loops with no iterations
    @Test
    public void testEdgeCases() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE n INT = 10, i INT;
                FOR i IN 5..3 LOOP -- No iterations should happen
                    SET n = n + 1;
                END LOOP;
                TRY
                    DECLARE x INT = 0;
                    SET n = n / x; -- Division by zero, should trigger CATCH
                CATCH
                    SET n = -50;
                END TRY;
                IF n = -50 THEN
                    SET n = n * 2; -- n becomes -100
                END IF;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'n' is -100
                assertNotNull("Variable 'n' should be declared.", context.getVariable("n"));
                assertEquals("Variable 'n' should be -100 after calculations.", -100, context.getVariable("n"));

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 5: Nested Try-Catch statements
    @Test
    public void testNestedTryCatch() throws InterruptedException {
        String blockQuery = """
           BEGIN
               DECLARE v INT = 1;
               TRY
                   TRY
                       SET v = v / 0; -- This should trigger the inner CATCH
                   CATCH
                       SET v = 10;
                       THROW 'Manual exception'; -- Simulate throwing another exception
                   END TRY;
               CATCH
                   SET v = 20;
               END TRY;
               IF v = 20 THEN
                   SET v = v + 5; -- v becomes 25
               ELSE
                   SET v = v + 100; -- This should not execute
               END IF;
           END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'v' is 25
                assertNotNull("Variable 'v' should be declared.", context.getVariable("v"));
                assertEquals("Variable 'v' should be 25 after nested TRY-CATCH.", 25, context.getVariable("v"));

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 6: Recursive Function Calls
    @Test
    public void testRecursiveFunctionCalls() throws InterruptedException {
        String blockQuery = """
        BEGIN
            DECLARE n INT = 5, result INT;
            FUNCTION factorial(x INT) BEGIN
                IF x <= 1 THEN
                    RETURN 1;
                ELSE
                    RETURN x * factorial(x - 1);
                END IF;
            END FUNCTION;
            SET result = factorial(n);
        END
    """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Expected result: 5! = 120

                // Check that 'result' is 120
                assertNotNull("Variable 'result' should be declared.", context.getVariable("result"));
                assertEquals("Variable 'result' should be 120 after factorial calculation.", 120, context.getVariable("result"));

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 7: Variable Shadowing and Scope
    @Test
    public void testVariableShadowing() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE x INT = 10;
                FUNCTION test() BEGIN
                    DECLARE x INT = 5;
                    SET x = x + 5; -- x in function scope
                    RETURN x;
                END FUNCTION;
                SET x = test(); -- x in global scope is set to function's return value
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Expected x: Function returns 10, so global x becomes 10

                // Check that 'x' is 10
                assertNotNull("Variable 'x' should be declared.", context.getVariable("x"));
                assertEquals("Variable 'x' should be 10 after function call.", 10, context.getVariable("x"));

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 8: Loop with Break Statement (assuming language supports BREAK)
    @Test
    public void testLoopWithBreak() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE sum INT = 0, i INT;
                FOR i IN 1..10 LOOP
                    IF i > 5 THEN
                        BREAK;
                    END IF;
                    SET sum = sum + i;
                END LOOP;
            END
        """;
        // Assuming BREAK is implemented in your language and handlers

        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Expected sum: 1 + 2 + 3 + 4 + 5 = 15

                // Check that 'sum' is 15
                assertNotNull("Variable 'sum' should be declared.", context.getVariable("sum"));
                assertEquals("Variable 'sum' should be 15 after loop with BREAK.", 15, context.getVariable("sum"));

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 9: Handling Undefined Variables
    @Test
    public void testUndefinedVariable() throws InterruptedException {
        String blockQuery = """
            BEGIN
                SET x = 10; -- x is not declared
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                fail("Expected an exception due to undefined variable 'x'.");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                // Expected exception
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 10: Complex Expression Evaluation
    @Test
    public void testComplexExpression() throws InterruptedException {
        String blockQuery = """
            BEGIN
                DECLARE a INT = 5, b INT = 3, c FLOAT;
                SET c = (a + b) * (a - b) / b; -- c = (5 + 3) * (5 - 3) / 3 = 8 * 2 / 3 = 5.333...
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                // Check that 'c' is approximately 5.3333
                assertNotNull("Variable 'c' should be declared.", context.getVariable("c"));
                Object cValue = context.getVariable("c");
                assertTrue("Variable 'c' should be a number.", cValue instanceof Number);
                double expectedC = (8.0 * 2.0) / 3.0; // 5.3333...
                double actualC = ((Number) cValue).doubleValue();
                assertEquals("Variable 'c' should be approximately 5.3333 after calculation.", expectedC, actualC, 0.0001);

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }
}
