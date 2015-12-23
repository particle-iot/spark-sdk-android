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

public class HttpRequestEvent {
    private static final String CLASS_NAME = HttpRequestEvent.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final long serialVersionUID = -7922410353957227356L;

    private HttpRequest source;
    private final Kind kind;
    private final WrappedByteBuffer data;

    /**
     * Type of the HttpRequestEvent.
     */
    public enum Kind {
        OPEN, LOAD, PROGRESS, ERROR, READYSTATECHANGE, ABORT
    }

    public HttpRequestEvent(HttpRequest source, Kind kind) {
        this(source, kind, null);
    }

    public HttpRequestEvent(HttpRequest source, Kind kind, WrappedByteBuffer data) {
        this.source = source;
        LOG.entering(CLASS_NAME, "<init>", new Object[] { source, kind, data });
        this.kind = kind;
        this.data = data;
    }

    public HttpRequest getSource() {
        return source;
    }
    
    public Kind getKind() {
        return kind;
    }

    public WrappedByteBuffer getData() {
        return data;
    }

}
