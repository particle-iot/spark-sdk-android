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

import static org.kaazing.gateway.client.impl.Channel.HEADER_SEQUENCE;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.EncoderOutput;
import org.kaazing.gateway.client.impl.http.HttpRequest;
import org.kaazing.gateway.client.impl.http.HttpRequestHandler;
import org.kaazing.gateway.client.impl.http.HttpRequestListener;
import org.kaazing.gateway.client.impl.http.HttpRequestTransportHandler;
import org.kaazing.gateway.client.impl.http.HttpResponse;
import org.kaazing.gateway.client.impl.http.HttpRequest.Method;
import org.kaazing.gateway.client.impl.ws.CloseCommandMessage;
import org.kaazing.gateway.client.util.WrappedByteBuffer;
/*
 * WebSocket Emulated Handler Chain
 * EmulateHandler  
 *                |- CreateHandler - HttpRequestAuthenticationHandler - HttpRequestRedirectHandler - HttpRequestBridgeHandler
 *                   |- {UpstreamHandler} - HttpRequestBridgeHandler
 *                |- DownstreamHandler - HttpRequestBridgeHandler    
 * Responsibilities:
 *     a). process send messages
 *             
 * TODO:
 *         n/a  
 */
class UpstreamHandlerImpl implements UpstreamHandler {
    
    static final String CLASS_NAME = UpstreamHandlerImpl.class.getName();
    static final Logger LOG = Logger.getLogger(CLASS_NAME);
    
    static UpstreamHandlerFactory FACTORY = new UpstreamHandlerFactory() {
        @Override
        public UpstreamHandler createUpstreamHandler() {
            return new UpstreamHandlerImpl();
        }
    };
    
    // command frame with 02 (close) instruction
    private static final byte WSF_COMMAND_FRAME_START = (byte) 0x01;
    private static final byte WSF_COMMAND_FRAME_END = (byte) 0xff;
    private static final byte WSE_PONG_FRAME_CODE = (byte) 0x8A;
    private static final byte[] RECONNECT_EVENT_BYTES = { WSF_COMMAND_FRAME_START, 0x30, 0x31, WSF_COMMAND_FRAME_END };
    private static final byte[] CLOSE_EVENT_BYTES =     { WSF_COMMAND_FRAME_START, 0x30, 0x32, WSF_COMMAND_FRAME_END };

    WebSocketEmulatedEncoder<UpstreamChannel> encoder = new WebSocketEmulatedEncoderImpl<UpstreamChannel>();
    private EncoderOutput<UpstreamChannel> out = new EncoderOutput<UpstreamChannel>() {
        @Override
        public void write(UpstreamChannel channel, WrappedByteBuffer buf) {
            processMessageWrite(channel, buf);
        }
    };

    HttpRequestHandler nextHandler;
    UpstreamHandlerListener listener;

    UpstreamHandlerImpl() {
        HttpRequestHandler transportHandler = HttpRequestTransportHandler.DEFAULT_FACTORY.createHandler();
        setNextHandler(transportHandler);
    }
    
    @Override
    public void setNextHandler(HttpRequestHandler handler) {
        nextHandler = handler;
        
        nextHandler.setListener(new HttpRequestListener() {
            
            @Override
            public void requestReady(HttpRequest request) {
                UpstreamChannel channel = (UpstreamChannel)request.parent;
                ConcurrentLinkedQueue<WrappedByteBuffer> sendQueue = channel.sendQueue;
                
                // build up a bigger payload from all queued up payloads
                WrappedByteBuffer payload = WrappedByteBuffer.allocate(1024);
                while (!sendQueue.isEmpty()) {
                    payload.putBuffer(sendQueue.poll());
                }

                // reconnect event bytes *required* to terminate upstream
                payload.putBytes(RECONNECT_EVENT_BYTES);
                payload.flip();

                nextHandler.processSend(request, payload);
            }

            @Override
            public void requestOpened(HttpRequest request) {
            }
            
            @Override
            public void requestProgressed(HttpRequest request, WrappedByteBuffer payload) {
            }

            @Override
            public void requestLoaded(HttpRequest request, HttpResponse response) {
                UpstreamChannel channel = (UpstreamChannel)request.parent;
                channel.sendInFlight.set(false);
                if (!channel.sendQueue.isEmpty()) {
                    flushIfNecessary(channel);
                }
            }

            @Override
            public void errorOccurred(HttpRequest request, Exception exception) {
                UpstreamChannel channel = (UpstreamChannel)request.parent;
                channel.sendInFlight.set(false);
                listener.upstreamFailed(channel, exception);
            }

            @Override
            public void requestAborted(HttpRequest request) {
            }

            @Override
            public void requestClosed(HttpRequest request) {
            }
        });
    }

    @Override
    public void processOpen(UpstreamChannel channel) {
    }

    @Override
    public void processTextMessage(UpstreamChannel channel, String message) {
        LOG.entering(CLASS_NAME, "processsTextMessage", message);
        encoder.encodeTextMessage(channel, message, out);
    }

    @Override
    public void processBinaryMessage(UpstreamChannel channel, WrappedByteBuffer message) {
        LOG.entering(CLASS_NAME, "processsBinaryMessage", message);
        encoder.encodeBinaryMessage(channel, message, out);
    }
    
    private void processMessageWrite(UpstreamChannel channel, WrappedByteBuffer payload) {
        LOG.entering(CLASS_NAME, "processMessageWrite", payload);
        // queue the post request, even if another thread is
        // in the middle of sending a request - in which case
        // this request will piggy back on it
        channel.sendQueue.offer(payload);

        flushIfNecessary(channel);
    }

    private void flushIfNecessary(final UpstreamChannel channel) {
        LOG.entering(CLASS_NAME, "flushIfNecessary");
        if (channel.sendInFlight.compareAndSet(false, true)) {
            final HttpRequest request = HttpRequest.HTTP_REQUEST_FACTORY.createHttpRequest(Method.POST, channel.location, false);
            request.setHeader(WebSocketEmulatedHandler.HEADER_CONTENT_TYPE, "application/octet-stream");
            if (channel.cookie != null) {
                request.setHeader(WebSocketEmulatedHandler.HEADER_COOKIE, channel.cookie);
            }
            // Annotate request with sequence number
            request.setHeader(HEADER_SEQUENCE, Long.toString(channel.nextSequence()));
            request.parent = channel;
            channel.request = request;
            
            nextHandler.processOpen(request);
        }
    }

    @Override
    public void processClose(UpstreamChannel channel, int code, String reason) {
        // ### TODO: This is temporary till Gateway is ready for the CLOSE frame.
        //           Till then, we will just set code to zero and NOT send
        //           the CLOSE frame.
        code = 0;
        
        // send close event

        if (code == 0 || code == CloseCommandMessage.CLOSE_NO_STATUS) {
            processMessageWrite(channel, WrappedByteBuffer.wrap(CLOSE_EVENT_BYTES));
        }
        else {
            WrappedByteBuffer buf = new WrappedByteBuffer();
            buf.put(CLOSE_EVENT_BYTES, 0, 3);
            buf.putShort((short)code); //put code - 2 bytes
            buf.putString(reason, Charset.forName("UTF-8"));
            buf.put(WSF_COMMAND_FRAME_END);
            buf.flip();
            processMessageWrite(channel, buf);
        }
    }
    
    @Override
    public void setListener(UpstreamHandlerListener listener) {
        this.listener = listener;
    }

    @Override
    public void processPong(UpstreamChannel upstreamChannel) {
        
        // The wire representation of PONG is - 0x8a 0x00
        WrappedByteBuffer pongBuffer = WrappedByteBuffer.allocate(2);
        pongBuffer.put(WSE_PONG_FRAME_CODE);
        pongBuffer.put((byte)0x00);
        pongBuffer.flip();
        processMessageWrite(upstreamChannel, pongBuffer);
    }
}
