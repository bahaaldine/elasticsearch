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
import static org.junit.Assert.fail;

public class ThrowStatementHandlerTests {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;

    @Before
    public void setup() {
        context = new ExecutionContext();
        threadPool = new TestThreadPool("test-thread-pool");
        Client mockClient = null; // or mock(Client.class);
        PlEsqlProcedureLexer lexer =
            new PlEsqlProcedureLexer(CharStreams.fromString("")); // empty source
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
    }

    @After
    public void tearDown() throws InterruptedException {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
    }

    // Helper method to parse a BEGIN ... END block
    private PlEsqlProcedureParser.ProcedureContext parseBlock(String query) {
        PlEsqlProcedureLexer lexer = new PlEsqlProcedureLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PlEsqlProcedureParser parser = new PlEsqlProcedureParser(tokens);
        return parser.procedure();
    }

    // Test 1: Basic THROW statement inside a procedure
    @Test
    public void testBasicThrowStatementInProcedure() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_function(INOUT x NUMBER)
                BEGIN
                    THROW 'Error occurred';
                END PROCEDURE
            """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                fail("Expected an exception due to THROW statement.");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                assertEquals("Error occurred", e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 2: THROW statement with a complex error message inside a procedure
    @Test
    public void testThrowStatementWithComplexMessageInProcedure() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_function(INOUT x NUMBER)
                BEGIN
                    THROW 'Complex error: something went wrong with details #$%!';
                END PROCEDURE
            """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                fail("Expected an exception due to THROW statement.");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                assertEquals("Complex error: something went wrong with details #$%!", e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 3: THROW statement in a TRY block with a CATCH block
    @Test
    public void testThrowInTryCatchBlock() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_function(INOUT x NUMBER)
                BEGIN
                    DECLARE v NUMBER = 1;
                    TRY
                        THROW 'Exception in TRY block';
                    CATCH
                        SET v = 10;
                    END TRY;
                END PROCEDURE
            """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertEquals(10.0, context.getVariable("v"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 4: THROW statement in a nested TRY-CATCH block
    @Test
    public void testNestedTryCatchWithThrow() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_function(INOUT x NUMBER)
                BEGIN
                    DECLARE v NUMBER = 1;
                    TRY
                        TRY
                            THROW 'Inner exception';
                        CATCH
                            SET v = 20;
                            THROW 'Outer exception';
                        END TRY;
                    CATCH
                        SET v = 30;
                    END TRY;
                END PROCEDURE
            """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                assertEquals(30.0, context.getVariable("v"));
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                fail("Execution failed: " + e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    // Test 5: THROW statement outside of any TRY-CATCH block
    @Test
    public void testThrowOutsideTryCatch() throws InterruptedException {
        String blockQuery = """
                PROCEDURE dummy_function(INOUT x NUMBER)
                BEGIN
                    THROW 'Uncaught exception';
                END PROCEDURE
            """;
        PlEsqlProcedureParser.ProcedureContext blockContext = parseBlock(blockQuery);

        CountDownLatch latch = new CountDownLatch(1);

        executor.visitProcedureAsync(blockContext, new ActionListener<Object>() {
            @Override
            public void onResponse(Object unused) {
                fail("Expected an exception due to THROW statement.");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                assertEquals("Uncaught exception", e.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }
}
