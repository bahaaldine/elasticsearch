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
public class DeleteProcedureStatementHandlerTests extends ESIntegTestCase {

    private static final Logger LOGGER = LogManager.getLogger(DeleteProcedureStatementHandlerTests.class);

    @Test
    public void testDeleteStoredProcedure() throws Exception {
        String procedureText = """
            PROCEDURE deleteMe()
            BEGIN
              RETURN "bye";
            END PROCEDURE;
        """;

        Client client = client();
        ThreadPool threadPool = new TestThreadPool("test-thread-pool");
        PlEsqlExecutor plEsqlExecutor = new PlEsqlExecutor(threadPool, client);

        CountDownLatch storeLatch = new CountDownLatch(1);
        plEsqlExecutor.storeProcedureAsync("deleteMe", procedureText, ActionListener.wrap(
            success -> storeLatch.countDown(),
            e -> {
                fail("Failed to store procedure: " + e.getMessage());
                storeLatch.countDown();
            }
        ));
        assertTrue("Timeout storing procedure", storeLatch.await(10, TimeUnit.SECONDS));

        String deleteStmt = "DELETE PROCEDURE deleteMe;";
        CountDownLatch deleteLatch = new CountDownLatch(1);
        plEsqlExecutor.executeProcedure(deleteStmt, Map.of(), ActionListener.wrap(
            success -> {
                LOGGER.info("Successfully deleted procedure");
                deleteLatch.countDown();
            },
            e -> {
                fail("Failed to delete procedure: " + e.getMessage());
                deleteLatch.countDown();
            }
        ));

        assertTrue("Timeout deleting procedure", deleteLatch.await(10, TimeUnit.SECONDS));
        terminate(threadPool);
    }
}
