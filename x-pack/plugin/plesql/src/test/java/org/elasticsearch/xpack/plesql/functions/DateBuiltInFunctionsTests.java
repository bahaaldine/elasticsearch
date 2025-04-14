/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.functions.builtin.types.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.plesql.handlers.FunctionDefinitionHandler;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.builtin.types.DateBuiltInFunctions;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DateBuiltInFunctionsTests extends ESTestCase {

    private ExecutionContext context;
    private ProcedureExecutor dummyExecutor;
    private FunctionDefinitionHandler handler;
    private ThreadPool threadPool;
    private ProcedureExecutor executor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("array-test-pool");
        Client mockClient = null; // or a proper mock
        // Create a lexer with an empty source as a placeholder (adjust if needed)
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(""));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        // Initialize the FunctionDefinitionHandler as done in other test classes
        handler = new FunctionDefinitionHandler(executor);
        // Register the array built-in functions so that they are available for testing.
        DateBuiltInFunctions.registerAll(context);
    }

    @Override
    public void tearDown() throws Exception {
        // Properly terminate the thread pool to avoid thread leaks.
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testCurrentDate() throws Exception {
        // Create a fresh execution context and register date functions.
        ExecutionContext context = new ExecutionContext();
        DateBuiltInFunctions.registerAll(context);

        // Retrieve the CURRENT_DATE function.
        FunctionDefinition currentDateFunc = context.getFunction("CURRENT_DATE");
        assertNotNull("CURRENT_DATE function should be registered", currentDateFunc);

        CountDownLatch latch = new CountDownLatch(1);
        // Execute the function with no arguments.
        currentDateFunc.execute(List.of(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result of CURRENT_DATE should not be null", result);
                assertTrue("Result must be an instance of Date", result instanceof Date);
                Date currentDate = (Date) result;
                // Compare with now; currentDate should be <= now.
                Date now = new Date();
                assertFalse("CURRENT_DATE should not be in the future", currentDate.after(now));
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution of CURRENT_DATE failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testCurrentTimestamp() throws Exception {
        FunctionDefinition currentTimestampFn = context.getFunction("CURRENT_TIMESTAMP");
        assertNotNull("CURRENT_TIMESTAMP function should be registered", currentTimestampFn);
        CountDownLatch latch = new CountDownLatch(1);
        currentTimestampFn.execute(Arrays.asList(), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result of CURRENT_TIMESTAMP should not be null", result);
                assertTrue("Result must be an instance of Date", result instanceof Date);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution of CURRENT_TIMESTAMP failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testDateAdd() throws Exception {
        // Create a fresh execution context and register date functions.
        ExecutionContext context = new ExecutionContext();
        DateBuiltInFunctions.registerAll(context);

        // Retrieve the DATE_ADD function.
        FunctionDefinition dateAddFunc = context.getFunction("DATE_ADD");
        assertNotNull("DATE_ADD function should be registered", dateAddFunc);

        // Prepare arguments: a base date and the number of days to add.
        Date baseDate = new Date();
        List<Object> args = List.of(baseDate, 5);

        CountDownLatch latch = new CountDownLatch(1);
        dateAddFunc.execute(args, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result of DATE_ADD should not be null", result);
                assertTrue("Result must be an instance of Date", result instanceof Date);
                Date newDate = (Date) result;
                // Calculate the expected difference in milliseconds.
                long diffMillis = newDate.getTime() - baseDate.getTime();
                long fiveDaysMillis = 5L * 24 * 60 * 60 * 1000;
                // Allow a small tolerance (here 1000 ms) for processing delay.
                assertTrue("Difference should be at least five days", diffMillis >= fiveDaysMillis);
                assertTrue("Difference should be less than five days plus tolerance",
                    diffMillis < fiveDaysMillis + 1000);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution of DATE_ADD failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testDateSub() throws Exception {
        FunctionDefinition dateSubFn = context.getFunction("DATE_SUB");
        assertNotNull("DATE_SUB function should be registered", dateSubFn);

        Calendar cal = Calendar.getInstance();
        cal.set(2020, Calendar.JANUARY, 11); // January 11, 2020
        Date baseDate = cal.getTime();

        List<Object> args = Arrays.asList(baseDate, 10);
        CountDownLatch latch = new CountDownLatch(1);
        dateSubFn.execute(args, new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result of DATE_SUB should not be null", result);
                assertTrue("Result must be an instance of Date", result instanceof Date);
                cal.add(Calendar.DAY_OF_MONTH, -10);
                Date expected = cal.getTime();
                assertEquals("DATE_SUB should subtract 10 days", expected, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution of DATE_SUB failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testExtractYear() throws Exception {
        FunctionDefinition extractYearFn = context.getFunction("EXTRACT_YEAR");
        assertNotNull("EXTRACT_YEAR function should be registered", extractYearFn);

        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.AUGUST, 15);
        Date date = cal.getTime();

        CountDownLatch latch = new CountDownLatch(1);
        extractYearFn.execute(Arrays.asList(date), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result of EXTRACT_YEAR should not be null", result);
                // The result should equal the year as an Integer.
                assertEquals("Extracted year should equal 2021", cal.get(Calendar.YEAR), result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution of EXTRACT_YEAR failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testExtractMonth() throws Exception {
        FunctionDefinition extractMonthFn = context.getFunction("EXTRACT_MONTH");
        assertNotNull("EXTRACT_MONTH function should be registered", extractMonthFn);

        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.AUGUST, 15);
        Date date = cal.getTime();

        CountDownLatch latch = new CountDownLatch(1);
        extractMonthFn.execute(Arrays.asList(date), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result of EXTRACT_MONTH should not be null", result);
                // Calendar.MONTH is 0-based, so add 1.
                int expectedMonth = cal.get(Calendar.MONTH) + 1;
                assertEquals("Extracted month should equal " + expectedMonth, expectedMonth, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution of EXTRACT_MONTH failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testExtractDay() throws Exception {
        FunctionDefinition extractDayFn = context.getFunction("EXTRACT_DAY");
        assertNotNull("EXTRACT_DAY function should be registered", extractDayFn);

        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.AUGUST, 15);
        Date date = cal.getTime();

        CountDownLatch latch = new CountDownLatch(1);
        extractDayFn.execute(Arrays.asList(date), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result of EXTRACT_DAY should not be null", result);
                int expectedDay = cal.get(Calendar.DAY_OF_MONTH);
                assertEquals("Extracted day should equal " + expectedDay, expectedDay, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution of EXTRACT_DAY failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }

    @Test
    public void testDateDiff() throws Exception {
        FunctionDefinition dateDiffFn = context.getFunction("DATEDIFF");
        assertNotNull("DATEDIFF function should be registered", dateDiffFn);

        Calendar cal1 = Calendar.getInstance();
        cal1.set(2021, Calendar.JANUARY, 15);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2021, Calendar.JANUARY, 10);

        CountDownLatch latch = new CountDownLatch(1);
        dateDiffFn.execute(Arrays.asList(cal1.getTime(), cal2.getTime()), new ActionListener<Object>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result of DATEDIFF should not be null", result);
                // Expect the difference to be 5 days.
                assertEquals("DATEDIFF should return 5", 5L, result);
                latch.countDown();
            }
            @Override
            public void onFailure(Exception e) {
                fail("Execution of DATEDIFF failed: " + e.getMessage());
                latch.countDown();
            }
        });
        latch.await();
    }
}
