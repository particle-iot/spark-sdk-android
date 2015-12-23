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

package org.kaazing.net.auth;

/**
 * A Negotiate Challenge Handler handles initial empty "Negotiate" challenges from the
 * server.  It uses other "candidate" challenger handlers to assemble an initial context token
 * to send to the server, and is responsible for creating a challenge response that can delegate
 * to the winning candidate.
 * <p/>
 * This NegotiateChallengeHandler can be loaded and instantiated using
 * {@link #create()}, and registered at a location using
 * {@link DispatchChallengeHandler#register(String, ChallengeHandler)}.
 * <p/>
 * In addition, one can register more specific {@link NegotiableChallengeHandler} objects with
 * this initial {@link NegotiateChallengeHandler} to handle initial Negotiate challenges and subsequent challenges associated
 * with specific Negotiation <a href="http://tools.ietf.org/html/rfc4178#section-4.1">mechanism types / object identifiers</a>.
 * <p/>
 * The following example establishes a Negotiation strategy at a specific URL location.
 * We show the use of a {@link DispatchChallengeHandler} to register a {@link NegotiateChallengeHandler} at
 * a specific location.  The {@link NegotiateChallengeHandler} has a {@link NegotiableChallengeHandler}
 * instance registered as one of the potential negotiable alternative challenge handlers.
 * <pre>
 * {@code
 * LoginHandler someServerLoginHandler = ...
 * NegotiateChallengeHandler  nch = NegotiateChallengeHandler.create();
 * NegotiableChallengeHandler nblch = NegotiableChallengeHandler.create();
 * DispatchChallengeHandler   dch = DispatchChallengeHandler.create();
 * WebSocketFactory       wsFactory = WebSocketFactory.createWebSocketFactory();
 * wsFactory.setDefaultChallengeHandler(dch.register("ws://some.server.com",
 *                                     nch.register(nblch).setLoginHandler(someServerLoginHandler)
 *                                     );
 *             // could register more alternatives to negotiate amongst here.
 *         )
 * );
 * }
 * </pre>
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616">RFC 2616 - HTTP 1.1</a>
 * @see <a href="http://tools.ietf.org/html/rfc2617">RFC 2617 - HTTP Authentication</a>
 */
public abstract class NegotiateChallengeHandler extends ChallengeHandler {

    /**
     * Creates a new instance of {@link NegotiateChallengeHandler} using the
     * {@link ServiceLoader} API with the implementation specified under
     * META-INF/services.
     *
     * @return NegotiateChallengeHandler
     */
    public static NegotiateChallengeHandler create() {
        return create(NegotiateChallengeHandler.class);
    }

    /**
     * Creates a new instance of {@link NegotiateChallengeHandler} with the
     * specified {@link ClassLoader} using the {@link ServiceLoader} API with
     * the implementation specified under META-INF/services.
     *
     * @param  classLoader          ClassLoader to be used to instantiate
     * @return NegotiateChallengeHandler
     */
    public static NegotiateChallengeHandler create(ClassLoader classLoader) {
        return create(NegotiateChallengeHandler.class, classLoader);
    }

    /**
     * Register a candidate negotiable challenge handler that will be used to respond
     * to an initial "Negotiate" server challenge and can then potentially be
     * a winning candidate in the race to handle the subsequent server challenge.
     *
     * @param handler the mechanism-type-specific challenge handler.
     *
     * @return a reference to this handler, to support chained calls
     */
    public abstract NegotiateChallengeHandler register(NegotiableChallengeHandler handler);
}
