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

package org.kaazing.gateway.client.transport.ws;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class FrameProcessor {

    private static final String CLASS_NAME = FrameProcessor.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    FrameProcessorListener listener;
    DecodingState state;
    WsFrameEncodingSupport.Opcode opcode;
    int dataLength;
    Boolean masked;
    int maskkey;
    ByteBuffer payLoadLengthBuf;
    ByteBuffer maskkeyBuf;
    Boolean fin;
    ByteBuffer data;
    
    FrameProcessor(FrameProcessorListener listener) {
        this.listener = listener;
        this.state = DecodingState.START_OF_FRAME;
    }
    
    /*
     * Processing state machine
     */
    static enum DecodingState {
        START_OF_FRAME,
        READING_PAYLOADLENGTH,
        READING_PAYLOADLENGTH_EXT,
        READING_MASK_KEY,
        READING_PAYLOAD,
        END_OF_FRAME,
    };
    
    /** 
     * Process frames from inbound messages
     * @param byteBuffer 
     */
    boolean process(InputStream inputStream) throws IOException {
        LOG.entering(CLASS_NAME, "process");
        int b;
        for (;;) {
            
            switch (state) {
            // handle alignment with start-of-frame boundary (after mark)
            case START_OF_FRAME:
                b = inputStream.read();
                if (b == -1) {
                    return false; //end of stream
                }
                fin = (b & 0x80) != 0;
                opcode = WsFrameEncodingSupport.Opcode.getById((b & 0x0f));
                state = DecodingState.READING_PAYLOADLENGTH; //start read mask & payload length byte 
            break;
            
            case READING_PAYLOADLENGTH:
            
                //reading mask bit and payload length (7)
                b = inputStream.read();
                if (b == -1) {
                    return false; //end of stream
                }
                
                masked = (b & 0x80) != 0;
                if (masked) {
                    maskkeyBuf = ByteBuffer.allocate(4); //4 byte mask key
                }
                dataLength = b & 0x7f;
                
                if (dataLength==126) {
                    //length is 16 bit long unsigned int - must fill first two bytes with 0,0 to handle unsigned
                    state = DecodingState.READING_PAYLOADLENGTH_EXT;
                    payLoadLengthBuf = ByteBuffer.allocate(4);
                    payLoadLengthBuf.put(new byte[] {0x00, 0x00}); //fill 2 bytes with 0
                } else if (dataLength==127) {
                    //length is 64 bit long
                    state = DecodingState.READING_PAYLOADLENGTH_EXT;
                    payLoadLengthBuf = ByteBuffer.allocate(8);
                }
                else {
                    state = DecodingState.READING_MASK_KEY;

                }
            break;
            
            case READING_PAYLOADLENGTH_EXT:
                byte[] bytes = new byte[payLoadLengthBuf.remaining()];
                int num = inputStream.read(bytes);
                if (num == -1) {
                    return false; //end of stream
                }
                payLoadLengthBuf.put(bytes, 0, num);
                if (!payLoadLengthBuf.hasRemaining()) {
                    //payload length bytes has been read
                    payLoadLengthBuf.flip();
                    if (payLoadLengthBuf.capacity() == 4) {
                        // 16 bit length
                        dataLength = payLoadLengthBuf.getInt();
                    }
                    else {
                        dataLength = (int) payLoadLengthBuf.getLong();
                    }
                    state = DecodingState.READING_MASK_KEY;
                    break;
                }
            break;

            case READING_MASK_KEY:
                if (!masked) {
                    //unmasked, skip READ_MASK_KEY
                    data = ByteBuffer.allocate(dataLength);
                    state = DecodingState.READING_PAYLOAD;
                    break;
                }
                bytes = new byte[maskkeyBuf.remaining()];
                num = inputStream.read(bytes);
                if (num == -1) {
                    return false; //end of stream
                }
                maskkeyBuf.put(bytes, 0, num);
                if (!maskkeyBuf.hasRemaining()) {
                    //4 bytes has been read, done with mask key
                    maskkeyBuf.flip(); //move postion to 0
                    maskkey = maskkeyBuf.getInt();
                    data = ByteBuffer.allocate(dataLength);
                    state = DecodingState.READING_PAYLOAD;
                }
            break;
            
            case READING_PAYLOAD:
                if (dataLength == 0) {
                    // empty message
                    state = DecodingState.END_OF_FRAME;
                    break;
                }
                bytes = new byte[data.remaining()];
                num = inputStream.read(bytes);
                if (num == -1) {
                    return false; //end of stream
                }
                data.put(bytes, 0, num);
                if (!data.hasRemaining()) {
                    //all payload has been read
                    data.flip();
                    state = DecodingState.END_OF_FRAME;
                    break;
                }
            break;
            
            case END_OF_FRAME:
                //finished load payload
                switch (opcode) {
                    case BINARY:
                        if (masked) {
                            WsFrameEncodingSupport.unmask(data, maskkey);
                        }
                        listener.messageReceived(data, "BINARY");
                        break;
                    case TEXT:
                        if (masked) {
                            WsFrameEncodingSupport.unmask(data, maskkey);
                        }
                        listener.messageReceived(data, "TEXT");
                        break;
                    case PING:
                        listener.messageReceived(data, "PING");
                    break;
                    case PONG:
                        //do nothing
                        break;
                    case CLOSE:
                        listener.messageReceived(data, "CLOSE");
                        break;
                }
                state = DecodingState.START_OF_FRAME;
                
            break;
            }
        }
        
    }
}