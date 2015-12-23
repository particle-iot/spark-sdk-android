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

/**
 * Copyright (c) 2007-2011, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.client.impl.bridge;

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.bridge.XoaEvent.XoaEventKind;
import org.kaazing.gateway.client.impl.http.HttpRequest;
import org.kaazing.gateway.client.impl.http.HttpRequestHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestListener;
import org.kaazing.gateway.client.impl.http.HttpRequestUtil;
import org.kaazing.gateway.client.impl.http.HttpResponse;
import org.kaazing.gateway.client.impl.http.HttpRequest.Method;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
/*
 * WebSocket Emulated Handler Chain
 * EmulateHandler  
 *                |- CreateHandler - HttpRequestAuthenticationHandler - HttpRequestRedirectHandler - {HttpRequestBridgeHandler}
 *                   |- UpstreamHandler - {HttpRequestBridgeHandler}
 *                |- DownstreamHandler - {HttpRequestBridgeHandler}    
 * Responsibilities:
 *     a). pass client actions over bridge as events
 *     b). fire corresponding event to client when receives events from bridge
 *             
 * TODO:
 *         a). shall we check http response status code? now this code is checked in bridge side
 */
public class HttpRequestBridgeHandler implements HttpRequestHandler, ProxyListener {
    private static final String CLASS_NAME = HttpRequestBridgeHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    
    private HttpRequestListener listener;

    public HttpRequestBridgeHandler() {
        LOG.entering(CLASS_NAME, "<init>");
    }

    @Override
    public synchronized void processOpen(HttpRequest request) {
        LOG.entering(CLASS_NAME, "open", request);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("processOpen: "+request);
        }

        HttpURI uri = request.getUri();
        Method method = request.getMethod();

        if (request.getProxy() != null) {
            throw new IllegalStateException("processOpen previously called with HttpRequest");
        }
        
        try {
            Proxy proxy = BridgeUtil.createProxy(uri.getURI(), this);
            proxy.setPeer(request);
            request.setProxy(proxy);
            
            /* Dispatch create event to the bridge */
            String[] params = new String[] { "HTTPREQUEST", uri.toString(), method.toString(), request.isAsync() ? "Y" : "N" };
            proxy.processEvent(XoaEventKind.CREATE, params);
        } catch (Exception e) {
            LOG.log(Level.FINE, "While initializing HttpRequest proxy: "+e.getMessage(), e);
            listener.errorOccurred(request, e);
        }
    }

    private void handleRequestCreated(HttpRequest request) {
        LOG.entering(CLASS_NAME, "handleRequestCreated");
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("handleRequestCreated: "+request);
        }
        
        request.setReadyState(HttpRequest.ReadyState.READY);
        try {
            for (Entry<String, String> entry : request.getHeaders().entrySet()) {
                String header = entry.getKey();
                String value = entry.getValue();
                HttpRequestUtil.validateHeader(header);
                Proxy proxy = (Proxy)request.getProxy();
                proxy.processEvent(XoaEventKind.SETREQUESTHEADER, new String[] { header, value });
            }

            // Nothing has been sent
            if (request.getMethod() == Method.POST) {
                listener.requestReady(request);
            }
            else {
                processSend(request, null);
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            listener.errorOccurred(request, e);
        }
    }

    @Override
    public void processSend(HttpRequest request, WrappedByteBuffer content) {
        LOG.entering(CLASS_NAME, "processSend", content);
        
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
        
        Proxy proxy = (Proxy)request.getProxy();
        proxy.processEvent(XoaEventKind.SEND, new Object[] { payload });
        request.setReadyState(HttpRequest.ReadyState.SENT);
    }

    private void handleRequestProgressed(HttpRequest request, WrappedByteBuffer payload) {
        LOG.entering(CLASS_NAME, "handleRequestProgressed", payload);

        request.setReadyState(HttpRequest.ReadyState.LOADING);
        try {
            listener.requestProgressed(request, payload);
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            listener.errorOccurred(request, e);
        }
    }

    private void handleRequestLoaded(HttpRequest request, WrappedByteBuffer responseBuffer) {
        LOG.entering(CLASS_NAME, "handleRequestLoaded", responseBuffer);

        request.setReadyState(HttpRequest.ReadyState.LOADED);

        HttpResponse response = request.getResponse();
        response.setBody(responseBuffer);
        
        try {
            listener.requestLoaded(request, response);
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            listener.errorOccurred(request, e);
        }
    }

    public static void parseResponseHeaders(HttpResponse response, String in) {
        LOG.entering(CLASS_NAME, "setResponseHeaders", in);
        String headers = in + "";
        int lf = headers.indexOf("\n");
        while (lf != -1) {
            String ret = headers.substring(0, lf);
            ret.trim();
            int colonAt = ret.indexOf(":");
            String name = ret.substring(0, colonAt);
            String value = ret.substring(colonAt + 1);
            response.setHeader(name, value);
            
            if (lf != headers.length()) {
                // check if the last char is the \n
                headers = headers.substring(lf + 1);
                if (headers.length() == 0) {
                    headers.trim();
                }
            } else {
                headers = "";
            }

            if (headers.length() == 0) {
                break;
            }
            lf = headers.indexOf("\n");
        }
    }

    @Override
    public void eventReceived(Proxy proxy, XoaEventKind eventKind, Object[] params) {
        LOG.entering(CLASS_NAME, "eventReceived", new Object[] { proxy, this, eventKind, params });
        
        HttpRequest request = (HttpRequest)proxy.getPeer();

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "SOA <-- XOA:" + "id = " + proxy.getHandlerId() + " name: " + eventKind + " " + request);
        }
        
        switch (eventKind) {
        case OPEN:
            handleRequestCreated(request);
            break;
        case READYSTATECHANGE:
            int state = Integer.parseInt((String) params[0]);
            if (state == 2) {
                HttpResponse response = new HttpResponse();
                request.setResponse(response);

                if (params.length > 1) {
                    int responseCode = Integer.parseInt((String) params[1]);
                    if (responseCode != 0) {
                        response.setStatusCode(responseCode);
                        response.setMessage(((String) params[2]));
                        parseResponseHeaders(response, ((String) params[3]));

                    }
                }
                request.setReadyState(HttpRequest.ReadyState.OPENED);
                listener.requestOpened(request);
            }
            break;
        case PROGRESS:
            WrappedByteBuffer messageBuffer = WrappedByteBuffer.wrap((java.nio.ByteBuffer) params[0]);
            handleRequestProgressed(request, messageBuffer);
            break;
        case LOAD:
            WrappedByteBuffer responseBuffer = WrappedByteBuffer.wrap((java.nio.ByteBuffer) params[0]);
            handleRequestLoaded(request, responseBuffer);
            break;
        case CLOSED:
            proxy = null;
            listener.requestClosed(request);
            break;
        case ERROR:
            proxy = null;
            String s = "HTTP Bridge Handler: ERROR event received";
            handleErrorOccurred(request, new IllegalStateException(s));
            break;
        default:
            throw new IllegalArgumentException("INVALID_STATE_ERR");
        }
    }
    
    private void handleErrorOccurred(HttpRequest request, Exception exception) {
        request.setReadyState(HttpRequest.ReadyState.ERROR);
        listener.errorOccurred(request, exception);
    }

    @Override
    public void processAbort(HttpRequest request) {
        if (request.getReadyState() == HttpRequest.ReadyState.UNSENT) {
            throw new IllegalStateException("INVALID_STATE_ERR");
        }
        Proxy proxy = (Proxy)request.getProxy();
        proxy.processEvent(XoaEventKind.ABORT, XoaEvent.EMPTY_ARGS);
    }

    @Override
    public void setListener(HttpRequestListener listener) {
        this.listener = listener;
    }
}
