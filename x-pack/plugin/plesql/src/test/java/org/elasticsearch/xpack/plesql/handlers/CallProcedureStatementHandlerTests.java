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
                        ReturnValue outerReturnValue = ((ReturnValue) result);
                        if ( outerReturnValue.getValue() instanceof  ReturnValue ) {
                            ReturnValue innerReturnValue = ((ReturnValue) outerReturnValue.getValue());
                            double sum = Double.parseDouble( ( innerReturnValue.getValue()).toString() );
                            assertEquals("Sum should be 12", 12.0, sum, 0.00001);
                        }
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
}
