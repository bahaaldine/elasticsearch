/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin.types;

import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DateBuiltInFunctions {

    public static void registerAll(ExecutionContext context) {
        // CURRENT_DATE: returns current date with time zeroed.
        context.declareFunction("CURRENT_DATE",
            Collections.emptyList(),
            new BuiltInFunctionDefinition("CURRENT_DATE",
                (org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 0) {
                    throw new RuntimeException("CURRENT_DATE expects no arguments");
                }
                Calendar cal = Calendar.getInstance();
                // Zero out the time fields.
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTime();
            })
        );

        // CURRENT_TIMESTAMP: returns the current date and time.
        context.declareFunction("CURRENT_TIMESTAMP",
            Collections.emptyList(),
            new BuiltInFunctionDefinition("CURRENT_TIMESTAMP",
                (org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 0) {
                    throw new RuntimeException("CURRENT_TIMESTAMP expects no arguments");
                }
                return new Date();
            })
        );

        // DATE_ADD: adds a given number of days to a date.
        context.declareFunction("DATE_ADD",
            // Expects two parameters: a date and a number of days.
            java.util.Arrays.asList(
                new Parameter("date", "DATE", ParameterMode.IN),
                new Parameter("days", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATE_ADD",
                (org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("DATE_ADD expects two arguments: a date and a number of days");
                }
                Date date = (Date) args.get(0);
                Number days = (Number) args.get(1);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.add(Calendar.DAY_OF_MONTH, days.intValue());
                return cal.getTime();
            })
        );

        // DATE_SUB: subtracts a given number of days from a date.
        context.declareFunction("DATE_SUB",
            // Expects two parameters: a date and a number of days.
            java.util.Arrays.asList(
                new Parameter("date", "DATE", ParameterMode.IN),
                new Parameter("days", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATE_SUB",
                (org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("DATE_SUB expects two arguments: a date and a number of days");
                }
                Date date = (Date) args.get(0);
                Number days = (Number) args.get(1);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.add(Calendar.DAY_OF_MONTH, -days.intValue());
                return cal.getTime();
            })
        );

        // EXTRACT_YEAR: extracts the year component from a date.
        context.declareFunction("EXTRACT_YEAR",
            // Expects one parameter: a date.
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_YEAR",
                (org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("EXTRACT_YEAR expects one argument");
                }
                Date date = (Date) args.get(0);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return cal.get(Calendar.YEAR);
            })
        );

        // EXTRACT_MONTH: extracts the month (1-12) from a date.
        context.declareFunction("EXTRACT_MONTH",
            // Expects one parameter: a date.
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_MONTH",
                (org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("EXTRACT_MONTH expects one argument");
                }
                Date date = (Date) args.get(0);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                // Calendar.MONTH is 0-based, so add 1 to yield 1 to 12.
                return cal.get(Calendar.MONTH) + 1;
            })
        );

        // EXTRACT_DAY: extracts the day of month from a date.
        context.declareFunction("EXTRACT_DAY",
            // Expects one parameter: a date.
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_DAY",
                (org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("EXTRACT_DAY expects one argument");
                }
                Date date = (Date) args.get(0);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return cal.get(Calendar.DAY_OF_MONTH);
            })
        );

        // DATEDIFF: returns the difference in days between two dates.
        context.declareFunction("DATEDIFF",
            // Expects two parameters: date1 and date2.
            java.util.Arrays.asList(
                new Parameter("date1", "DATE", ParameterMode.IN),
                new Parameter("date2", "DATE", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATEDIFF",
                (org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("DATEDIFF expects two arguments");
                }
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
                return diffInDays;
            })
        );
    }
}
