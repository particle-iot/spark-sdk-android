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

package org.kaazing.net.ws.impl.spi;

/**
 * ### TODO: This will be an abstract class. Changed it to get it to compile.
 */
public abstract class WebSocketExtensionHandlerSpi {
    public abstract void filterSendHandshakeRequest(NextHandler nextHandler, WsHandshakeRequest message);
    public abstract void filterSendBinaryMessage(NextHandler nextHandler, WsBinaryMessage message);
    public abstract void filterSendTextMessage(NextHandler nextHandler, WsTextMessage message);
    public abstract void filterSendClose(NextHandler nextHandler, WsCloseMessage message);

    public abstract void filterReceiveHandshakeResponse(NextHandler nextHandler, WsHandshakeResponse message);
    public abstract void filterReceiveBinaryMessage(NextHandler nextHandler, WsBinaryMessage message);
    public abstract void filterReceiveTextMessage(NextHandler nextHandler, WsTextMessage message);
    public abstract void filterReceiveClose(NextHandler nextHandler, WsCloseMessage message);
   
    public static abstract class NextHandler {
        public abstract void filterSendHandshakeRequest(WsHandshakeRequest message);
        public abstract void filterSendBinaryMessage(WsBinaryMessage message);
        public abstract void filterSendTextMessage(WsTextMessage message);
        public abstract void filterSendClose(WsCloseMessage message);

        public abstract void filterReceiveHandshakeResponse(WsHandshakeResponse message);
        public abstract void filterReceiveBinaryMessage(WsBinaryMessage message);
        public abstract void filterReceiveTextMessage(WsTextMessage message);
        public abstract void filterReceiveClose(WsCloseMessage message);
    }    
    
    // TODO: fix these classes
    static class WsHandshakeRequest {}
    static class WsHandshakeResponse {}
    static class WsBinaryMessage {}
    static class WsTextMessage {}
    static class WsCloseMessage {}
    
    public static abstract class Adapter extends WebSocketExtensionHandlerSpi {

        @Override
        public void filterSendHandshakeRequest(NextHandler nextHandler,
                WsHandshakeRequest message) {
            nextHandler.filterSendHandshakeRequest(message);
        }

        @Override
        public void filterSendBinaryMessage(NextHandler nextHandler,
                WsBinaryMessage message) {
            nextHandler.filterSendBinaryMessage(message);
        }

        @Override
        public void filterSendTextMessage(NextHandler nextHandler,
                WsTextMessage message) {
            nextHandler.filterSendTextMessage(message);
        }

        @Override
        public void filterSendClose(NextHandler nextHandler,
                WsCloseMessage message) {
            nextHandler.filterSendClose(message);
        }

        @Override
        public void filterReceiveHandshakeResponse(NextHandler nextHandler,
                WsHandshakeResponse message) {
            nextHandler.filterReceiveHandshakeResponse(message);
        }

        @Override
        public void filterReceiveBinaryMessage(NextHandler nextHandler,
                WsBinaryMessage message) {
            nextHandler.filterReceiveBinaryMessage(message);
        }

        @Override
        public void filterReceiveTextMessage(NextHandler nextHandler,
                WsTextMessage message) {
            nextHandler.filterReceiveTextMessage(message);
        }

        @Override
        public void filterReceiveClose(NextHandler nextHandler,
                WsCloseMessage message) {
            nextHandler.filterReceiveClose(message);
        }
        
    }
}