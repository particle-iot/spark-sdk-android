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

package org.kaazing.net.ws.impl;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;

import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.http.HttpRedirectPolicy;
import org.kaazing.net.ws.WebSocket;
import org.kaazing.net.ws.WebSocketExtension.Parameter;
import org.kaazing.net.ws.WebSocketFactory;
import org.kaazing.net.ws.impl.spi.WebSocketExtensionFactorySpi;

public final class DefaultWebSocketFactory extends WebSocketFactory {
    private static final Map<String, WebSocketExtensionFactorySpi>  _extensionFactories;

    private final Map<String, WsExtensionParameterValuesSpiImpl> _parameters;
    private HttpRedirectPolicy        _redirectOption;
    private Collection<String>        _supportedExtensions;
    private Collection<String>        _enabledExtensions;
    private ChallengeHandler          _challengeHandler;
    private int                       _connectTimeout = 0; // milliseconds
    
    static {
        Class<WebSocketExtensionFactorySpi> clazz = WebSocketExtensionFactorySpi.class;
        ServiceLoader<WebSocketExtensionFactorySpi> loader = ServiceLoader.load(clazz);
        Map<String, WebSocketExtensionFactorySpi> factories = new HashMap<String, WebSocketExtensionFactorySpi>();
        
        for (WebSocketExtensionFactorySpi factory: loader) {
            String extensionName = factory.getExtensionName();
            
            if (extensionName != null)
            {
                factories.put(extensionName, factory);
            }
        }
        _extensionFactories = unmodifiableMap(factories);
    }

    public DefaultWebSocketFactory() {
        _parameters = new HashMap<String, WsExtensionParameterValuesSpiImpl>();

        _supportedExtensions = new HashSet<String>();
        _supportedExtensions.addAll(_extensionFactories.keySet());

        // ### TODO: Should _redirectOption be null or 
        //           HttpRedirectOption.ALWAYS by default. Note that in
        //           HttpURLConnection, followRedirects is true by default.
        _redirectOption = HttpRedirectPolicy.ALWAYS;

    }

    @Override
    public WebSocket createWebSocket(URI location)
            throws URISyntaxException {
        return createWebSocket(location, (String[]) null);
    }

    @Override
    public WebSocket createWebSocket(URI location, String... protocols)
            throws URISyntaxException {
        Collection<String> enabledProtocols = null;
        Collection<String> enabledExtensions = null;
        
        // Clone enabled protocols maintained at the WebSocketFactory level to
        // pass into the WebSocket instance.
        if (protocols != null) {
            enabledProtocols = new HashSet<String>(Arrays.asList(protocols));
        }

        // Clone enabled extensions maintained at the WebSocketFactory level to
        // pass into the WebSocket instance.
        if (_enabledExtensions != null) {
            enabledExtensions = new ArrayList<String>(_enabledExtensions);
        }
        
        // Clone the map of default parameters maintained at the 
        // WebSocketFactory level to pass into the WebSocket instance.
        Map<String, WsExtensionParameterValuesSpiImpl> enabledParams = 
                      new HashMap<String, WsExtensionParameterValuesSpiImpl>();
        enabledParams.putAll(_parameters);
        
        // Create a WebSocket instance that inherits the enabled protocols,
        // enabled extensions, enabled parameters, the HttpRedirectOption,
        // the extension factories(ie. the supported extensions).
        WebSocketImpl   ws = new WebSocketImpl(location,
                                               _extensionFactories,
                                               _redirectOption,
                                               enabledExtensions,
                                               enabledProtocols,
                                               enabledParams,
                                               _challengeHandler,
                                               _connectTimeout);
        return ws;
    }

    @Override
    public int getDefaultConnectTimeout() {
       return _connectTimeout;
    }

    @Override
    public ChallengeHandler getDefaultChallengeHandler() {
        return _challengeHandler;
    }
    

    
    @Override
    public Collection<String> getDefaultEnabledExtensions() {
        return (_enabledExtensions == null) ? Collections.<String>emptySet() :
                                              unmodifiableCollection(_enabledExtensions);
    }
    

    @Override
    public HttpRedirectPolicy getDefaultRedirectPolicy() {
        return _redirectOption;
    }

    @Override
    public <T> T getDefaultParameter(Parameter<T> parameter) {
        String                            extName = parameter.extension().name();
        WsExtensionParameterValuesSpiImpl paramValues = _parameters.get(extName);
        
        if (paramValues == null) {
            return null;
        }
        
        return paramValues.getParameterValue(parameter);
    }

    @Override
    public Collection<String> getSupportedExtensions() {
        return (_supportedExtensions == null) ? Collections.<String>emptySet() :
                                                unmodifiableCollection(_supportedExtensions);
    }

    @Override
    public void setDefaultChallengeHandler(ChallengeHandler challengeHandler) {
        _challengeHandler = challengeHandler;
    }

    @Override
    public void setDefaultConnectTimeout(int connectTimeout) {
       _connectTimeout = connectTimeout;
    }

    @Override
    public void setDefaultEnabledExtensions(Collection<String> extensions) {
        if (extensions == null) {
            _enabledExtensions = extensions;
            return;
        }
        
        Collection<String> supportedExtns = getSupportedExtensions();
        for (String extension : extensions) {
            if (!supportedExtns.contains(extension)) {
                String s = String.format("'%s' is not a supported extension", extension);
                throw new IllegalStateException(s);
            }
            
            if (_enabledExtensions == null) {
                _enabledExtensions = new ArrayList<String>();
            }

            _enabledExtensions.add(extension);
        }        
    }
    
    @Override
    public void setDefaultRedirectPolicy(HttpRedirectPolicy redirectOption) {
        _redirectOption = redirectOption;
    }

    @Override
    public <T> void setDefaultParameter(Parameter<T> parameter, T value) {
        String extensionName = parameter.extension().name();
        
        WsExtensionParameterValuesSpiImpl parameterValues = _parameters.get(extensionName);
        if (parameterValues == null) {
            parameterValues = new WsExtensionParameterValuesSpiImpl();
            _parameters.put(extensionName, parameterValues);
        }
        
        parameterValues.setParameterValue(parameter, value);
    }
}
