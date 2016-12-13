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
import java.util.stream.Collectors;

import com.vmware.fms.tile.common.TileExecutable;
import com.vmware.fms.tile.common.TileExecutableRequest;
import com.vmware.fms.tile.common.TileExecutableResponse;
import com.vmware.fms.tile.common.TileProperties;
import com.vmware.vrcs.plugin.rest.utils.RESTClient;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTException;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTRequest;
import com.vmware.vrcs.plugin.rest.utils.RESTClient.RESTResponse;

public class PreviewTile implements TileExecutable {

    private static final Logger logger = Logger.getLogger(PreviewTile.class.getName());

    public void handleExecute(TileExecutableRequest request, TileExecutableResponse response) {
        // Helper tiles provide a way for the tile UIs to get information that would be useful for the user of that UI.
        // In this case this helper allows a user of the config UI to see a preview of the REST call they are configuring
        // which is built by concatenating the status, headers and response into a single string output.
        logger.info("Getting REST preview.");

        // Get the endpoint properties from the request.
        TileProperties endpointProperties = request.getInputProperties().getAsProperties("endpoint");
        String endpointUrl = endpointProperties.getAsString("url");
        String endpointUsername = endpointProperties.getAsString("username", "");
        String endpointPassword = endpointProperties.getAsString("password", "");

        // Get the REST properties from the request. Custom datatypes are stored as a TileProperties object which
        // represent the underlying JSON object. In this case we retrieve a list of TileProperties objects
        // representing each header as the RESTHeader datatype which has the following schema:
        // {
        //   name : <<header name>>,
        //   value : <<header value>>
        // }
        // This data is generated by the the table Alpaca control in config.js which has a schema maching the RESTHeader
        // datatype and we use the code below to convert it to a map of headers as required by the RESTClient
        Map<String, String> headers = request.getInputProperties().getAsPropertiesArray("headers").stream().collect(Collectors.toMap(
                headerProperty -> headerProperty.getAsString("name"),
                headerProperty -> headerProperty.getAsString("value")));
        String path = request.getInputProperties().getAsString("path");
        String method = request.getInputProperties().getAsString("method");
        String body = request.getInputProperties().getAsString("body", "");

        try {
            // Execute the request using the given input properties
            RESTResponse restResponse = RESTClient.execute(new RESTRequest()
                    .setEndpointUrl(endpointUrl)
                    .setEndpointCredentials(endpointUsername, endpointPassword)
                    .setPath(path)
                    .setHeaders(headers)
                    .setMethod(method)
                    .setBody(body));

            // Construct the response preview from the RESTResponse information
            StringBuilder responsePreviewBuilder = new StringBuilder();

            // Add the response status
            responsePreviewBuilder.append("Response status: ")
                    .append(restResponse.getStatus())
                    .append('\n');

            // Add the header fields
            responsePreviewBuilder.append("\nResponse headers:\n");
            restResponse.getHeaders().entrySet().stream().forEach(entries -> responsePreviewBuilder.append(entries.getKey())
                    .append(": ")
                    .append(entries.getValue())
                    .append('\n'));

            // Add the response body
            responsePreviewBuilder.append("\nResponse body:\n");
            responsePreviewBuilder.append(restResponse.getBody());

            //Set the responsePreview output as a String
            response.getOutputProperties().setString("responsePreview", responsePreviewBuilder.toString());
        } catch (RESTException ex) {
            logger.info("Failed to get REST preview.");
            response.setFailed(ex.getMessage());
        }
    }
}