/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the
 * Elastic License 2.0; you may not use this file except in compliance with
 * the Elastic License 2.0.
 */
package org.elasticsearch.xpack.escript.integration;

import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.esql.plugin.EsqlPlugin;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 1)
public class ElasticScriptRestApiIntegrationTests extends ESIntegTestCase {

    private RestClient restClient;

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        List<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(EsqlPlugin.class);
        return plugins;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        if (restClient != null) {
            restClient.close();
        }
        super.tearDown();
    }

    @Test
    public void testStoreProcedure() throws Exception {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", Integer.parseInt(System.getProperty("tests.rest.cluster.port", "9200")), "http")
        );
        restClient = builder.build();

        assertNotNull("RestClient must not be null", restClient);

        Request request = new Request("POST", "/_escript");
        request.setJsonEntity("""
            {
              "query": "CREATE PROCEDURE simpleTest() BEGIN RETURN 'hello'; END PROCEDURE"
            }
            """);
        Response response = restClient.performRequest(request);
        assertEquals(RestStatus.OK.getStatus(), response.getStatusLine().getStatusCode());

        // Assert that the response object is not null
        assertNotNull("Response should not be null", response);
        // Assert that the request URI matches the expected endpoint
        assertEquals("/_escript", response.getRequestLine().getUri());
    }

    @Test
    public void testDeleteProcedure() throws Exception {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", Integer.parseInt(System.getProperty("tests.rest.cluster.port", "9200")), "http")
        );
        restClient = builder.build();

        assertNotNull("RestClient must not be null", restClient);

        Request request = new Request("POST", "/_escript");
        request.setJsonEntity("""
            {
              "query": "CREATE PROCEDURE toDelete() BEGIN RETURN 'goodbye'; END PROCEDURE"
            }
            """);
        Response createResponse = restClient.performRequest(request);
        assertEquals(RestStatus.OK.getStatus(), createResponse.getStatusLine().getStatusCode());

        Request deleteRequest = new Request("POST", "/_escript");
        deleteRequest.setJsonEntity("""
            {
              "query": "DELETE PROCEDURE toDelete;"
            }
            """);
        Response deleteResponse = restClient.performRequest(deleteRequest);
        assertEquals(RestStatus.OK.getStatus(), deleteResponse.getStatusLine().getStatusCode());

        // Assert that the response object is not null
        assertNotNull("Delete response should not be null", deleteResponse);
        // Assert that the request URI matches the expected endpoint
        assertEquals("/_escript", deleteResponse.getRequestLine().getUri());
    }

    @Test
    public void testRunProcedureById() throws Exception {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", Integer.parseInt(System.getProperty("tests.rest.cluster.port", "9200")), "http")
        );
        restClient = builder.build();

        assertNotNull("RestClient must not be null", restClient);

        Request create = new Request("POST", "/_escript");
        create.setJsonEntity("""
            {
              "query": "CREATE PROCEDURE testRunById(p1 STRING) BEGIN RETURN p1; END PROCEDURE"
            }
            """);
        Response createResponse = restClient.performRequest(create);
        assertEquals(RestStatus.OK.getStatus(), createResponse.getStatusLine().getStatusCode());

        Request call = new Request("POST", "/_escript");
        call.setJsonEntity("""
            {
              "query": "CALL testRunById('executed')"
            }
            """);
        Response callResponse = restClient.performRequest(call);

        assertEquals(RestStatus.OK.getStatus(), callResponse.getStatusLine().getStatusCode());
        assertNotNull("Execution response should not be null", callResponse);
        assertEquals("/_escript", callResponse.getRequestLine().getUri());

        Map<String, Object> responseMap = XContentHelper.convertToMap(
            new BytesArray(callResponse.getEntity().getContent().readAllBytes()), true, XContentType.JSON
        ).v2();
        assertEquals("executed", responseMap.get("result"));
    }

    @Test
    public void testRunProcedureByIdWithMultipleParams() throws Exception {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", Integer.parseInt(System.getProperty("tests.rest.cluster.port", "9200")), "http")
        );
        restClient = builder.build();

        assertNotNull("RestClient must not be null", restClient);

        Request create = new Request("POST", "/_escript");
        create.setJsonEntity("""
            {
              "query": "CREATE PROCEDURE addNumbers(a NUMBER, b NUMBER) BEGIN RETURN a + b; END PROCEDURE"
            }
            """);
        Response createResponse = restClient.performRequest(create);
        assertEquals(RestStatus.OK.getStatus(), createResponse.getStatusLine().getStatusCode());

        Request call = new Request("POST", "/_escript");
        call.setJsonEntity("""
            {
              "query": "CALL addNumbers(7, 5)"
            }
            """);
        Response callResponse = restClient.performRequest(call);

        assertEquals(RestStatus.OK.getStatus(), callResponse.getStatusLine().getStatusCode());
        assertNotNull("Execution response should not be null", callResponse);
        assertEquals("/_escript", callResponse.getRequestLine().getUri());

        Map<String, Object> responseMap = XContentHelper.convertToMap(
            new BytesArray(callResponse.getEntity().getContent().readAllBytes()), true, XContentType.JSON
        ).v2();
        assertEquals("12.0", responseMap.get("result").toString());
    }

    @Test
    public void testRunProcedureByIdWithDocumentsAndLoop() throws Exception {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost",
                Integer.parseInt(System.getProperty("tests.rest.cluster.port", "9200")), "http")
        );
        restClient = builder.build();

        assertNotNull("RestClient must not be null", restClient);

        Request create = new Request("POST", "/_escript");

        String procedureText =
            "CREATE PROCEDURE sumHighScores(scores ARRAY OF DOCUMENT) BEGIN " +
                "DECLARE total NUMBER = 0; " +
                "FOR doc IN scores LOOP " +
                "IF doc['score'] > 5 THEN SET total = total + doc['score']; END IF; " +
                "END LOOP; " +
                "RETURN total; " +
                "END PROCEDURE";
        String procedureBody = "{ \"query\": \"" + procedureText + "\" }";
        create.setJsonEntity(procedureBody);

        Response createResponse = restClient.performRequest(create);
        assertEquals(RestStatus.OK.getStatus(), createResponse.getStatusLine().getStatusCode());

        Request call = new Request("POST", "/_escript");
        call.setJsonEntity("""
        {
          "query": "CALL sumHighScores([{\\"score\\":3},{\\"score\\":7},{\\"score\\":9},{\\"score\\":4},{\\"score\\":10}])"
        }
        """);

        Response callResponse = restClient.performRequest(call);

        assertEquals(RestStatus.OK.getStatus(), callResponse.getStatusLine().getStatusCode());
        assertNotNull("Execution response should not be null", callResponse);

        Map<String, Object> responseMap = XContentHelper.convertToMap(
            new BytesArray(callResponse.getEntity().getContent().readAllBytes()), true, XContentType.JSON
        ).v2();

        // Only scores > 5 are summed: 7 + 9 + 10 = 26
        assertEquals("26.0", responseMap.get("result").toString());
    }
}
