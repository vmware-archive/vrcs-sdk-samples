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

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import com.vmware.fms.tile.common.TileExecutableResponse;
import com.vmware.fms.tile.test.TileVerificationExecutor;

public class EndpointTileTest extends TestCase {
    private static final int TILE_EXECUTION_TIMEOUT_SEC = 100;
    private static final String LOCALHOST_ERROR = "REST Endpoint URL cannot be localhost.";
    private static final String MALFORMED_ERROR = "URL is malformed.";
    private static final String AUTH_MALFORMED_ERROR = "REST Endpoint username or password is empty.";
    private static final String IO_ERROR = "Unable to read from/write to connection: ";

    private EndpointTile tile;


    @Before
    public void setUp() throws Exception {
        this.tile = new EndpointTile();
    }

    @Test
    public void testEndpointTileExecution() throws Exception {
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setEndpoint("http://www.aabbccxyzxusdfw.com", "", "");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertTrue(tileResponse.isFailed());
        assertTrue(tileResponse.getFailureMessage().contains(IO_ERROR));

    }

    @Test
    public void testAuthMalformedFail() throws Exception {
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setEndpoint("http://www.aabbcc", "abc", "");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertTrue(tileResponse.isFailed());
        assertEquals(AUTH_MALFORMED_ERROR, tileResponse.getFailureMessage());
    }


    @Test
    public void testMalformedFail() throws Exception {
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setEndpoint("www.aabbcc", "", "");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertTrue(tileResponse.isFailed());
        assertTrue(tileResponse.getFailureMessage().contains(MALFORMED_ERROR));
    }

    @Test
    public void testLocalHostFail() throws Exception {
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setEndpoint("localhost:9090", "", "");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        assertTrue(tileResponse.isFailed());
        assertEquals(LOCALHOST_ERROR, tileResponse.getFailureMessage());

        requestProperty.setEndpoint("127.0.0.1", "", "");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        tileResponse = tileExecutor.getResponse();
        assertEquals(LOCALHOST_ERROR, tileResponse.getFailureMessage());

        requestProperty.setEndpoint("::1", "", "");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        tileResponse = tileExecutor.getResponse();
        assertEquals(LOCALHOST_ERROR, tileResponse.getFailureMessage());
    }
}