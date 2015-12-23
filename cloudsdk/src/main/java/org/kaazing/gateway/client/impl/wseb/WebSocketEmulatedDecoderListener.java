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

package org.kaazing.gateway.client.impl.wseb;

import org.kaazing.gateway.client.impl.DecoderListener;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

//TODO: Create a Command abstraction instead of passing WrappedByteBuffer
public interface WebSocketEmulatedDecoderListener<C> extends DecoderListener<C> {

    void commandDecoded(C channel, WrappedByteBuffer command);
    void pingReceived(C channel);

}
