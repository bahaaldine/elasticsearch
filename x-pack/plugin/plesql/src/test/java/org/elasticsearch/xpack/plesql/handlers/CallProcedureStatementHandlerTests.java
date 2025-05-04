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
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.context.ExecutionContext;
import org.elasticsearch.xpack.plesql.executors.PlEsqlExecutor;
import org.elasticsearch.xpack.plesql.executors.ProcedureExecutor;
import org.elasticsearch.xpack.plesql.functions.builtin.datasources.EsqlBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.DocumentBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.plesql.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureLexer;
import org.elasticsearch.xpack.plesql.parser.PlEsqlProcedureParser;
import org.elasticsearch.xpack.plesql.primitives.ReturnValue;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 1)
public class CallProcedureStatementHandlerTests extends ESIntegTestCase {

    private static final Logger LOGGER = LogManager.getLogger(CallProcedureStatementHandlerTests.class);
    private ThreadPool threadPool;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool("test-thread-pool");
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }

    @Test
    public void testSimpleProcedureCall() throws Exception {
        LOGGER.info("Starting test testSimpleProcedureCall");

        String procedureText = """
            PROCEDURE addNumbers(a NUMBER, b NUMBER)
            BEGIN
              RETURN a + b;
            END PROCEDURE;
        """;

        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(procedureText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        Client client = client();
        ExecutionContext context = new ExecutionContext();
        PlEsqlExecutor plEsqlExecutor = new PlEsqlExecutor(threadPool, client);
        ProcedureExecutor procedureExecutor = new ProcedureExecutor(context, threadPool, client, tokens);

        LOGGER.info("Storing procedure");

        // Step 1: Store the procedure using the real cluster
        CountDownLatch storeLatch = new CountDownLatch(1);
        plEsqlExecutor.storeProcedureAsync("addNumbers", procedureText, ActionListener.wrap(
            success -> storeLatch.countDown(),
            e -> {
                fail("Failed to store procedure: " + e.getMessage());
                storeLatch.countDown();
            }
        ));
        assertTrue("Timeout waiting for procedure storage", storeLatch.await(10, TimeUnit.SECONDS));

        // Step 2: Call the procedure
        LOGGER.info("Now trying to call the procedure");

        String proc = """
            PROCEDURE callWrapper()
            BEGIN
              RETURN CALL_PROCEDURE addNumbers(5, 7);
            END PROCEDURE;
            """;

        PlEsqlProcedureLexer callLexer = new PlEsqlProcedureLexer(CharStreams.fromString(proc));
        CommonTokenStream callTokens = new CommonTokenStream(callLexer);
        PlEsqlProcedureParser callParser = new PlEsqlProcedureParser(callTokens);
        var procCtx = callParser.procedure();

        CountDownLatch callLatch = new CountDownLatch(1);

        try {
            // Initialize a new ExecutionContext and register built‑in functions.
            ExecutionContext differentContext = new ExecutionContext();
            ProcedureExecutor executor = new ProcedureExecutor(differentContext, threadPool, client, tokens);

            executor.visitProcedureAsync(procCtx, new ActionListener<>() {

                @Override
                public void onResponse(Object result) {
                    LOGGER.info("Procedure called");
                    if ( result instanceof ReturnValue ) {
                        assertNotNull("Result should not be null", result);
                        ReturnValue returnedValue = ((ReturnValue) result);
                        double sum = Double.parseDouble( ( returnedValue.getValue()).toString() );
                        assertEquals("Sum should be 12", 12.0, sum, 0.00001);
                    } else {
                        fail();
                    }
                    callLatch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    fail("Failed to execute procedure call: " + e.getMessage());
                    callLatch.countDown();
                }
            });
        } finally {
            threadPool.shutdown();
        }

        assertTrue("Timeout waiting for procedure call", callLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testProcedureWithConditionalLogicAndNestedCalls() throws Exception {
        LOGGER.info("Starting testProcedureWithConditionalLogicAndNestedCalls");

        String isEvenProc = """
            PROCEDURE isEven(n NUMBER)
            BEGIN
              RETURN MOD(n,2) == 0;
            END PROCEDURE;
            """;

        Client client = client();
        PlEsqlExecutor plEsqlExecutor = new PlEsqlExecutor(threadPool, client);
        CountDownLatch storeLatch = new CountDownLatch(1);
        plEsqlExecutor.storeProcedureAsync("isEven", isEvenProc, ActionListener.wrap(
            success -> storeLatch.countDown(),
            e -> {
                fail("Failed to store isEven procedure: " + e.getMessage());
                storeLatch.countDown();
            }
        ));
        assertTrue("Timeout storing isEven", storeLatch.await(10, TimeUnit.SECONDS));

        String wrapperProc = """
            PROCEDURE sumIfEven()
            BEGIN
              DECLARE a NUMBER;
              DECLARE b NUMBER;
              DECLARE sum NUMBER;
              DECLARE even BOOLEAN;

              SET a = 2;
              SET b = 4;
              SET sum = a + b;
              SET even = CALL_PROCEDURE isEven(sum);

              IF even THEN
                RETURN sum;
              ELSE
                RETURN -1;
              END IF;
            END PROCEDURE;
            """;

        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(wrapperProc));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        var procCtx = parser.procedure();

        CountDownLatch callLatch = new CountDownLatch(1);


        // Initialize a new ExecutionContext and register built‑in functions.
        ExecutionContext context = new ExecutionContext();
        ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, client, tokens);

        StringBuiltInFunctions.registerAll(context);
        NumberBuiltInFunctions.registerAll(context);
        ArrayBuiltInFunctions.registerAll(context);
        DocumentBuiltInFunctions.registerAll(context);
        EsqlBuiltInFunctions.registerAll(context,executor,client);

        executor.visitProcedureAsync(procCtx, new ActionListener<>() {
            @Override
            public void onResponse(Object result) {
                Object raw = (result instanceof ReturnValue rv) ? rv.getValue() : result;
                double value = Double.parseDouble(raw.toString());
                assertEquals("Expected result is 6 (even sum)", 6.0, value, 0.00001);
                callLatch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Procedure call failed: " + e.getMessage());
                callLatch.countDown();
            }
        });

        assertTrue("Timeout waiting for procedure call", callLatch.await(10, TimeUnit.SECONDS));
    }
}
