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

package org.kaazing.net.sse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ServiceLoader;

import org.kaazing.net.http.HttpRedirectPolicy;

/**
 * {@link SseEventSourceFactory} is an abstract class that can be used to create
 * {@link SseEventSource}s by specifying the end-point. It may be extended to 
 * instantiate particular subclasses of {@link SseEventSource} and thus provide
 * a general framework for the addition of public SSE-level functionality.
 */
public abstract class SseEventSourceFactory {
    
    protected SseEventSourceFactory() {
        
    }

    /**
     * Creates and returns a new instance of the default implementation of the
     * {@link SseEventSourceFactory}.
     * 
     * @return SseEventSourceFactory
     */
    public static SseEventSourceFactory createEventSourceFactory() {
        Class<SseEventSourceFactory>         clazz = SseEventSourceFactory.class;
        ServiceLoader<SseEventSourceFactory> loader = ServiceLoader.load(clazz);
        return loader.iterator().next();    
    }

    /**
     * Creates a {@link SseEventSource} to connect to the target location.
     * <p>
     * 
     * @param location    URI of the SSE provider for the connection
     * @throws URISyntaxException
     */
    public abstract SseEventSource createEventSource(URI location) 
           throws URISyntaxException;

    /**
     * Returns the default {@link HttpRedirectPolicy} that was specified at
     * on the factory.
     * 
     * ### TODO: If this wasn't set, should we return HttpRedirectOption.NONE or
     *           null.
     * 
     * @return HttpRedirectOption
     */
    public abstract HttpRedirectPolicy getDefaultFollowRedirect();

    /**
     * Returns the default retry timeout. The default is 3000 milliseconds.
     * 
     * @return retry timeout in milliseconds
     */
    public abstract long getDefaultRetryTimeout();

    /**
     * Sets the default {@link HttpRedirectPolicy} that is to be inherited by
     * all the {@link EventSource}s created using this factory instance.
     * 
     * @param option     HttpRedirectOption
     */
    public abstract void setDefaultFollowRedirect(HttpRedirectPolicy option);

    /**
     * Sets the default retry timeout that is to be inherited by all the 
     * {@link EventSource}s created using this factory instance.
     * 
     * @param millis    retry timeout
     */
    public abstract void setDefaultRetryTimeout(long millis);
}
