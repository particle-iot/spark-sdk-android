/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.client.impl.http;

import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.Channel;
import org.kaazing.gateway.client.impl.auth.AuthenticationUtil;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.impl.wseb.WebSocketEmulatedChannel;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.StringUtils;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.auth.ChallengeRequest;
import org.kaazing.net.auth.ChallengeResponse;
import org.kaazing.net.impl.util.ResumableTimer;

/*
 * WebSocket Emulated Handler Chain
 * EmulateHandler  
 *                |- CreateHandler - HttpRequestAuthenticationHandler - {HttpRequestRedirectHandler} - HttpRequestBridgeHandler
 *                   |- UpstreamHandler - HttpRequestBridgeHandler
 *                |- DownstreamHandler - HttpRequestBridgeHandler    
 * Responsibilities:
 *     a). handle authentication challenge (HTTP 401)
 *             
 * TODO:
 *         n/a
 */
public class HttpRequestAuthenticationHandler extends HttpRequestHandlerAdapter {

    private static final String CLASS_NAME = HttpRequestAuthenticationHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String WWW_AUTHENTICATE = "WWW-Authenticate: ";
    private static final String APPLICATION_PREFIX = "Application ";

    private static final String HTTP_1_1_START = "HTTP/1.1";
    private static final int HTTP_1_1_START_LEN = HTTP_1_1_START.length();
    private static final byte[] HTTP_1_1_START_BYTES = StringUtils.getUtf8Bytes(HTTP_1_1_START);

    

    
    private void handleClearAuthenticationData(HttpRequest request) {
        Channel channel = getWebSocketChannel(request);
        if(channel == null)
            return;
        ChallengeHandler nextChallengeHandler = null;
        if (channel.challengeResponse != null) {
            nextChallengeHandler = channel.challengeResponse.getNextChallengeHandler();
            channel.challengeResponse.clearCredentials();
            channel.challengeResponse = null;
        }
        channel.challengeResponse = new ChallengeResponse(null, nextChallengeHandler);
    }

    private void handleRemoveAuthenticationData(HttpRequest request) {
        handleClearAuthenticationData(request);
    }

    protected static String[] getLines(WrappedByteBuffer buf) {
        List<String> lineList = new ArrayList<String>();
        while (buf.hasRemaining()) {
            byte next = buf.get();
            List<Byte> lineText = new ArrayList<Byte>();
            while (next != 13) { // CR
                lineText.add(next);
                if (buf.hasRemaining()) {
                    next = buf.get();
                } else {
                    break;
                }
            }
            if (buf.hasRemaining()) {
                next = buf.get(); // should be LF
            }
            byte[] lineTextBytes = new byte[lineText.size()];
            int i = 0;
            for (Byte text : lineText) {
                lineTextBytes[i] = text;
                i++;
            }
            lineList.add(new String(lineTextBytes, UTF_8));
        }
        String[] lines = new String[lineList.size()];
        lineList.toArray(lines);
        return lines;
    }

    public static boolean isHTTPResponse(WrappedByteBuffer buf) {
        if ( buf.remaining() < HTTP_1_1_START_LEN) {
            return false;
        }

        for (int i = 0; i < HTTP_1_1_START_LEN; i++) {
            if (buf.getAt(i) != HTTP_1_1_START_BYTES[i]) {
                return false;
            }
        }

        return true;
    }

    private void onLoadWrappedHTTPResponse(HttpRequest request, HttpResponse response) throws Exception {
        LOG.entering(CLASS_NAME, "onLoadWrappedHTTPResponse");

        WrappedByteBuffer responseBody = response.getBody();
        String[] lines = getLines(responseBody);

        int statusCode = Integer.parseInt(lines[0].split(" ")[1]);
        if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            String wwwAuthenticate = null;
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].startsWith(WWW_AUTHENTICATE)) {
                    wwwAuthenticate = lines[i].substring(WWW_AUTHENTICATE.length());
                    break;
                }
            }
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("connectToWebSocket.onLoadWrappedHTTPResponse: WWW-Authenticate: " + StringUtils.stripControlCharacters(wwwAuthenticate));
            }

            if (wwwAuthenticate == null || "".equals(wwwAuthenticate)) {
                throw new IllegalStateException("Missing authentication challenge in wrapped HTTP 401 response");
            } else if (!wwwAuthenticate.startsWith(APPLICATION_PREFIX)) {
                throw new IllegalStateException("Only Application challenges are supported by the client");
            }
            
            String rawChallenge = wwwAuthenticate.substring(APPLICATION_PREFIX.length());
            handle401(request, rawChallenge);
        }
        else {
            throw new IllegalStateException("Unsupported wrapped response with HTTP status code " + statusCode);
        }
    }

    private void handle401(HttpRequest request, String challenge) throws Exception {
        LOG.entering(CLASS_NAME, "handle401");

        HttpURI uri = request.getUri();
        WebSocketEmulatedChannel channel = (WebSocketEmulatedChannel)getWebSocketChannel(request);
        if(channel == null) {
            throw new IllegalStateException("There is no WebSocketChannel associated with this request");    
        }
        if (isWebSocketClosing(request)) {
            return;   //WebSocket is closing/closed, quit authenticate process
        }
        
        ResumableTimer connectTimer = null;
        if (((WebSocketCompositeChannel)channel.getParent()) != null) {
            WebSocketCompositeChannel parent = (WebSocketCompositeChannel)channel.getParent();
            connectTimer = parent.getConnectTimer();
            if (connectTimer != null) {
                // Pause the connect timer while the user is providing the credentials.
                connectTimer.pause();
            }
        }

        channel.authenticationReceived = true;
        String challengeUrl = channel.getLocation().toString();
        if (channel.redirectUri != null) {
            String path = channel.redirectUri.getPath();
            if ((path != null) && path.contains("/;e/")) {
                // path "/;e/cbm" was added in WebSocketEmulatedHandler.  It is 
                // returned by balancer, so we should remove it.
                int index = path.indexOf("/;e/");
                path = path.substring(0, index);
            }

            challengeUrl = channel.redirectUri.getScheme() + "://" + channel.redirectUri.getURI().getAuthority() + path;
        }
        ChallengeRequest challengeRequest = new ChallengeRequest(challengeUrl, challenge);
        try {
            channel.challengeResponse = AuthenticationUtil.getChallengeResponse(channel, challengeRequest, channel.challengeResponse);
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage());
            handleClearAuthenticationData(request);
            throw new IllegalStateException("Unexpected error processing challenge "+challenge, e);
        }

        if (channel.challengeResponse == null || channel.challengeResponse.getCredentials() == null) {
            throw new IllegalStateException("No response possible for challenge "+challenge);
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("response from challenge handler = " + StringUtils.stripControlCharacters(String.valueOf(channel.challengeResponse.getCredentials())));
        }

        try {
            HttpRequest newRequest = new HttpRequest(request.getMethod(), uri, request.isAsync());
            newRequest.parent = request.parent;
            
//            newRequest.setHeader("Content-Type", "text/plain");
            for (Entry<String, String> entry : request.getHeaders().entrySet()) {
                newRequest.setHeader(entry.getKey(), entry.getValue());
            }

            // Resume the connect timer before invoking processOpen().
            if (connectTimer != null) {
                connectTimer.resume();
            }

            processOpen(newRequest);
        }
        catch (Exception e1) {
            LOG.log(Level.FINE, e1.getMessage(), e1);
            throw new Exception("Unable to authenticate user", e1);
        }
    }

    @Override
    public void processOpen(HttpRequest request) {
        WebSocketEmulatedChannel channel = (WebSocketEmulatedChannel)getWebSocketChannel(request);
        if(channel != null) {
            if (isWebSocketClosing(request)) {
                return;   //WebSocket is closing/closed, quit authenticate process
            }
            if (channel.challengeResponse.getCredentials() != null) {
                String credentials = new String(channel.challengeResponse.getCredentials());
                LOG.finest("requestOpened: Authorization: " + StringUtils.stripControlCharacters(credentials));
                request.setHeader(HEADER_AUTHORIZATION, credentials);
                handleClearAuthenticationData(request);
            }
        }
        nextHandler.processOpen(request);
    }

    @Override
    public void setNextHandler(HttpRequestHandler handler) {
        super.setNextHandler(handler);
        
        handler.setListener(new HttpRequestListener() {
            
            @Override
            public void requestReady(HttpRequest request) {
                listener.requestReady(request);
            }
            
            @Override
            public void requestOpened(HttpRequest request) {
                listener.requestOpened(request);
            }
            
            @Override
            public void requestProgressed(HttpRequest request, WrappedByteBuffer payload) {
                listener.requestProgressed(request, payload);
            }
            
            @Override
            public void requestLoaded(HttpRequest request, HttpResponse response) {
                int responseCode = response.getStatusCode();
                switch (responseCode) {
                case 200:
                    WrappedByteBuffer responseBuffer = response.getBody();
                    if (isHTTPResponse(responseBuffer)) {
                        try {
                            onLoadWrappedHTTPResponse(request, response);
                        } catch (Exception e) {
                            LOG.log(Level.FINE, e.getMessage(), e);
                            listener.errorOccurred(request, e);
                        }
                    }
                    else {
                        handleRemoveAuthenticationData(request);
                        listener.requestLoaded(request, response);
                    }
                    break;
                
                case 401:
                    String challenge = response.getHeader(HEADER_WWW_AUTHENTICATE);
                    try {
                        handle401(request, challenge);
                    } catch (Exception e) {
                        LOG.log(Level.FINE, e.getMessage());
                        listener.errorOccurred(request, e);
                    }
                    break;
                    
                default:
                    handleRemoveAuthenticationData(request);
                    listener.requestLoaded(request, response);
                }
            }
            
            @Override
            public void requestClosed(HttpRequest request) {
                handleRemoveAuthenticationData(request);
            }

            @Override
            public void errorOccurred(HttpRequest request, Exception exception) {
                handleRemoveAuthenticationData(request);
                listener.errorOccurred(request, exception);
            }

            @Override
            public void requestAborted(HttpRequest request) {
                handleRemoveAuthenticationData(request);
                listener.requestAborted(request);
            }
        });
    }
        
    
    @Override
    public void setListener(HttpRequestListener listener) {
        this.listener = listener;
    }
}
