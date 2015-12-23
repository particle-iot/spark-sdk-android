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

package org.kaazing.gateway.client.util;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class GenericURI<T> {

    protected URI uri;

    protected GenericURI(String location) throws URISyntaxException {
        this(new URI(location));
    }
    
    protected GenericURI(URI uri) throws URISyntaxException {
        this.uri = uri;
        validateScheme();
    }

    abstract protected boolean isValidScheme(String scheme);
    
    private void validateScheme() throws URISyntaxException {
        String scheme = getScheme();
        if (!isValidScheme(scheme)) {
            throw new URISyntaxException(uri.toString(), "Invalid scheme");
        }
    }

    abstract protected T duplicate(URI uri);
    
    public T replacePath(String path) {
        return duplicate(URIUtils.replacePath(uri, path));
    }
    
    public T addQueryParameter(String newParam) {
        String queryParams = uri.getQuery();
        if (queryParams == null) {
            queryParams = newParam;
        }
        else {
            queryParams += "&" + newParam;
        }
        
        return duplicate(URIUtils.replaceQueryParameters(uri, queryParams));
    }
    
    public URI getURI() {
        return uri;
    }

    public String getHost() {
        return uri.getHost();
    }
    
    public int getPort() {
        return uri.getPort();
    }
    
    public String getScheme() {
        return uri.getScheme();
    }
    
    public String getPath() {
        return uri.getPath();
    }
    
    public String getQuery() {
        return uri.getQuery();
    }
    
    @Override
    public String toString() {
        return uri.toString();
    }
}
