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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.Channel;
import org.kaazing.gateway.client.util.HttpURI;

public class HttpRequest {

    private static final String CLASS_NAME = HttpRequest.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    static volatile int nextId = 1;
    final int id;

    public static final HttpRequestFactory HTTP_REQUEST_FACTORY = new HttpRequestFactory() {
        @Override
        public HttpRequest createHttpRequest(Method method, HttpURI uri, boolean async) {
            return new HttpRequest(method, uri, async);
        }
    };

    /** Possible ready states for the request */
    public enum ReadyState {
        /** Request has not yet been sent */
        UNSENT,
        /** Request is ready to be sent.  Data can be sent at this time for POST requests */
        READY,
        /** Request is in the process of sending.  No further data can be written at this time. */
        SENDING,
        /** Request has been sent, but no response has been received */ 
        SENT,
        /** Response has been partially received.  All headers are available. */
        OPENED,
        /** Response has been partially received.  Some data is available. */
        LOADING,
        /** Response has been completed.  All data is available. */
        LOADED,
        /** An error occurred during the request or response. */
        ERROR;
    }

    /** Current ready state for this request */
    private ReadyState readyState = ReadyState.UNSENT;

    /** Methods available for HttpRequest */
    public enum Method {
        GET, POST
    }
    
    /** Method specified for this request */
    private Method method;
    
    /** URI specified for this request */
    private HttpURI uri;

    /** True if progress events are returned as data is received */
    private boolean async;
    
    /** Headers associated with request.  Headers must be set before or during requestReady event */
    private Map<String, String> headers = new HashMap<String, String>();

    /** Response received for this request */
    private HttpResponse response;

    /** Higher layer managing this request */
    public Channel parent;
    
    /** Underlying object representing this request */
    private Object proxy;

    /** Handler for this request */
    HttpRequestHandler transportHandler;
    
    /** Creates an HttpRequest with method and uri specified.  Async is true by default. */
    public HttpRequest(Method method, HttpURI uri) {
        this(method, uri, true);
    }
    
    /** Creates an HttpRequest with method and uri specified.
      * Async true means fire progress events as data is received. */
    public HttpRequest(Method method, HttpURI uri, boolean async) {
        this.id = nextId++;
        
        if (uri == null) {
            LOG.severe("HTTP request URL is null");
            throw new IllegalArgumentException("HTTP request URL is null");
        }

        if (method == null) {
            LOG.severe("Invalid Method in an HTTP request");
            throw new IllegalArgumentException("Invalid Method in an HTTP request");
        }

        this.method = method;
        this.uri = uri;
        this.async = async;
    }

    /** Get Method specified for this request */
    public Method getMethod() {
        return method;
    }

    /** Get URI specified for this request */
    public HttpURI getUri() {
        return uri;
    }

    /** Return true if progress events will fire as data is received */
    public boolean isAsync() {
        return async;
    }

    /** Set ready state for this request */
    public void setReadyState(ReadyState readyState) {
        this.readyState = readyState;
    }

    /** Get ready state for this request */
    public ReadyState getReadyState() {
        return readyState;
    }

    /** Set header for this request */
    public void setHeader(String header, String value) {
        headers.put(header, value);
    }
    
    /** Get all headers for this request */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /** Get the response associated with this request.
      * Returns null if response has not yet been received. */
    public HttpResponse getResponse() {
        return response;
    }

    /** Sets the response associated with this request. */
    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    /** Gets the proxy object associated with this request. */
    public Object getProxy() {
        return proxy;
    }

    /** Sets the proxy object associated with this request */
    public void setProxy(Object proxy) {
        this.proxy = proxy;
    }

    @Override
    public String toString() {
        return "[Request "+id+": "+method+" "+uri+" async:"+async+"]";
    }
}
