/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.escript.functions.community;

import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.escript.functions.api.FunctionSpec;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.HashSet;

public class FunctionLoader {

    public static void loadCommunityFunctions(ExecutionContext context) {
        Set<Class<?>> allFunctionClasses = getAllAnnotatedFunctions();

        for (Class<?> clazz : allFunctionClasses) {
            try {
                if (clazz.isAnnotationPresent(FunctionSpec.class)) {
                    // Expect method: public static void register(ExecutionContext)
                    Method register = clazz.getMethod("register", ExecutionContext.class);
                    register.invoke(null, context);

                } else if (clazz.isAnnotationPresent(FunctionCollectionSpec.class)) {
                    // Expect method: public static void registerAll(ExecutionContext)
                    Method registerAll = clazz.getMethod("registerAll", ExecutionContext.class);
                    registerAll.invoke(null, context);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load community function: " + clazz.getName(), e);
            }
        }
    }

    private static Set<Class<?>> getAllAnnotatedFunctions() {
        Set<Class<?>> classes = new HashSet<>();
        try (InputStream input = FunctionLoader.class.getClassLoader()
                .getResourceAsStream("org/elasticsearch/xpack/escript/functions/community/registry.txt")) {

            if (input == null) {
                throw new RuntimeException("registry.txt not found in resources.");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if ( trimmed.isEmpty() == false && trimmed.startsWith("#") == false ) {
                        try {
                            classes.add(Class.forName(trimmed));
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException("Class not found in registry.txt: " + trimmed, e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load annotated function classes from registry.txt", e);
        }

        return classes;
    }
}
