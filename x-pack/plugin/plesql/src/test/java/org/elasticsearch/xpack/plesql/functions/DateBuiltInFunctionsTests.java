/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.functions.builtin.DateBuiltInFunctions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateBuiltInFunctionsTests {

    private ExecutionContext context;

    @Before
    public void setup() {
        // Create a new global ExecutionContext and register all date functions.
        context = new ExecutionContext();
        DateBuiltInFunctions.registerAll(context);
    }

    @Test
    public void testCurrentDate() {
        BuiltInFunctionDefinition currentDateFn = context.getBuiltInFunction("CURRENT_DATE");
        Object result = currentDateFn.execute(Arrays.asList());
        // Verify that the returned date has time fields set to zero.
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) result);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    @Test
    public void testCurrentTimestamp() {
        BuiltInFunctionDefinition currentTimestampFn = context.getBuiltInFunction("CURRENT_TIMESTAMP");
        Object result = currentTimestampFn.execute(Arrays.asList());
        assertTrue(result instanceof Date);
    }

    @Test
    public void testDateAdd() {
        BuiltInFunctionDefinition dateAddFn = context.getBuiltInFunction("DATE_ADD");
        Calendar cal = Calendar.getInstance();
        cal.set(2020, Calendar.JANUARY, 1); // January 1, 2020
        Date baseDate = cal.getTime();
        // Add 10 days.
        Object result = dateAddFn.execute(Arrays.asList(baseDate, 10));
        cal.add(Calendar.DAY_OF_MONTH, 10);
        assertEquals(cal.getTime(), result);
    }

    @Test
    public void testDateSub() {
        BuiltInFunctionDefinition dateSubFn = context.getBuiltInFunction("DATE_SUB");
        Calendar cal = Calendar.getInstance();
        cal.set(2020, Calendar.JANUARY, 11); // January 11, 2020
        Date baseDate = cal.getTime();
        // Subtract 10 days.
        Object result = dateSubFn.execute(Arrays.asList(baseDate, 10));
        cal.add(Calendar.DAY_OF_MONTH, -10);
        assertEquals(cal.getTime(), result);
    }

    @Test
    public void testExtractYear() {
        BuiltInFunctionDefinition extractYearFn = context.getBuiltInFunction("EXTRACT_YEAR");
        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.AUGUST, 15);
        Date date = cal.getTime();
        Object result = extractYearFn.execute(Arrays.asList(date));
        assertEquals(cal.get(Calendar.YEAR), result);
    }

    @Test
    public void testExtractMonth() {
        BuiltInFunctionDefinition extractMonthFn = context.getBuiltInFunction("EXTRACT_MONTH");
        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.AUGUST, 15);
        Date date = cal.getTime();
        Object result = extractMonthFn.execute(Arrays.asList(date));
        // Calendar.MONTH is 0-based so add 1.
        assertEquals(cal.get(Calendar.MONTH) + 1, result);
    }

    @Test
    public void testExtractDay() {
        BuiltInFunctionDefinition extractDayFn = context.getBuiltInFunction("EXTRACT_DAY");
        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.AUGUST, 15);
        Date date = cal.getTime();
        Object result = extractDayFn.execute(Arrays.asList(date));
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), result);
    }

    @Test
    public void testDateDiff() {
        BuiltInFunctionDefinition dateDiffFn = context.getBuiltInFunction("DATEDIFF");
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2021, Calendar.JANUARY, 15);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2021, Calendar.JANUARY, 10);
        Object result = dateDiffFn.execute(Arrays.asList(cal1.getTime(), cal2.getTime()));
        // Difference should be 5 days.
        assertEquals(5L, result);
    }
}
