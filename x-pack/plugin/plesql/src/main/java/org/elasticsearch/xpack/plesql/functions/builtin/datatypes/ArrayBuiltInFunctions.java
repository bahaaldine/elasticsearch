/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin.datatypes;

import org.elasticsearch.xpack.plesql.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.plesql.functions.api.FunctionCategory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.plesql.functions.api.FunctionParam;
import org.elasticsearch.xpack.plesql.functions.api.FunctionReturn;
import org.elasticsearch.xpack.plesql.functions.api.FunctionSpec;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.action.ActionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collection of built-in array manipulation functions for PL|ESQL.
 *
 * This class defines multiple utility functions to operate on arrays,
 * such as computing length, appending/prepending elements, filtering for uniqueness, etc.
 *
 * These functions are registered into the execution context during initialization.
 */
@FunctionCollectionSpec(
    category = FunctionCategory.ARRAY,
    description = "Built-in array manipulation functions like ARRAY_LENGTH, ARRAY_APPEND, etc."
)
public class ArrayBuiltInFunctions {
    private static final Logger LOGGER = LogManager.getLogger(ArrayBuiltInFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Array built-in functions");

        registerArrayLength(context);
        registerArrayAppend(context);
        registerArrayPrepend(context);
        registerArrayRemove(context);
        registerArrayContains(context);
        registerArrayDistinct(context);
    }

    @FunctionSpec(
        name = "ARRAY_LENGTH",
        description = "Returns the number of elements in the given array.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array whose length is to be determined")
        },
        returnType = @FunctionReturn(type = "INTEGER", description = "The length of the array"),
        examples = {
            "ARRAY_LENGTH([1, 2, 3]) -> 3"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayLength(ExecutionContext context) {
        context.declareFunction("ARRAY_LENGTH",
            Collections.singletonList(new Parameter("array", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARRAY_LENGTH", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("ARRAY_LENGTH expects one argument"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_LENGTH expects an array argument"));
                }
                List<?> array = (List<?>) args.get(0);
                listener.onResponse(array.size());
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_APPEND",
        description = "Returns a new array with the specified element appended to the end.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to append to"),
            @FunctionParam(name = "element", type = "ANY", description = "The element to append")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "A new array with the element appended"),
        examples = {
            "ARRAY_APPEND([1, 2], 3) -> [1, 2, 3]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayAppend(ExecutionContext context) {
        context.declareFunction("ARRAY_APPEND",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_APPEND", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_APPEND expects two arguments: an array and an element"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_APPEND expects the first argument to be an array"));
                }
                List<Object> array = new ArrayList<>((List<?>) args.get(0));
                array.add(args.get(1));
                listener.onResponse(array);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_PREPEND",
        description = "Returns a new array with the specified element prepended to the beginning.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to prepend to"),
            @FunctionParam(name = "element", type = "ANY", description = "The element to prepend")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "A new array with the element prepended"),
        examples = {
            "ARRAY_PREPEND([2, 3], 1) -> [1, 2, 3]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayPrepend(ExecutionContext context) {
        context.declareFunction("ARRAY_PREPEND",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_PREPEND", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_PREPEND expects two arguments: an array and an element"));
                }
                if ((args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_PREPEND expects the first argument to be an array"));
                }
                List<Object> array = new ArrayList<>();
                array.add(args.get(1));
                array.addAll((List<?>) args.get(0));
                listener.onResponse(array);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_REMOVE",
        description = "Returns a new array with all occurrences of the specified element removed.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to remove elements from"),
            @FunctionParam(name = "element", type = "ANY", description = "The element to remove")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "A new array with the element removed"),
        examples = {
            "ARRAY_REMOVE([1, 2, 1, 3], 1) -> [2, 3]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayRemove(ExecutionContext context) {
        context.declareFunction("ARRAY_REMOVE",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_REMOVE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_REMOVE expects two arguments: an array and an element to remove"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_REMOVE expects the first argument to be an array"));
                }
                List<Object> array = new ArrayList<>((List<?>) args.get(0));
                Object toRemove = args.get(1);
                array.removeIf(e -> e == null ? toRemove == null : e.equals(toRemove));
                listener.onResponse(array);
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_CONTAINS",
        description = "Returns true if the array contains the specified element.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to search"),
            @FunctionParam(name = "element", type = "ANY", description = "The element to search for")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if the array contains the element, otherwise false"),
        examples = {
            "ARRAY_CONTAINS([1, 2, 3], 2) -> true"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayContains(ExecutionContext context) {
        context.declareFunction("ARRAY_CONTAINS",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_CONTAINS", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("ARRAY_CONTAINS expects two arguments: an array and an element"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_CONTAINS expects the first argument to be an array"));
                }
                List<?> array = (List<?>) args.get(0);
                Object element = args.get(1);
                listener.onResponse(array.contains(element));
            })
        );
    }

    @FunctionSpec(
        name = "ARRAY_DISTINCT",
        description = "Returns a new array with duplicate elements removed, preserving original order.",
        parameters = {
            @FunctionParam(name = "array", type = "ARRAY", description = "The array to deduplicate")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "A new array with duplicates removed"),
        examples = {
            "ARRAY_DISTINCT([1, 2, 2, 3]) -> [1, 2, 3]"
        },
        category = FunctionCategory.ARRAY
    )
    public static void registerArrayDistinct(ExecutionContext context) {
        context.declareFunction("ARRAY_DISTINCT",
            Collections.singletonList(new Parameter("array", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARRAY_DISTINCT", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("ARRAY_DISTINCT expects one argument: an array"));
                }
                if ( (args.get(0) instanceof List) == false ) {
                    listener.onFailure(new RuntimeException("ARRAY_DISTINCT expects an array argument"));
                }
                List<?> array = (List<?>) args.get(0);
                Set<Object> seen = new HashSet<>();
                List<Object> result = new ArrayList<>();
                for (Object e : array) {
                    if (seen.add(e)) {
                        result.add(e);
                    }
                }
                listener.onResponse(result);
            })
        );
    }
}
