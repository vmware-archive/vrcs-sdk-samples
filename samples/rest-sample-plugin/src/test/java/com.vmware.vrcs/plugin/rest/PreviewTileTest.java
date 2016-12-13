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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import com.vmware.fms.tile.common.TileExecutableResponse;
import com.vmware.fms.tile.test.TileVerificationExecutor;

public class PreviewTileTest extends TestCase {
    private static final int TILE_EXECUTION_TIMEOUT_SEC = 100;
    private PreviewTile tile;
    WireMockServer server = new WireMockServer(options().port(9091));

    @Before
    public void setUp() throws Exception {
        this.tile = new PreviewTile();
        this.server.start();
    }

    @Test
    public void testPreview() throws Exception {
        this.server.stubFor(WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Success</response>")));
        TileVerificationExecutor tileExecutor = new TileVerificationExecutor(this.tile);
        RequestProperty requestProperty = new RequestProperty(tileExecutor);
        requestProperty.setEndpoint("http://localhost:9091", "", "");
        requestProperty.setPath("/test");
        tileExecutor.setInputProperties(requestProperty.getRequestTileProperty());
        tileExecutor.executeOneIteration(TILE_EXECUTION_TIMEOUT_SEC);
        TileExecutableResponse tileResponse = tileExecutor.getResponse();
        String responseString = tileResponse.getOutputProperties().getAsString("responsePreview");
        assertTrue(responseString.contains("Response status: 200"));
        assertTrue(responseString.contains("Response headers:"));
        assertTrue(responseString.contains("Content-Type"));
        assertTrue(responseString.contains("text/xml"));
        assertTrue(responseString.contains("Response body:"));
    }
}