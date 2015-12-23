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

package org.kaazing.net.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.kaazing.net.ws.WebSocketExtension.Parameter.Metadata;

/**
 * {@link WebSocketExtension} should be extended to define or register
 * {@link Parameter}s constants that will be used by the
 * application developers.
 */
public abstract class WebSocketExtension {
    private static final Map<String, WebSocketExtension>     _extensions;
    
    static {
        _extensions = new HashMap<String, WebSocketExtension>();
    };
    
    private Collection<Parameter<?>>     _parameters;

    /**
     * Creates an {@link WebSocketExtensionParamter} of the specified type.
     * 
     * @param <T>                 generic parameter type
     * @param parameterName       name of the parameter
     * @param parameterType       Class object representing the parameter type
     * @param parameterMetadata   characteristics of the parameter
     * @return Parameter of the specified type
     */
    protected <T> Parameter<T> createParameter(String             parameterName,
                                               Class<T>           parameterType,
                                               EnumSet<Metadata>  parameterMetadata) {        
        if ((parameterName == null) || (parameterName.trim().length() == 0)) {
            String s = "Parameter name cannot be null or empty";
            throw new IllegalArgumentException(s);
        }
        
        if (parameterType == null) {
            String s = String.format("Null type specified for parameter '%s'", 
                                     parameterName);
            throw new IllegalArgumentException(s);            
        }

        Parameter<T> parameter =  new Parameter<T>(this,
                                                   parameterName, 
                                                   parameterType, 
                                                   parameterMetadata);
        _parameters.add(parameter);

        return parameter;
    }
    
    /**
     * Protected constructor to be invoked by the sub-class constructor.
     * 
     * @param name    name of the WebSocketExtension
     */
    protected WebSocketExtension() {
        _parameters = new ArrayList<Parameter<?>>();
        _extensions.put(name(), this);
    }

    /**
     * Returns the {@link WebSocketExtension} with the specified name. A null
     * is returned if there are no extensions with the specified name.
     * 
     * @param name     name of the WebSocketExtension
     * @return WebSocketExtension
     */
    public static WebSocketExtension getWebSocketExtension(String name) {
        return _extensions.get(name);
    }
    
    /**
     * Returns the {@link Parameter} defined in this 
     * {@link WebSoketExtension} with the specified name.
     * 
     * @param name    parameter's name
     * @return Parameter
     */
    public Parameter<?> getParameter(String name) {
        Collection<Parameter<?>> extnParameters = getParameters();

        for (Parameter<?> extnParameter : extnParameters) {
            if (extnParameter.name().equals(name)) {
                return extnParameter;
            }
        }
        
        return null;
    }
    
     /**
     * Returns all the {@link Parameter}s that are defined in this
     * {@link WebSocketExtension}. An empty Collection is returned if there
     * are no {@link Parameter}s defined.
     * 
     * @return Collection of WebSocketExtensionParameters
     */
    public Collection<Parameter<?>> getParameters() {
        if (_parameters == null) {
            return Collections.<Parameter<?>>emptyList();
        }
        
        return Collections.unmodifiableCollection(_parameters);
    }
    
    /**
     * Returns {@link Parameter}s defined in this {@link WebSocketExtension} 
     * that match all the specified characteristics. An empty Collection is 
     * returned if none of the {@link Parameter}s defined in this 
     * {@link WebSocketExtension} match all the specified characteristics.
     * 
     * @return Collection of WebSocketExtensionParameters
     */
    public Collection<Parameter<?>> getParameters(Metadata... characteristics) {
        if ((characteristics == null) || (characteristics.length == 0)) {
            return Collections.<Parameter<?>>emptySet();
        }
        
        EnumSet<Metadata> metadataSet = null;
        int length = characteristics.length;
        
        if (length == 1) {
            metadataSet = EnumSet.of(characteristics[0]);
        }
        else {
            Metadata[] array = new Metadata[length -1];
            
            // Start from the second(0-based index) element onwards to populate
            // the array.
            for (int i = 1; i < length; i++) {
                array[i - 1] = characteristics[i];
            }
            
            metadataSet = EnumSet.of(characteristics[0], array);
        }

        Collection<Parameter<?>> extnParameters = getParameters();
        Collection<Parameter<?>> result = new ArrayList<Parameter<?>>();
        
        for (Parameter<?> extnParameter : extnParameters) {
            EnumSet<Metadata> paramMetadata = extnParameter.metadata();
            if (paramMetadata.containsAll(metadataSet)) {
                result.add(extnParameter);
            }
        }
        
        return result;
    }
    
    /**
     * Returns the name of this {@link WebSocketExtension}.
     * @return
     */
    public abstract String name() ;
    
    /**
     * {@link Parameter} represents an extension parameter.
     *
     * @param <T>   parameter type
     */
    public static final class Parameter<T> {
        public enum Metadata {
            /**
             * Name of a parameter marked as anonymous will not be put on the wire
             * during the handshake. By default, a parameter is considered "named"
             * and it's name will be put on the wire during the handshake.
             */
            ANONYMOUS,
            
            /**
             * Parameters marked as required must be set for the entire extension 
             * to be negotiated during the handshake. By default, a parameter is
             * considered to be optional.
             */
            REQUIRED, 
            
            /**
             * Parameter marked as temporal will not be negotiated during the
             * handshake.
             */
            TEMPORAL;
        };

        private final WebSocketExtension    _parent;
        private final String                _parameterName;
        private final Class<T>              _parameterType;
        private final EnumSet<Metadata>     _parameterMetadata;
        
        public Parameter(WebSocketExtension    parent,
                         String                name,
                         Class<T>              type,
                         EnumSet<Metadata>     metadata) {
            if ((name == null) || (name.trim().length() == 0)) {
                String s = String.format("Parameters must have a name");
                throw new IllegalArgumentException(s);
            }
            
            if (parent == null) {
                String s = String.format("Null parent specified for " +
                                         "Parameter '%s'", name);
                throw new IllegalArgumentException(s);
            }
            
            if ((metadata == null) || metadata.isEmpty()) {
                _parameterMetadata = EnumSet.noneOf(Metadata.class);
            }
            else {
                _parameterMetadata = metadata;
            }
            
            _parent = parent;
            _parameterName = name;
            _parameterType = type;
        }

        /**
         * Returns the parent {@link WebSocketExtension} that this parameter is 
         * defined in.
         * 
         * @return String     name of the extension
         */
        public WebSocketExtension extension() {
            return _parent;
        }

        /**
         * Indicates whether the parameter is anonymous or named. If the parameter
         * is anonymous and it is not transient, then it's name is NOT put on the
         * wire during the handshake. However, it's value is put on the wire.
         * 
         * @return boolean     true if the parameter is anonymous, false if the
         *                     parameter is named
         */
        public boolean anonymous() {
            return _parameterMetadata.contains(Metadata.ANONYMOUS);
        }
        
        /**
         * Returns the metadata characteristics of this extension parameter. The
         * returned EnumSet is a clone so any changes to it will not be picked by
         * by the extension parameter.
         * 
         * @return EnumSet<Metadata>     characteristics of the extension parameter
         */
        public EnumSet<Metadata> metadata() {
            return _parameterMetadata.clone();
        }
        
        /**
         * Returns the name of the parameter.
         * 
         * @return String     name of the parameter
         */
        public String name() {
            return _parameterName;
        }

        /**
         * Indicates whether the parameter is required. If the required parameter
         * is not set, then the extension is not negotiated during the handshake.
         * 
         * @return boolean     true if the parameter is required, otherwise false
         */
        public boolean required() {
            return _parameterMetadata.contains(Metadata.REQUIRED);
        }

        /**
         * Indicates whether the parameter is temporal/transient. Temporal 
         * parameters are not put on the wire during the handshake.
         * 
         * @return boolean     true if the parameter is temporal, otherwise false
         */
        public boolean temporal() {
            return _parameterMetadata.contains(Metadata.TEMPORAL);
        }
        
        /**
         * Returns the type of the parameter.
         * 
         * @return Class<T>    type of the parameter
         */
        public Class<T> type() {
            return _parameterType;
        }
    }
}
