/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V.
 * under one or more contributor license agreements. Licensed under the
 * Elastic License 2.0; you may not use this file except in compliance with
 * the Elastic License 2.0.
 */
package org.elasticsearch.xpack.plesql.integration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.esql.plugin.EsqlPlugin;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ESIntegTestCase.ClusterScope(scope = ESIntegTestCase.Scope.TEST, numDataNodes = 1)
public class PlEsqlRestApiIntegrationTests extends ESIntegTestCase {

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

        String procedureId = "test_proc_1";
        String procedureBody = """
            {
              "procedure": "PROCEDURE simpleTest() BEGIN RETURN 'hello'; END PROCEDURE"
            }
            """;

        Request request = new Request("PUT", "/_query/plesql/procedure/" + procedureId);
        request.setJsonEntity(procedureBody);
        Response response = restClient.performRequest(request);
        assertEquals(RestStatus.OK.getStatus(), response.getStatusLine().getStatusCode());

        // Assert that the response object is not null
        assertNotNull("Response should not be null", response);
        // Assert that the request URI matches the expected endpoint
        assertEquals("/_query/plesql/procedure/" + procedureId, response.getRequestLine().getUri());
    }

    @Test
    public void testDeleteProcedure() throws Exception {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", Integer.parseInt(System.getProperty("tests.rest.cluster.port", "9200")), "http")
        );
        restClient = builder.build();

        assertNotNull("RestClient must not be null", restClient);

        String procedureId = "test_proc_to_delete";
        String procedureBody = """
            {
              "procedure": "PROCEDURE toDelete() BEGIN RETURN 'goodbye'; END PROCEDURE"
            }
            """;

        // First, store the procedure
        Request putRequest = new Request("PUT", "/_query/plesql/procedure/" + procedureId);
        putRequest.setJsonEntity(procedureBody);
        Response putResponse = restClient.performRequest(putRequest);
        assertEquals(RestStatus.OK.getStatus(), putResponse.getStatusLine().getStatusCode());

        // Then, delete the procedure
        Request deleteRequest = new Request("DELETE", "/_query/plesql/procedure/" + procedureId);
        Response deleteResponse = restClient.performRequest(deleteRequest);
        assertEquals(RestStatus.OK.getStatus(), deleteResponse.getStatusLine().getStatusCode());

        // Assert that the response object is not null
        assertNotNull("Delete response should not be null", deleteResponse);
        // Assert that the request URI matches the expected endpoint
        assertEquals("/_query/plesql/procedure/" + procedureId, deleteResponse.getRequestLine().getUri());
    }

    @Test
    public void testTestProcedure() throws Exception {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", Integer.parseInt(System.getProperty("tests.rest.cluster.port", "9200")), "http")
        );
        restClient = builder.build();

        assertNotNull("RestClient must not be null", restClient);

        String runProcedureBody = """
            {
              "query": "PROCEDURE runTest() BEGIN PRINT 'SIMPLE PROCEDURE RUNNING ... ';  RETURN 'running'; END PROCEDURE"
            }
            """;
        Request runRequest = new Request("POST", "/_query/plesql/test");
        runRequest.setJsonEntity(runProcedureBody);
        Response runResponse = restClient.performRequest(runRequest);

        assertEquals(RestStatus.OK.getStatus(), runResponse.getStatusLine().getStatusCode());
        assertNotNull("Run response should not be null", runResponse);
        assertEquals("/_query/plesql/test", runResponse.getRequestLine().getUri());

        Map<String, Object> responseMap = XContentHelper.convertToMap(
            new BytesArray(runResponse.getEntity().getContent().readAllBytes()), true, XContentType.JSON
        ).v2();
        assertEquals("running", responseMap.get("result"));
    }

    @Test
    public void testRunProcedureById() throws Exception {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost("localhost", Integer.parseInt(System.getProperty("tests.rest.cluster.port", "9200")), "http")
        );
        restClient = builder.build();

        assertNotNull("RestClient must not be null", restClient);

        String procedureId = "test_proc_run_by_id";
        String procedureBody = """
            {
              "procedure": "PROCEDURE testRunById() BEGIN RETURN 'executed'; END PROCEDURE"
            }
            """;

        // First, store the procedure
        Request putRequest = new Request("PUT", "/_query/plesql/procedure/" + procedureId);
        putRequest.setJsonEntity(procedureBody);
        Response putResponse = restClient.performRequest(putRequest);
        assertEquals(RestStatus.OK.getStatus(), putResponse.getStatusLine().getStatusCode());

        // Then, execute the procedure by ID
        Request execRequest = new Request("POST", "/_query/plesql/procedure/" + procedureId + "/_execute");
        Response execResponse = restClient.performRequest(execRequest);

        assertEquals(RestStatus.OK.getStatus(), execResponse.getStatusLine().getStatusCode());
        assertNotNull("Execution response should not be null", execResponse);
        assertEquals("/_query/plesql/procedure/" + procedureId + "/_execute", execResponse.getRequestLine().getUri());

        Map<String, Object> responseMap = XContentHelper.convertToMap(
            new BytesArray(execResponse.getEntity().getContent().readAllBytes()), true, XContentType.JSON
        ).v2();
        assertEquals("executed", responseMap.get("result"));
    }
}
