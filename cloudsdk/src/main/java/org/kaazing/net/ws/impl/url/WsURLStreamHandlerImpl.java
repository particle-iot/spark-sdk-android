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

package org.kaazing.net.ws.impl.url;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Map;

import org.kaazing.net.ws.WsURLConnection;
import org.kaazing.net.ws.impl.WsURLConnectionImpl;
import org.kaazing.net.ws.impl.spi.WebSocketExtensionFactorySpi;

class WsURLStreamHandlerImpl extends URLStreamHandler {

    private final Map<String, WebSocketExtensionFactorySpi> _extensionFactories;
    
    private String    _scheme;
    
    public WsURLStreamHandlerImpl(Map<String, WebSocketExtensionFactorySpi> extensionFactories) {
        _extensionFactories = extensionFactories;
    }
    
    @Override
    protected int getDefaultPort() {
        return 80;
    }

    @Override
    protected WsURLConnection openConnection(URL location) throws IOException {
        return new WsURLConnectionImpl(location, _extensionFactories);
    }

    @Override
    protected void parseURL(URL location, String spec, int start, int limit) {
        _scheme = spec.substring(0, spec.indexOf("://"));
        
        // start needs to be adjusted for schemes that include a ':' such as
        // java:wse, etc.
        // int index = spec.indexOf(":/");
        // start = index + 1;

        URI    specURI = _getSpecURI(spec);
        String host = specURI.getHost();
        int    port = specURI.getPort();
        String authority = specURI.getAuthority();
        String userInfo = specURI.getUserInfo();
        String path = specURI.getPath();
        String query = specURI.getQuery();
        
        setURL(location, _scheme, host, port, authority, userInfo, path, query, null);
        // super.parseURL(location, spec, start, limit);
    }
    
    // ### TODO:
    // This method is no longer needed as we are not supporting 'java:'
    // prefixes. Keeping it for time being.
    @Override
    protected String toExternalForm(URL location) {
        return super.toExternalForm(location);
    }
    
    // ----------------- Private Methods -----------------------------------
    // Creates a URI that can be used to retrieve various parts such as host,
    // port, etc. Based on whether the scheme includes ':', the method returns
    // the appropriate URI that can be used to retrieve the needed parts.
    private URI _getSpecURI(String spec) {
        URI specURI = URI.create(spec);

        if (_scheme.indexOf(':') == -1) {
            return specURI;
        }
        
        String schemeSpecificPart = specURI.getSchemeSpecificPart();
        return URI.create(schemeSpecificPart);
    }
}
