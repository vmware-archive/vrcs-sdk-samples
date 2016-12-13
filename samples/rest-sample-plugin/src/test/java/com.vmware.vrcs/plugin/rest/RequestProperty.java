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

import java.util.LinkedList;
import java.util.List;

import com.vmware.fms.tile.common.TileProperties;
import com.vmware.fms.tile.common.TileUtils;
import com.vmware.fms.tile.test.TileVerificationExecutor;

public class RequestProperty {
    private String endpointUrl;
    private String endpointUsername;
    private String endpointPassword;
    private String path;
    private List<TileProperties> headers;
    private String method;
    private String body;
    private String expectedStatuses;
    private String expectedResponse;
    private boolean setPoll;
    private int pollInterval;
    private int timeout;
    private TileUtils tileUtils;

    public RequestProperty(TileVerificationExecutor tileExecutor) {
        this.endpointUrl = "http://localhost:9090";
        this.endpointUsername = "";
        this.endpointPassword = "";
        this.path = "/";
        this.headers = new LinkedList<TileProperties>();
        this.method = "GET";
        this.body = "";
        this.expectedStatuses = "";
        this.expectedResponse = "";
        this.setPoll = false;
        this.pollInterval = 0;
        this.timeout = 0;
        this.tileUtils = tileExecutor.getRequest().getTileUtils();
    }

    public void setEndpoint(String url, String username, String password) {
        this.endpointUrl = url;
        this.endpointUsername = username;
        this.endpointPassword = password;
    }

    public void setPath(String requestPath) {
        this.path = requestPath;
    }

    public void setHeaders(LinkedList<TileProperties> headers) {
        this.headers = headers;
    }

    public void setMethod(String requestAction) {
        this.method = requestAction;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setExpectedStatus(String status) {
        this.expectedStatuses = status;
    }

    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    public void setAsynchronous(boolean setPoll) {
        this.setPoll = setPoll;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    public TileProperties getRequestTileProperty() {
        TileProperties endpointTileProperty = this.tileUtils.newTileProperties();
        endpointTileProperty.setString("url", this.endpointUrl);
        endpointTileProperty.setString("username", this.endpointUsername);
        endpointTileProperty.setString("password", this.endpointPassword);

        TileProperties tileProperties = this.tileUtils.newTileProperties();
        tileProperties.setProperties("endpoint", endpointTileProperty);
        tileProperties.setString("path", this.path);
        tileProperties.setPropertiesArray("headers", this.headers);
        tileProperties.setString("method", this.method);
        tileProperties.setString("body", this.body);
        tileProperties.setString("expectedStatuses", this.expectedStatuses);
        tileProperties.setString("expectedResponse", this.expectedResponse);
        tileProperties.setBoolean("poll", this.setPoll);
        tileProperties.setInteger("interval", this.pollInterval);
        tileProperties.setInteger("timeout", this.timeout);
        return tileProperties;
    }

    public TileProperties getEndpointTileProperty() {
        TileProperties endpointTileProperty = this.tileUtils.newTileProperties();
        endpointTileProperty.setString("url", this.endpointUrl);
        endpointTileProperty.setString("username", this.endpointUsername);
        endpointTileProperty.setString("password", this.endpointPassword);
        return endpointTileProperty;
    }
}
