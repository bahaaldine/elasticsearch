/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.primitives.functions.builtin;

import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.functions.interfaces.BuiltInFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentBuiltInFunctions {

    public static void registerAll(ExecutionContext context) {
        // DOCUMENT_KEYS: returns the list of keys in a document.
        context.declareFunction("DOCUMENT_KEYS", new BuiltInFunctionDefinition("DOCUMENT_KEYS",
            (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("DOCUMENT_KEYS expects one argument");
                }
                Object docObj = args.get(0);
                if ( (docObj instanceof Map) == false ) {
                    throw new RuntimeException("DOCUMENT_KEYS expects a document");
                }
                Map<?, ?> doc = (Map<?, ?>) docObj;
                return new ArrayList<>(doc.keySet());
            }));

        // DOCUMENT_VALUES: returns the list of values in a document.
        context.declareFunction("DOCUMENT_VALUES", new BuiltInFunctionDefinition("DOCUMENT_VALUES",
            (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 1) {
                    throw new RuntimeException("DOCUMENT_VALUES expects one argument");
                }
                Object docObj = args.get(0);
                if ( (docObj instanceof Map) == false ) {
                    throw new RuntimeException("DOCUMENT_VALUES expects a document");
                }
                Map<?, ?> doc = (Map<?, ?>) docObj;
                return new ArrayList<>(doc.values());
            }));

        // DOCUMENT_GET: returns the value for a given key.
        context.declareFunction("DOCUMENT_GET", new BuiltInFunctionDefinition("DOCUMENT_GET",
            (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("DOCUMENT_GET expects two arguments: a document and a key");
                }
                Object docObj = args.get(0);
                if ( (docObj instanceof Map) == false ) {
                    throw new RuntimeException("DOCUMENT_GET expects a document as the first argument");
                }
                Map<?, ?> doc = (Map<?, ?>) docObj;
                Object key = args.get(1);
                return doc.get(key);
            }));

        // DOCUMENT_MERGE: returns a new document that is the result of merging two documents.
        // DOCUMENT_MERGE: returns a new document that is the result of merging two documents.
        context.declareFunction("DOCUMENT_MERGE", new BuiltInFunctionDefinition("DOCUMENT_MERGE",
            (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("DOCUMENT_MERGE expects two arguments: document1 and document2");
                }
                Object docObj1 = args.get(0);
                Object docObj2 = args.get(1);
                // Ensure both arguments are maps.
                if ( (docObj1 instanceof Map && docObj2 instanceof Map) == false ) {
                    throw new RuntimeException("DOCUMENT_MERGE expects both arguments to be documents");
                }
                Map<Object, Object> merged = new HashMap<>((Map<?, ?>) docObj1);
                merged.putAll((Map<?, ?>) docObj2);
                return merged;
            }));

        // DOCUMENT_REMOVE: returns a new document with the specified key removed.
        context.declareFunction("DOCUMENT_REMOVE", new BuiltInFunctionDefinition("DOCUMENT_REMOVE",
            (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("DOCUMENT_REMOVE expects two arguments: a document and a key");
                }
                Object docObj = args.get(0);
                if ( (docObj instanceof Map) == false ) {
                    throw new RuntimeException("DOCUMENT_REMOVE expects the first argument to be a document");
                }
                Map<Object, Object> doc = new HashMap<>((Map<?, ?>) docObj);
                Object key = args.get(1);
                doc.remove(key);
                return doc;
            }));

        // DOCUMENT_CONTAINS: returns true if the document contains the given key.
        context.declareFunction("DOCUMENT_CONTAINS", new BuiltInFunctionDefinition("DOCUMENT_CONTAINS",
            (BuiltInFunction) (List<Object> args) -> {
                if (args.size() != 2) {
                    throw new RuntimeException("DOCUMENT_CONTAINS expects two arguments: a document and a key");
                }
                Object docObj = args.get(0);
                if ( (docObj instanceof Map) == false) {
                    throw new RuntimeException("DOCUMENT_CONTAINS expects the first argument to be a document");
                }
                Map<?, ?> doc = (Map<?, ?>) docObj;
                Object key = args.get(1);
                return doc.containsKey(key);
            }));
    }
}
