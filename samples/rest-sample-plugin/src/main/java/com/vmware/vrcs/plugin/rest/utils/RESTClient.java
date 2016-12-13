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

package com.vmware.vrcs.plugin.rest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RESTClient {

    private static final Logger logger = Logger.getLogger(RESTClient.class.getName());

    private static final String MALFORMED_ERROR = "URL is malformed.";
    private static final String BAD_METHOD_ERROR = "Method is not supported.";
    private static final String IO_ERROR = "Unable to read from/write to connection: ";
    private static final String DISABLE_SSL_ERROR = "Failed to dissable SSL certificate verification with error: ";
    private static final String MAX_RESPONSE_EXCEEDED_ERROR_FMT = "Unable to read response body as it exceeds 4MB, actual size: %.2fMB";

    private static final Double MB = 1048576.0;
    private static final Double MAX_RESPONSE_SIZE_BYTES = 4L * MB;

    public enum Method {
        GET(false),
        POST(true),
        HEAD(false),
        OPTIONS(false),
        PUT(true),
        DELETE(false),
        TRACE(false);

        private Boolean hasBody;

        private Method(Boolean hasBody) {
            this.hasBody = hasBody;
        }
    }

    public static class RESTException extends Exception {
        protected RESTException(String message, Throwable innerException) {
            super(message, innerException);
        }

        protected RESTException(String format, Object... args) {
            super(String.format(format, args));
        }
    }

    public static class RESTRequest {
        private URL endpointUrl;
        private String endpointUsername;
        private String endpointPassword;
        private String path;
        private String body;
        private Method method;
        private Map<String, String> headers;

        public RESTRequest() {
            this.endpointUrl = null;
            this.endpointUsername = "";
            this.endpointPassword = "";
            this.path = "";
            this.body = "";
            this.method = Method.GET;
            this.headers = new HashMap<>();
        }

        public RESTRequest setEndpointUrl(String endpointUrl) throws RESTException {
            try {
                this.endpointUrl = new URL(endpointUrl);
            } catch (MalformedURLException e) {
                logger.severe(MALFORMED_ERROR + e);
                throw new RESTException(MALFORMED_ERROR + e.getMessage(), e);
            }
            return this;
        }

        public RESTRequest setEndpointCredentials(String endpointUsername, String endpointPassword) throws RESTException {
            this.endpointUsername = endpointUsername;
            this.endpointPassword = endpointPassword;
            return this;
        }

        public RESTRequest setPath(String path) {
            this.path = path;
            return this;
        }

        public RESTRequest setBody(String body) {
            this.body = body;
            return this;
        }

        public RESTRequest setMethod(String method) throws RESTException {
            try {
                this.method = Method.valueOf(method);
            } catch (IllegalArgumentException e) {
                logger.severe(BAD_METHOD_ERROR);
                throw new RESTException(BAD_METHOD_ERROR);
            }
            return this;
        }

        public RESTRequest setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        protected URL getUrl() throws RESTException {
            try {
                return new URL(this.endpointUrl, this.path);
            } catch (MalformedURLException e) {
                logger.severe(MALFORMED_ERROR + e);
                throw new RESTException(MALFORMED_ERROR + e.getMessage(), e);
            }
        }

        protected Map<String, String> getHeaders() {
            // Add a basic auth header using information from the endpoint if it an Authorization header was not set
            Map<String, String> requestHeaders = new HashMap<>(this.headers);
            if (!this.headers.containsKey("Authorization") && !this.endpointUsername.isEmpty() && !this.endpointPassword.isEmpty()) {
                logger.info("Adding Basic authentication header using information from the endpoint");
                String encodedCredentials = this.endpointUsername + ":" + this.endpointPassword;
                encodedCredentials = Base64.getEncoder().encodeToString(encodedCredentials.getBytes(StandardCharsets.UTF_8));
                requestHeaders.put("Authorization", "Basic " + encodedCredentials);
            }
            return requestHeaders;
        }

        protected String getBody() {
            return this.body;
        }

        protected Method getMethod() {
            return this.method;
        }
    }


    public static class RESTResponse {
        private int status = 0;
        private Map<String, Object> headers = new HashMap<>();
        private String body = "";
        private long contentLength = 0;

        public int getStatus() {
            return this.status;
        }

        public Map<String, Object> getHeaders() {
            return this.headers;
        }

        public String getBody() throws RESTException {
            if (this.contentLength > MAX_RESPONSE_SIZE_BYTES) {
                String error = String.format(MAX_RESPONSE_EXCEEDED_ERROR_FMT, this.contentLength / MB);
                logger.severe(error);
                throw new RESTException(error);
            }
            return this.body;
        }

        protected RESTResponse(HttpURLConnection connection) throws RESTException {
            try {
                // Get the status code
                this.status = connection.getResponseCode();

                // Get the response headers adding the key Status-Line for the status line
                connection.getHeaderFields().entrySet().stream().forEach(entry -> this.headers.put(
                        entry.getKey() != null ? entry.getKey() : "Status-Line",
                        entry.getValue().stream().collect(Collectors.joining())));

                // Ensure that the response would be less than 4MB before reading as that is the maximum size for tile outputs
                this.contentLength = connection.getHeaderFieldLong("Content-Length", 0L);
                if (this.contentLength > MAX_RESPONSE_SIZE_BYTES) {
                    logger.info("Skipping response body because it exceeds 4MB");
                    return;
                }

                // Get the response body from the input or error stream as needed
                StringBuilder responseBodyBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(this.status < 400 ? connection.getInputStream() : connection.getErrorStream(),
                                StandardCharsets.UTF_8))) {
                    String inputLine;
                    while ((inputLine = reader.readLine()) != null) {
                        responseBodyBuilder.append(inputLine)
                                .append('\n');
                    }
                }
                this.body = responseBodyBuilder.toString();
            } catch (IOException e) {
                logger.severe(IO_ERROR + e);
                throw new RESTException(IO_ERROR + e.getMessage(), e);
            }
        }
    }

    public static RESTResponse execute(RESTRequest restRequest) throws RESTException {
        // Set up the connection object
        HttpURLConnection connection = null;
        try {
            URL url = restRequest.getUrl();
            Method method = restRequest.getMethod();
            String body = restRequest.getBody();
            Map<String, String> headers = restRequest.getHeaders();
            logger.info(String.format("Making %s request to URL %s", method, url));

            // Set HTTP/HTTPS request
            if (url.getProtocol().equals("https")) {
                // Get an HttpsURLConnection that trusts all certificates
                connection = getTrustingHttpsConnection(url);
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            // Set the request headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Set the request method and body if there is one
            connection.setRequestMethod(method.name());
            if (method.hasBody) {
                logger.info("Adding request body");
                connection.setDoOutput(true);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();
            }

            // Process the HTTP response
            return new RESTResponse(connection);
        } catch (IOException e) {
            logger.severe(IO_ERROR + e);
            throw new RESTException(IO_ERROR + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpsURLConnection getTrustingHttpsConnection(URL url) throws RESTException {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            // Set trust manager that trusts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, null);
            connection.setSSLSocketFactory(sc.getSocketFactory());

            // Set hostname verifier that accepts all hosts
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            connection.setHostnameVerifier(allHostsValid);

            return connection;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.severe(DISABLE_SSL_ERROR + e);
            throw new RESTException(DISABLE_SSL_ERROR + e.getMessage(), e);
        } catch (IOException e) {
            logger.severe(IO_ERROR + e);
            throw new RESTException(IO_ERROR + e.getMessage(), e);
        }
    }
}
