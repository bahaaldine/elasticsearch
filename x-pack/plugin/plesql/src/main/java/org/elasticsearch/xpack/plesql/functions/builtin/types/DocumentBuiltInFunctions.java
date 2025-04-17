/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions.builtin.types;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.xpack.plesql.functions.interfaces.BuiltInFunction;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers all built‐in functions for DOCUMENT types used in PLESQL.
 * <p>
 * This class is responsible for adding document‐related functions to the global
 * ExecutionContext. It registers functions that allow operations on DOCUMENT values,
 * represented as Java Map objects. The supported functions include:
 * <ul>
 *   <li><strong>DOCUMENT_KEYS</strong>: Returns a list of keys from a document.
 *       <br>Expected arguments: 1 (a DOCUMENT).</li>
 *   <li><strong>DOCUMENT_VALUES</strong>: Returns a list of values from a document.
 *       <br>Expected arguments: 1 (a DOCUMENT).</li>
 *   <li><strong>DOCUMENT_GET</strong>: Retrieves the value associated with a specified key.
 *       <br>Expected arguments: 2 (a DOCUMENT and a key).</li>
 *   <li><strong>DOCUMENT_MERGE</strong>: Merges two documents into one.
 *       <br>Expected arguments: 2 (document1 and document2).</li>
 *   <li><strong>DOCUMENT_REMOVE</strong>: Returns a new document with the given key removed.
 *       <br>Expected arguments: 2 (a DOCUMENT and a key).</li>
 *   <li><strong>DOCUMENT_CONTAINS</strong>: Returns true if the document contains the given key.
 *       <br>Expected arguments: 2 (a DOCUMENT and a key).</li>
 * </ul>
 * <p>
 * Each function is declared using the three‐argument version of the
 * {@code declareFunction(String, List<Parameter>, FunctionDefinition)} method on the
 * ExecutionContext. The registration provides a specific parameter list that defines the
 * expected arguments (including their types and modes) for each function.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 *   ExecutionContext context = new ExecutionContext();
 *   DocumentBuiltInFunctions.registerAll(context);
 * </pre>
 *
 * @see org.elasticsearch.xpack.plesql.primitives.ExecutionContext
 * @see BuiltInFunctionDefinition
 * @see org.elasticsearch.xpack.plesql.functions.Parameter
 * @see org.elasticsearch.xpack.plesql.functions.ParameterMode
 */
public class DocumentBuiltInFunctions {

    public static void registerAll(ExecutionContext context) {
        // In DocumentBuiltInFunctions.java

// DOCUMENT_KEYS: returns the list of keys in a document.
        context.declareFunction("DOCUMENT_KEYS",
            Collections.singletonList(new Parameter("doc", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DOCUMENT_KEYS", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("DOCUMENT_KEYS expects one argument"));
                } else {
                    Object docObj = args.get(0);
                    if ((docObj instanceof Map) == false) {
                        listener.onFailure(new RuntimeException("DOCUMENT_KEYS expects a document"));
                    } else {
                        Map<?, ?> doc = (Map<?, ?>) docObj;
                        listener.onResponse(new ArrayList<>(doc.keySet()));
                    }
                }
            })
        );

// DOCUMENT_VALUES: returns the list of values in a document.
        context.declareFunction("DOCUMENT_VALUES",
            Collections.singletonList(new Parameter("doc", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DOCUMENT_VALUES", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1) {
                    listener.onFailure(new RuntimeException("DOCUMENT_VALUES expects one argument"));
                } else {
                    Object docObj = args.get(0);
                    if ((docObj instanceof Map) == false) {
                        listener.onFailure(new RuntimeException("DOCUMENT_VALUES expects a document"));
                    } else {
                        Map<?, ?> doc = (Map<?, ?>) docObj;
                        listener.onResponse(new ArrayList<>(doc.values()));
                    }
                }
            })
        );

// DOCUMENT_GET: returns the value for a given key.
        context.declareFunction("DOCUMENT_GET",
            Arrays.asList(
                new Parameter("doc", "DOCUMENT", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DOCUMENT_GET", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("DOCUMENT_GET expects two arguments: a document and a key"));
                } else {
                    Object docObj = args.get(0);
                    if ((docObj instanceof Map) == false) {
                        listener.onFailure(new RuntimeException("DOCUMENT_GET expects a document as the first argument"));
                    } else {
                        Map<?, ?> doc = (Map<?, ?>) docObj;
                        Object key = args.get(1);
                        listener.onResponse(doc.get(key));
                    }
                }
            })
        );

// DOCUMENT_MERGE: returns a new document that is the result of merging two documents.
        context.declareFunction("DOCUMENT_MERGE",
            Arrays.asList(
                new Parameter("doc1", "DOCUMENT", ParameterMode.IN),
                new Parameter("doc2", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DOCUMENT_MERGE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("DOCUMENT_MERGE expects two arguments: document1 and document2"));
                } else {
                    Object docObj1 = args.get(0);
                    Object docObj2 = args.get(1);
                    if ((docObj1 instanceof Map && docObj2 instanceof Map) == false) {
                        listener.onFailure(new RuntimeException("DOCUMENT_MERGE expects both arguments to be documents"));
                    } else {
                        Map<Object, Object> merged = new HashMap<>((Map<?, ?>) docObj1);
                        merged.putAll((Map<?, ?>) docObj2);
                        listener.onResponse(merged);
                    }
                }
            })
        );

// DOCUMENT_REMOVE: returns a new document with the specified key removed.
        context.declareFunction("DOCUMENT_REMOVE",
            Arrays.asList(
                new Parameter("doc", "DOCUMENT", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DOCUMENT_REMOVE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("DOCUMENT_REMOVE expects two arguments: a document and a key"));
                } else {
                    Object docObj = args.get(0);
                    if ((docObj instanceof Map) == false) {
                        listener.onFailure(new RuntimeException("DOCUMENT_REMOVE expects the first argument to be a document"));
                    } else {
                        Map<Object, Object> doc = new HashMap<>((Map<?, ?>) docObj);
                        Object key = args.get(1);
                        doc.remove(key);
                        listener.onResponse(doc);
                    }
                }
            })
        );

// DOCUMENT_CONTAINS: returns true if the document contains the given key.
        context.declareFunction("DOCUMENT_CONTAINS",
            Arrays.asList(
                new Parameter("doc", "DOCUMENT", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DOCUMENT_CONTAINS", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2) {
                    listener.onFailure(new RuntimeException("DOCUMENT_CONTAINS expects two arguments: a document and a key"));
                } else {
                    Object docObj = args.get(0);
                    if ((docObj instanceof Map) == false) {
                        listener.onFailure(new RuntimeException("DOCUMENT_CONTAINS expects the first argument to be a document"));
                    } else {
                        Map<?, ?> doc = (Map<?, ?>) docObj;
                        Object key = args.get(1);
                        listener.onResponse(doc.containsKey(key));
                    }
                }
            })
        );
    }
}
