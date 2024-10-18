/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LoopStatementHandlerTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;

    @Before
    public void setup() {
        context = new ExecutionContext();  // Real ExecutionContext
        executor = new ProcedureExecutor(context);  // Use real ProcedureExecutor
    }

    // Helper method to parse a BEGIN ... END block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(new ANTLRInputStream(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();  // Return the parsed block
    }

    // Test 1: Simple FOR loop
    @Test
    public void testSimpleForLoop() {
        String blockQuery = "BEGIN DECLARE j INT, i INT; FOR i IN 1..3 LOOP SET j = i + 1; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'i' is set to 4 at the end of the loop
        assertNotNull("j should be declared.", context.getVariable("j"));
        assertEquals(4, context.getVariable("j"));  // After incrementing 3 times (1, 2, 3 -> i = i + 1)
    }

    // Test 2: Simple WHILE loop
    @Test
    public void testSimpleWhileLoop() {
        String blockQuery = "BEGIN DECLARE i INT = 1; WHILE i < 4 LOOP SET i = i + 1; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'i' is set to 4 after the loop terminates
        assertNotNull("i should be declared.", context.getVariable("i"));
        assertEquals(4, context.getVariable("i"));  // Increment until i < 4 becomes false
    }

    // Test 3: Reverse FOR loop with iterations
    @Test
    public void testReverseForLoop() {
        String blockQuery = "BEGIN DECLARE j INT = 0, i INT; FOR i IN 5..3 LOOP SET j = j + i; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // The loop should iterate from 5 down to 3
        assertNotNull("j should be declared.", context.getVariable("j"));
        assertEquals(5 + 4 + 3, context.getVariable("j"));  // Sum of 5, 4, and 3
    }

    // Test 4: WHILE loop with false initial condition
    @Test
    public void testWhileLoopFalseInitialCondition() {
        String blockQuery = "BEGIN DECLARE i INT = 5; WHILE i < 4 LOOP SET i = i + 1; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // The loop should not run because the condition is false initially
        assertNotNull("i should be declared.", context.getVariable("i"));
        assertEquals(5, context.getVariable("i"));  // No loop iteration
    }

    // Test 5: Nested FOR loop
    @Test
    public void testNestedForLoop() {
        String blockQuery = "BEGIN DECLARE i INT; DECLARE j INT; FOR i IN 1..2 LOOP FOR j IN 1..2 LOOP SET j = j + 1; END LOOP END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // Check that 'i' and 'j' are properly updated
        assertNotNull("i should be declared.", context.getVariable("i"));
        assertNotNull("j should be declared.", context.getVariable("j"));
        assertEquals(2, context.getVariable("i"));  // 'i' will be 2 after the loop
        assertEquals(3, context.getVariable("j"));  // 'j' will be 3 after the loop (incremented twice in inner loop)
    }

    // Test 6: Infinite WHILE loop (with break condition)
    @Test
    public void testInfiniteWhileLoop() {
        String blockQuery = "BEGIN DECLARE i INT = 1; WHILE 1 = 1 LOOP SET i = i + 1; IF i > 1000 THEN BREAK; END IF; END LOOP END";
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        executor.visitProcedure(blockContext);

        // 'i' should not exceed 1000, and the loop should break when i > 1000
        assertEquals(1001, context.getVariable("i"));
    }
}
