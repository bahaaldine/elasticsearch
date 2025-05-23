/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the
 * Elastic License 2.0; you may not use this file except in compliance with
 * the Elastic License 2.0.
 */
package org.elasticsearch.xpack.escript.integration;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.esql.plugin.EsqlPlugin;
import org.elasticsearch.xpack.escript.executors.ProcedureExecutor;
import org.elasticsearch.xpack.escript.functions.builtin.datasources.EsqlBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.ArrayBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.DocumentBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.NumberBuiltInFunctions;
import org.elasticsearch.xpack.escript.functions.builtin.datatypes.StringBuiltInFunctions;
import org.elasticsearch.xpack.escript.parser.ElasticScriptLexer;
import org.elasticsearch.xpack.escript.parser.ElasticScriptParser;
import org.elasticsearch.xpack.escript.context.ExecutionContext;
import org.elasticsearch.xpack.escript.primitives.ReturnValue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 1)
public class VideoGameIntegrationTests extends ESIntegTestCase {

    private ExecutionContext context;
    private ProcedureExecutor executor;
    private ThreadPool threadPool;

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
        threadPool = new TestThreadPool("test") {
            public Executor executor(ThreadPool.Names name) {
                return EsExecutors.DIRECT_EXECUTOR_SERVICE;  // Runs everything on the same thread
            }
        };
        Client mockClient = null; // Keeping client null since our tests do not use real querying
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString("")); // empty source
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        executor = new ProcedureExecutor(context, threadPool, mockClient, tokens);
    }

    @Override
    public void tearDown() throws Exception {
        terminate(threadPool);
        super.tearDown();
    }


    /**
     * Populates the "videogames" index with 1,000 generated 1990s video game records.
     * Each document contains: title, genre, release_year, score, and developer.
     *
     * @param client the Elasticsearch client to use.
     */
    public void populateVideoGameIndex(Client client) {
        createIndex("videogames");
        ensureGreen("videogames");
        BulkRequestBuilder bulk = client.prepareBulk();
        String[] genres = {"Platformer", "RPG", "Shooter", "Strategy", "Sports", "Adventure"};
        String[] developers = {"Nintendo", "Sega", "Capcom", "Square", "Konami", "Namco"};
        Random random = new Random();

        for (int i = 1; i <= 1000; i++) {
            int year = 1990 + random.nextInt(10);
            double score = 1.0 + (9.0 * random.nextDouble());
            // Force decimal with Locale.US to avoid comma as decimal separator.
            String formattedScore = String.format(Locale.US, "%.1f", score);
            String doc = "{" +
                "\"title\": \"Video Game " + i + "\"," +
                "\"genre\": \"" + genres[random.nextInt(genres.length)] + "\"," +
                "\"release_year\": " + year + "," +
                "\"score\": " + formattedScore + "," +
                "\"developer\": \"" + developers[random.nextInt(developers.length)] + "\"" +
                "}";
            bulk.add(client.prepareIndex("videogames").setSource(doc, XContentType.JSON));
        }
        BulkResponse bulkResponse = bulk.get();
        if (bulkResponse.hasFailures()) {
            fail("Bulk indexing failed: " + bulkResponse.buildFailureMessage());
        }
        refresh("videogames");
    }

    /**
     * A minimal integration test that verifies 1,000 video game documents are indexed.
     */
    @Test
    public void testVideoGameIndexDocumentCount() {
        Client client = client();
        assertNotNull("Client should not be null", client);
        populateVideoGameIndex(client);
        SearchResponse response = client.prepareSearch("videogames")
            .setSize(0)
            .setTrackTotalHits(true)
            .get();
        long count = response.getHits().getTotalHits().value();
        assertEquals("Expected 1000 documents in the videogames index", 1000, count);
    }

    /**
     * An integration test that runs a procedure executing an ESQL-like query against the "videogames" index.
     * The procedure is defined as follows:
     *
     * <pre>
     * PROCEDURE testEsqlQuery()
     * BEGIN
     *     EXECUTE result = (FROM videogames | WHERE _id = '1' | KEEP title, score);
     *     RETURN result;
     * END PROCEDURE;
     * </pre>
     *
     * The test verifies that the procedure returns a non-null result.
     *
     * Note: The EXECUTE statement and the query syntax here assume that your ESQL processor
     * supports pipeline syntax (using the "|" operator) as well as a KEEP clause. Adjust accordingly.
     */
    @Test
    public void testVideoGameSampleQuery() throws Exception {
        Client client = client();
        assertNotNull("Client should not be null", client);
        populateVideoGameIndex(client);

        String proc = """
            PROCEDURE testEsqlQuery()
            BEGIN
                DECLARE result ARRAY OF DOCUMENT;
                SET result = ESQL_QUERY('FROM videogames | WHERE title == "Video Game 195" | KEEP title, score');
                RETURN result;
            END PROCEDURE
            """;
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(proc));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        var procCtx = parser.procedure();

        // Create a local thread pool using TestThreadPool.
        try {
            // Initialize a new ExecutionContext and register built‑in functions.
            ExecutionContext context = new ExecutionContext();
            ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, client, tokens);

            StringBuiltInFunctions.registerAll(context);
            NumberBuiltInFunctions.registerAll(context);
            ArrayBuiltInFunctions.registerAll(context);
            DocumentBuiltInFunctions.registerAll(context);
            EsqlBuiltInFunctions.registerAll(context,executor,client);

            CountDownLatch latch = new CountDownLatch(1);
            executor.visitProcedureAsync(procCtx, new ActionListener<>() {
                @Override
                public void onResponse(Object result) {
                    // Unwrap the result if it's wrapped in a ReturnValue
                    Object raw = (result instanceof ReturnValue) ? ((ReturnValue) result).getValue() : result;
                    assertNotNull("Procedure result should not be null", raw);
                    System.out.println("Procedure returned: " + raw);

                    // Check that the result is a List and has at least one element.
                    assertTrue("Expected result to be a List", raw instanceof List);
                    List<?> resultList = (List<?>) raw;
                    assertFalse("Result list should not be empty", resultList.isEmpty());

                    // Check that the first element is a Map with expected fields.
                    Object first = resultList.get(0);
                    assertTrue("Each result must be a Map", first instanceof java.util.Map);
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> resultMap = (java.util.Map<String, Object>) first;
                    assertEquals("Title should be 'Video Game 195'",
                        "Video Game 195", resultMap.get("title"));

                    // For the score, you may check that it is a number and optionally compare it to an expected value.
                    Object scoreObj = resultMap.get("score");
                    assertNotNull("Score field must not be null", scoreObj);
                    double score;
                    try {
                        score = Double.parseDouble(scoreObj.toString());
                    } catch (NumberFormatException e) {
                        fail("Score is not a valid number: " + scoreObj);
                        return;
                    }
                    // If you have an expected value, for example 7.1, assert it (adjust delta as needed)
                    // Here we simply assert that the score is greater than 0.
                    assertTrue("Score should be > 0", score > 0);

                    latch.countDown();
                }
                @Override
                public void onFailure(Exception e) {
                    fail("Procedure execution failed: " + e.getMessage());
                    latch.countDown();
                }
            });
            assertTrue("Procedure did not complete in time", latch.await(10, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * An integration test that exercises a more complex procedure using Elastic Script processing.
     * <p>
     * This procedure demonstrates a variety of Elastic Script capabilities by:
     * <ul>
     *   <li>Declaring numeric variables.</li>
     *   <li>Assigning an initial value.</li>
     *   <li>Populating an array variable with DOCUMENTs selected from the "videogames" index (keeping the score field).</li>
     *   <li>Looping over the document array.</li>
     *   <li>Using an IF statement to filter documents where the score is greater than 5.</li>
     *   <li>Accumulating the total score and counting the number of documents processed.</li>
     *   <li>Computing the average score for documents that meet the condition.</li>
     *   <li>Returning the computed average.</li>
     * </ul>
     *
     * The procedure is defined as follows:
     *
     * <pre>
     * PROCEDURE complexVideoGameProcessing()
     * BEGIN
     *     DECLARE totalScore NUMBER = 0;
     *     DECLARE validCount NUMBER = 0;
     *     DECLARE arr ARRAY OF DOCUMENT = (FROM videogames | KEEP score);
     *     FOR doc IN arr LOOP
     *         IF doc['score'] > 5 THEN
     *             SET totalScore = totalScore + doc['score'];
     *             SET validCount = validCount + 1;
     *         END IF;
     *     END LOOP;
     *     IF validCount > 0 THEN
     *         SET totalScore = totalScore / validCount;
     *     END IF;
     *     RETURN totalScore;
     * END PROCEDURE
     * </pre>
     *
     * The test asserts that the returned average is a valid number between 5 and 10.
     */
    @Test
    public void testVideoGameProcessing() throws Exception {
        Client client = client();
        assertNotNull("Client should not be null", client);
        // Populate the index first.
        populateVideoGameIndex(client);

        String proc = """
            PROCEDURE complexVideoGameProcessing()
            BEGIN
                DECLARE totalScore NUMBER = 0;
                DECLARE validCount NUMBER = 0;
                DECLARE arr ARRAY OF DOCUMENT;
                SET arr = ESQL_QUERY('FROM videogames | KEEP score');
                FOR doc IN arr LOOP
                    IF doc['score'] > 5 THEN
                        SET totalScore = totalScore + doc['score'];
                        SET validCount = validCount + 1;
                    END IF;
                END LOOP;
                IF validCount > 0 THEN
                    SET totalScore = totalScore / validCount;
                END IF;
                RETURN totalScore;
            END PROCEDURE
            """;
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(proc));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        var procCtx = parser.procedure();

        try {
            // Create a new ExecutionContext for this procedure and register functions.
            ExecutionContext context = new ExecutionContext();
            ProcedureExecutor executor = new ProcedureExecutor(context, threadPool, client, tokens);

            StringBuiltInFunctions.registerAll(context);
            NumberBuiltInFunctions.registerAll(context);
            ArrayBuiltInFunctions.registerAll(context);
            DocumentBuiltInFunctions.registerAll(context);
            EsqlBuiltInFunctions.registerAll(context,executor,client);

            CountDownLatch latch = new CountDownLatch(1);
            executor.visitProcedureAsync(procCtx, new ActionListener<>() {
                @Override
                public void onResponse(Object result) {
                    Object raw = (result instanceof ReturnValue) ? ((ReturnValue) result).getValue() : result;
                    assertNotNull("Procedure result should not be null", raw);
                    System.out.println("Complex procedure returned: " + raw);
                    double avgScore;
                    try {
                        avgScore = Double.parseDouble(raw.toString());
                    } catch (NumberFormatException e) {
                        fail("Result is not a valid number: " + raw);
                        return;
                    }
                    // Since scores are between 1 and 10 and we only average those > 5,
                    // the average should logically be between 5 and 10.
                    assertTrue("Average score should be >= 5", avgScore >= 5);
                    assertTrue("Average score should be <= 10", avgScore <= 10);
                    latch.countDown();
                }
                @Override
                public void onFailure(Exception e) {
                    fail("Procedure execution failed: " + e.getMessage());
                    latch.countDown();
                }
            });
            assertTrue("Procedure did not complete in time", latch.await(10, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdown();
        }
    }

    @Test
    public void testComplexVideoGameProcessing() throws Exception {
        Client client = client();
        assertNotNull("Client should not be null", client);
        populateVideoGameIndex(client);

        // This procedure calculates the developer with the highest average score.
        // It retrieves an array of DOCUMENTs keeping only score and developer,
        // aggregates scores per developer, computes averages, and returns the best one.
        String proc = """
            PROCEDURE computeBestDeveloper()
            BEGIN
                -- Step 1: Initialize counters and a document to hold aggregate stats
                DECLARE totalScore NUMBER = 0;
                DECLARE validCount NUMBER = 0;
                DECLARE stats DOCUMENT = {};
                DECLARE games ARRAY OF DOCUMENT;

                -- Step 2: Query video games, keeping developer and score only
                SET games = ESQL_QUERY('FROM videogames | KEEP developer, score');

                -- Temporary holder for individual dev stats
                DECLARE dev STRING;
                DECLARE tempStats DOCUMENT = {};

                -- Step 3: Loop over each game and accumulate score/count per developer
                FOR game IN games LOOP
                    SET dev = game['developer'];
                    IF stats[dev] == null THEN
                        SET tempStats["sum"] = game['score'];
                        SET tempStats["count"] = 1;
                        SET stats[dev] = tempStats;
                    ELSE
                        SET stats[dev]["sum"] = stats[dev]["sum"] + game['score'];
                        SET stats[dev]["count"] = stats[dev]["count"] + 1;
                    END IF;
                END LOOP;

                -- Step 4: Determine the best developer based on average score
                DECLARE bestDev STRING = "";
                DECLARE bestAvg NUMBER = 0;
                DECLARE devStats DOCUMENT;
                DECLARE avg NUMBER;

                FOR key IN DOCUMENT_KEYS(stats) LOOP
                    SET devStats = DOCUMENT_GET(stats, key);
                    SET avg = devStats["sum"] / devStats["count"];
                    IF avg > bestAvg THEN
                        SET bestAvg = avg;
                        SET bestDev = key;
                    END IF;
                END LOOP;

                -- Step 5: Construct result document and return
                DECLARE result DOCUMENT = {};
                SET result["developer"] = bestDev;
                SET result["average_score"] = bestAvg;
                RETURN result;
            ENDPROCEDURE
            """;
        ElasticScriptLexer lexer = new ElasticScriptLexer(CharStreams.fromString(proc));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ElasticScriptParser parser = new ElasticScriptParser(tokens);
        var procCtx = parser.procedure();

        ThreadPool tp = new TestThreadPool("testPool");
        try {
            ExecutionContext context = new ExecutionContext();
            ProcedureExecutor executor = new ProcedureExecutor(context, tp, client, tokens);

            StringBuiltInFunctions.registerAll(context);
            NumberBuiltInFunctions.registerAll(context);
            ArrayBuiltInFunctions.registerAll(context);
            DocumentBuiltInFunctions.registerAll(context);
            EsqlBuiltInFunctions.registerAll(context,executor,client);

            CountDownLatch latch = new CountDownLatch(1);
            executor.visitProcedureAsync(procCtx, new ActionListener<>() {
                @Override
                public void onResponse(Object result) {
                    Object raw = (result instanceof ReturnValue) ? ((ReturnValue) result).getValue() : result;
                    assertNotNull("Procedure result should not be null", raw);
                    System.out.println("Complex procedure returned: " + raw);
                    // Expect raw to be a document with keys 'developer' and 'average_score'
                    assertTrue("Result should be a Map", raw instanceof java.util.Map);
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> resMap = (java.util.Map<String, Object>) raw;
                    assertTrue("Result map must contain developer", resMap.containsKey("developer"));
                    assertTrue("Result map must contain average_score", resMap.containsKey("average_score"));

                    double avgScore;
                    try {
                        avgScore = Double.parseDouble(resMap.get("average_score").toString());
                    } catch (NumberFormatException e) {
                        fail("average_score is not a valid number: " + resMap.get("average_score"));
                        return;
                    }
                    // Assuming scores are between 1 and 10 and only games with score > 5 are aggregated,
                    // best average should logically fall between 5 and 10.
                    assertTrue("Average score should be >= 5", avgScore >= 5);
                    assertTrue("Average score should be <= 10", avgScore <= 10);
                    latch.countDown();
                }
                @Override
                public void onFailure(Exception e) {
                    fail("Procedure execution failed: " + e.getMessage());
                    latch.countDown();
                }
            });
            assertTrue("Procedure did not complete in time", latch.await(10, TimeUnit.SECONDS));
        } finally {
            tp.shutdown();
        }
    }

}
