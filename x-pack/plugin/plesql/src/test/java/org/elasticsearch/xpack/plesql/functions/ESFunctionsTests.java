/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.functions;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.esql.plugin.EsqlPlugin;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.functions.builtin.datasources.ESFunctions;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 1)
public class ESFunctionsTests extends ESIntegTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;
    private CommonTokenStream tokens;

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        List<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(EsqlPlugin.class);
        return plugins;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new ExecutionContext();
        threadPool = new TestThreadPool("es-functions-test");
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(""));
        tokens = new CommonTokenStream(lexer);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testIndexSingleDocument() throws Exception {
        String proc = """
            PROCEDURE testIndex()
            BEGIN
                DECLARE doc DOCUMENT = {"title": "My Test", "rating": 5};
                DECLARE result DOCUMENT = INDEX_DOCUMENT("esfunctions_index", doc);
                RETURN result;
            END PROCEDURE
        """;

        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(
            new CommonTokenStream(new PlEsqlProcedureLexer(CharStreams.fromString(proc))));
        CountDownLatch latch = new CountDownLatch(1);

        Client client = client();
        ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, client, tokens);
        ESFunctions.registerIndexDocumentFunction(context, client);

        executor.visitProcedureAsync(parser.procedure(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                assertNotNull("Result from INDEX_DOCUMENT function should not be null", result);
                Object value = (result instanceof ReturnValue) ? ((ReturnValue) result).getValue() : result;
                assertNotNull("Unwrapped result should not be null", value);
                assertThat("Expected a Map result", value, instanceOf(Map.class));
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) value;
                assertTrue("Result should contain index", resultMap.containsKey("index"));
                assertTrue("Result should contain id", resultMap.containsKey("id"));
                assertEquals("esfunctions_index", resultMap.get("index"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        assertTrue("Procedure did not complete in time", latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testIndexArrayOfDocumentsReturnsResponse() throws Exception {
        String proc = """
        PROCEDURE testIndexArray()
        BEGIN
            DECLARE docs ARRAY OF DOCUMENT = [
                {"title": "Game One", "rating": 7},
                {"title": "Game Two", "rating": 8}
            ];
            DECLARE result DOCUMENT = INDEX_DOCUMENT("esfunctions_index_array", docs);
            RETURN result;
        END PROCEDURE
    """;

        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(
            new CommonTokenStream(new PlEsqlProcedureLexer(CharStreams.fromString(proc))));
        CountDownLatch latch = new CountDownLatch(1);

        Client client = client();
        ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, client, tokens);
        ESFunctions.registerIndexDocumentFunction(context, client);

        executor.visitProcedureAsync(parser.procedure(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                Object unwrapped = (result instanceof ReturnValue) ? ((ReturnValue) result).getValue() : result;
                System.out.println(unwrapped);
                assertNotNull("Result from INDEX_DOCUMENT should not be null", unwrapped);
                assertTrue("Result should be a Map", unwrapped instanceof Map);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) unwrapped;
                assertTrue("Response must include 'took'", map.containsKey("took"));
                assertTrue("Response must include 'items'", map.containsKey("items"));

                Object items = map.get("items");
                assertTrue("Expected 'items' to be a number", items instanceof Integer);
                assertEquals("Expected two index responses", 2, ((Integer) items).intValue());

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        assertTrue("Procedure did not complete in time", latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testIndexBulkDocuments() throws Exception {
        String proc = """
            PROCEDURE testBulkIndex()
            BEGIN
                DECLARE docs ARRAY OF DOCUMENT;
                SET docs = [{ "title": "test 1" }, { "title": "test 2" }];
                DECLARE result DOCUMENT = INDEX_BULK("esfunctions_bulk_index", docs);
                RETURN result;
            END PROCEDURE
        """;

        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(
            new CommonTokenStream(new PlEsqlProcedureLexer(CharStreams.fromString(proc))));
        CountDownLatch latch = new CountDownLatch(1);

        Client client = client(); // ✅ safe here
        ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, client, tokens);
        ESFunctions.registerIndexBulkFunction(context, client);

        executor.visitProcedureAsync(parser.procedure(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                Object value = (result instanceof ReturnValue) ? ((ReturnValue) result).getValue() : result;

                System.out.println("VALUE : " + value );

                assertNotNull("Result from INDEX_BULK function should not be null", value);
                assertThat("Expected result to be a Map", value, instanceOf(Map.class));
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) value;

                assertTrue("Result should contain 'items'", resultMap.containsKey("items"));
                assertTrue("Result should contain 'took'", resultMap.containsKey("took"));

                Object items = resultMap.get("items");
                assertThat("Expected 'items' to be an Integer", items, instanceOf(Integer.class));
                assertEquals("Expected two documents to be indexed", 2, ((Integer) items).intValue());

                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Bulk execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        assertTrue("Procedure did not complete in time", latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testIndexInvalidPayload() throws Exception {
        String proc = """
            PROCEDURE testInvalidIndex()
            BEGIN
                DECLARE result DOCUMENT = INDEX_DOCUMENT("esfunctions_index", 42);
            END PROCEDURE
        """;

        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(
            new CommonTokenStream(new PlEsqlProcedureLexer(CharStreams.fromString(proc))));
        CountDownLatch latch = new CountDownLatch(1);

        Client client = client(); // ✅ safe here
        ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, client, tokens);
        ESFunctions.registerIndexDocumentFunction(context, client);

        executor.visitProcedureAsync(parser.procedure(), new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                fail("Expected failure but got success: " + result);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                assertThat(e, instanceOf(RuntimeException.class));
                assertThat(e.getMessage(), startsWith("Type mismatch for parameter 'document'. Expected 'DOCUMENT'"));
                latch.countDown();
            }
        });

        assertTrue("Procedure did not complete in time", latch.await(10, TimeUnit.SECONDS));
    }
}
