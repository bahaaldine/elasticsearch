/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin.datatypes;

import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.action.ActionListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Registers all built‑in functions (mostly string functions) in the given ExecutionContext.
 */
public class StringBuiltInFunctions {

    public static void registerAll(ExecutionContext context) {
        // LENGTH: expects one argument of type STRING.
        context.declareFunction("LENGTH",
            Collections.singletonList(new Parameter("input", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("LENGTH", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("LENGTH expects one argument"));
                } else {
                    listener.onResponse(args.get(0).toString().length());
                }
            })
        );

        // SUBSTR: for simplicity, we register with three parameters:
        //   input (STRING), start (NUMBER), length (NUMBER)
        // The lambda accepts 2 or 3 arguments.
        context.declareFunction("SUBSTR",
            Arrays.asList(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("start", "NUMBER", ParameterMode.IN),
                new Parameter("length", "NUMBER", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SUBSTR", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() < 2 || args.size() > 3) {
                    listener.onFailure(new RuntimeException("SUBSTR expects 2 or 3 arguments"));
                } else {
                    String s = args.get(0).toString();
                    int start = ((Number) args.get(1)).intValue();
                    if (args.size() == 2) {
                        listener.onResponse(s.substring(start - 1));
                    } else {
                        int length = ((Number) args.get(2)).intValue();
                        listener.onResponse(s.substring(start - 1, Math.min(s.length(), start - 1 + length)));
                    }
                }
            })
        );

        // UPPER: expects one STRING argument.
        context.declareFunction("UPPER",
            Collections.singletonList(new Parameter("input", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("UPPER", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("UPPER expects one argument"));
                } else {
                    listener.onResponse(args.get(0).toString().toUpperCase());
                }
            })
        );

        // LOWER: expects one STRING argument.
        context.declareFunction("LOWER",
            Collections.singletonList(new Parameter("input", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("LOWER", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("LOWER expects one argument"));
                } else {
                    listener.onResponse(args.get(0).toString().toLowerCase());
                }
            })
        );

        // TRIM: expects one STRING argument.
        context.declareFunction("TRIM",
            Collections.singletonList(new Parameter("input", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("TRIM", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("TRIM expects one argument, but " + args.size() + " were provided."));
                } else {
                    listener.onResponse(args.get(0).toString().trim());
                }
            })
        );

        // LTRIM: expects one STRING argument.
        context.declareFunction("LTRIM",
            Collections.singletonList(new Parameter("input", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("LTRIM", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("LTRIM expects one argument"));
                } else {
                    listener.onResponse(args.get(0).toString().replaceAll("^\\s+", ""));
                }
            })
        );

        // RTRIM: expects one STRING argument.
        context.declareFunction("RTRIM",
            Collections.singletonList(new Parameter("input", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("RTRIM", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("RTRIM expects one argument"));
                } else {
                    listener.onResponse(args.get(0).toString().replaceAll("\\s+$", ""));
                }
            })
        );

        // REPLACE: expects three STRING arguments.
        context.declareFunction("REPLACE",
            Arrays.asList(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("target", "STRING", ParameterMode.IN),
                new Parameter("replacement", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("REPLACE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("REPLACE expects three arguments"));
                } else {
                    String s = args.get(0).toString();
                    String target = args.get(1).toString();
                    String replacement = args.get(2).toString();
                    listener.onResponse(s.replace(target, replacement));
                }
            })
        );

        // INSTR: expects two STRING arguments.
        context.declareFunction("INSTR",
            Arrays.asList(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("substring", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("INSTR", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("INSTR expects two arguments"));
                } else {
                    String s = args.get(0).toString();
                    String sub = args.get(1).toString();
                    int pos = s.indexOf(sub);
                    listener.onResponse(pos >= 0 ? pos + 1 : 0);
                }
            })
        );

        // LPAD: expects three arguments: STRING, NUMBER, STRING.
        context.declareFunction("LPAD",
            Arrays.asList(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("totalLength", "NUMBER", ParameterMode.IN),
                new Parameter("padStr", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("LPAD", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("LPAD expects three arguments"));
                } else {
                    String s = args.get(0).toString();
                    int totalLength = ((Number) args.get(1)).intValue();
                    String padStr = args.get(2).toString();
                    StringBuilder sb = new StringBuilder();
                    while (sb.length() + s.length() < totalLength) {
                        sb.append(padStr);
                    }
                    String pad = sb.toString();
                    if (pad.length() > totalLength - s.length()) {
                        pad = pad.substring(0, totalLength - s.length());
                    }
                    listener.onResponse(pad + s);
                }
            })
        );

        // RPAD: expects three arguments: STRING, NUMBER, STRING.
        context.declareFunction("RPAD",
            Arrays.asList(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("totalLength", "NUMBER", ParameterMode.IN),
                new Parameter("padStr", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("RPAD", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("RPAD expects three arguments"));
                } else {
                    String s = args.get(0).toString();
                    int totalLength = ((Number) args.get(1)).intValue();
                    String padStr = args.get(2).toString();
                    StringBuilder sb = new StringBuilder(s);
                    while (sb.length() < totalLength) {
                        sb.append(padStr);
                    }
                    listener.onResponse(sb.substring(0, totalLength));
                }
            })
        );

        // SPLIT: expects two STRING arguments.
        context.declareFunction("SPLIT",
            Arrays.asList(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("delimiter", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("SPLIT", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("SPLIT expects two arguments"));
                } else {
                    String s = args.get(0).toString();
                    String delimiter = args.get(1).toString();
                    String[] parts = s.split(Pattern.quote(delimiter));
                    listener.onResponse(Arrays.asList(parts));
                }
            })
        );

        // Concatenation operator "||": expects two arguments.
        context.declareFunction("||",
            Arrays.asList(
                new Parameter("left", "STRING", ParameterMode.IN),
                new Parameter("right", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("||", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("|| expects exactly two arguments"));
                } else {
                    listener.onResponse(args.get(0).toString() + args.get(1).toString());
                }
            })
        );

        // REGEXP_REPLACE: expects three STRING arguments.
        context.declareFunction("REGEXP_REPLACE",
            Arrays.asList(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("regex", "STRING", ParameterMode.IN),
                new Parameter("replacement", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("REGEXP_REPLACE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 3) {
                    listener.onFailure(new RuntimeException("REGEXP_REPLACE expects three arguments"));
                } else {
                    String s = args.get(0).toString();
                    String regex = args.get(1).toString();
                    String replacement = args.get(2).toString();
                    listener.onResponse(s.replaceAll(regex, replacement));
                }
            })
        );

        // REGEXP_SUBSTR: expects two STRING arguments.
        context.declareFunction("REGEXP_SUBSTR",
            Arrays.asList(
                new Parameter("input", "STRING", ParameterMode.IN),
                new Parameter("regex", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("REGEXP_SUBSTR", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("REGEXP_SUBSTR expects two arguments"));
                } else {
                    String s = args.get(0).toString();
                    String regex = args.get(1).toString();
                    java.util.regex.Matcher matcher = Pattern.compile(regex).matcher(s);
                    if (matcher.find()) {
                        listener.onResponse(matcher.group());
                    } else {
                        listener.onResponse("");
                    }
                }
            })
        );

        // REVERSE: expects one STRING argument.
        context.declareFunction("REVERSE",
            Collections.singletonList(new Parameter("input", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("REVERSE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("REVERSE expects one argument"));
                } else {
                    String s = args.get(0).toString();
                    listener.onResponse(new StringBuilder(s).reverse().toString());
                }
            })
        );

        // INITCAP: expects one STRING argument.
        context.declareFunction("INITCAP",
            Collections.singletonList(new Parameter("input", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("INITCAP", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("INITCAP expects one argument"));
                } else {
                    String s = args.get(0).toString().toLowerCase();
                    String[] words = s.split("\\s+");
                    StringBuilder sb = new StringBuilder();
                    for (String word : words) {
                        if (word.isEmpty() == false) {
                            sb.append(Character.toUpperCase(word.charAt(0)));
                            if (word.length() > 1) {
                                sb.append(word.substring(1));
                            }
                            sb.append(" ");
                        }
                    }
                    listener.onResponse(sb.toString().trim());
                }
            })
        );
    }
}
