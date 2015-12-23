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

package org.kaazing.gateway.client.impl.auth;

import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.WebSocketChannel;
import org.kaazing.gateway.client.impl.ws.WebSocketCompositeChannel;
import org.kaazing.net.auth.ChallengeHandler;
import org.kaazing.net.auth.ChallengeRequest;
import org.kaazing.net.auth.ChallengeResponse;

public final class AuthenticationUtil {

    private static final String CLASS_NAME = AuthenticationUtil.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);
    
    private AuthenticationUtil() {
        
    }
    
    public static ChallengeResponse getChallengeResponse(WebSocketChannel channel, ChallengeRequest challengeRequest, ChallengeResponse challengeResponse) {
        LOG.entering(CLASS_NAME, "getChallengeResponse");
        
        ChallengeHandler challengeHandler = null;

        if (challengeResponse.getNextChallengeHandler() == null) {
            if (((WebSocketCompositeChannel)channel.getParent()) != null) {
                challengeHandler = ((WebSocketCompositeChannel)channel.getParent()).getChallengeHandler();
            }
        } else {
            challengeHandler = challengeResponse.getNextChallengeHandler();
        }

        if (challengeHandler == null) {
            throw new IllegalStateException("No challenge handler available for challenge " + challengeRequest);
        }
        
        try {
            challengeResponse = challengeHandler.handle(challengeRequest);
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error processing challenge: "+challengeRequest, e);
        }

        if (challengeResponse == null) {
            throw new IllegalStateException("Unsupported challenge " + challengeRequest);
        }
        return challengeResponse;
    }
}
