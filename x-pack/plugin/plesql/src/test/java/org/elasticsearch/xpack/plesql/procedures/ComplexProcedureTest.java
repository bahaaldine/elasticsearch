/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.procedures;

import java.util.concurrent.CountDownLatch;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.DocumentBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.elasticsearch.xpack.plesql.utils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Scenario tests for complex procedures that simulate real-life PL/SQL usage.
 * These tests validate that procedures with arrays, conditional logic,
 * and TRY/CATCH exception handling operate as expected.
 */
public class ComplexProcedureTest extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Create a clean global execution context.
        context = new ExecutionContext();
        // (Ensure that built-in functions for arrays, documents, strings, numbers, etc. are registered)
        StringBuiltInFunctions.registerAll(context);
        NumberBuiltInFunctions.registerAll(context);
        ArrayBuiltInFunctions.registerAll(context);
        DocumentBuiltInFunctions.registerAll(context);

        threadPool = new TestThreadPool("test-thread-pool");
        // Create a dummy token stream (the procedures will be parsed from strings).
        PlEsqlProcedureLexer dummyLexer = new PlEsqlProcedureLexer(CharStreams.fromString(""));
        CommonTokenStream dummyTokens = new CommonTokenStream(dummyLexer);
        executor = new ProcedureExecutor(context, threadPool, null, dummyTokens);
    }

    @After
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    /**
     * Test a procedure that processes an array of numbers with all positive values.
     * The procedure loops over the array, sums up the numbers, and returns the total.
     * In this case, no error is thrown.
     * Expected: (10+20+30+40)+50 = 150.
     */
    @Test
    public void testComplexProcedureAllPositives() throws InterruptedException {
        String proc =
            "PROCEDURE aggregatePositiveNumbers() " +
                "BEGIN " +
                "  DECLARE arr ARRAY OF NUMBER = [10, 20, 30, 40]; " +
                "  DECLARE total NUMBER = 0; " +
                "  TRY " +
                "    FOR num IN arr LOOP " +
                "       IF num < 0 THEN THROW 'Negative number encountered'; END IF; " +
                "       SET total = total + num; " +
                "    END LOOP; " +
                "    SET total = total + 50; " +
                "  CATCH " +
                "    SET total = -1; " +
                "  END TRY; " +
                "  RETURN total; " +
                "END PROCEDURE";

        PlEsqlProcedureParser.ProcedureContext procCtx = TestUtils.parseBlock(proc);

        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(procCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                Object rawResult = result;
                if (rawResult instanceof ReturnValue) {
                    rawResult = ((ReturnValue) rawResult).getValue();
                }
                double returned = Double.parseDouble(rawResult.toString());
                assertEquals("Expected aggregate of positive numbers to be 150", 150.0, returned, 0.001);
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

    /**
     * Test a procedure that processes an array of numbers containing a negative value.
     * The procedure loops over the array and throws an error if any number is negative.
     * This exception is caught in the CATCH block, which sets the total to -1.
     * Expected: -1 (because a negative number triggers the catch block).
     */
    @Test
    public void testComplexProcedureWithNegative() throws InterruptedException {
        String proc =
            "PROCEDURE aggregateWithNegative() " +
                "BEGIN " +
                "  DECLARE arr ARRAY OF NUMBER = [10, 20, -5, 40]; " +
                "  DECLARE total NUMBER = 0; " +
                "  TRY " +
                "    FOR num IN arr LOOP " +
                "       IF num < 0 THEN THROW 'Negative number encountered'; END IF; " +
                "       SET total = total + num; " +
                "    END LOOP; " +
                "    SET total = total + 50; " +
                "  CATCH " +
                "    SET total = -1; " +
                "  END TRY; " +
                "  RETURN total; " +
                "END PROCEDURE";

        PlEsqlProcedureParser.ProcedureContext procCtx = TestUtils.parseBlock(proc);

        CountDownLatch latch = new CountDownLatch(1);
        executor.visitProcedureAsync(procCtx, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                Object rawResult = result;
                if (rawResult instanceof ReturnValue) {
                    rawResult = ((ReturnValue) rawResult).getValue();
                }
                double returned = Double.parseDouble(rawResult.toString());
                assertEquals("Expected aggregate to be -1 when a negative number is encountered", -1.0, returned, 0.001);
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
}
