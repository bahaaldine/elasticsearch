/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction;

import java.util.List;
import java.util.Map;

// Make sure you have the appropriate imports for your FunctionDefinition, etc.
public class EsqlBuiltInFunctions {

    public static void registerAll(ExecutionContext context) {
        // Register the new ESQL_QUERY function
        context.declareFunction("ESQL_QUERY",
            List.of(
                new Parameter("query", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("ESQL_QUERY", (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("ESQL_QUERY expects 1 argument");
                }
                String query = args.get(0).toString();
                // Invoke your ESQL engine to execute the query.
                // Replace the following line with your actual ESQL query execution logic.
                Object result = null;// MyEsqlEngine.execute(query);
                return result;
            })
        );
    }

    public List<Map<String, Object>> executeEsqlQuery(String qyery) {
        return null;
    }
}
