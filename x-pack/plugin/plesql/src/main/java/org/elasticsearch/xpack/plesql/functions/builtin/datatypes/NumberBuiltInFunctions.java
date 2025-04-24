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
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.action.ActionListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NumberBuiltInFunctions {
    private static final Logger LOGGER = LogManager.getLogger(NumberBuiltInFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Number built-in functions");

        // ABS: returns the absolute value of a number.
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

        // CEIL: returns the smallest integer value greater than or equal to the number.
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

        // FLOOR: returns the largest integer value less than or equal to the number.
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

        // ROUND: rounds the number. If only one argument is given, then the second parameter is ignored.
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

        // POWER: raises the first argument to the power of the second.
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

        // SQRT: returns the square root of the number.
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

        // LOG: returns the natural logarithm, or with base if provided.
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

        // EXP: returns the exponential of the number.
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

        // MOD: returns the remainder of the division of the first argument by the second.
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

        // SIGN: returns 1 if positive, -1 if negative, and 0 if zero.
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

        // TRUNC: truncates a number. If only one argument is provided, scale is ignored.
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
