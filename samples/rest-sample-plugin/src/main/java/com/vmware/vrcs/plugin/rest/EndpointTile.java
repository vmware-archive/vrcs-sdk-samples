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

import java.net.HttpURLConnection;
import java.util.logging.Logger;

import com.vmware.fms.tile.common.TileExecutable;
import com.vmware.fms.tile.common.TileExecutableRequest;
import com.vmware.fms.tile.common.TileExecutableResponse;
import com.vmware.fms.tile.common.TileProperties;
import com.vmware.vrcs.plugin.rest.utils.RESTClient;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTException;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTRequest;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTResponse;

public class EndpointTile implements TileExecutable {

    private static final Logger logger = Logger.getLogger(EndpointTile.class.getName());

    private static final String NO_URL_ERROR = "REST Endpoint must contain URL.";
    private static final String LOCALHOST_ERROR = "REST Endpoint URL cannot be localhost.";
    private static final String AUTH_MALFORMED_ERROR = "REST Endpoint username or password is empty.";
    private static final String UNAUTHORIZED_ERROR = "REST Endpoint credentials are invalid. (Credentials are passed using basic auth)";

    public void handleExecute(TileExecutableRequest request, TileExecutableResponse response) {
        // Endpoint tiles validate all the properties of an endpoint. If all properties are valid the tile succeeds and
        // has no output properties. In the case of an issue with an endpoint property, the tile should fail by calling
        // setFailed with an appropriate message indicating the validation error and returning.
        logger.info("Validating REST endpoint.");

        // Get the endpoint properties from the request. Optional properties are retrieved with default values.
        TileProperties endpointProperties = request.getInputProperties().getAsProperties("endpoint");
        String endpointUrl = endpointProperties.getAsString("url");
        String endpointUsername = endpointProperties.getAsString("username", "");
        String endpointPassword = endpointProperties.getAsString("password", "");

        // Validate the endpoint properties
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            logger.severe(NO_URL_ERROR);
            response.setFailed(NO_URL_ERROR);
            return;
        }

        if (endpointUrl.contains("localhost") || endpointUrl.contains("127.0.0.1") || endpointUrl.contains("::1")) {
            logger.severe(LOCALHOST_ERROR);
            response.setFailed(LOCALHOST_ERROR);
            return;
        }

        if (endpointUsername.isEmpty() != endpointPassword.isEmpty()) {
            logger.severe(AUTH_MALFORMED_ERROR);
            response.setFailed(AUTH_MALFORMED_ERROR);
            return;
        }


        try {
            // Execute the request as a GET (default)
            RESTResponse restResponse = RESTClient.execute(new RESTRequest()
                    .setEndpointUrl(endpointUrl)
                    .setEndpointCredentials(endpointUsername, endpointPassword));

            // Get the response status to ensure the server was reachable
            Integer responseCode = restResponse.getStatus();
            logger.info("Response status: " + responseCode);

            // If Basic Auth was used make sure the response status was not unauthorized
            boolean isUsingBasicAuth = !(endpointUsername.isEmpty() || endpointPassword.isEmpty());
            if (isUsingBasicAuth && responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                logger.severe(UNAUTHORIZED_ERROR);
                response.setFailed(UNAUTHORIZED_ERROR);
            }
        } catch (RESTException ex) {
            // Any unhandled exceptions will also result in an automatic failure of the tile
            logger.info("Failed to validate REST endpoint.");
            response.setFailed(ex.getMessage());
        }
    }
}
