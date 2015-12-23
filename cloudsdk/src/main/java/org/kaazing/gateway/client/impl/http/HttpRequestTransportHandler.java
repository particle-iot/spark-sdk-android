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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.bridge.HttpRequestBridgeHandler;
import org.kaazing.gateway.client.impl.ws.WebSocketTransportHandler;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class HttpRequestTransportHandler extends HttpRequestHandlerAdapter {

    private static final String CLASS_NAME = HttpRequestTransportHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public static HttpRequestHandlerFactory DEFAULT_FACTORY = new HttpRequestHandlerFactory() {
        
        @Override
        public HttpRequestHandler createHandler() {
            HttpRequestHandler requestHandler = new HttpRequestTransportHandler();
            
            if (LOG.isLoggable(Level.FINE)) {
                HttpRequestLoggingHandler loggingHandler = new HttpRequestLoggingHandler();
                loggingHandler.setNextHandler(requestHandler);
                requestHandler = loggingHandler;
            }
            
            return requestHandler;
        }
    };

    @Override
    public void processOpen(HttpRequest request) {
        LOG.entering(CLASS_NAME, "processOpen: "+request);
        
        HttpRequestHandler transportHandler;
        if (WebSocketTransportHandler.useBridge(request.getUri().getURI())) {
            transportHandler = new HttpRequestBridgeHandler();
        }
        else {
            transportHandler = new HttpRequestDelegateHandler();
        }
        
        request.transportHandler = transportHandler;

        transportHandler.setListener(new HttpRequestListener() {
            
            @Override
            public void requestReady(HttpRequest request) {
                listener.requestReady(request);
            }
            
            @Override
            public void requestProgressed(HttpRequest request, WrappedByteBuffer payload) {
                listener.requestProgressed(request, payload);
            }
            
            @Override
            public void requestOpened(HttpRequest request) {
                listener.requestOpened(request);
            }
            
            @Override
            public void requestLoaded(HttpRequest request, HttpResponse response) {
                listener.requestLoaded(request, response);
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

        transportHandler.processOpen(request);
    }

    @Override
    public void processSend(HttpRequest request, WrappedByteBuffer buffer) {
        LOG.entering(CLASS_NAME, "processSend: "+request);
        
        HttpRequestHandler transportHandler = request.transportHandler;
        transportHandler.processSend(request, buffer);
    }

    @Override
    public void processAbort(HttpRequest request) {
        LOG.entering(CLASS_NAME, "processAbort: "+request);
        
        HttpRequestHandler transportHandler = request.transportHandler;
        transportHandler.processAbort(request);
    }
}
