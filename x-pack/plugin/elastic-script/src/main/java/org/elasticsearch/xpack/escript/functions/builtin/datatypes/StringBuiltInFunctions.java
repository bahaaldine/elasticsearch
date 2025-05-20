/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.builtin.datatypes;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionCategory;
import org.elasticsearch.xpack.escript.functions.api.FunctionParam;
import org.elasticsearch.xpack.escript.functions.api.FunctionReturn;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.action.ActionListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Registers all built‑in functions (mostly string functions) in the given ExecutionContext.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.STRING,
    description = "Built-in string manipulation functions like LENGTH, SUBSTR, UPPER, etc."
)
public class StringBuiltInFunctions {
    private static final Logger LOGGER = LogManager.getLogger(StringBuiltInFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering String built-in functions");
        registerLength(context);
        registerSubstr(context);
        registerUpper(context);
        registerLower(context);
        registerTrim(context);
        registerLtrim(context);
        registerRtrim(context);
        registerReplace(context);
        registerInstr(context);
        registerLpad(context);
        registerRpad(context);
        registerSplit(context);
        registerConcat(context);
        registerRegexpReplace(context);
        registerRegexpSubstr(context);
        registerReverse(context);
        registerInitcap(context);
    }

    @FunctionSpec(
        name = "LENGTH",
        description = "Returns the length of a string.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to measure")
        },
        returnType = @FunctionReturn(type = "INTEGER", description = "The number of characters in the string"),
        examples = {
            "LENGTH('Elastic') -> 7"
        },
        category = FunctionCategory.STRING
    )
    public static void registerLength(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "SUBSTR",
        description = "Returns a substring from a string starting at a given position with optional length.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to extract from"),
            @FunctionParam(name = "start", type = "NUMBER", description = "The starting position (1-based)"),
            @FunctionParam(name = "length", type = "NUMBER", description = "The length of the substring (optional)")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The extracted substring"),
        examples = {
            "SUBSTR('Elastic', 2, 3) -> 'las'",
            "SUBSTR('Elastic', 2) -> 'lastic'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerSubstr(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "UPPER",
        description = "Converts a string to upper case.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to convert")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The upper-cased string"),
        examples = {
            "UPPER('Elastic') -> 'ELASTIC'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerUpper(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "LOWER",
        description = "Converts a string to lower case.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to convert")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The lower-cased string"),
        examples = {
            "LOWER('Elastic') -> 'elastic'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerLower(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "TRIM",
        description = "Removes leading and trailing whitespace from a string.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to trim")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The trimmed string"),
        examples = {
            "TRIM('  Elastic  ') -> 'Elastic'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerTrim(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "LTRIM",
        description = "Removes leading whitespace from a string.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to trim")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The left-trimmed string"),
        examples = {
            "LTRIM('   Elastic') -> 'Elastic'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerLtrim(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "RTRIM",
        description = "Removes trailing whitespace from a string.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to trim")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The right-trimmed string"),
        examples = {
            "RTRIM('Elastic   ') -> 'Elastic'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerRtrim(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "REPLACE",
        description = "Replaces all occurrences of a substring with a replacement string.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to search"),
            @FunctionParam(name = "target", type = "STRING", description = "The substring to replace"),
            @FunctionParam(name = "replacement", type = "STRING", description = "The replacement string")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The string with replacements"),
        examples = {
            "REPLACE('Elastic', 'E', 'e') -> 'elastic'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerReplace(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "INSTR",
        description = "Returns the position of the first occurrence of a substring in a string (1-based). Returns 0 if not found.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to search"),
            @FunctionParam(name = "substring", type = "STRING", description = "The substring to search for")
        },
        returnType = @FunctionReturn(type = "INTEGER", description = "The position of the substring, or 0 if not found"),
        examples = {
            "INSTR('Elastic', 'as') -> 2",
            "INSTR('Elastic', 'xyz') -> 0"
        },
        category = FunctionCategory.STRING
    )
    public static void registerInstr(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "LPAD",
        description = "Pads the left side of a string with another string to a certain length.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The original string"),
            @FunctionParam(name = "totalLength", type = "NUMBER", description = "The desired total length"),
            @FunctionParam(name = "padStr", type = "STRING", description = "The string to pad with")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The padded string"),
        examples = {
            "LPAD('Elastic', 10, '*') -> '***Elastic'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerLpad(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "RPAD",
        description = "Pads the right side of a string with another string to a certain length.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The original string"),
            @FunctionParam(name = "totalLength", type = "NUMBER", description = "The desired total length"),
            @FunctionParam(name = "padStr", type = "STRING", description = "The string to pad with")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The padded string"),
        examples = {
            "RPAD('Elastic', 10, '*') -> 'Elastic***'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerRpad(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "SPLIT",
        description = "Splits a string by the given delimiter.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to split"),
            @FunctionParam(name = "delimiter", type = "STRING", description = "The delimiter string")
        },
        returnType = @FunctionReturn(type = "ARRAY<STRING>", description = "The list of split parts"),
        examples = {
            "SPLIT('a,b,c', ',') -> ['a','b','c']"
        },
        category = FunctionCategory.STRING
    )
    public static void registerSplit(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "||",
        description = "Concatenates two strings.",
        parameters = {
            @FunctionParam(name = "left", type = "STRING", description = "The left string"),
            @FunctionParam(name = "right", type = "STRING", description = "The right string")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The concatenated string"),
        examples = {
            "'foo' || 'bar' -> 'foobar'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerConcat(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "REGEXP_REPLACE",
        description = "Replaces each substring that matches a regular expression with a replacement string.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to search"),
            @FunctionParam(name = "regex", type = "STRING", description = "The regular expression"),
            @FunctionParam(name = "replacement", type = "STRING", description = "The replacement string")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The string with regex replacements"),
        examples = {
            "REGEXP_REPLACE('Elastic', '[aeiou]', '*') -> 'El*st*c'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerRegexpReplace(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "REGEXP_SUBSTR",
        description = "Returns the first substring that matches the given regular expression.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to search"),
            @FunctionParam(name = "regex", type = "STRING", description = "The regular expression")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The first matching substring, or empty if none"),
        examples = {
            "REGEXP_SUBSTR('Elastic', '[aeiou]') -> 'a'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerRegexpSubstr(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "REVERSE",
        description = "Reverses a string.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to reverse")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The reversed string"),
        examples = {
            "REVERSE('Elastic') -> 'citsalE'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerReverse(ExecutionContext context) {
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
    }

    @FunctionSpec(
        name = "INITCAP",
        description = "Converts the first letter of each word to upper case and the rest to lower case.",
        parameters = {
            @FunctionParam(name = "input", type = "STRING", description = "The string to convert")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The converted string"),
        examples = {
            "INITCAP('elastic search') -> 'Elastic Search'"
        },
        category = FunctionCategory.STRING
    )
    public static void registerInitcap(ExecutionContext context) {
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
