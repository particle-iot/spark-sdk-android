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

public class URIUtils {

    public static URI replaceScheme(String location, String scheme) {
        try {
            return replaceScheme(new URI(location), scheme);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI/Scheme: replacing scheme with "+scheme+" for "+location);
        }
    }

    public static URI replaceScheme(URI uri, String scheme) {
        try {
            String location = uri.toString();
            int index = location.indexOf("://");
            return new URI(scheme + location.substring(index));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI/Scheme: replacing scheme with "+scheme+" for "+uri);
        }
    }

    public static URI replacePath(URI uri, String path) {
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI/Scheme: replacing path with '"+path+"' for "+uri);
        }
    }

    public static URI replaceQueryParameters(URI uri, String queryParams) {
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), queryParams, uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI/Scheme: replacing query parameters with '"+queryParams+"' for "+uri);
        }
    }
}
