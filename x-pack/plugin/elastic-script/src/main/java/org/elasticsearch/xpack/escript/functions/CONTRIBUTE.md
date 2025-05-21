# Contributing a New Function to Elastic Script

Welcome! If you're a developer looking to extend Elastic's Elastic Script with new capabilities for agent execution, you're in the right place. This guide explains how to add your own **function** or **function collection** to the project.

---

##  Requirements

- Your function must follow the `@FunctionSpec` annotation model
- Each function must have:
    - A `name`
    - A `description`
    - A `returnType`
    - A list of `@FunctionParam`s
    - At least one example

---

##  Option 1: Contributing a Single Function

### Step 1: Create Your Class

Put your class here:

```
x-pack/plugin/elastic-script/src/main/java/org/elasticsearch/xpack/escript/functions/custom/
```

Example:

```java
package org.elasticsearch.xpack.escript.functions.custom;

import org.elasticsearch.xpack.escript.functions.api.*;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.functions.Parameter;
import org.elasticsearch.xpack.escript.functions.ParameterMode;
import org.elasticsearch.xpack.escript.functions.builtin.BuiltInFunctionDefinition;
import org.elasticsearch.action.ActionListener;

import java.util.List;

@FunctionSpec(
        name = "REVERSE_STRING",
        description = "Reverses the input string.",
        parameters = {
                @FunctionParam(name = "input", type = "STRING", description = "The string to reverse")
        },
        returnType = @FunctionReturn(type = "STRING", description = "The reversed string"),
        examples = {
                "REVERSE_STRING('hello') -> 'olleh'"
        },
        category = FunctionCategory.STRING
)
public class ReverseStringFunction {
    public static void registerAll(ExecutionContext context) {
        context.declareFunction("REVERSE_STRING",
                List.of(new Parameter("input", "STRING", ParameterMode.IN)),
                new BuiltInFunctionDefinition("REVERSE_STRING", (args, listener) -> {
                    String input = args.get(0).toString();
                    listener.onResponse(new StringBuilder(input).reverse().toString());
                }));
    }
}
```

### Step 2: Register Your Function

Add your class to the loader:

```java
ReverseStringFunction.registerAll(context);
```

---

##  Option 2: Contributing a Function Collection

If you're contributing several related functions (e.g., document helpers), you can define them in a single class.

### Step 1: Create the Collection

```java
@FunctionCollectionSpec(
  category = FunctionCategory.DOCUMENT,
  description = "Utility functions for manipulating document fields"
)
public class DocumentUtils {

    @FunctionSpec(
        name = "DOCUMENT_KEY_EXISTS",
        description = "Checks whether a document has the specified key.",
        parameters = {
            @FunctionParam(name = "doc", type = "DOCUMENT", description = "The document to check"),
            @FunctionParam(name = "key", type = "STRING", description = "The key to look for")
        },
        returnType = @FunctionReturn(type = "BOOLEAN", description = "true if key exists, false otherwise"),
        examples = {
            "DOCUMENT_KEY_EXISTS({"name": "test"}, "name") -> true"
        },
        category = FunctionCategory.DOCUMENT
    )
    public static void registerDocumentKeyExists(ExecutionContext context) {
        context.declareFunction("DOCUMENT_KEY_EXISTS",
            List.of(new Parameter("doc", "DOCUMENT", ParameterMode.IN), new Parameter("key", "STRING", ParameterMode.IN)),
            new BuiltInFunctionDefinition("DOCUMENT_KEY_EXISTS", (args, listener) -> {
                var doc = (java.util.Map<?, ?>) args.get(0);
                var key = args.get(1).toString();
                listener.onResponse(doc.containsKey(key));
            }));
    }

    public static void registerAll(ExecutionContext context) {
        registerDocumentKeyExists(context);
        // Add more functions here
    }
}
```

### Step 2: Add to Loader

```java
DocumentUtils.registerAll(context);
```

---

##  CI Requirements

- CI will **fail** if you forget to annotate your function with `@FunctionSpec`
- CI will **fail** if required metadata like description, category, or return type is missing
- You **must** annotate with `@FunctionReturn` and at least one `@FunctionParam`

---

##  Why This Matters

Your function becomes a **skill** for the Elastic Script Agent to use. This is not just a database function—it's a new capability the Agent can reason with.

Welcome to the next generation of developer experience on top of Elasticsearch.
