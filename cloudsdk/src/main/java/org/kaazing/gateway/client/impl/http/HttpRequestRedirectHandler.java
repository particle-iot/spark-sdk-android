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

import static java.util.Collections.unmodifiableMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.impl.wseb.WebSocketEmulatedChannel;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.StringUtils;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
import org.kaazing.net.http.HttpRedirectPolicy;
/*
 * WebSocket Emulated Handler Chain
 * EmulateHandler  
 *                |- CreateHandler - HttpRequestAuthenticationHandler - {HttpRequestRedirectHandler} - HttpRequestBridgeHandler
 *                   |- UpstreamHandler - HttpRequestBridgeHandler
 *                |- DownstreamHandler - HttpRequestBridgeHandler    
 * Responsibilities:
 *     a). handle redirect (HTTP 301)
 *             
 * TODO:
 *         n/a
 */
public class HttpRequestRedirectHandler extends HttpRequestHandlerAdapter {

    private static final String CLASS_NAME = HttpRequestRedirectHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    
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
                case 301:
                case 302:
                case 307:
                    // handle the redirect (possibly cross-scheme)
                    String redirectedLocation = response.getHeader("Location");
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("redirectedLocation = " + StringUtils.stripControlCharacters(redirectedLocation));
                    }
                    
                    if (redirectedLocation == null) {
                        throw new IllegalStateException("Redirect response missing location header: " + responseCode);
                    }

                    try {
                        HttpURI uri = new HttpURI(redirectedLocation);
                        
                        HttpRequest redirectRequest = new HttpRequest(request.getMethod(), uri, request.isAsync());
                        redirectRequest.parent = request.parent;
                        WebSocketEmulatedChannel channel = (WebSocketEmulatedChannel)request.parent.getParent();
                        channel.redirectUri = uri;               
                       
                        WebSocketCompositeChannel compChannel = (WebSocketCompositeChannel)channel.getParent();
                        HttpRedirectPolicy policy = compChannel.getFollowRedirect();
                        URI currentURI = channel.getLocation().getURI();
                        URI redirectURI = uri.getURI();
                        
                        // When redirected while using emulated connection. the schemes of the currentURI and
                        // redirectURI will be different. So, we should normalize it before enforcing the
                        // redirect policy.
                        String normalizedRedirectScheme = redirectURI.getScheme().toLowerCase().replace("http", "ws");
                        URI normalizedRedirectURI = new URI(normalizedRedirectScheme, redirectURI.getSchemeSpecificPart(), null);
                        if ((policy != null) && (policy.compare(currentURI, normalizedRedirectURI) != 0)) {
                            String s = String.format("%s: Cannot redirect from '%s' to '%s'",
                                                     policy, currentURI, normalizedRedirectURI);
                            channel.preventFallback = true;
                            throw new IllegalStateException(s);
                        }

                        for (Entry<String, String> entry : request.getHeaders().entrySet()) {
                            redirectRequest.setHeader(entry.getKey(), entry.getValue());
                        }
                        nextHandler.processOpen(redirectRequest);

                    } catch (Exception e) {
                        LOG.log(Level.WARNING, e.getMessage(), e);
                        throw new IllegalStateException("Redirect to a malformed URL: " + redirectedLocation, e);
                    }
                    break;
                
                default:
                    listener.requestLoaded(request, response);
                    break;
                }
            }
            
            @Override
            public void requestClosed(HttpRequest request) {
                listener.requestClosed(request);
            }
            
            @Override
            public void requestAborted(HttpRequest request) {
                listener.requestAborted(request);
            }
            
            @Override
            public void errorOccurred(HttpRequest request, Exception exception) {
                listener.errorOccurred(request, exception);
            }
        });
    }
}
