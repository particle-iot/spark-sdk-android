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

package org.kaazing.net.ws;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.ServiceLoader;

import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.http.HttpRedirectPolicy;
import org.kaazing.net.ws.WebSocketExtension.Parameter;

/**
 * {@link WebSocketFactory} is an abstract class that can be used to create
 * {@link WebSocket}s by specifying the end-point and the enabled protocols.
 * It may be extended to instantiate particular subclasses of {@link WebSocket} 
 * and thus provide a general framework for the addition of public 
 * WebSocket-level functionality.
 * <p>
 * Using {@link WebSocketFactory} instance, application developers can set 
 * {@link Parameter}s that will be inherited by all the
 * {@link WebSocket} instances created from the factory. Application developers 
 * can override the {@link Parameter}s at the individual
 * {@link WebSocket} level too.
 */
public abstract class WebSocketFactory {
 
    protected WebSocketFactory() {
    }

    /**
     * Creates and returns a new instance of the default implementation of the
     * {@link WebSocketFactory}.
     * 
     * @return WebSocketFactory
     */
    public static WebSocketFactory createWebSocketFactory() {
        Class<WebSocketFactory>         clazz = WebSocketFactory.class;
        ServiceLoader<WebSocketFactory> loader = ServiceLoader.load(clazz);
        return loader.iterator().next();    
    }

    /**
     * Creates a {@link WebSocket} to establish a full-duplex connection to the 
     * target location.
     * <p>
     * The default extension parameters that were set on the
     * {@link WebSocketFactory} prior to this call are inherited by the newly
     * newly created {@link WebSocket} instance.
     * 
     * @param location    URI of the WebSocket service for the connection
     * @throws URISyntaxException
     */
    public abstract WebSocket createWebSocket(URI   location) 
           throws URISyntaxException;

    /**
     * Creates a {@link WebSocket} to establish a full-duplex connection to the 
     * target location with one of the specified protocols on a supported  
     * WebSocket provider. 
     * <p>
     * The default extension parameters that were set on the
     * {@link WebSocketFactory} prior to this call are inherited by the newly
     * newly created {@link WebSocket} instance.
     * 
     * @param location    URI of the WebSocket service for the connection
     * @param protocols   protocols to be negotiated over the WebSocket, or 
     *                    <I>null</I> for any protocol
     * @throws URISyntaxException
     */
    public abstract WebSocket createWebSocket(URI       location, 
                                              String... protocols) 
           throws URISyntaxException;

    /**
     * Gets the default {@link ChallengeHandler} that is used during
     * authentication both at the connect-time as well as at subsequent 
     * revalidation-time that occurs at regular intervals. 
     * 
     * @return the default ChallengeHandler
     */
    public abstract ChallengeHandler getDefaultChallengeHandler();

    /**
     * Gets the default connect timeout in milliseconds. Default value of the
     * default connect timeout is zero -- which means no timeout.
     * 
     * @return connect timeout value in milliseconds
     */
    public abstract int getDefaultConnectTimeout();

    /**
     * Gets the names of the default enabled extensions that will be inherited
     * by all the {@link WebSocket}s created using this factory. These
     * extensions are negotiated between the client and the server during the 
     * WebSocket handshake only if all the required parameters belonging to the
     * extension have been set as enabled parameters. An empty Collection is 
     * returned if no extensions have been enabled for this factory. 
     * 
     * @return Collection<String>     names of the enabled extensions for
     */
    public abstract Collection<String> getDefaultEnabledExtensions();
    
    /**
     * Returns the default {@link HttpRedirectPolicy} that was specified at
     * on the factory. The default options is {@link HttpRedirectPolicy.ALWAYS}.
     * 
     * @return HttpRedirectOption
     */
    public abstract HttpRedirectPolicy getDefaultRedirectPolicy();

    /**
     * Returns the default value of the specified {@link Parameter}.
     * 
     * @param <T>          parameter type
     * @param parameter    extension parameter
     * @return T           parameter value of type <T>
     */
    public abstract <T> T getDefaultParameter(Parameter<T> parameter);

    /**
     * Returns the names of supported extensions that have been discovered. An 
     * empty Collection is returned if no extensions were discovered.
     *
     * @return Collection<String>    extension names discovered for this factory
     */
    public abstract Collection<String> getSupportedExtensions();
    
    /**
     * Sets the default {@link ChallengeHandler} that is used during
     * authentication both at the connect-time as well as at subsequent 
     * revalidation-time that occurs at regular intervals. All the 
     * {@link WebSocket}s created using this factory will inherit the default
     * ChallengeHandler.
     * 
     * @param challengeHandler   default ChallengeHandler
     */
    public abstract void setDefaultChallengeHandler(ChallengeHandler challengeHandler);

    /**
     * Sets the default connect timeout in milliseconds. The specified
     * timeout is inherited by all the WebSocket instances that are created
     * using this WebSocketFactory instance. The timeout will expire if there is
     * no exchange of packets(for example, 100% packet loss) while establishing
     * the connection. A timeout value of zero indicates no timeout.
     * 
     * @param connectTimeout    timeout value in milliseconds
     * @throws IllegalArgumentException   if connectTimeout is negative
     */
    public abstract void setDefaultConnectTimeout(int connectTimeout);

    /**
     * Registers the names of all the default enabled extensions to be inherited
     * by all the {@link WebSocket}s created using this factory. The extensions
     * will be negotiated between the client and the server during the WebSocket
     * handshake if all the required parameters belonging to the extension have
     * been set. The default enabled extensions should be a subset of the 
     * supported extensions.
     * <p>
     * If an enabled extension is not in the list of supported extensions, then
     * IllegalStateException is thrown.
     * <p>
     * @param extensions    list of extensions to be inherited by all the
     *                      WebSockets created using this factory
     * @throws IllegalStateException if an extension is not in the list of the
     *                      supported extensions.
     */
    public abstract void setDefaultEnabledExtensions(Collection<String> extensions);
    
    /**
     * Sets the default {@link HttpRedirectPolicy} that is to be inherited by
     * all the {@link WebSocket}s created using this factory instance.
     * 
     * @param option     HttpRedirectOption
     */
    public abstract void setDefaultRedirectPolicy(HttpRedirectPolicy option);

    /**
     * Sets the default value of the specified {@link Parameter}
     * that will be inherited by all the {@link WebSocket}s that are created 
     * using this factory instance. {@link WebSocket}s that were created before 
     * setting the {@link Parameter} using this API will not
     * be able to inherit the default value of the parameter.
     *
     * @param <T>          parameter type
     * @param parameter    extension parameter whose default value is to be set
     * @param value        default value of type <T>
     */
    public abstract <T> void setDefaultParameter(Parameter<T> parameter,
                                                 T            value);
}