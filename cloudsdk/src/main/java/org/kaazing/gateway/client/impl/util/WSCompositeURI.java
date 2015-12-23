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
import java.util.HashMap;
import java.util.Map;

import org.kaazing.gateway.client.util.GenericURI;


/**
 * URI with guarantee to be a valid, non-null, ws URI
 */
public class WSCompositeURI extends GenericURI<WSCompositeURI> {

    static Map<String, String> wsEquivalent = new HashMap<String, String>();
    static {
        wsEquivalent.put("ws", "ws");
        wsEquivalent.put("wse", "ws");
        wsEquivalent.put("wsn", "ws");
        
        wsEquivalent.put("wss", "wss");
        wsEquivalent.put("wssn", "wss");
        wsEquivalent.put("wse+ssl", "wss");

        wsEquivalent.put("java:ws", "ws");
        wsEquivalent.put("java:wse", "ws");
        wsEquivalent.put("java:wss", "wss");
        wsEquivalent.put("java:wse+ssl", "wss");
    }
    
    String scheme = null;
    
    protected boolean isValidScheme(String scheme) {
        return wsEquivalent.get(scheme) != null;
    }
    
    /* 
     * This class is needed to workaround java URI's inability to handle java:ws style composite prefixes. 
     */
    public WSCompositeURI(String location) throws URISyntaxException {
        this(new URI(location));
    }

    public WSCompositeURI(URI uri) throws URISyntaxException {
        super(uri);
    }
    
//    private static String normalize(String location) {
//        return location.startsWith("java:") ? location.substring(5) : location;
//    }

    protected WSCompositeURI duplicate(URI uri) {
        try {
            return new WSCompositeURI(uri);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public boolean isSecure() {
        String scheme = getScheme();
        return "wss".equals(wsEquivalent.get(scheme));
    }

    public WSURI getWSEquivalent() {
        try {
            String wsEquivScheme = wsEquivalent.get(getScheme());
            return WSURI.replaceScheme(this.uri, wsEquivScheme);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Override
    public String getScheme() {
        // Workaround URI behavior that returns only "java" instead of "java:ws"
        if (scheme == null) {
            String location = uri.toString();
            int schemeEndIndex = location.indexOf("://");
            if (schemeEndIndex != -1) {
                scheme = location.substring(0, schemeEndIndex);
            }
            else {
                scheme = uri.toString();
            }
        }
        return scheme;
    }
}
