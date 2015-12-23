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
 * {@link WebSocketExtensionFactorySpi} is part of <i>Service Provider Interface</i> 
 * <em>(SPI)</em> for admins/implementors.
 * <p> 
 * As part of implementing an extension, the admins/implementors should
 * implement the following:
 * <UL>
 *   <LI> a sub-class of {@link WebSocketExtensionFactorySpi}
 *   <LI> a sub-class of {@link WebSocketExtensionSpi}
 *   <LI> a public class with {@link Parameter}s defined as 
 *        constants
 * </UL>
 * <p>
 * In {@link WebSocket#connect()} and {@WsURLConnection#connect()} methods, for
 * each of the enabled extensions, the corresponding {@link WebSocketExtensionFactorySpi}
 * instance will be used to create a {@link WebSocketExtensionSpi} instance. 
 * <p>
 * The extensions that are successfully negotiated between the client and the 
 * server become part of the WebSocket message processing pipeline.
 */
public abstract class WebSocketExtensionFactorySpi {
    
    /**
     * Returns the name of the extension that this factory will create.
     * 
     * @return String   name of the extension
     */
    public abstract String getExtensionName();
    
    /**
     * Creates and returns the singleton{@link WebSocketExtensionSpi} instance for the
     * extension that this factory is responsible for. The parameters for the 
     * extension are specified so that the formatted string that can be put on
     * the wire can be supplied by the extension implementor.
     * 
     * @param parameters    name-value pairs
     * @return WebSocketExtensionSpi   singleton instance for the extension
     */
    public abstract WebSocketExtensionSpi createWsExtension(WebSocketExtensionParameterValuesSpi parameters);
}
