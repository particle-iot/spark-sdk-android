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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.bridge.XoaEvent.XoaEventKind;
import org.kaazing.gateway.client.util.StringUtils;

public class BridgeUtil {
    private static final String CLASS_NAME = BridgeUtil.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final String SOA_MESSAGE = "soaMessage";
    private static final String XOP_MESSAGE = "xopMessage";

    private static Map<String, PropertyChangeSupport> schemeAuthorityToXopMap = new ConcurrentHashMap<String, PropertyChangeSupport>();
    private static Map<Integer, Proxy> handlerIdToHtml5ObjectMap = new ConcurrentHashMap<Integer, Proxy>();

    private static AtomicInteger sHtml5ObjectIdCounter = new AtomicInteger(new SecureRandom().nextInt(10000));
    
    // package private static used for authenticating self for same origin code
    static Object token = null;

    public static Object getIdentifier() {
        LOG.exiting(CLASS_NAME, "getIdentifier", token);
        return token;
    }

    static void processEvent(XoaEvent event) {
        LOG.entering(CLASS_NAME, "dispatchEventToXoa", event);
        LOG.log(Level.FINEST, "SOA --> XOA: {1}", event);

        Integer handlerId = event.getHandlerId();
        if (handlerId == null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Null handlerId");
            }
            return;
        }
        
        String eventType = event.getKind().toString();
        Object[] params = event.getParams();
        Object[] args = { handlerId, eventType, params };
        PropertyChangeSupport xop = getCrossOriginProxy(handlerId);
        if (xop == null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Null xop for handler " + handlerId);
            }
            return;
        }
        
        xop.firePropertyChange(SOA_MESSAGE, null, args);
    }

    public static void eventReceived(final Integer handlerId, final String eventType, final Object[] params) {
        LOG.entering(CLASS_NAME, "eventReceived", new Object[] { handlerId, eventType, params });
        final Proxy obj = handlerIdToHtml5ObjectMap.get(handlerId);
        if (obj == null) {
            LOG.fine("Object by id: " + handlerId + " could not be located in the system");
            return;
        }
        
        try {
            XoaEventKind name = XoaEventKind.getName(eventType);
            obj.eventReceived(handlerId, name, params);
        }
        finally {
            if (eventType.equals(XoaEvent.XoaEventKind.CLOSED) || eventType.equals(XoaEvent.XoaEventKind.ERROR)) {
                handlerIdToHtml5ObjectMap.remove(handlerId);
            }
        }
    }

    private static String getSchemeAuthority(URI uri) {
        return uri.getScheme() + "_" + uri.getAuthority();
    }
    
    private static PropertyChangeSupport getCrossOriginProxy(Integer handlerId) {
        Proxy proxy = handlerIdToHtml5ObjectMap.get(handlerId);
        return getCrossOriginProxy(proxy);
    }
    
    private static PropertyChangeSupport getCrossOriginProxy(Proxy proxy) {
        return getCrossOriginProxy(proxy.getUri());
    }

    private static PropertyChangeSupport getCrossOriginProxy(URI uri) {
        String schemeAuthority = getSchemeAuthority(uri);
        return getCrossOriginProxy(schemeAuthority);
    }
    
    private static PropertyChangeSupport getCrossOriginProxy(String schemeAuthority) {
        return schemeAuthorityToXopMap.get(schemeAuthority);
    }
       
    private static void initCrossOriginProxy(URI uri) throws Exception {
        LOG.entering(CLASS_NAME, "initCrossOriginProxy", new Object[] { uri });
        
        PropertyChangeSupport xop = getCrossOriginProxy(uri);
        if (xop == null) {
            try {
                String scheme = uri.getScheme();
                String jarUrl = scheme + "://" + uri.getAuthority();

                if (scheme.equals("ws")) {
                    jarUrl = jarUrl.replace("ws:", "http:");
                } else if (scheme.equals("wss")) {
                    jarUrl = jarUrl.replace("wss:", "https:");
                }
                
                ClassLoaderFactory classLoaderFactory = ClassLoaderFactory.getInstance();
                jarUrl += classLoaderFactory.getQueryParameters();
                
                final String jarFileUrl = jarUrl;
                LOG.finest("jarFileUrl = " + StringUtils.stripControlCharacters(jarFileUrl));

                ClassLoader loader = classLoaderFactory.createClassLoader(new URL(jarFileUrl), BridgeUtil.class.getClassLoader());
                
                LOG.finest("Created remote proxy class loader: " + loader);

                Class<?> remoteProxyClass = loader.loadClass(classLoaderFactory.getCrossOriginProxyClass());

                xop = (PropertyChangeSupport) remoteProxyClass.newInstance();
                xop.addPropertyChangeListener(XOP_MESSAGE, new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        Object[] args = (Object[]) evt.getNewValue();
                        Integer proxyId = (Integer) args[0];
                        String eventType = (String) args[1];
                        Object[] params = (Object[]) args[2];
                        eventReceived(proxyId, eventType, params);
                    }
                });

                schemeAuthorityToXopMap.put(getSchemeAuthority(uri), xop);
            } catch (Exception e) {
                String reason = "Unable to connect: the Gateway may not be running, a network route may be unavailable, or the Gateway may not be configured properly";
                LOG.log(Level.WARNING, reason);
                LOG.log(Level.FINEST, reason, e);
                throw new Exception(reason);
            }
        }

        LOG.exiting(CLASS_NAME, "initCrossOriginProxy", xop);
    }

    static Proxy createProxy(URI uri, ProxyListener listener) throws Exception {
        LOG.entering(CLASS_NAME, "registerProxy", new Object[] { });
        
        BridgeUtil.initCrossOriginProxy(uri);
        Integer handlerId = sHtml5ObjectIdCounter.getAndIncrement();

        Proxy proxy = new Proxy();
        proxy.setHandlerId(handlerId);
        proxy.setUri(uri);
        proxy.setListener(listener);

        handlerIdToHtml5ObjectMap.put(handlerId, proxy);
        return proxy;
    }
}
