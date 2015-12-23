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

import java.net.URL;
import java.util.Map.Entry;

import org.kaazing.gateway.client.impl.bridge.HttpRequestBridgeHandler;
import org.kaazing.gateway.client.impl.http.HttpRequest.Method;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.gateway.client.impl.wseb.WebSocketEmulatedChannel;
import org.kaazing.gateway.client.impl.wsn.WebSocketNativeDelegateHandler;
import org.kaazing.gateway.client.transport.CloseEvent;
import org.kaazing.gateway.client.transport.ErrorEvent;
import org.kaazing.gateway.client.transport.LoadEvent;
import org.kaazing.gateway.client.transport.OpenEvent;
import org.kaazing.gateway.client.transport.ProgressEvent;
import org.kaazing.gateway.client.transport.ReadyStateChangedEvent;
import org.kaazing.gateway.client.transport.http.HttpRequestDelegate;
import org.kaazing.gateway.client.transport.http.HttpRequestDelegateImpl;
import org.kaazing.gateway.client.transport.http.HttpRequestDelegateListener;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class HttpRequestDelegateHandler implements HttpRequestHandler {

    HttpRequestListener listener;
    
    @Override
    public void processOpen(final HttpRequest request) {
        HttpRequestDelegate delegate = new HttpRequestDelegateImpl();
        try {
            request.setProxy(delegate);
            String origin = "privileged://" + WebSocketNativeDelegateHandler.getCanonicalHostPort(request.getUri().getURI());
            
            delegate.setListener(new HttpRequestDelegateListener() {
                
                @Override
                public void readyStateChanged(ReadyStateChangedEvent event) {
                    Object[] params = (Object[]) event.getParams();
                    int state = Integer.parseInt((String) params[0]);
                    if (state == 2) {
                        HttpResponse response = new HttpResponse();
                        request.setResponse(response);

                        if (params.length > 1) {
                            int responseCode = Integer.parseInt((String) params[1]);
                            if (responseCode != 0) {
                                response.setStatusCode(responseCode);
                                response.setMessage(((String) params[2]));
                                HttpRequestBridgeHandler.parseResponseHeaders(response, ((String) params[3]));
                            }
                        }
                        request.setReadyState(HttpRequest.ReadyState.OPENED);
                        listener.requestOpened(request);
                    }
                }
                
                @Override
                public void progressed(ProgressEvent progressEvent) {
                    java.nio.ByteBuffer payload = progressEvent.getPayload();
                    WrappedByteBuffer buffer = WrappedByteBuffer.wrap(payload);
                    
                    request.setReadyState(HttpRequest.ReadyState.LOADING);
                    try {
                        listener.requestProgressed(request, buffer);
                    } catch (Exception e) {
//                        LOG.log(Level.FINE, e.getMessage(), e);
                        listener.errorOccurred(request, e);
                    }
                }
                
                @Override
                public void opened(OpenEvent event) {
                    HttpRequestDelegate delegate = (HttpRequestDelegate)request.getProxy();
                    
                    // Allow headers to be set via opened
                    request.setReadyState(HttpRequest.ReadyState.READY);
                    listener.requestOpened(request);
                    
                    // Then set headers
                    for (Entry<String, String> entry : request.getHeaders().entrySet()) {
                        String header = entry.getKey();
                        String value = entry.getValue();
                        HttpRequestUtil.validateHeader(header);
                        delegate.setRequestHeader(header, value);
                    }

                    // Nothing has been sent
                    if (request.getMethod() == Method.POST) {
                        listener.requestReady(request);
                    }
                    else {
                        processSend(request, null);
                    }
                }
                
                @Override
                public void loaded(LoadEvent event) {
                    WrappedByteBuffer responseBuffer = WrappedByteBuffer.wrap(event.getResponseBuffer());
                    request.setReadyState(HttpRequest.ReadyState.LOADED);

                    HttpResponse response = request.getResponse();
                    response.setBody(responseBuffer);
                    
                    try {
                        listener.requestLoaded(request, response);
                    } catch (Exception e) {
//                        LOG.log(Level.FINE, e.getMessage(), e);
                        listener.errorOccurred(request, e);
                    }
                }

                @Override
                public void closed(CloseEvent event) {
                    listener.requestClosed(request);
                }
                
                @Override
                public void errorOccurred(ErrorEvent event) {
                    listener.errorOccurred(request, event.getException());
                }
            });
            
            String method = request.getMethod().toString();
            URL url = request.getUri().getURI().toURL();
            boolean isAsync = request.isAsync();
            int connectTimeout = (int) getConnectTimeout(request);
            delegate.processOpen(method, url, origin, isAsync, connectTimeout);
        } catch (Exception e) {
            listener.errorOccurred(request, e);
        }
    }

    @Override
    public void processSend(HttpRequest request, WrappedByteBuffer content) {
//        LOG.entering(CLASS_NAME, "processSend", content);
        
        if (request.getReadyState() != HttpRequest.ReadyState.READY) {
            throw new IllegalStateException("HttpRequest must be in READY state to send");
        }
        
        request.setReadyState(HttpRequest.ReadyState.SENDING);
        
        java.nio.ByteBuffer payload;
        if (content == null) {
            payload = java.nio.ByteBuffer.allocate(0);
        } else {
            payload = java.nio.ByteBuffer.wrap(content.array(), content.arrayOffset(), content.remaining());
        }
        
        HttpRequestDelegate delegate = (HttpRequestDelegate)request.getProxy();
        delegate.processSend(payload);
        request.setReadyState(HttpRequest.ReadyState.SENT);
    }

    @Override
    public void processAbort(HttpRequest request) {
        HttpRequestDelegate delegate = (HttpRequestDelegate)request.getProxy();
        delegate.processAbort();
    }

    @Override
    public void setListener(HttpRequestListener listener) {
        this.listener = listener;
    }

    private long getConnectTimeout(HttpRequest request) {
        WebSocketCompositeChannel compChannel = getWebSocketCompositeChannel(request);
        if (compChannel != null) {
            if (compChannel.getConnectTimer() != null) {
                return compChannel.getConnectTimer().getDelay();
            }
        }
        
        return 0L;
    }

    private WebSocketCompositeChannel getWebSocketCompositeChannel(HttpRequest request) {
        if (request.parent != null) {
            WebSocketEmulatedChannel emulatedChannel = (WebSocketEmulatedChannel) request.parent.getParent();
            if (emulatedChannel != null) {
                return (WebSocketCompositeChannel) emulatedChannel.getParent();
            }
            
            return null;
        }

        return null;
    }
}
