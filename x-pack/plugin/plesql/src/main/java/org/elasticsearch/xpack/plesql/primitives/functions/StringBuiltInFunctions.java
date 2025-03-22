/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.primitives.functions;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Registers all built‑in functions (mostly string functions) in the given ExecutionContext.
 */
public class StringBuiltInFunctions {

    public static void registerAll(ExecutionContext context) {
        context.declareFunction("LENGTH", new BuiltInFunctionDefinition("LENGTH", (List<Object> args) -> {
            if (args.size() != 1) {
                throw new RuntimeException("LENGTH expects one argument");
            }
            return args.get(0).toString().length();
        }));

        context.declareFunction("SUBSTR", new BuiltInFunctionDefinition("SUBSTR", (List<Object> args) -> {
            if (args.size() < 2 || args.size() > 3) {
                throw new RuntimeException("SUBSTR expects 2 or 3 arguments");
            }
            String s = args.get(0).toString();
            int start = ((Number) args.get(1)).intValue();
            if (args.size() == 2) {
                return s.substring(start - 1);  // Assuming 1-indexed positions.
            } else {
                int length = ((Number) args.get(2)).intValue();
                return s.substring(start - 1, Math.min(s.length(), start - 1 + length));
            }
        }));

        context.declareFunction("UPPER", new BuiltInFunctionDefinition("UPPER", (List<Object> args) -> {
            if (args.size() != 1) {
                throw new RuntimeException("UPPER expects one argument");
            }
            return args.get(0).toString().toUpperCase();
        }));

        context.declareFunction("LOWER", new BuiltInFunctionDefinition("LOWER", (List<Object> args) -> {
            if (args.size() != 1) {
                throw new RuntimeException("LOWER expects one argument");
            }
            return args.get(0).toString().toLowerCase();
        }));

        context.declareFunction("TRIM", new BuiltInFunctionDefinition("TRIM", (List<Object> args) -> {
            if (args.size() != 1) {
                throw new RuntimeException("TRIM expects one argument");
            }
            return args.get(0).toString().trim();
        }));

        context.declareFunction("LTRIM", new BuiltInFunctionDefinition("LTRIM", (List<Object> args) -> {
            if (args.size() != 1) {
                throw new RuntimeException("LTRIM expects one argument");
            }
            return args.get(0).toString().replaceAll("^\\s+", "");
        }));

        context.declareFunction("RTRIM", new BuiltInFunctionDefinition("RTRIM", (List<Object> args) -> {
            if (args.size() != 1) {
                throw new RuntimeException("RTRIM expects one argument");
            }
            return args.get(0).toString().replaceAll("\\s+$", "");
        }));

        context.declareFunction("REPLACE", new BuiltInFunctionDefinition("REPLACE", (List<Object> args) -> {
            if (args.size() != 3) {
                throw new RuntimeException("REPLACE expects three arguments");
            }
            String s = args.get(0).toString();
            String target = args.get(1).toString();
            String replacement = args.get(2).toString();
            return s.replace(target, replacement);
        }));

        context.declareFunction("INSTR", new BuiltInFunctionDefinition("INSTR", (List<Object> args) -> {
            if (args.size() != 2) {
                throw new RuntimeException("INSTR expects two arguments");
            }
            String s = args.get(0).toString();
            String sub = args.get(1).toString();
            int pos = s.indexOf(sub);
            return pos >= 0 ? pos + 1 : 0; // Return 1-indexed position; 0 if not found.
        }));

        context.declareFunction("LPAD", new BuiltInFunctionDefinition("LPAD", (List<Object> args) -> {
            if (args.size() != 3) {
                throw new RuntimeException("LPAD expects three arguments");
            }
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
            return pad + s;
        }));

        context.declareFunction("RPAD", new BuiltInFunctionDefinition("RPAD", (List<Object> args) -> {
            if (args.size() != 3) {
                throw new RuntimeException("RPAD expects three arguments");
            }
            String s = args.get(0).toString();
            int totalLength = ((Number) args.get(1)).intValue();
            String padStr = args.get(2).toString();
            StringBuilder sb = new StringBuilder(s);
            while (sb.length() < totalLength) {
                sb.append(padStr);
            }
            return sb.substring(0, totalLength);
        }));

        context.declareFunction("SPLIT", new BuiltInFunctionDefinition("SPLIT", (List<Object> args) -> {
            if (args.size() != 2) {
                throw new RuntimeException("SPLIT expects two arguments");
            }
            String s = args.get(0).toString();
            String delimiter = args.get(1).toString();
            String[] parts = s.split(Pattern.quote(delimiter));
            return Arrays.asList(parts);
        }));

        context.declareFunction("CONCAT", new BuiltInFunctionDefinition("CONCAT", (List<Object> args) -> {
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg != null) {
                    sb.append(arg.toString());
                }
            }
            return sb.toString();
        }));

        context.declareFunction("REGEXP_REPLACE", new BuiltInFunctionDefinition("REGEXP_REPLACE", (List<Object> args) -> {
            if (args.size() != 3) {
                throw new RuntimeException("REGEXP_REPLACE expects three arguments");
            }
            String s = args.get(0).toString();
            String regex = args.get(1).toString();
            String replacement = args.get(2).toString();
            return s.replaceAll(regex, replacement);
        }));

        context.declareFunction("REGEXP_SUBSTR", new BuiltInFunctionDefinition("REGEXP_SUBSTR", (List<Object> args) -> {
            if (args.size() != 2) {
                throw new RuntimeException("REGEXP_SUBSTR expects two arguments");
            }
            String s = args.get(0).toString();
            String regex = args.get(1).toString();
            java.util.regex.Matcher matcher = Pattern.compile(regex).matcher(s);
            if (matcher.find()) {
                return matcher.group();
            }
            return "";
        }));

        context.declareFunction("REVERSE", new BuiltInFunctionDefinition("REVERSE", (List<Object> args) -> {
            if (args.size() != 1) {
                throw new RuntimeException("REVERSE expects one argument");
            }
            String s = args.get(0).toString();
            return new StringBuilder(s).reverse().toString();
        }));

        context.declareFunction("INITCAP", new BuiltInFunctionDefinition("INITCAP", (List<Object> args) -> {
            if (args.size() != 1) {
                throw new RuntimeException("INITCAP expects one argument");
            }
            String s = args.get(0).toString().toLowerCase();
            String[] words = s.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if ( word.isEmpty() == false ) {
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        sb.append(word.substring(1));
                    }
                    sb.append(" ");
                }
            }
            return sb.toString().trim();
        }));
    }
}
