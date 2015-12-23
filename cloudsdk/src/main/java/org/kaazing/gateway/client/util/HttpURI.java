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


/**
 * URI with guarantee to be a valid, non-null, http or https URI
 */
public class HttpURI extends GenericURI<HttpURI> {

    @Override
    protected boolean isValidScheme(String scheme) {
        return "http".equals(scheme) || "https".equals(scheme);
    }
    
    public HttpURI(String location) throws URISyntaxException {
        this(new URI(location));
    }
    
    HttpURI(URI uri) throws URISyntaxException {
        super(uri);
    }

    protected HttpURI duplicate(URI uri) {
        try {
            return new HttpURI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public boolean isSecure() {
        return "https".equals(uri.getScheme());
    }

    public static HttpURI replaceScheme(GenericURI<?> location, String scheme) throws URISyntaxException {
        return HttpURI.replaceScheme(location.getURI(), scheme);
    }
    
    public static HttpURI replaceScheme(URI location, String scheme) throws URISyntaxException {
        URI uri = URIUtils.replaceScheme(location, scheme);
        return new HttpURI(uri);
    }
}
