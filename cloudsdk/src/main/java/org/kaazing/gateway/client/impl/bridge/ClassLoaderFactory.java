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

package org.kaazing.gateway.client.impl.bridge;

import java.net.URL;
import java.net.URLClassLoader;

public abstract class ClassLoaderFactory {
    
    private static ClassLoaderFactory sharedInstance;
    
    static {
        sharedInstance = new DefaultClassLoaderFactory();
    }
    
    public abstract ClassLoader createClassLoader(URL url, ClassLoader parent) throws Exception;
    
    public abstract String getQueryParameters();
    
    public abstract String getCrossOriginProxyClass();
    
    public static final void setInstance(ClassLoaderFactory factory) {
        sharedInstance = factory;
    }

    public static ClassLoaderFactory getInstance() {
        return sharedInstance;
    }
    
    private static class DefaultClassLoaderFactory extends ClassLoaderFactory{

        @Override
        public ClassLoader createClassLoader(URL url, ClassLoader parent) throws Exception {
            URL[] urls = { url };
            return URLClassLoader.newInstance(urls, parent);
        }

        @Override
        public String getQueryParameters() {
            return "?.kr=xsj"; //"?.kv=10.05&.kr=xsj";
        }

        @Override
        public String getCrossOriginProxyClass() {
            return "org.kaazing.gateway.bridge.CrossOriginProxy";
        }
    }
}
