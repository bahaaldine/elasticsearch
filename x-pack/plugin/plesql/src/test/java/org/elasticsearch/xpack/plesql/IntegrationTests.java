/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IntegrationTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;

    @Before
    public void setup() {
        context = new ExecutionContext();
        executor = new ProcedureExecutor(context, null);  // Use a default exception listener
    }

    // Helper method to parse a block of PL|ES|QL code
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        return TestUtils.parseProcedure(query);
    }

    // Test 1: Basic integration test combining multiple handlers
    @Test
    public void testBasicIntegration() {
        String blockQuery = """
            BEGIN
                DECLARE x INT, y FLOAT, i INT;
                SET x = 5;
                SET y = 10.0;
                IF x = 5 THEN
                    SET x = x + 1;
                END IF;
                FOR i IN 1..3 LOOP
                    SET y = y + i;
                END LOOP;
                TRY
                    SET x = x / 0; -- This should trigger the catch block
                CATCH
                    SET x = 0;
                END TRY;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'x' is 0 (set in the CATCH block)
        assertNotNull("x should be declared.", context.getVariable("x"));
        assertEquals(0, context.getVariable("x"));

        // Check that 'y' is 16.0 (10.0 + 1 + 2 + 3)
        assertNotNull("y should be declared.", context.getVariable("y"));
        assertEquals(16.0, context.getVariable("y"));
    }

    // Test 2: Nested control flow test
    @Test
    public void testNestedControlFlow() {
        String blockQuery = """
            BEGIN
                DECLARE a INT = 1, b INT = 2, c INT = 3;
                IF a < b THEN
                    FOR i IN 1..3 LOOP
                        SET a = a + 1;
                        IF a = 3 THEN
                            SET b = b + i;
                        END IF;
                    END LOOP;
                END IF;
                TRY
                    SET c = a / (b - 9); -- This should not trigger the catch
                CATCH
                    SET c = 0;
                END TRY;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'a' is 4
        assertNotNull("a should be declared.", context.getVariable("a"));
        assertEquals(4, context.getVariable("a"));

        // Check that 'b' is 5
        assertNotNull("b should be declared.", context.getVariable("b"));
        assertEquals(5, context.getVariable("b"));

        // Check that 'c' is 4 (division succeeds)
        assertNotNull("c should be declared.", context.getVariable("c"));
        assertEquals(4, context.getVariable("c"));
    }

    // Test 3: Function calls within control flow
    @Test
    public void testFunctionCallsWithinControlFlow() {
        String blockQuery = """
            BEGIN
                DECLARE result INT;
                FUNCTION add(a INT, b INT) BEGIN
                    RETURN a + b;
                END FUNCTION;
                SET result = add(1, 2); -- result should be 3
                IF result > 2 THEN
                    FOR i IN 1..2 LOOP
                        SET result = add(result, i);
                    END LOOP;
                END IF;
                TRY
                    SET result = add(result, 10) / (result - 20); -- Should trigger the catch
                CATCH
                    SET result = -1;
                END TRY;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'result' is -1 (set in the CATCH block)
        assertNotNull("result should be declared.", context.getVariable("result"));
        assertEquals(-1, context.getVariable("result"));
    }

    // Test 4: Edge cases, including loops with no iterations
    @Test
    public void testEdgeCases() {
        String blockQuery = """
            BEGIN
                DECLARE n INT = 10;
                FOR i IN 5..3 LOOP -- No iterations should happen
                    SET n = n + 1;
                END LOOP;
                TRY
                    DECLARE x INT;
                    SET n = n / x; -- This should trigger a CATCH due to uninitialized x
                CATCH
                    SET n = 0;
                END TRY;
                IF n = 0 THEN
                    SET n = 100;
                END IF;
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'n' is 100
        assertNotNull("n should be declared.", context.getVariable("n"));
        assertEquals(100, context.getVariable("n"));
    }

    // Test 5: Nested Try-Catch statements
    @Test
    public void testNestedTryCatch() {
        String blockQuery = """
           BEGIN
               DECLARE v INT = 1;
               TRY
                   TRY
                       SET v = v / 0; -- This should trigger the inner catch
                   CATCH
                       SET v = 10;
                       THROW 'Manual exception'; -- Simulate throwing another exception
                   END TRY;
               CATCH
                   SET v = 20;
               END TRY;
               IF v = 20 THEN
                   SET v = v + 5;
               END IF;
           END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'v' is 25
        assertNotNull("v should be declared.", context.getVariable("v"));
        assertEquals(25, context.getVariable("v"));
    }
}
