/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.escript.functions.builtin.datatypes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@FunctionCollectionSpec(description = "Built-in functions for number data type", category = FunctionCategory.NUMBER)
public class NumberBuiltInFunctions {
    private static final Logger LOGGER = LogManager.getLogger(NumberBuiltInFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Number built-in functions");
        registerABS(context);
        registerCEIL(context);
        registerFLOOR(context);
        registerROUND(context);
        registerPOWER(context);
        registerSQRT(context);
        registerLOG(context);
        registerEXP(context);
        registerMOD(context);
        registerSIGN(context);
        registerTRUNC(context);
    }

    @FunctionSpec(
        name = "ABS",
        description = "Returns the absolute value of a number.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Absolute value of the input"),
        examples = {"ABS(-5) => 5"},
        category = FunctionCategory.NUMBER
    )
    public static void registerABS(ExecutionContext context) {
        context.declareFunction("ABS",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ABS", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("ABS expects one argument"));
                } else {
                    Number num = (Number) args.get(0);
                    listener.onResponse(Math.abs(num.doubleValue()));
                }
            })
        );
    }

    @FunctionSpec(
        name = "CEIL",
        description = "Returns the smallest integer value greater than or equal to the number.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Ceiling value of the input"),
        examples = {"CEIL(4.3) => 5.0"},
        category = FunctionCategory.NUMBER
    )
    public static void registerCEIL(ExecutionContext context) {
        context.declareFunction("CEIL",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("CEIL", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("CEIL expects one argument"));
                } else {
                    Number num = (Number) args.get(0);
                    listener.onResponse(Math.ceil(num.doubleValue()));
                }
            })
        );
    }

    @FunctionSpec(
        name = "FLOOR",
        description = "Returns the largest integer value less than or equal to the number.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Floor value of the input"),
        examples = {"FLOOR(4.7) => 4.0"},
        category = FunctionCategory.NUMBER
    )
    public static void registerFLOOR(ExecutionContext context) {
        context.declareFunction("FLOOR",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("FLOOR", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("FLOOR expects one argument"));
                } else {
                    Number num = (Number) args.get(0);
                    listener.onResponse(Math.floor(num.doubleValue()));
                }
            })
        );
    }

    @FunctionSpec(
        name = "ROUND",
        description = "Rounds the number. If only one argument is given, then the second parameter is ignored.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number"),
            @FunctionParam(name = "scale", type = "NUMBER", description = "Number of decimal places to round to (optional)")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Rounded value of the input"),
        examples = {"ROUND(4.567) => 5.0", "ROUND(4.567, 2) => 4.57"},
        category = FunctionCategory.NUMBER
    )
    public static void registerROUND(ExecutionContext context) {
        context.declareFunction("ROUND",
            Arrays.asList(
                new Parameter("input", "NUMBER", ParameterMode.IN),
                new Parameter("scale", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ROUND", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() < 1 || args.size() > 2) {
                    listener.onFailure(new RuntimeException("ROUND expects one or two arguments"));
                } else {
                    Number num = (Number) args.get(0);
                    if (args.size() == 1) {
                        listener.onResponse((double) Math.round(num.doubleValue()));
                    } else {
                        Number scale = (Number) args.get(1);
                        int places = scale.intValue();
                        double factor = Math.pow(10, places);
                        listener.onResponse(Math.round(num.doubleValue() * factor) / factor);
                    }
                }
            })
        );
    }

    @FunctionSpec(
        name = "POWER",
        description = "Raises the first argument to the power of the second.",
        parameters = {
            @FunctionParam(name = "base", type = "NUMBER", description = "Base number"),
            @FunctionParam(name = "exponent", type = "NUMBER", description = "Exponent")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Result of base raised to the exponent"),
        examples = {"POWER(2, 3) => 8.0"},
        category = FunctionCategory.NUMBER
    )
    public static void registerPOWER(ExecutionContext context) {
        context.declareFunction("POWER",
            Arrays.asList(
                new Parameter("base", "NUMBER", ParameterMode.IN),
                new Parameter("exponent", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("POWER", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("POWER expects two arguments"));
                } else {
                    Number base = (Number) args.get(0);
                    Number exponent = (Number) args.get(1);
                    listener.onResponse(Math.pow(base.doubleValue(), exponent.doubleValue()));
                }
            })
        );
    }

    @FunctionSpec(
        name = "SQRT",
        description = "Returns the square root of the number.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Square root of the input"),
        examples = {"SQRT(9) => 3.0"},
        category = FunctionCategory.NUMBER
    )
    public static void registerSQRT(ExecutionContext context) {
        context.declareFunction("SQRT",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SQRT", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("SQRT expects one argument"));
                } else {
                    Number num = (Number) args.get(0);
                    listener.onResponse(Math.sqrt(num.doubleValue()));
                }
            })
        );
    }

    @FunctionSpec(
        name = "LOG",
        description = "Returns the natural logarithm, or with base if provided.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number"),
            @FunctionParam(name = "base", type = "NUMBER", description = "Logarithm base (optional)")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Logarithm value"),
        examples = {"LOG(10) => 2.302585092994046", "LOG(8, 2) => 3.0"},
        category = FunctionCategory.NUMBER
    )
    public static void registerLOG(ExecutionContext context) {
        context.declareFunction("LOG",
            Arrays.asList(
                new Parameter("input", "NUMBER", ParameterMode.IN),
                new Parameter("base", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LOG", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() < 1 || args.size() > 2) {
                    listener.onFailure(new RuntimeException("LOG expects one or two arguments"));
                } else {
                    Number num = (Number) args.get(0);
                    if (args.size() == 1) {
                        listener.onResponse(Math.log(num.doubleValue()));
                    } else {
                        Number base = (Number) args.get(1);
                        listener.onResponse(Math.log(num.doubleValue()) / Math.log(base.doubleValue()));
                    }
                }
            })
        );
    }

    @FunctionSpec(
        name = "EXP",
        description = "Returns the exponential of the number.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Exponential value of the input"),
        examples = {"EXP(1) => 2.718281828459045"},
        category = FunctionCategory.NUMBER
    )
    public static void registerEXP(ExecutionContext context) {
        context.declareFunction("EXP",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXP", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("EXP expects one argument"));
                } else {
                    Number num = (Number) args.get(0);
                    listener.onResponse(Math.exp(num.doubleValue()));
                }
            })
        );
    }

    @FunctionSpec(
        name = "MOD",
        description = "Returns the remainder of the division of the first argument by the second.",
        parameters = {
            @FunctionParam(name = "a", type = "NUMBER", description = "Dividend"),
            @FunctionParam(name = "b", type = "NUMBER", description = "Divisor")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Remainder after division"),
        examples = {"MOD(10, 3) => 1.0"},
        category = FunctionCategory.NUMBER
    )
    public static void registerMOD(ExecutionContext context) {
        context.declareFunction("MOD",
            Arrays.asList(
                new Parameter("a", "NUMBER", ParameterMode.IN),
                new Parameter("b", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("MOD", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("MOD expects two arguments"));
                } else {
                    Number a = (Number) args.get(0);
                    Number b = (Number) args.get(1);
                    if (b.doubleValue() == 0) {
                        listener.onFailure(new RuntimeException("MOD division by zero"));
                    } else {
                        listener.onResponse(a.doubleValue() % b.doubleValue());
                    }
                }
            })
        );
    }

    @FunctionSpec(
        name = "SIGN",
        description = "Returns 1 if positive, -1 if negative, and 0 if zero.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Sign of the input"),
        examples = {"SIGN(10) => 1", "SIGN(-5) => -1", "SIGN(0) => 0"},
        category = FunctionCategory.NUMBER
    )
    public static void registerSIGN(ExecutionContext context) {
        context.declareFunction("SIGN",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SIGN", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("SIGN expects one argument"));
                } else {
                    Number num = (Number) args.get(0);
                    double value = num.doubleValue();
                    if (value > 0) {
                        listener.onResponse(1);
                    } else if (value < 0) {
                        listener.onResponse(-1);
                    } else {
                        listener.onResponse(0);
                    }
                }
            })
        );
    }

    @FunctionSpec(
        name = "TRUNC",
        description = "Truncates a number. If only one argument is provided, scale is ignored.",
        parameters = {
            @FunctionParam(name = "input", type = "NUMBER", description = "Input number"),
            @FunctionParam(name = "scale", type = "NUMBER", description = "Number of decimal places to truncate to (optional)")
        },
        returnType = @FunctionReturn(type = "NUMBER", description = "Truncated value of the input"),
        examples = {"TRUNC(4.567) => 4.0", "TRUNC(4.567, 2) => 4.56"},
        category = FunctionCategory.NUMBER
    )
    public static void registerTRUNC(ExecutionContext context) {
        context.declareFunction("TRUNC",
            Arrays.asList(
                new Parameter("input", "NUMBER", ParameterMode.IN),
                new Parameter("scale", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("TRUNC", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() < 1 || args.size() > 2) {
                    listener.onFailure(new RuntimeException("TRUNC expects one or two arguments"));
                } else {
                    Number num = (Number) args.get(0);
                    if (args.size() == 1) {
                        listener.onResponse((double) ((int) num.doubleValue()));
                    } else {
                        Number scale = (Number) args.get(1);
                        int places = scale.intValue();
                        double factor = Math.pow(10, places);
                        listener.onResponse(Math.floor(num.doubleValue() * factor) / factor);
                    }
                }
            })
        );
    }
}
