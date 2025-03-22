/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.plesql.handlers;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.esql.action.EsqlQueryAction;
import org.elasticsearch.xpack.esql.action.EsqlQueryRequest;
import org.elasticsearch.xpack.esql.action.EsqlQueryResponse;
import org.elasticsearch.xpack.plesql.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ExecutionContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class ExecuteStatementHandlerTests {

    private ExecutionContext context;
    private ThreadPool threadPool;
    private ProcedureExecutor executor;
    private ExecuteStatementHandler executeHandler;
    private Client mockClient;

    @Before
    public void setup() {
        context = new ExecutionContext();
        mockClient = mock(Client.class);
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // or mock(Client.class);
        PlEsqlProcedureLexer lexer =
            new PlEsqlProcedureLexer(CharStreams.fromString("")); // empty source
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
        executeHandler = new ExecuteStatementHandler(executor, mockClient);
    }

    @After
    public void tearDown() throws InterruptedException {
        ThreadPool.terminate(threadPool, 5, TimeUnit.SECONDS);
    }

    /**
     * Simple test:
     * EXECUTE my_var=(ROW a=10 | KEEP a);
     */
    @Test
    public void testBasicExecuteStatement() throws InterruptedException {
        // 1) Build a small block
        String queryBlock = """
            BEGIN
                EXECUTE my_result=(ROW a=10 | KEEP a);
            END
        """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseProcedure(queryBlock);

        // 2) Stub mockClient to respond with a "fake" ESQL response
        EsqlQueryResponse fakeResponse = mock(EsqlQueryResponse.class);

        doAnswer(invocation -> {
            // Grab the listener
            @SuppressWarnings("unchecked")
            ActionListener<EsqlQueryResponse> esqlListener =
                (ActionListener<EsqlQueryResponse>) invocation.getArgument(2);

            // Simulate success
            esqlListener.onResponse(fakeResponse);
            return null;
        }).when(mockClient).execute(
            eq(EsqlQueryAction.INSTANCE),
            any(EsqlQueryRequest.class),
            any()
        );

        // 3) Run the code
        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<>() {
            @Override
            public void onResponse(Object unused) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Should not fail: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await(2, TimeUnit.SECONDS);

        // 4) Check that "my_result" variable got "Mock ESQL result"
        assertTrue("Should have 'my_result' in context", context.hasVariable("my_result"));
        assertEquals("Mock ESQL result", context.getVariable("my_result"));
    }


    /**
     * Helper to parse a procedure block using your grammar.
     */
    private PlEsqlProcedureParser.ProcedureContext parseProcedure(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();
    }
}
