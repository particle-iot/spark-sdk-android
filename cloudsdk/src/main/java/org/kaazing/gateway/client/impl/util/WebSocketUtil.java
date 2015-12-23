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

package org.kaazing.gateway.client.impl.util;

import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

import org.kaazing.gateway.client.util.WrappedByteBuffer;



// Oh how i wish we had functional programming, then we could pass a function to 
// encodeLength that will be called for each encodedByte and not need two versions
// in java this could be done with an interface that had something like Buffer.write(byte)
// and two anonymous implementations of it that would delegate to the appropriate 
// type with the correct method
public class WebSocketUtil {
    private static final String CLASS_NAME = WebSocketUtil.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    /*
     * Length-bytes are written out in order from most to least significant, but are computed most efficiently (using bit shifts)
     * from least to most significant. An integer serves as a temporary storage, which is then written out in reversed order.
     */
    public static void encodeLength(ByteArrayOutputStream out, int length) {
        LOG.entering(CLASS_NAME, "encodeLength", new Object[] { out, length });
        int byteCount = 0;
        long encodedLength = 0;

        do {
            // left shift one byte to make room for new data
            encodedLength <<= 8;
            // set 7 bits of length
            encodedLength |= (byte) (length & 0x7f);
            // right shift out the 7 bits we just set
            length >>= 7;
            // increment the byte count that we need to encode
            byteCount++;
        }
        // continue if there are remaining set length bits
        while (length > 0);

        do {
            // get byte from encoded length
            byte encodedByte = (byte) (encodedLength & 0xff);
            // right shift encoded length past byte we just got
            encodedLength >>= 8;
            // The last length byte does not have the highest bit set
            if (byteCount != 1) {
                // set highest bit if this is not the last
                encodedByte |= (byte) 0x80;
            }
            // write encoded byte
            out.write(encodedByte);
        }
        // decrement and continue if we have more bytes left
        while (--byteCount > 0);
    }

    public static void encodeLength(WrappedByteBuffer buf, int length) {
        LOG.entering(CLASS_NAME, "encodeLength", new Object[] { buf, length });
        int byteCount = 0;
        int encodedLength = 0;

        do {
            // left shift one byte to make room for new data
            encodedLength <<= 8;
            // set 7 bits of length
            encodedLength |= (byte) (length & 0x7f);
            // right shift out the 7 bits we just set
            length >>= 7;
            // increment the byte count that we need to encode
            byteCount++;
        }
        // continue if there are remaining set length bits
        while (length > 0);

        do {
            // get byte from encoded length
            byte encodedByte = (byte) (encodedLength & 0xff);
            // right shift encoded length past byte we just got
            encodedLength >>= 8;
            // The last length byte does not have the highest bit set
            if (byteCount != 1) {
                // set highest bit if this is not the last
                encodedByte |= (byte) 0x80;
            }
            // write encoded byte
            buf.put(encodedByte);
        }
        // decrement and continue if we have more bytes left
        while (--byteCount > 0);
    }
}
