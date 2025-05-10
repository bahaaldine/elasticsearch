/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.functions.builtin.datatypes;
import org.elasticsearch.xpack.plesql.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.plesql.functions.api.FunctionCategory;
import org.elasticsearch.xpack.plesql.functions.api.FunctionSpec;
import org.elasticsearch.xpack.plesql.functions.api.FunctionReturn;
import org.elasticsearch.xpack.plesql.functions.api.FunctionParam;

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

/**
 * Provides built-in functions for working with dates.
 * Includes utilities for retrieving the current date/time, adding/subtracting days,
 * and extracting specific date components.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.DATE,
    description = "Date and time manipulation functions including extraction, arithmetic, and current timestamps."
)
public class DateBuiltInFunctions {
    private static final Logger LOGGER = LogManager.getLogger(DateBuiltInFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Date built-in functions");
        registerCurrentDate(context);
        registerCurrentTimestamp(context);
        registerDateAdd(context);
        registerDateSub(context);
        registerExtractYear(context);
        registerExtractMonth(context);
        registerExtractDay(context);
        registerDateDiff(context);
    }

    @FunctionSpec(
        name = "CURRENT_DATE",
        description = "Returns the current date with time set to midnight.",
        parameters = {},
        returnType = @FunctionReturn(type = "DATE", description = "The current date with time zeroed."),
        examples = { "CURRENT_DATE() -> 2024-05-10" },
        category = FunctionCategory.DATE
    )
    public static void registerCurrentDate(ExecutionContext context) {
        context.declareFunction("CURRENT_DATE",
            Collections.emptyList(),
            new BuiltInFunctionDefinition("CURRENT_DATE", DateBuiltInFunctions::currentDateFunctionAsync)
        );
    }

    @FunctionSpec(
        name = "CURRENT_TIMESTAMP",
        description = "Returns the current date and time.",
        parameters = {},
        returnType = @FunctionReturn(type = "DATE", description = "The current date and time."),
        examples = { "CURRENT_TIMESTAMP() -> 2024-05-10T12:34:56" },
        category = FunctionCategory.DATE
    )
    public static void registerCurrentTimestamp(ExecutionContext context) {
        context.declareFunction("CURRENT_TIMESTAMP",
            Collections.emptyList(),
            new BuiltInFunctionDefinition("CURRENT_TIMESTAMP", DateBuiltInFunctions::currentTimestampFunctionAsync)
        );
    }

    @FunctionSpec(
        name = "DATE_ADD",
        description = "Adds a given number of days to a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The original date."),
            @FunctionParam(name = "days", type = "NUMBER", description = "Number of days to add.")
        },
        returnType = @FunctionReturn(type = "DATE", description = "The resulting date after addition."),
        examples = { "DATE_ADD('2024-05-10', 3) -> 2024-05-13" },
        category = FunctionCategory.DATE
    )
    public static void registerDateAdd(ExecutionContext context) {
        context.declareFunction("DATE_ADD",
            java.util.Arrays.asList(
                new Parameter("date", "DATE", ParameterMode.IN),
                new Parameter("days", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATE_ADD", DateBuiltInFunctions::dateAddFunctionAsync)
        );
    }

    @FunctionSpec(
        name = "DATE_SUB",
        description = "Subtracts a given number of days from a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The original date."),
            @FunctionParam(name = "days", type = "NUMBER", description = "Number of days to subtract.")
        },
        returnType = @FunctionReturn(type = "DATE", description = "The resulting date after subtraction."),
        examples = { "DATE_SUB('2024-05-10', 2) -> 2024-05-08" },
        category = FunctionCategory.DATE
    )
    public static void registerDateSub(ExecutionContext context) {
        context.declareFunction("DATE_SUB",
            java.util.Arrays.asList(
                new Parameter("date", "DATE", ParameterMode.IN),
                new Parameter("days", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATE_SUB", DateBuiltInFunctions::dateSubFunctionAsync)
        );
    }

    @FunctionSpec(
        name = "EXTRACT_YEAR",
        description = "Extracts the year component from a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The date to extract from.")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "The year component."),
        examples = { "EXTRACT_YEAR('2024-05-10') -> 2024" },
        category = FunctionCategory.DATE
    )
    public static void registerExtractYear(ExecutionContext context) {
        context.declareFunction("EXTRACT_YEAR",
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_YEAR", DateBuiltInFunctions::extractYearFunctionAsync)
        );
    }

    @FunctionSpec(
        name = "EXTRACT_MONTH",
        description = "Extracts the month (1-12) from a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The date to extract from.")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "The month component (1-12)."),
        examples = { "EXTRACT_MONTH('2024-05-10') -> 5" },
        category = FunctionCategory.DATE
    )
    public static void registerExtractMonth(ExecutionContext context) {
        context.declareFunction("EXTRACT_MONTH",
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_MONTH", DateBuiltInFunctions::extractMonthFunctionAsync)
        );
    }

    @FunctionSpec(
        name = "EXTRACT_DAY",
        description = "Extracts the day of month from a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The date to extract from.")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "The day of the month."),
        examples = { "EXTRACT_DAY('2024-05-10') -> 10" },
        category = FunctionCategory.DATE
    )
    public static void registerExtractDay(ExecutionContext context) {
        context.declareFunction("EXTRACT_DAY",
            Collections.singletonList(new Parameter("date", "DATE", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXTRACT_DAY", DateBuiltInFunctions::extractDayFunctionAsync)
        );
    }

    @FunctionSpec(
        name = "DATEDIFF",
        description = "Returns the difference in days between two dates.",
        parameters = {
            @FunctionParam(name = "date1", type = "DATE", description = "The first date."),
            @FunctionParam(name = "date2", type = "DATE", description = "The second date.")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Number of days between date1 and date2."),
        examples = { "DATEDIFF('2024-05-13', '2024-05-10') -> 3" },
        category = FunctionCategory.DATE
    )
    public static void registerDateDiff(ExecutionContext context) {
        context.declareFunction("DATEDIFF",
            java.util.Arrays.asList(
                new Parameter("date1", "DATE", ParameterMode.IN),
                new Parameter("date2", "DATE", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DATEDIFF", DateBuiltInFunctions::dateDiffFunctionAsync)
        );
    }

    @FunctionSpec(
        name = "CURRENT_DATE",
        description = "Returns the current date with time set to midnight.",
        parameters = {},
        returnType = @FunctionReturn(type = "DATE", description = "The current date with time zeroed."),
        examples = { "CURRENT_DATE() -> 2024-05-10" },
        category = FunctionCategory.DATE
    )
    private static void currentDateFunctionAsync(List<Object> args, ActionListener<Object> listener) {
        try {
            listener.onResponse(currentDateFunction(args));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    private static Object currentDateFunction(List<Object> args) {
        if (args.size() != 0) throw new IllegalArgumentException("CURRENT_DATE expects no arguments");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @FunctionSpec(
        name = "CURRENT_TIMESTAMP",
        description = "Returns the current date and time.",
        parameters = {},
        returnType = @FunctionReturn(type = "DATE", description = "The current date and time."),
        examples = { "CURRENT_TIMESTAMP() -> 2024-05-10T12:34:56" },
        category = FunctionCategory.DATE
    )
    private static void currentTimestampFunctionAsync(List<Object> args, ActionListener<Object> listener) {
        try {
            listener.onResponse(currentTimestampFunction(args));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    private static Object currentTimestampFunction(List<Object> args) {
        if (args.size() != 0) throw new IllegalArgumentException("CURRENT_TIMESTAMP expects no arguments");
        return new Date();
    }

    @FunctionSpec(
        name = "DATE_ADD",
        description = "Adds a given number of days to a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The original date."),
            @FunctionParam(name = "days", type = "NUMBER", description = "Number of days to add.")
        },
        returnType = @FunctionReturn(type = "DATE", description = "The resulting date after addition."),
        examples = { "DATE_ADD('2024-05-10', 3) -> 2024-05-13" },
        category = FunctionCategory.DATE
    )
    private static void dateAddFunctionAsync(List<Object> args, ActionListener<Object> listener) {
        try {
            listener.onResponse(dateAddFunction(args));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    private static Object dateAddFunction(List<Object> args) {
        if (args.size() != 2) throw new IllegalArgumentException("DATE_ADD expects two arguments: a date and a number of days");
        Date date = (Date) args.get(0);
        Number days = (Number) args.get(1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days.intValue());
        return cal.getTime();
    }

    @FunctionSpec(
        name = "DATE_SUB",
        description = "Subtracts a given number of days from a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The original date."),
            @FunctionParam(name = "days", type = "NUMBER", description = "Number of days to subtract.")
        },
        returnType = @FunctionReturn(type = "DATE", description = "The resulting date after subtraction."),
        examples = { "DATE_SUB('2024-05-10', 2) -> 2024-05-08" },
        category = FunctionCategory.DATE
    )
    private static void dateSubFunctionAsync(List<Object> args, ActionListener<Object> listener) {
        try {
            listener.onResponse(dateSubFunction(args));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    private static Object dateSubFunction(List<Object> args) {
        if (args.size() != 2) throw new IllegalArgumentException("DATE_SUB expects two arguments: a date and a number of days");
        Date date = (Date) args.get(0);
        Number days = (Number) args.get(1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, -days.intValue());
        return cal.getTime();
    }

    @FunctionSpec(
        name = "EXTRACT_YEAR",
        description = "Extracts the year component from a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The date to extract from.")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "The year component."),
        examples = { "EXTRACT_YEAR('2024-05-10') -> 2024" },
        category = FunctionCategory.DATE
    )
    private static void extractYearFunctionAsync(List<Object> args, ActionListener<Object> listener) {
        try {
            listener.onResponse(extractYearFunction(args));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    private static Object extractYearFunction(List<Object> args) {
        if (args.size() != 1) throw new IllegalArgumentException("EXTRACT_YEAR expects one argument");
        Date date = (Date) args.get(0);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    @FunctionSpec(
        name = "EXTRACT_MONTH",
        description = "Extracts the month (1-12) from a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The date to extract from.")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "The month component (1-12)."),
        examples = { "EXTRACT_MONTH('2024-05-10') -> 5" },
        category = FunctionCategory.DATE
    )
    private static void extractMonthFunctionAsync(List<Object> args, ActionListener<Object> listener) {
        try {
            listener.onResponse(extractMonthFunction(args));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    private static Object extractMonthFunction(List<Object> args) {
        if (args.size() != 1) throw new IllegalArgumentException("EXTRACT_MONTH expects one argument");
        Date date = (Date) args.get(0);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // Calendar.MONTH is 0-based, so add 1 to yield 1 to 12.
        return cal.get(Calendar.MONTH) + 1;
    }

    @FunctionSpec(
        name = "EXTRACT_DAY",
        description = "Extracts the day of month from a date.",
        parameters = {
            @FunctionParam(name = "date", type = "DATE", description = "The date to extract from.")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "The day of the month."),
        examples = { "EXTRACT_DAY('2024-05-10') -> 10" },
        category = FunctionCategory.DATE
    )
    private static void extractDayFunctionAsync(List<Object> args, ActionListener<Object> listener) {
        try {
            listener.onResponse(extractDayFunction(args));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    private static Object extractDayFunction(List<Object> args) {
        if (args.size() != 1) throw new IllegalArgumentException("EXTRACT_DAY expects one argument");
        Date date = (Date) args.get(0);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    @FunctionSpec(
        name = "DATEDIFF",
        description = "Returns the difference in days between two dates.",
        parameters = {
            @FunctionParam(name = "date1", type = "DATE", description = "The first date."),
            @FunctionParam(name = "date2", type = "DATE", description = "The second date.")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Number of days between date1 and date2."),
        examples = { "DATEDIFF('2024-05-13', '2024-05-10') -> 3" },
        category = FunctionCategory.DATE
    )
    private static void dateDiffFunctionAsync(List<Object> args, ActionListener<Object> listener) {
        try {
            listener.onResponse(dateDiffFunction(args));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
    private static Object dateDiffFunction(List<Object> args) {
        if (args.size() != 2) throw new IllegalArgumentException("DATEDIFF expects two arguments");
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
    }
}
