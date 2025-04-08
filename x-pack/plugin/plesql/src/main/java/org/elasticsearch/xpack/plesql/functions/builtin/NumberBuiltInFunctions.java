/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NumberBuiltInFunctions {

    public static void registerAll(ExecutionContext context) {

        // ABS: returns the absolute value of a number.
        context.declareFunction("ABS",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ABS", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("ABS expects one argument");
                }
                Number num = (Number) args.get(0);
                return Math.abs(num.doubleValue());
            })
        );

        // CEIL: returns the smallest integer value greater than or equal to the number.
        context.declareFunction("CEIL",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("CEIL", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("CEIL expects one argument");
                }
                Number num = (Number) args.get(0);
                return Math.ceil(num.doubleValue());
            })
        );

        // FLOOR: returns the largest integer value less than or equal to the number.
        context.declareFunction("FLOOR",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("FLOOR", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("FLOOR expects one argument");
                }
                Number num = (Number) args.get(0);
                return Math.floor(num.doubleValue());
            })
        );

        // ROUND: rounds the number. We choose a canonical signature with two parameters.
        // If only one argument is given, then the second parameter is ignored inside the lambda.
        context.declareFunction("ROUND",
            Arrays.asList(
                new Parameter("input", "NUMBER", ParameterMode.IN),
                new Parameter("scale", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ROUND", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() < 1 || args.size() > 2) {
                    throw new RuntimeException("ROUND expects one or two arguments");
                }
                Number num = (Number) args.get(0);
                if (args.size() == 1) {
                    return (double) Math.round(num.doubleValue());
                } else {
                    Number scale = (Number) args.get(1);
                    int places = scale.intValue();
                    double factor = Math.pow(10, places);
                    return Math.round(num.doubleValue() * factor) / factor;
                }
            })
        );

        // POWER: raises the first argument to the power of the second.
        context.declareFunction("POWER",
            Arrays.asList(
                new Parameter("base", "NUMBER", ParameterMode.IN),
                new Parameter("exponent", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("POWER", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("POWER expects two arguments");
                }
                Number base = (Number) args.get(0);
                Number exponent = (Number) args.get(1);
                return Math.pow(base.doubleValue(), exponent.doubleValue());
            })
        );

        // SQRT: returns the square root of the number.
        context.declareFunction("SQRT",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SQRT", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("SQRT expects one argument");
                }
                Number num = (Number) args.get(0);
                return Math.sqrt(num.doubleValue());
            })
        );

        // LOG: returns the natural logarithm of the number.
        // We register LOG with a canonical signature of 2 parameters.
        context.declareFunction("LOG",
            Arrays.asList(
                new Parameter("input", "NUMBER", ParameterMode.IN),
                new Parameter("base", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LOG", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() < 1 || args.size() > 2) {
                    throw new RuntimeException("LOG expects one or two arguments");
                }
                Number num = (Number) args.get(0);
                if (args.size() == 1) {
                    return Math.log(num.doubleValue());
                } else {
                    Number base = (Number) args.get(1);
                    return Math.log(num.doubleValue()) / Math.log(base.doubleValue());
                }
            })
        );

        // EXP: returns the exponential of the number.
        context.declareFunction("EXP",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("EXP", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("EXP expects one argument");
                }
                Number num = (Number) args.get(0);
                return Math.exp(num.doubleValue());
            })
        );

        // MOD: returns the remainder of the division of the first argument by the second.
        context.declareFunction("MOD",
            Arrays.asList(
                new Parameter("a", "NUMBER", ParameterMode.IN),
                new Parameter("b", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("MOD", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("MOD expects two arguments");
                }
                Number a = (Number) args.get(0);
                Number b = (Number) args.get(1);
                if (b.doubleValue() == 0) {
                    throw new RuntimeException("MOD division by zero");
                }
                return a.doubleValue() % b.doubleValue();
            })
        );

        // SIGN: returns 1 if positive, -1 if negative, and 0 if zero.
        context.declareFunction("SIGN",
            Collections.singletonList(new Parameter("input", "NUMBER", ParameterMode.IN)),
            new BuiltInFunctionDefinition("SIGN", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("SIGN expects one argument");
                }
                Number num = (Number) args.get(0);
                double value = num.doubleValue();
                if (value > 0) {
                    return 1;
                } else if (value < 0) {
                    return -1;
                } else {
                    return 0;
                }
            })
        );

        // TRUNC: truncates a number. Canonical signature: two parameters.
        context.declareFunction("TRUNC",
            Arrays.asList(
                new Parameter("input", "NUMBER", ParameterMode.IN),
                new Parameter("scale", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("TRUNC", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() < 1 || args.size() > 2) {
                    throw new RuntimeException("TRUNC expects one or two arguments");
                }
                Number num = (Number) args.get(0);
                if (args.size() == 1) {
                    return (double) ((int) num.doubleValue());
                } else {
                    Number scale = (Number) args.get(1);
                    int places = scale.intValue();
                    double factor = Math.pow(10, places);
                    return Math.floor(num.doubleValue() * factor) / factor;
                }
            })
        );
    }
}
