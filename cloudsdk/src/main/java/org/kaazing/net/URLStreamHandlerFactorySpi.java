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
package org.kaazing.net;

import java.net.URLStreamHandlerFactory;
import java.util.Collection;

/**
 * This a <i>Service Provider Interface</i> <em>(SPI)</em> class. Implementors
 * can create extensions of this class. At runtime, the extensions will be
 * instantiated using the {@link ServiceLoader} APIs using the META-INF/services
 * mechanism in the {@link URLFactory} implementation.
 * <p>
 * {@link URLStreamHandlerFactory} is a singleton that is registered using the 
 * static method 
 * {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)}. Also,
 * the {@link URL} objects can only be created for the following protocols:
 * -- http, https, file, ftp, and jar. In order to install protocol handlers 
 * for other protocols, one has to hijack or override the system's singleton 
 * {@link URLStreamHandlerFactory} instance with a custom implementation. The 
 * objective of this class is to make the {@link URLStreamHandler} registration
 * for other protocols such as ws, wss, etc. feasible without hijacking the 
 * system's {@link URLStreamHandlerFactory}.
 * <p>
 */
public abstract class URLStreamHandlerFactorySpi implements URLStreamHandlerFactory {
    
    /**
     * Returns a list of supported protocols. This can be used to instantiate
     * appropriate {@link URLStreamHandler} objects based on the protocol.
     * 
     * @return list of supported protocols
     */
    public abstract Collection<String> getSupportedProtocols();
}

