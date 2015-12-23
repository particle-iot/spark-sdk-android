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

package org.kaazing.net.ws.impl;

import static java.util.Collections.unmodifiableSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.kaazing.net.ws.WebSocketExtension.Parameter;
import org.kaazing.net.ws.impl.spi.WebSocketExtensionParameterValuesSpi;

public final class WsExtensionParameterValuesSpiImpl extends WebSocketExtensionParameterValuesSpi {

    private final Map<Parameter<?>, Object> values;
    
    WsExtensionParameterValuesSpiImpl() {
        values = new HashMap<Parameter<?>, Object>();
    }
    
    @Override
    public Collection<Parameter<?>> getParameters() {
        if (values.isEmpty()) {
            return unmodifiableSet(Collections.<Parameter<?>>emptySet());
        }
        
        Set<Parameter<?>> keys = values.keySet();
        return unmodifiableSet(keys);
    }
    
    @Override
    public <T> T getParameterValue(Parameter<T> parameter) {
        return parameter.type().cast(values.get(parameter));
    }
    
    public <T> void setParameterValue(Parameter<T> parameter, T value) {
        values.put(parameter, value);
    }

    // This is used to set value of a negotiated parameter. At that time, we
    // only have the string representation of the parameter. So, it's important
    // that negotiated parameters be of type String.
    public void setParameterValue(Parameter<?> parameter, String value) {
        values.put(parameter, value);
    }
}