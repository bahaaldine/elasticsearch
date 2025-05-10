package org.elasticsearch.xpack.plesql.functions.builtin.datatypes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.functions.Parameter;
import org.elasticsearch.xpack.plesql.functions.ParameterMode;
import org.elasticsearch.xpack.plesql.functions.api.FunctionCategory;
import org.elasticsearch.xpack.plesql.functions.api.FunctionCollectionSpec;
import org.elasticsearch.xpack.plesql.functions.api.FunctionParam;
import org.elasticsearch.xpack.plesql.functions.api.FunctionReturn;
import org.elasticsearch.xpack.plesql.functions.api.FunctionSpec;
import org.elasticsearch.xpack.plesql.functions.builtin.BuiltInFunctionDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FunctionCollectionSpec(
    category = FunctionCategory.DOCUMENT,
    description = "Document manipulation functions such as merging, extracting keys, values, and conditional logic."
)
public class DocumentBuiltInFunctions {

    private static final Logger LOGGER = LogManager.getLogger(DocumentBuiltInFunctions.class);

    public static void registerAll(ExecutionContext context) {
        LOGGER.info("Registering Document built-in functions");

        registerDocumentKeys(context);
        registerDocumentValues(context);
        registerDocumentGet(context);
        registerDocumentMerge(context);
        registerDocumentRemove(context);
        registerDocumentContains(context);
    }

    @FunctionSpec(
        name = "DOCUMENT_KEYS",
        description = "Returns the list of keys in the given document.",
        parameters = {
            @FunctionParam(name = "doc", type = "DOCUMENT", description = "The input document.")
        },
        returnType = @FunctionReturn(type = "ARRAY OF STRING", description = "An array of keys from the document."),
        examples = {"DOCUMENT_KEYS({\"a\":1,\"b\":2}) -> [\"a\", \"b\"]"},
        category = FunctionCategory.DOCUMENT
    )
    public static void registerDocumentKeys(ExecutionContext context) {
        context.declareFunction("DOCUMENT_KEYS",
            Collections.singletonList(new Parameter("doc", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DOCUMENT_KEYS", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1 || (args.get(0) instanceof Map) == false ) {
                    listener.onFailure(new RuntimeException("DOCUMENT_KEYS expects one DOCUMENT argument"));
                    return;
                }
                Map<?, ?> doc = (Map<?, ?>) args.get(0);
                listener.onResponse(new ArrayList<>(doc.keySet()));
            })
        );
    }

    @FunctionSpec(
        name = "DOCUMENT_VALUES",
        description = "Returns the list of values in the given document.",
        parameters = {
            @FunctionParam(name = "doc", type = "DOCUMENT", description = "The input document.")
        },
        returnType = @FunctionReturn(type = "ARRAY", description = "An array of values from the document."),
        examples = {"DOCUMENT_VALUES({\"a\":1,\"b\":2}) -> [1, 2]"},
        category = FunctionCategory.DOCUMENT
    )
    public static void registerDocumentValues(ExecutionContext context) {
        context.declareFunction("DOCUMENT_VALUES",
            Collections.singletonList(new Parameter("doc", "DOCUMENT", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DOCUMENT_VALUES", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 1 || (args.get(0) instanceof Map) == false) {
                    listener.onFailure(new RuntimeException("DOCUMENT_VALUES expects one DOCUMENT argument"));
                    return;
                }
                Map<?, ?> doc = (Map<?, ?>) args.get(0);
                listener.onResponse(new ArrayList<>(doc.values()));
            })
        );
    }

    @FunctionSpec(
        name = "DOCUMENT_GET",
        description = "Returns the value for a given key in the document.",
        parameters = {
            @FunctionParam(name = "doc", type = "DOCUMENT", description = "The input document."),
            @FunctionParam(name = "key", type = "STRING", description = "The key to retrieve from the document.")
        },
        returnType = @FunctionReturn(type = "ANY", description = "The value associated with the given key."),
        examples = {"DOCUMENT_GET({\"a\":1,\"b\":2}, \"a\") -> 1"},
        category = FunctionCategory.DOCUMENT
    )
    public static void registerDocumentGet(ExecutionContext context) {
        context.declareFunction("DOCUMENT_GET",
            List.of(
                new Parameter("doc", "DOCUMENT", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DOCUMENT_GET", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2 || (args.get(0) instanceof Map) == false ) {
                    listener.onFailure(new RuntimeException("DOCUMENT_GET expects DOCUMENT and STRING arguments"));
                    return;
                }
                Map<?, ?> doc = (Map<?, ?>) args.get(0);
                Object key = args.get(1);
                listener.onResponse(doc.get(key));
            })
        );
    }

    @FunctionSpec(
        name = "DOCUMENT_MERGE",
        description = "Returns a new document that is the result of merging two documents.",
        parameters = {
            @FunctionParam(name = "doc1", type = "DOCUMENT", description = "The first document."),
            @FunctionParam(name = "doc2", type = "DOCUMENT", description = "The second document.")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "A new document containing all keys and values from both documents."),
        examples = {"DOCUMENT_MERGE({\"a\":1}, {\"b\":2}) -> {\"a\":1, \"b\":2}"},
        category = FunctionCategory.DOCUMENT
    )
    public static void registerDocumentMerge(ExecutionContext context) {
        context.declareFunction("DOCUMENT_MERGE",
            List.of(
                new Parameter("doc1", "DOCUMENT", ParameterMode.IN),
                new Parameter("doc2", "DOCUMENT", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DOCUMENT_MERGE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2 || (args.get(0) instanceof Map == false || (args.get(1) instanceof Map) == false )) {
                    listener.onFailure(new RuntimeException("DOCUMENT_MERGE expects two DOCUMENT arguments"));
                    return;
                }
                Map<Object, Object> merged = new HashMap<>((Map<?, ?>) args.get(0));
                merged.putAll((Map<?, ?>) args.get(1));
                listener.onResponse(merged);
            })
        );
    }

    @FunctionSpec(
        name = "DOCUMENT_REMOVE",
        description = "Returns a new document with the specified key removed.",
        parameters = {
            @FunctionParam(name = "doc", type = "DOCUMENT", description = "The input document."),
            @FunctionParam(name = "key", type = "STRING", description = "The key to remove from the document.")
        },
        returnType = @FunctionReturn(type = "DOCUMENT", description = "A new document without the specified key."),
        examples = {"DOCUMENT_REMOVE({\"a\":1,\"b\":2}, \"a\") -> {\"b\":2}"},
        category = FunctionCategory.DOCUMENT
    )
    public static void registerDocumentRemove(ExecutionContext context) {
        context.declareFunction("DOCUMENT_REMOVE",
            List.of(
                new Parameter("doc", "DOCUMENT", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DOCUMENT_REMOVE", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2 || (args.get(0) instanceof Map) == false ) {
                    listener.onFailure(new RuntimeException("DOCUMENT_REMOVE expects DOCUMENT and STRING arguments"));
                    return;
                }
                Map<Object, Object> doc = new HashMap<>((Map<?, ?>) args.get(0));
                doc.remove(args.get(1));
                listener.onResponse(doc);
            })
        );
    }

    @FunctionSpec(
        name = "DOCUMENT_CONTAINS",
        description = "Returns true if the document contains the given key.",
        parameters = {
            @FunctionParam(name = "doc", type = "DOCUMENT", description = "The input document."),
            @FunctionParam(name = "key", type = "STRING", description = "The key to check in the document.")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "True if the document contains the key, false otherwise."),
        examples = {"DOCUMENT_CONTAINS({\"a\":1,\"b\":2}, \"a\") -> true"},
        category = FunctionCategory.DOCUMENT
    )
    public static void registerDocumentContains(ExecutionContext context) {
        context.declareFunction("DOCUMENT_CONTAINS",
            List.of(
                new Parameter("doc", "DOCUMENT", ParameterMode.IN),
                new Parameter("key", "STRING", ParameterMode.IN)
            ),
            new BuiltInFunctionDefinition("DOCUMENT_CONTAINS", (List<Object> args, ActionListener<Object> listener) -> {
                if (args.size() != 2 || (args.get(0) instanceof Map) == false ) {
                    listener.onFailure(new RuntimeException("DOCUMENT_CONTAINS expects DOCUMENT and STRING arguments"));
                    return;
                }
                Map<?, ?> doc = (Map<?, ?>) args.get(0);
                Object key = args.get(1);
                listener.onResponse(doc.containsKey(key));
            })
        );
    }
}
