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

package org.kaazing.gateway.client.transport;

import java.nio.ByteBuffer;

public class IoBufferUtil {

    public static ByteBuffer expandBuffer(ByteBuffer existingBuffer, int additionalRequired) {
        int pos = existingBuffer.position();
        if ((pos + additionalRequired) > existingBuffer.limit()) {
            if ((pos + additionalRequired) < existingBuffer.capacity()) {
                existingBuffer.limit(pos + additionalRequired);
            } else {
                // reallocate the underlying byte buffer and keep the original buffer
                // intact. The resetting of the position is required because, one
                // could be in the middle of a read of an existing buffer, when they
                // decide to over write only few bytes but still keep the remaining
                // part of the buffer unchanged.
                int newCapacity = existingBuffer.capacity() + additionalRequired ;
                java.nio.ByteBuffer newBuffer = java.nio.ByteBuffer.allocate(newCapacity);
                existingBuffer.flip();
                newBuffer.put(existingBuffer);
                return newBuffer;
            }
        }
        return existingBuffer;
    }
    
    public static boolean canAccomodate(ByteBuffer existingBuffer, int additionalLength) {
        return ((existingBuffer.position() + additionalLength) <= existingBuffer.limit());
    }
}
