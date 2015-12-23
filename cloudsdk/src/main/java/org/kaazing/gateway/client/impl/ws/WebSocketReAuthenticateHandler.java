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

package org.kaazing.gateway.client.impl.ws;

import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.Channel;
import org.kaazing.gateway.client.impl.Handler;
import org.kaazing.gateway.client.impl.http.HttpRequest;
import org.kaazing.gateway.client.impl.http.HttpRequestAuthenticationHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestListener;
import org.kaazing.gateway.client.impl.http.HttpRequestTransportHandler;
import org.kaazing.gateway.client.impl.http.HttpResponse;
import org.kaazing.gateway.client.impl.http.HttpRequest.Method;
import org.kaazing.gateway.client.util.HttpURI;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class WebSocketReAuthenticateHandler implements Handler {

    private static final String CLASS_NAME = WebSocketReAuthenticateHandler.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    static final String HEADER_CONTENT_TYPE = "Content-Type";
    static final String HEADER_COOKIE = "Cookie";
    static final String HEADER_SET_COOKIE = "Set-Cookie";
    static final Charset UTF_8 = Charset.forName("UTF-8");
    HttpRequestHandler nextHandler;
    //CreateHandlerListener listener; //no listener for this operation.

    HttpRequestAuthenticationHandler authHandler = new HttpRequestAuthenticationHandler();
    HttpRequestHandler transportHandler = HttpRequestTransportHandler.DEFAULT_FACTORY.createHandler();

    public WebSocketReAuthenticateHandler() {
        setNextHandler(authHandler);
        authHandler.setNextHandler(transportHandler);
    }

    public void processOpen(Channel channel, HttpURI location) {
        LOG.entering(CLASS_NAME, "processOpen", location);
        
        HttpRequest request = HttpRequest.HTTP_REQUEST_FACTORY.createHttpRequest(Method.GET, location, false);
        /* 
         * create a dummy channel in the middle to match emulated Channel structure
         * WebSoecktEmulatedChannel->CreateChannel->HttpRequest
         */
        request.parent = new Channel();
        request.parent.setParent(channel);
        nextHandler.processOpen(request);
    }

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
            }

            @Override
            public void requestAborted(HttpRequest request) {
            }

            @Override
            public void requestClosed(HttpRequest request) {
            }

            @Override
            public void errorOccurred(HttpRequest request, Exception exception) {
            }
        });
    }

}
