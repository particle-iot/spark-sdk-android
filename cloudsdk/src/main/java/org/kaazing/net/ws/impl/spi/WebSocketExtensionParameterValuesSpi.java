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

package org.kaazing.net.ws.impl.spi;

import java.util.Collection;

import org.kaazing.net.ws.WebSocket;
import org.kaazing.net.ws.WebSocketExtension.Parameter;
import org.kaazing.net.ws.WsURLConnection;

/**
 * WsExtensionParameterValues is part of <i>Service Provider Interface</i> 
 * <em>(SPI)</em> for admins/implementors.
 * <p> 
 * WsExtensionParameterValues is used to cache extension parameters as 
 * name-value pairs in a very generic type-safe way. The implementations of
 * {@link WebSocket#connect()} and {@link WsURLConnection#connect()} invoke
 * {@link WebSocketExtensionFactorySpi#createWsExtension(WebSocketExtensionParameterValuesSpi)}
 * method and pass in all the extension parameters that have been earlier set 
 * by the developer for the enabled extensions.
 */
public abstract class WebSocketExtensionParameterValuesSpi {
    /**
     * Returns the collection of {@link Parameter} objects of a 
     * {@link WebSocketExtension} that have been set. Returns an empty
     * Collection if no parameters belonging to the extension have been set.
     * 
     * @return Collection<Parameter<?>>
     */
    public abstract Collection<Parameter<?>> getParameters();

    /**
     * Returns the value of type T of the specified parameter. A null is 
     * returned if value is not set.
     * 
     * @param <T>           Generic type T of the parameter's value
     * @param parameter     extension parameter
     * @return value of type T of the specified extension parameter 
     */
   public abstract <T> T getParameterValue(Parameter<T> parameter);   
}
