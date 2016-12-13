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

import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.vmware.fms.tile.common.TileExecutable;
import com.vmware.fms.tile.common.TileExecutableRequest;
import com.vmware.fms.tile.common.TileExecutableResponse;
import com.vmware.fms.tile.common.TileProperties;
import com.vmware.vrcs.plugin.rest.utils.RESTClient;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTException;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTRequest;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTResponse;

public class TaskTile implements TileExecutable {

    private static final Logger logger = Logger.getLogger(TaskTile.class.getName());

    private static final String ASYNC_PROGRESS_CODE = "Polling";
    private static final String ASYNC_PROGRESS_MESSAGE = "Asynchronous request has been polling for %d sec";
    private static final String ASYNC_TIMEOUT_FAIL = "Asynchronous request timed out after %d sec";
    private static final String ASYNC_PARAMETERS_FAIL = "Asynchronous request failed because interval, timeout and expected response must be specified";
    private static final String EXPECTED_RESPONSE_FAIL = "Request failed with unexpected response";

    public void handleExecute(TileExecutableRequest request, TileExecutableResponse response) {
        // Execution tiles are the ones that run when a pipeline containing a task backed by your plugin executes. These
        // tiles can operate synchronously like the EndpointTile or PreviewTile or they can operate asynchronously. In
        // case of an asynchronous execution, this method is called multiple times are an interval that can be specified
        // by the tile. At the end of each call to handleExecute the tile must also indicate if it needs to be called again.
        logger.info("Executing REST task.");

        // Get the endpoint properties from the request.
        TileProperties endpointProperties = request.getInputProperties().getAsProperties("endpoint");
        String endpointUrl = endpointProperties.getAsString("url");
        String endpointUsername = endpointProperties.getAsString("username", "");
        String endpointPassword = endpointProperties.getAsString("password", "");

        // Get the REST properties from the request.
        Map<String, String> headers = request.getInputProperties().getAsPropertiesArray("headers").stream().collect(Collectors.toMap(
                headerProperty -> headerProperty.getAsString("name"),
                headerProperty -> headerProperty.getAsString("value")));
        String path = request.getInputProperties().getAsString("path");
        String method = request.getInputProperties().getAsString("method");
        String body = request.getInputProperties().getAsString("body", "");
        String expectedStatuses = request.getInputProperties().getAsString("expectedStatuses", "");
        String expectedResponse = request.getInputProperties().getAsString("expectedResponse", "");

        // Get the polling properties from the request. In the case of primitives other than strings there are
        // typed getters for Boolean, Integer and Number types where default values can also be provided.
        boolean isPoll = request.getInputProperties().getAsBoolean("poll", false);
        int interval = request.getInputProperties().getAsInteger("interval", 0);
        int timeout = request.getInputProperties().getAsInteger("timeout", 0);

        // If this is a poll request and it is the first time the tile is executing we need to set up for asynchronous execution
        if (isPoll && request.isFirstExecution()) {
            // Validate that all the information needed for an asynchronous execution is present.
            if (interval == 0 || timeout == 0 || expectedResponse.isEmpty()) {
                // If not, fail the request by calling setFailed with the error message and returning
                logger.severe(ASYNC_PARAMETERS_FAIL);
                response.setFailed(ASYNC_PARAMETERS_FAIL);
                return;
            }

            // Set up for asynchronous execution by taking the following steps:
            // 1. Create a hidden output property (designated by the __ prefix) used to store our execution state.
            //    This is not communicated back to the user. (Optional)
            // 2. Set the execution interval to the pollInterval so that we are always called back at the correct interval.
            //    This is persistent and only needs to be done once. (Optional)
            // 3. Mark the request as not completed so that we will be called back after the execution interval.
            //    This must be done every time we want to be called back otherwise the execution is considered completed.
            response.getOutputProperties().setInteger("__attempts", 1);
            response.setExecutionIntervalSeconds(interval);
            response.setCompleted(false);
            logger.info(String.format("Starting asynchronous request with a pollInterval of %s and a timeout of %s",
                    interval, timeout));
            return;
        }

        try {
            // Execute the request using the given input properties
            RESTResponse restResponse = RESTClient.execute(new RESTRequest()
                    .setEndpointUrl(endpointUrl)
                    .setEndpointCredentials(endpointUsername, endpointPassword)
                    .setPath(path)
                    .setHeaders(headers)
                    .setMethod(method)
                    .setBody(body));


            // Set status code output as an Integer
            int responseStatus = restResponse.getStatus();
            response.getOutputProperties().setInteger("responseStatus", responseStatus);

            // Set headers output as a JSON object represented by a Map
            response.getOutputProperties().setJson("responseHeaders", restResponse.getHeaders());

            // Set response output as a String
            String responseBody = restResponse.getBody();
            response.getOutputProperties().setString("responseBody", responseBody);

            // Check the expected status codes so we know what status codes should result in failure or continued polling
            boolean isExpected = true;
            if (!expectedStatuses.isEmpty() && !expectedStatuses.contains(String.valueOf(responseStatus))) {
                logger.info("Response status was not one of the expected statuses: " + responseStatus);
                isExpected = false;
            }

            // Check the expected response so we know what response should result in failure or continued polling
            if (!expectedResponse.isEmpty() && !Pattern.compile(expectedResponse).matcher(responseBody).find()) {
                logger.info("Response body did not match the expected expression");
                isExpected = false;
            }

            // Check to see if we received the expected response and can complete this tile execution
            if (isExpected) {
                // The request has completed successfully so no further action is needed
                logger.info("Request completed successfully");
            } else if (isPoll) {
                // If this is an asynchronous request check how long the tile has been running and get the execution state
                int duration = request.getDurationSeconds();
                int attempts = response.getOutputProperties().getAsInteger("__attempts");
                logger.info(String.format("Response did not match expectations after %d attempts over %d sec", attempts, duration));
                if (duration >= timeout) {
                    // If we have reached the timeout, fail the request. The tile will not be called back after this
                    String failureMessage = String.format(ASYNC_TIMEOUT_FAIL, duration);
                    logger.severe(failureMessage);
                    response.setFailed(failureMessage);
                } else {
                    // Otherwise, in order to continue polling we take the following steps:
                    // 1. Update our execution state (Optional)
                    // 2. Set a user friendly progress message and code (Optional)
                    // 3. Mark the request as not completed. The execution interval need not be set unless you want to change it
                    response.getOutputProperties().setInteger("__attempts", attempts + 1);
                    response.setProgressMessage(String.format(ASYNC_PROGRESS_MESSAGE, duration));
                    response.setProgressCode(ASYNC_PROGRESS_CODE);
                    response.setCompleted(false);
                }
            } else {
                // If this a synchronous request then fail the request
                logger.severe(EXPECTED_RESPONSE_FAIL);
                response.setFailed(EXPECTED_RESPONSE_FAIL);
            }
        } catch (RESTException ex) {
            logger.info("Failed to execute REST task.");
            response.setFailed(ex.getMessage());
        }
    }
}