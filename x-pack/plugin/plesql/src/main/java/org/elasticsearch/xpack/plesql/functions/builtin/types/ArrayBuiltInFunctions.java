/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin.types;

import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArrayBuiltInFunctions {

    public static void registerAll(ExecutionContext context) {
        // ARRAY_LENGTH: returns the length of an array.
        context.declareFunction("ARRAY_LENGTH",
            Collections.singletonList(new Parameter("array", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARRAY_LENGTH", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("ARRAY_LENGTH expects one argument");
                }
                if ( (args.get(0) instanceof List) == false ) {
                    throw new RuntimeException("ARRAY_LENGTH expects an array argument");
                }
                List<?> array = (List<?>) args.get(0);
                return array.size();
            })
        );

        // ARRAY_APPEND: returns a new array with the given element appended.
        context.declareFunction("ARRAY_APPEND",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_APPEND", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("ARRAY_APPEND expects two arguments: an array and an element");
                }
                if ( (args.get(0) instanceof List) == false ) {
                    throw new RuntimeException("ARRAY_APPEND expects the first argument to be an array");
                }
                List<Object> array = new ArrayList<>((List<?>) args.get(0));
                array.add(args.get(1));
                return array;
            })
        );

        // ARRAY_PREPEND: returns a new array with the given element prepended.
        context.declareFunction("ARRAY_PREPEND",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_PREPEND", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("ARRAY_PREPEND expects two arguments: an array and an element");
                }
                if ( (args.get(0) instanceof List) == false ) {
                    throw new RuntimeException("ARRAY_PREPEND expects the first argument to be an array");
                }
                List<Object> array = new ArrayList<>();
                array.add(args.get(1));
                array.addAll((List<?>) args.get(0));
                return array;
            })
        );

        // ARRAY_REMOVE: returns a new array with all occurrences of a given element removed.
        context.declareFunction("ARRAY_REMOVE",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_REMOVE", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("ARRAY_REMOVE expects two arguments: an array and an element to remove");
                }
                if ( (args.get(0) instanceof List) == false ) {
                    throw new RuntimeException("ARRAY_REMOVE expects the first argument to be an array");
                }
                List<Object> array = new ArrayList<>((List<?>) args.get(0));
                Object toRemove = args.get(1);
                array.removeIf(e -> e == null ? toRemove == null : e.equals(toRemove));
                return array;
            })
        );

        // ARRAY_CONTAINS: returns true if the array contains the given element.
        context.declareFunction("ARRAY_CONTAINS",
            Arrays.asList(
                new Parameter("array", "ARRAY", ParameterMode.IN),
                new Parameter("element", "ANY", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ARRAY_CONTAINS", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("ARRAY_CONTAINS expects two arguments: an array and an element");
                }
                if ( (args.get(0) instanceof List) == false ) {
                    throw new RuntimeException("ARRAY_CONTAINS expects the first argument to be an array");
                }
                List<?> array = (List<?>) args.get(0);
                Object element = args.get(1);
                return array.contains(element);
            })
        );

        // ARRAY_DISTINCT: returns a new array with duplicate elements removed.
        context.declareFunction("ARRAY_DISTINCT",
            Collections.singletonList(new Parameter("array", "ARRAY", ParameterMode.IN)),
            new BuiltInFunctionDefinition("ARRAY_DISTINCT", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("ARRAY_DISTINCT expects one argument: an array");
                }
                if ( (args.get(0) instanceof List) == false ) {
                    throw new RuntimeException("ARRAY_DISTINCT expects an array argument");
                }
                List<?> array = (List<?>) args.get(0);
                Set<Object> seen = new HashSet<>();
                List<Object> result = new ArrayList<>();
                for (Object e : array) {
                    if (seen.add(e)) {
                        result.add(e);
                    }
                }
                return result;
            })
        );
    }
}
