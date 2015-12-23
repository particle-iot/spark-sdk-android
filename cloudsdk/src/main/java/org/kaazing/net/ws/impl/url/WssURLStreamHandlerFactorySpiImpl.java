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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.net.URLStreamHandler;
import java.util.Collection;

public class WssURLStreamHandlerFactorySpiImpl extends WsURLStreamHandlerFactorySpiImpl {
    private static final Collection<String> _supportedProtocols = unmodifiableList(asList("wss", "wse+ssl", "wssn"));
    
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (!_supportedProtocols.contains(protocol)) {
            throw new IllegalArgumentException(String.format("Protocol not supported '%s'", protocol));
        }
 
        return new WssURLStreamHandlerImpl(getExtensionFactories());
    }

    @Override
    public Collection<String> getSupportedProtocols() {
        return _supportedProtocols;
    }
}
