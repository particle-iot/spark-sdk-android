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

package org.kaazing.gateway.client.impl.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.kaazing.gateway.client.util.GenericURI;
import org.kaazing.gateway.client.util.URIUtils;


/**
 * URI with guarantee to be a valid, non-null, ws URI
 */
public class WSURI extends GenericURI<WSURI> {

    @Override
    protected boolean isValidScheme(String scheme) {
        return "ws".equals(scheme) || "wss".equals(scheme);
    }
    
    public WSURI(String location) throws URISyntaxException {
        this(new URI(location));
    }

    public WSURI(URI location) throws URISyntaxException {
        super(location);
    }
    
    protected WSURI duplicate(URI uri) {
        try {
            return new WSURI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    static WSURI replaceScheme(URI uri, String scheme) throws URISyntaxException {
        URI wsUri = URIUtils.replaceScheme(uri, scheme);
        return new WSURI(wsUri);
    }

    public boolean isSecure() {
        String scheme = getScheme();
        return "wss".equals(scheme);
    }

    public String getHttpEquivalentScheme() {
        return (uri.getScheme().equals("ws") ? "http" : "https");
    }
}
