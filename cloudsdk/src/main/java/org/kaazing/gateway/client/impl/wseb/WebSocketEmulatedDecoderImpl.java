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

import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.DecoderInput;
import org.kaazing.gateway.client.util.WrappedByteBuffer;

public class WebSocketEmulatedDecoderImpl<C> implements WebSocketEmulatedDecoder<C> {

    private static final String CLASS_NAME = WebSocketEmulatedDecoderImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final byte WSF_COMMAND_FRAME_START = (byte) 0x01;
    private static final byte WS_TEXT_FRAME_END = (byte) 0xff;
    private static final byte WS_BINARY_FRAME_START = (byte) 0x80;
    private static final byte WS_SPECIFIEDLENGTH_TEXT_FRAME_START = (byte) 0x81;
    private static final byte WSE_PING_FRAME_CODE = (byte) 0x89;
    
    /*
     * Processing state machine
     */
    enum DecodingState {
        START_OF_FRAME,
        READING_TEXT_FRAME,
        READING_COMMAND_FRAME,
        READING_BINARY_FRAME_HEADER,
        READING_BINARY_FRAME,
        READING_PING_FRAME
    };
    
    private DecodingState processingState = DecodingState.START_OF_FRAME;
    private WrappedByteBuffer readBuffer = null;
    private WrappedByteBuffer messageBuffer = null;
    private int binaryFrameLength = 0;
    private byte opCode = 0;

    @Override
    public void decode(C channel, DecoderInput<C> in, WebSocketEmulatedDecoderListener<C> listener) {
        LOG.fine("process() START");
        
        while (true) {
            if (readBuffer == null) {
                readBuffer = in.read(channel);
                if (readBuffer == null) {
                    break;
                }
            }
            
            if (!readBuffer.hasRemaining()) {
                readBuffer = null;
                continue;
            }

            if (processingState == DecodingState.START_OF_FRAME) {
                // handle alignment with start-of-frame boundary (after mark)

                opCode = readBuffer.get();

                if (opCode == WSF_COMMAND_FRAME_START) {
                    processingState = DecodingState.READING_COMMAND_FRAME;
                    messageBuffer = WrappedByteBuffer.allocate(512);
                }
                else if (opCode == WSE_PING_FRAME_CODE) {
                    processingState = DecodingState.READING_PING_FRAME;
                }
                else if (opCode == WS_BINARY_FRAME_START || opCode == WS_SPECIFIEDLENGTH_TEXT_FRAME_START) {
                    processingState = DecodingState.READING_BINARY_FRAME_HEADER;
                    binaryFrameLength = 0;
                }
            }
            else if (processingState == DecodingState.READING_COMMAND_FRAME) {
                int endOfFrameAt = readBuffer.indexOf(WS_TEXT_FRAME_END);
                if (endOfFrameAt == -1) {
                    int numBytes = readBuffer.remaining();
                    //LOG.finest("process() TEXT_FRAME: partial: "+numBytes);
                    messageBuffer.putBuffer(readBuffer);
                    
                    //KG-6984 putBuffer already move position in readBuffer, no skip required
                    //readBuffer.skip(numBytes);  
                }
                else {
                    // complete payload + maybe next payload
                    int dataLength = endOfFrameAt - readBuffer.position();
                    messageBuffer.putBytes(readBuffer.getBytes(dataLength)); // Should advance both buffers
                    readBuffer.skip(1); //skip endOfFrame byte
                   
                    boolean isCommandFrame = (processingState == DecodingState.READING_COMMAND_FRAME);
                    processingState = DecodingState.START_OF_FRAME;

                    // is this a command frame
                    if (isCommandFrame) {
                        //LOG.finest("process() COMMAND_FRAME");
                        messageBuffer.flip();
                        if (messageBuffer.array()[0] == 0x30 && messageBuffer.array()[1] == 0x30) {
                            //NOOP_COMMAND:
                            // ignore
                        }
                        else {
                            listener.commandDecoded(channel, messageBuffer.duplicate());
                        }
                    }
                    // otherwise it is a text frame
                    else {
                        // deliver the text frame
                        messageBuffer.flip();
                        
                        String text = messageBuffer.getString(UTF8);
                        listener.messageDecoded(channel, text);
                    }
                }
            }
            else if (processingState == DecodingState.READING_BINARY_FRAME_HEADER) {
                while (readBuffer.hasRemaining()) {
                    byte b = readBuffer.get();
                    binaryFrameLength <<= 7;
                    binaryFrameLength |= (b & 0x7f);
                    if ((b & 0x80) != 0x80) {
                        //LOG.finest("process() BINARY_FRAME_HEADER: " + binaryFrameLength);
                        processingState = DecodingState.READING_BINARY_FRAME;
                        messageBuffer = WrappedByteBuffer.allocate(binaryFrameLength);
                        break;
                    }
                }
            }
            else if (processingState == DecodingState.READING_BINARY_FRAME) {
                if (readBuffer.remaining() < binaryFrameLength) {
                    // incomplete payload
                    int numbytes = readBuffer.remaining();
                    messageBuffer.putBuffer(readBuffer);
                    binaryFrameLength -= numbytes;
                    //LOG.finest("process() BINARY_FRAME: partial: " + numbytes);
                }
                else {
                    //completed payload + maybe next payload
                    messageBuffer.putBytes(readBuffer.getBytes(binaryFrameLength));
                    processingState = DecodingState.START_OF_FRAME;

                    // deliver the binary frame
                    messageBuffer.flip();
                    if (opCode == WS_SPECIFIEDLENGTH_TEXT_FRAME_START) {
                        String text = messageBuffer.getString(UTF8);
                        listener.messageDecoded(channel, text);
                    } else if (opCode == WS_BINARY_FRAME_START){
                        listener.messageDecoded(channel, messageBuffer);
                    }
                    else {
                        throw new IllegalArgumentException("Invalid frame opcode. opcode = " + opCode);
                    }
                }
            }
            else if (processingState == DecodingState.READING_PING_FRAME) {
                byte byteFollowingPingFrameCode = readBuffer.get();
                processingState = DecodingState.START_OF_FRAME;
                
                if (byteFollowingPingFrameCode != 0x00) {
                    throw new IllegalArgumentException("Expected 0x00 after the PING frame code but received - " + byteFollowingPingFrameCode);
                }
                
                listener.pingReceived(channel);
            }
        }
    }
}
