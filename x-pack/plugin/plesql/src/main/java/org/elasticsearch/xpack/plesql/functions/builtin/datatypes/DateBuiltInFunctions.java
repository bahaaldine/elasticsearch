/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.functions.builtin.datatypes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.action.ActionListener;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DateBuiltInFunctions {
    private static final Logger LOGGER = LogManager.getLogger(DateBuiltInFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Date built-in functions");
        // CURRENT_DATE: returns current date with time zeroed.
        context.declareFunction("CURRENT_DATE",
            Collections.emptyList(),
            new BuiltInFunctionDefinition("CURRENT_DATE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 0) {
                    listener.onFailure(new RuntimeException("CURRENT_DATE expects no arguments"));
                } else {
                    Calendar cal = Calendar.getInstance();
                    // Zero out the time fields.
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    listener.onResponse(cal.getTime());
                }
            })
        );

        // CURRENT_TIMESTAMP: returns the current date and time.
        context.declareFunction("CURRENT_TIMESTAMP",
            Collections.emptyList(),
            new BuiltInFunctionDefinition("CURRENT_TIMESTAMP", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 0) {
                    listener.onFailure(new RuntimeException("CURRENT_TIMESTAMP expects no arguments"));
                } else {
                    listener.onResponse(new Date());
                }
            })
        );

        // DATE_ADD: adds a given number of days to a date.
        context.declareFunction("DATE_ADD",
            // Expects two parameters: a date and a number of days.
            java.util.Arrays.asList(
                new Parameter("date", "DATE", ParameterMode.IN),
                new Parameter("days", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATE_ADD", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("DATE_ADD expects two arguments: a date and a number of days"));
                } else {
                    Date date = (Date) args.get(0);
                    Number days = (Number) args.get(1);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    cal.add(Calendar.DAY_OF_MONTH, days.intValue());
                    listener.onResponse(cal.getTime());
                }
            })
        );

        // DATE_SUB: subtracts a given number of days from a date.
        context.declareFunction("DATE_SUB",
            // Expects two parameters: a date and a number of days.
            java.util.Arrays.asList(
                new Parameter("date", "DATE", ParameterMode.IN),
                new Parameter("days", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATE_SUB", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("DATE_SUB expects two arguments: a date and a number of days"));
                } else {
                    Date date = (Date) args.get(0);
                    Number days = (Number) args.get(1);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    cal.add(Calendar.DAY_OF_MONTH, -days.intValue());
                    listener.onResponse(cal.getTime());
                }
            })
        );

        // EXTRACT_YEAR: extracts the year component from a date.
        context.declareFunction("EXTRACT_YEAR",
            // Expects one parameter: a date.
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_YEAR", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("EXTRACT_YEAR expects one argument"));
                } else {
                    Date date = (Date) args.get(0);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    listener.onResponse(cal.get(Calendar.YEAR));
                }
            })
        );

        // EXTRACT_MONTH: extracts the month (1-12) from a date.
        context.declareFunction("EXTRACT_MONTH",
            // Expects one parameter: a date.
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_MONTH", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("EXTRACT_MONTH expects one argument"));
                } else {
                    Date date = (Date) args.get(0);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    // Calendar.MONTH is 0-based, so add 1 to yield 1 to 12.
                    listener.onResponse(cal.get(Calendar.MONTH) + 1);
                }
            })
        );

        // EXTRACT_DAY: extracts the day of month from a date.
        context.declareFunction("EXTRACT_DAY",
            // Expects one parameter: a date.
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_DAY", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("EXTRACT_DAY expects one argument"));
                } else {
                    Date date = (Date) args.get(0);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    listener.onResponse(cal.get(Calendar.DAY_OF_MONTH));
                }
            })
        );

        // DATEDIFF: returns the difference in days between two dates.
        context.declareFunction("DATEDIFF",
            // Expects two parameters: date1 and date2.
            java.util.Arrays.asList(
                new Parameter("date1", "DATE", ParameterMode.IN),
                new Parameter("date2", "DATE", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATEDIFF", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("DATEDIFF expects two arguments"));
                } else {
                    Date date1 = (Date) args.get(0);
                    Date date2 = (Date) args.get(1);

                    // Normalize both dates to midnight
                    Calendar cal1 = Calendar.getInstance();
                    cal1.setTime(date1);
                    cal1.set(Calendar.HOUR_OF_DAY, 0);
                    cal1.set(Calendar.MINUTE, 0);
                    cal1.set(Calendar.SECOND, 0);
                    cal1.set(Calendar.MILLISECOND, 0);

                    Calendar cal2 = Calendar.getInstance();
                    cal2.setTime(date2);
                    cal2.set(Calendar.HOUR_OF_DAY, 0);
                    cal2.set(Calendar.MINUTE, 0);
                    cal2.set(Calendar.SECOND, 0);
                    cal2.set(Calendar.MILLISECOND, 0);

                    long diffInMillis = cal1.getTimeInMillis() - cal2.getTimeInMillis();
                    long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                    listener.onResponse(diffInDays);
                }
            })
        );
    }
}
