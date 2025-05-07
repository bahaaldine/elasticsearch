/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.plesql.handlers;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.plesql.executors.PlEsqlExecutor;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 1)
public class CreateProcedureStatementHandlerTests extends ESIntegTestCase {

    private static final Logger LOGGER = LogManager.getLogger(CreateProcedureStatementHandlerTests.class);

    @Test
    public void testCreateStoredProcedureViaExecutor() throws Exception {
        String createText = """
            CREATE PROCEDURE helloWorld()
            BEGIN
              RETURN "hello";
            END PROCEDURE;
        """;

        Client client = client();
        ThreadPool threadPool = new TestThreadPool("test-thread-pool");
        PlEsqlExecutor executor = new PlEsqlExecutor(threadPool, client);

        CountDownLatch latch = new CountDownLatch(1);

        executor.executeProcedure(createText, Map.of(), ActionListener.wrap(
            result -> {
                LOGGER.info("Create procedure executed with result: {}", result);
                latch.countDown();
            },
            e -> {
                LOGGER.error("Failed to execute create procedure", e);
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        ));

        assertTrue("Timeout waiting for create execution", latch.await(10, TimeUnit.SECONDS));
        threadPool.shutdown();
    }
}
