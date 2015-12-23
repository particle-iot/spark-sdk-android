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

package org.kaazing.gateway.client.impl.wseb;

import static org.kaazing.gateway.client.impl.Channel.HEADER_SEQUENCE;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.Channel;
import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.http.HttpRequest;
import org.kaazing.gateway.client.impl.http.HttpRequest.Method;
import org.kaazing.gateway.client.impl.http.HttpRequestAuthenticationHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestListener;
import org.kaazing.gateway.client.impl.http.HttpRequestRedirectHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestTransportHandler;
import org.kaazing.gateway.client.impl.http.HttpResponse;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.impl.ws.WebSocketSelectedChannel;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
/*
 * WebSocket Emulated Handler Chain
 * EmulateHandler  
 *                |- {CreateHandler} - HttpRequestAuthenticationHandler - HttpRequestRedirectHandler - HttpRequestBridgeHandler
 *                   |- UpstreamHandler - HttpRequestBridgeHandler
 *                |- DownstreamHandler - HttpRequestBridgeHandler    
 * Responsibilities:
 *     a). process Connect
 *             send httpRequest
 *             if connected, save ControlFrame bytes to channel
 * TODO:
 *         n/a  
 */
class CreateHandlerImpl implements CreateHandler {
    private static final String CLASS_NAME = CreateHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    
    static CreateHandlerFactory FACTORY = new CreateHandlerFactory() {
        @Override
        public CreateHandler createCreateHandler() {
            return new CreateHandlerImpl();
        }
    };
    
    private static final String HEADER_WEBSOCKET_PROTOCOL = "X-WebSocket-Protocol";
    private static final String HEADER_SEC_EXTENSIONS = "X-WebSocket-Extensions";
    private static final String HEADER_WEBSOCKET_VERSION = "X-WebSocket-Version";
    private static final String HEADER_ACCEPT_COMMANDS = "X-Accept-Commands";
    private static final String WEBSOCKET_VERSION = "wseb-1.0";

    HttpRequestHandler nextHandler;
    CreateHandlerListener listener;
    
    HttpRequestAuthenticationHandler authHandler = new HttpRequestAuthenticationHandler();
    HttpRequestRedirectHandler redirectHandler = new HttpRequestRedirectHandler();
    HttpRequestHandler transportHandler = HttpRequestTransportHandler.DEFAULT_FACTORY.createHandler();

    public CreateHandlerImpl() {
        setNextHandler(authHandler);
        authHandler.setNextHandler(redirectHandler);
        redirectHandler.setNextHandler(transportHandler);
    }

    @Override
    public void processOpen(CreateChannel channel, HttpURI location) {
        HttpRequest request = HttpRequest.HTTP_REQUEST_FACTORY.createHttpRequest(Method.GET, location, false);
        
        // request.getHeaders().put(HEADER_SEC_EXTENSIONS, WebSocketHandshakeObject.KAAZING_SEC_EXTENSION_REVALIDATE);
        if (channel.getProtocols() != null && channel.getProtocols().length > 0 ) {
            StringBuilder sb = new StringBuilder();
            for (String p : channel.getProtocols()) {
                sb.append(p);
                sb.append(',');
            }
            
            // strip out comma that gets appended at the end
            request.getHeaders().put(HEADER_WEBSOCKET_PROTOCOL, sb.substring(0, sb.length() - 1));
        }
        request.getHeaders().put(HEADER_SEC_EXTENSIONS, getEnabledExtensions(channel));
        request.getHeaders().put(HEADER_WEBSOCKET_VERSION, WEBSOCKET_VERSION);
        request.getHeaders().put(HEADER_ACCEPT_COMMANDS, "ping");
        request.getHeaders().put(HEADER_SEQUENCE, Long.toString(channel.nextSequence()));
        request.parent = channel;
        channel.setRequest(request);
        nextHandler.processOpen(request);
    }

    @Override
    public void processClose(CreateChannel channel){
       HttpRequest request = channel.getRequest();
       if (request != null) {
           nextHandler.processAbort(request);
       }
    }
    
    @Override
    public void setNextHandler(HttpRequestHandler handler) {
        this.nextHandler = handler;
        
        handler.setListener(new HttpRequestListener() {
            @Override
            public void requestReady(HttpRequest request) {
            }
            
            @Override
            public void requestOpened(HttpRequest request) {
            }
            
            @Override
            public void requestProgressed(HttpRequest request, WrappedByteBuffer data) {
            }
            
            @Override
            public void requestLoaded(HttpRequest request, HttpResponse response) {
                WebSocketEmulatedHandler.LOG.entering(WebSocketEmulatedHandler.CLASS_NAME, "requestLoaded");

                CreateChannel channel = (CreateChannel)request.parent;
                try {
                    channel.cookie = response.getHeader(WebSocketEmulatedHandler.HEADER_SET_COOKIE);
                    
                    //get supported extensions escape bytes
                    String protocol = response.getHeader(HEADER_WEBSOCKET_PROTOCOL);
                    ((WebSocketChannel)channel.getParent()).setProtocol(protocol);
                    
                    //get supported extensions escape bytes
                    String extensionsHeader = response.getHeader(HEADER_SEC_EXTENSIONS);
                    ((WebSocketChannel)channel.getParent()).setNegotiatedExtensions(extensionsHeader);
                    if (extensionsHeader != null && extensionsHeader.length() > 0) {
                        String[] extensions = extensionsHeader.split(",");
                        for (String extension : extensions) {
                            String[] tmp = extension.split(";");
                            if (tmp.length > 1) {
                                //has escape bytes
                                String escape = tmp[1].trim();
                                if (escape.length() == 8) {
                                    try {
                                        int escapeKey = Integer.parseInt(escape, 16);
                                        channel.controlFrames.put(escapeKey, tmp[0].trim());
                                    } catch(NumberFormatException e) {
                                        // this is not an escape parameter
                                        LOG.log(Level.FINE, e.getMessage(), e);
                                    }
                                
                                }
                            }
                        }
                    }

                    WrappedByteBuffer responseBody = response.getBody();
                    String urls = responseBody.getString(WebSocketEmulatedHandler.UTF_8);
                    String[] parts = urls.split("\n");
                    
                    HttpURI upstreamUri = new HttpURI(parts[0]);
                    HttpURI downstreamUri = new HttpURI((parts.length == 2) ? parts[1] : parts[0]);
                    listener.createCompleted(channel, upstreamUri, downstreamUri, null);

                } catch (Exception e) {
                    LOG.log(Level.FINE, e.getMessage(), e);
                    
                    listener.createFailed(channel, e);
                    throw new IllegalStateException("WebSocketEmulation failed", e);
                }
            }
            
            @Override
            public void requestAborted(HttpRequest request) {
                CreateChannel channel = (CreateChannel) request.parent;
                channel.setRequest(null);
            }
            
            @Override
            public void requestClosed(HttpRequest request) {
                CreateChannel channel = (CreateChannel) request.parent;
                channel.setRequest(null);
            }
            
            
            @Override
            public void errorOccurred(HttpRequest request, Exception exception) {
                CreateChannel createChannel = (CreateChannel)request.parent;
                listener.createFailed(createChannel, exception);
            }
        });
    }
    
    public void setListener(CreateHandlerListener listener) {
        this.listener = listener;
    }
    
    private String getEnabledExtensions(CreateChannel channel) {
        WebSocketSelectedChannel selChannel = (WebSocketSelectedChannel) channel.getParent();
        WebSocketCompositeChannel compChannel = (WebSocketCompositeChannel) selChannel.getParent();
        return compChannel.getEnabledExtensions();
    }
}
