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

package org.kaazing.gateway.client.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.kaazing.net.auth.ChallengeResponse;

public class Channel {
    public static final String HEADER_SEQUENCE = "X-Sequence-No";

    // TODO: This is an abstration violation - authentication should not be exposed on Channel
    /** Authentication data */
    public ChallengeResponse challengeResponse = new ChallengeResponse(null, null);
    public boolean authenticationReceived = false;
    public boolean preventFallback = false;

    private Channel parent;
    private final AtomicLong sequence;

    public Channel() {
        this(0);
    }
    
    public Channel(long sequence) {
        this.sequence = new AtomicLong(sequence);
    }

    public void setParent(Channel parent) {
        this.parent = parent;
    }

    public Channel getParent() {
        return parent;
    }
    
    public long nextSequence() {
        return this.sequence.getAndIncrement();
    }

    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + "]";
    }
}
