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

import java.util.logging.Logger;

import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class HttpRequestLoggingHandler extends HttpRequestHandlerAdapter {

    private static final String CLASS_NAME = HttpRequestLoggingHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    @Override
    public void processOpen(HttpRequest request) {
        LOG.fine("->OPEN: "+request);
        super.processOpen(request);
    }

    @Override
    public void processSend(HttpRequest request, WrappedByteBuffer buffer) {
        LOG.fine("->SEND: "+request+" "+buffer.getHexDump());
        super.processSend(request, buffer);
    }

    @Override
    public void processAbort(HttpRequest request) {
        LOG.fine("->ABORT: "+request);
        super.processAbort(request);
    }
    
    @Override
    public void setNextHandler(HttpRequestHandler handler) {
        this.nextHandler = handler;
        
        handler.setListener(new HttpRequestListener() {
            
            @Override
            public void requestReady(HttpRequest request) {
                LOG.fine("<-READY: "+request);
                listener.requestReady(request);
            }
            
            @Override
            public void requestProgressed(HttpRequest request, WrappedByteBuffer payload) {
                LOG.fine("<-PROGRESSED: "+request+" "+payload.getHexDump());
                listener.requestProgressed(request, payload);
            }
            
            @Override
            public void requestOpened(HttpRequest request) {
                LOG.fine("<-OPENED: "+request);
                listener.requestOpened(request);
            }
            
            @Override
            public void requestLoaded(HttpRequest request, HttpResponse response) {
                LOG.fine("<-LOADED: "+request+" "+response);
                listener.requestLoaded(request, response);
            }
            
            @Override
            public void requestClosed(HttpRequest request) {
                LOG.fine("<-CLOSED: "+request);
                listener.requestClosed(request);
            }
            
            @Override
            public void requestAborted(HttpRequest request) {
                LOG.fine("<-ABORTED: "+request);
                listener.requestAborted(request);
            }
            
            @Override
            public void errorOccurred(HttpRequest request, Exception exception) {
                LOG.fine("<-ERROR: "+request);
                listener.errorOccurred(request, exception);
            }
        });
    }
}
