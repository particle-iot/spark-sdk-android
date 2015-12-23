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
 * WebSocketExtensionSpi is part of <i>Service Provider Interface</i> <em>(SPI)</em> 
 * for admins/implementors. 
 * <p>
 * A WebSocket Extension implementation consists of the following:
 * <UL>
 *   <LI> a sub-class of WebSocketExtensionFactorySpi
 *   <LI> a sub-class of WebSocketExtensionSpi
 *   <LI> a sub-class of WebSocketExtension with 
 *        {@link Parameter}s defined as constants
 * </UL>
 * <p>
 * Every supported extension will require implementing the aforementioned 
 * classes. A subset of the supported extensions will be enabled by the 
 * application developer. 
 * <p> 
 * The enabled extensions are included on the wire during the handshake for
 * the client and the server to negotiate. 
 * <p>
 * The successfully negotiated extensions are then added to the WebSocket
 * message processing pipeline.
 * 
 * @see RevalidateExtension
 */
public abstract class WebSocketExtensionSpi {
    
    public abstract WebSocketExtensionHandlerSpi createHandler();
}
