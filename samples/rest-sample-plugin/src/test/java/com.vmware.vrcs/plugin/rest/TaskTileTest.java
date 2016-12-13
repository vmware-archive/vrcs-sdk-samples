/*
 * Copyright © 2016 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Some files may be comprised of various open source software components, each of which
 * has its own license that is located in the source code of the respective component.
 */

package com.vmware.vrcs.plugin.rest;


import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.StrictAssertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vmware.fms.tile.common.TileExecutableResponse;
import com.vmware.fms.tile.common.TileProperties;
import com.vmware.fms.tile.executor.TilePropertiesImpl;
import com.vmware.fms.tile.test.TileVerificationExecutor;

public class TaskTileTest extends TestCase {
    private static final int TILE_EXECUTION_TIMEOUT_SEC = 100;
    private static final String MALFORMED_ERROR = "URL is malformed.";
    private static final String BAD_METHOD = "Method is not supported.";
    private static final String IO_ERROR = "Unable to read from/write to connection: ";
    private static final String REQUEST_FAIL = "Request failed with response code:";
    private static final String EXPECTED_RESPONSE_FAIL = "Request failed with unexpected response";
    private static final String ASYNC_TIMEOUT_FAIL = "Asynchronous request timed out";

    private TaskTile tile;

    // WireMock is an HTTP mock server. The core is web server that can be primed to serve canned responses to particular requests.
    WireMockServer server = new WireMockServer();

    @Before
    // If you need to initialize the same data for each test, you put that data in instance variables and initialize
    // them in a @Before setUp method. The setUp method is called before each @Test method.
    // If that data needs to be cleaned up, implement an @After tearDown method. The tearDown method is called after each @Test method.
    // In this case, the same WireMock server in port 9090 is used, so manual start() and stop() should be called before and after each @Test method.
    // WireMock supports matching of requests to stubs, one way to create the stub is via JSON API, JSON files are placed under resources/mappings directory.
    public void setUp() throws Exception {
        this.tile = new TaskTile();
        this.server = new WireMockServer(wireMockConfig().port(9090)
                                        .extensions(new PollableHostTransformer()));
        this.server.start();
    }

    @After
    public void tearDown() throws Exception {
        this.server.stop();
    }

    @Test
    public void testRESTExecution() throws Exception {
        // This is another way to create stub, the following stub code will configure a response with a status of 200 to be returned when the relative
        // URL exactly matches "/test". Body of the response will be “<response>Success</response>” and a Content-Type header will be sent with value of text/xml.
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Success</response>")));
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/test");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int result = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        Map<String, Object> headers = tileResponse.getOutputProperties().getAsJson("responseHeaders");
        assertEquals(200, result);
        assertTrue(responseBody.contains("<response>Success</response>"));
        assertTrue(headers.keySet().contains("Content-Type") && headers.get("Content-Type").equals("text/xml"));
        assertTrue(headers.keySet().contains("Status-Line") && headers.get("Status-Line").toString().contains("200"));
    }

    @Test
    public void testRESTPost() throws Exception {
        this.server.stubFor(WireMock.post(WireMock.urlEqualTo("/test/post"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Sample post is successful! </response>")));
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/test/post");
        requestProperty.setMethod("POST");
        requestProperty.setBody("It's sample post contents.");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        Map<String, Object> headers = tileResponse.getOutputProperties().getAsJson("responseHeaders");
        assertEquals(200, responseStatus);
        assertTrue(responseBody.contains("Sample post is successful!"));
        assertTrue(headers.keySet().contains("Content-Type") && headers.get("Content-Type").equals("text/xml"));
        assertTrue(headers.keySet().contains("Status-Line") && headers.get("Status-Line").toString().contains("200"));
    }

    @Test
    public void testRESTPut() throws Exception {
        this.server.stubFor(WireMock.put(WireMock.urlEqualTo("/test/put"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Sample put is successful! </response>")));
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/test/put");
        requestProperty.setMethod("PUT");
        requestProperty.setBody("It's sample put contents.");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(200, responseStatus);
        assertTrue(responseBody.contains("Sample put is successful!"));
    }

    @Test
    public void testRequestType() throws Exception {
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setMethod("REQUEST");
        TileProperties inputProperties = requestProperty.getRequestTileProperty();
        tileExecutor.setInputProperties(inputProperties);
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertTrue(tileResponse.isFailed());
        assertEquals(BAD_METHOD, tileResponse.getFailureMessage());
    }

    @Test
    public void testNoEndpointFail() throws Exception {
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setEndpoint("", "", "");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertTrue(tileResponse.isFailed());
        assertThat(tileResponse.getFailureMessage().contains(MALFORMED_ERROR));
    }

    @Test
    public void testUnknownHost() throws Exception {
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setEndpoint("http://www.aabbcc", "", "");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertTrue(tileResponse.isFailed());
        String failureMessage = tileResponse.getFailureMessage();
        assertThat(failureMessage).contains(IO_ERROR);
    }

    @Test
    public void testMultipleExpectedStatuses() throws Exception {
        // This test is calling file: /resources/mappings/mockDataTest.json
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/Test");
        requestProperty.setExpectedStatus("201, 202");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertThat(tileResponse.isFailed());
        assertThat(tileResponse.getFailureMessage().contains(REQUEST_FAIL));

        requestProperty.setExpectedStatus("200, 201, 202");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        tileResponse = tileExecutor.getResponse();
        assertThat(!tileResponse.isFailed());
        assertEquals(200, tileResponse.getOutputProperties().getAsInteger("responseStatus").intValue());
        assertThat(tileResponse.getOutputProperties().getAsString("responseBody").contains("Test is successful!"));
    }

    @Test
    public void testExpectedResponse() throws Exception {
        // This test is calling file: /resources/mappings/mockDataTest.json
        // Empty regular expression
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setExpectedResponse("");
        requestProperty.setPath("/Test");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertEquals(false, tileResponse.isFailed());

        // Actual response matches with expected response
        requestProperty.setExpectedResponse("^.*Test is successful!.*$");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        tileResponse = tileExecutor.getResponse();
        assertEquals(false, tileResponse.isFailed());

        // Do not match with expected response
        requestProperty.setExpectedResponse("\\.TEST");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        tileResponse = tileExecutor.getResponse();
        assertThat(tileResponse.getFailureMessage().contains(EXPECTED_RESPONSE_FAIL));
    }

    @Test
    public void testDifferentHeaders() throws Exception {
        // This test is calling file: /resources/mappings/mockDataHeaders.json
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/header/post");
        requestProperty.setMethod("POST");

        // Correct header setting
        LinkedList<TileProperties> headers1 = new LinkedList<TileProperties>();
        TileProperties tileProperty1 = new TilePropertiesImpl();
        tileProperty1.setString("name", "Content-Type");
        tileProperty1.setString("value", "application/json");
        headers1.add(tileProperty1);
        requestProperty.setHeaders(headers1);
        requestProperty.setBody("{\n"
                + "\t\"requestType\": \"LOGIN\"\n"
                + "}");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(200, responseStatus);
        assertThat(responseBody.contains("Header test is successful!"));

        // Wrong header setting
        LinkedList<TileProperties> headers2 = new LinkedList<TileProperties>();
        TileProperties tileProperty2 = new TilePropertiesImpl();
        tileProperty2.setString("name", "Accept");
        tileProperty2.setString("value", "application/json");
        headers2.add(tileProperty2);
        requestProperty.setHeaders(headers2);
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        tileResponse = tileExecutor.getResponse();
        responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(404, responseStatus);
        assertThat(responseBody.contains("Error 404"));
    }

    @Test
    public void testBasicAuthentication() throws Exception {
        // This test is calling file: /resources/mappings/mockDataAuthentication.json
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setEndpoint("http://localhost:9090", "myUsername", "myPassword");
        requestProperty.setPath("/header/auth");

        // Endpoint Basic Authentication
        LinkedList<TileProperties> headers = new LinkedList<TileProperties>();
        String encodingAuthorization = requestProperty.getEndpointTileProperty().getAsString("username") + ":"
                + requestProperty.getEndpointTileProperty().getAsString("password");
        encodingAuthorization = Base64.getEncoder().encodeToString(encodingAuthorization.getBytes(StandardCharsets.UTF_8));
        TileProperties tileProperty = new TilePropertiesImpl();
        tileProperty.setString("name", "Authorization");
        tileProperty.setString("value", "Basic " + encodingAuthorization);
        headers.add(tileProperty);
        requestProperty.setHeaders(headers);
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(200, responseStatus);
        assertThat(responseBody.contains("Header authentication test is successful!"));

        // Overwrite Basic Authentication
        LinkedList<TileProperties> headers1 = new LinkedList<TileProperties>();
        TileProperties tileProperty1 = new TilePropertiesImpl();
        tileProperty1.setString("name", "Authorization");
        tileProperty1.setString("value", "Basic bXlVc2VybmFtZTpteVBhc3N3b3Jk");
        headers1.add(tileProperty1);
        requestProperty.setHeaders(headers1);
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        tileResponse = tileExecutor.getResponse();
        responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(200, responseStatus);
        assertThat(responseBody.contains("Header authentication test is successful!"));
    }

    @Test
    public void testBasicAuthenticationFail() throws Exception {
        // This test is calling file: /resources/mappings/mockDataAuthenticationFail.json
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/header/auth/fail");

        // Basic Authentication Fail
        LinkedList<TileProperties> headers = new LinkedList<TileProperties>();
        TileProperties tileProperty = new TilePropertiesImpl();
        tileProperty.setString("name", "Authorization");
        tileProperty.setString("value", "Basic wrongAuth");
        headers.add(tileProperty);
        requestProperty.setHeaders(headers);
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(401, responseStatus);
        assertThat(responseBody.contains("Unauthorized"));
    }

    @Test
    public void testDifferentAuthentication() throws Exception {
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/header/auth/token"))
                .withHeader("auth-token", equalTo("my-auth-token"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Token authentication test is successful! </response>")));
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/header/auth/token");
        LinkedList<TileProperties> headers = new LinkedList<TileProperties>();
        TileProperties tileProperty = new TilePropertiesImpl();
        tileProperty.setString("name", "auth-token");
        tileProperty.setString("value", "my-auth-token");
        headers.add(tileProperty);
        requestProperty.setHeaders(headers);
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(200, responseStatus);
        assertThat(responseBody.contains("Token authentication test is successful! "));
    }

    static class PollableHostTransformer extends ResponseDefinitionTransformer {
        int attempts = 0;

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
            int numAttempts = parameters.getInt("numAttempts");
            this.attempts++;
            if (this.attempts == 1) {
                return new ResponseDefinitionBuilder()
                        .withStatus(201)
                        .withBody("Body is created")
                        .build();
            } else if (this.attempts < numAttempts) {
                return new ResponseDefinitionBuilder()
                        .withStatus(206)
                        .withBody("Body is in progress")
                        .build();
            }
            return responseDefinition;
        }

        @Override
        public String getName() {
            return "pollableHostTransformer";
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }
    }

    @Test
    public void testPollWithExpectedResponse() throws Exception {
        int pollInterval = 2;
        int timeout = 15;
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/poll/Success"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("Original content")
                        .withTransformers("pollableHostTransformer")
                        .withTransformerParameter("numAttempts", 3)));

        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/poll/Success");
        requestProperty.setAsynchronous(true);
        requestProperty.setPollInterval(pollInterval);
        requestProperty.setTimeout(timeout);
        requestProperty.setExpectedResponse("Original content");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeAndWaitForCompletion(timeout);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertThat(!tileResponse.isFailed());
        assertThat(responseBody.contains("Original content"));
    }

    @Test
    public void testPollWithExpectedStatuses() throws Exception {
        int pollInterval = 2;
        int timeout = 15;
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/poll/Success"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("Original content")
                        .withTransformers("pollableHostTransformer")
                        .withTransformerParameter("numAttempts", 3)));

        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/poll/Success");
        requestProperty.setAsynchronous(true);
        requestProperty.setPollInterval(pollInterval);
        requestProperty.setTimeout(timeout);
        requestProperty.setExpectedStatus("200");
        requestProperty.setExpectedResponse("Original content");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeAndWaitForCompletion(timeout);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        assertThat(!tileResponse.isFailed());
        assertEquals(200, responseStatus);
    }

    @Test
    public void testPollBadExpectedResponse() throws TimeoutException {
        int pollInterval = 3;
        int timeout = 3;
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/poll/ExpectedResponse"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("Original content")
                        .withTransformers("pollableHostTransformer")
                        .withTransformerParameter("numAttempts", 3)));

        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/poll/ExpectedResponse");
        requestProperty.setAsynchronous(true);
        requestProperty.setPollInterval(pollInterval);
        requestProperty.setTimeout(timeout);
        requestProperty.setExpectedStatus("201");
        requestProperty.setExpectedResponse("Bad Content");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeAndWaitForCompletion(timeout + 1);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertThat(tileResponse.getFailureMessage().contains(ASYNC_TIMEOUT_FAIL));
    }

    @Test
    public void testPollBadExpectedStatuses() throws TimeoutException {
        int pollInterval = 3;
        int timeout = 3;
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/poll/ExpectedResponse"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("Original content")
                        .withTransformers("pollableHostTransformer")
                        .withTransformerParameter("numAttempts", 3)));

        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/poll/ExpectedResponse");
        requestProperty.setAsynchronous(true);
        requestProperty.setPollInterval(pollInterval);
        requestProperty.setTimeout(timeout);
        requestProperty.setExpectedStatus("404");
        requestProperty.setExpectedResponse("Body is created");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeAndWaitForCompletion(timeout + 1);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertThat(tileResponse.getFailureMessage().contains(ASYNC_TIMEOUT_FAIL));
    }

    @Test
    public void testPollExecuteOneIteration() throws TimeoutException {
        int pollInterval = 3;
        int timeout = 4;
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/poll/ExpectedResponse"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("Original content")
                        .withTransformers("pollableHostTransformer")
                        .withTransformerParameter("numAttempts", 3)));

        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/poll/ExpectedResponse");
        requestProperty.setAsynchronous(true);
        requestProperty.setPollInterval(pollInterval);
        requestProperty.setTimeout(timeout);
        requestProperty.setExpectedResponse("Body is created");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeAndWaitForCompletion(timeout);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(201, responseStatus);
        assertThat(responseBody.contains("Body is created"));
    }

    @Test
    public void testPollExecuteInIterations() throws TimeoutException {
        int pollInterval = 2;
        int timeout = 7;
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/poll/ExpectedResponse"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("Original content")
                        .withTransformers("pollableHostTransformer")
                        .withTransformerParameter("numAttempts", 5)));

        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/poll/ExpectedResponse");
        requestProperty.setAsynchronous(true);
        requestProperty.setPollInterval(pollInterval);
        requestProperty.setTimeout(timeout);
        requestProperty.setExpectedStatus("206");
        requestProperty.setExpectedResponse("Body is in progress");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeAndWaitForCompletion(timeout);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        String responseBody = tileResponse.getOutputProperties().getAsString("responseBody");
        assertEquals(206, responseStatus);
        assertThat(responseBody.contains("Body is in progress"));
    }

    @Test
    public void testPollServerError() throws Exception {
        int pollInterval = 2;
        int timeout = 15;
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/poll/ServerError"))
                .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                        .withTransformers("pollableHostTransformer")
                        .withTransformerParameter("numAttempts", 3)));

        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setPath("/poll/ServerError");
        requestProperty.setAsynchronous(true);
        requestProperty.setPollInterval(pollInterval);
        requestProperty.setTimeout(timeout);
        requestProperty.setExpectedStatus("500");
        requestProperty.setExpectedResponse("Internal Server Error");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeAndWaitForCompletion(timeout);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        int responseStatus = tileResponse.getOutputProperties().getAsInteger("responseStatus");
        assertThat(!tileResponse.isFailed());
        assertEquals(500, responseStatus);
    }
}