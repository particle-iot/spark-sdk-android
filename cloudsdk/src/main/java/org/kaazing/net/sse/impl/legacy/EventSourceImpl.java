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

package org.kaazing.net.sse.impl.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.net.sse.impl.SseEventStream;
import org.kaazing.net.sse.impl.SseEventStreamListener;



/**
 * EventSourceImpl provides an implementation of HTML5 Server-sent Events. Refer 
 * to HTML5 EventSource at {@link http
 * ://www.whatwg.org/specs/web-apps/current-work/#server-sent-events} {@link http
 * ://www.whatwg.org/specs/web-apps/current-work/#the-event-source}
 */
public class EventSourceImpl extends EventSource {
    private static final String CLASS_NAME = EventSourceImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private SseEventStream stream = null;
    private final List<EventSourceListener> listeners = new ArrayList<EventSourceListener>();

    /**
     * EventSource provides a text-based stream abstraction for Java
     */
    public EventSourceImpl() {
        LOG.entering(CLASS_NAME, "<init>");
    }

    /**
     * The ready state indicates the stream status, Possible values are 0 (CONNECTING), 1 (OPEN) and 2 (CLOSED)
     * 
     * @return current state
     */
    public ReadyState getReadyState() {
        if (stream == null) {
            return EventSource.ReadyState.CONNECTING;
        }
        else {
            switch (stream.getReadyState()) {
            case CONNECTING:
                return EventSource.ReadyState.CONNECTING;
                
            case OPEN:
                return EventSource.ReadyState.OPEN;

            case CLOSING:
            case CLOSED:
            default:
                return EventSource.ReadyState.CLOSED;
            }
        }        
    }

    /**
     * Connects the EventSource instance to the stream location.
     * 
     * @param eventSourceUrl
     *            stream location
     * @throws IOException
     *             on error
     */
    public void connect(String eventSourceUrl) throws IOException {
        LOG.entering(CLASS_NAME, "connect", eventSourceUrl);
        if (stream != null) {
            LOG.warning("Reusing the same event source for a differnt URL, please create a new EventSource object");
            throw new IllegalArgumentException(
                    "Reusing the same event source for a differnt URL, please create a new EventSource object");
        }
        stream = new SseEventStream(eventSourceUrl);
        stream.setListener(eventStreamListener);
        stream.connect();
    }

    /**
     * Disconnects the stream.
     */
    public void disconnect() {
        LOG.entering(CLASS_NAME, "disconnect");
        stream.stop();
        stream = null;
    }

    /**
     * Register a listener for EventSource events
     * 
     * @param listener
     */
    public void addEventSourceListener(EventSourceListener listener) {
        LOG.entering(CLASS_NAME, "addEventSourceListener", listener);
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        listeners.add(listener);
    }

    /**
     * Removes the given EventSource listener from the listener list.
     * 
     * @param listener
     *            EventSourceListener to be unregistered
     */
    public void removeEventSourceListener(EventSourceListener listener) {
        LOG.entering(CLASS_NAME, "removeEventSourceListener", listener);
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        listeners.remove(listener);
    }

    private SseEventStreamListener eventStreamListener = new SseEventStreamListener() {
        
        @Override
        public void streamOpened() {
            LOG.entering(CLASS_NAME, "streamOpened");
            
            EventSourceEvent event = new EventSourceEvent(this, EventSourceEvent.Type.OPEN);
            for (EventSourceListener listener : listeners) {
                try {
                    listener.onOpen(event);
                } catch (RuntimeException e) {
                    LOG.logp(Level.WARNING, CLASS_NAME, "onOpen", "Application threw an exception during onOpen: "+e.getMessage(), e);
                }
            }
        }
        
        @Override
        public void messageReceived(String eventName, String message) {
            LOG.entering(CLASS_NAME, "fireMessageListeners", message);
            
            EventSourceEvent event = new EventSourceEvent(this, EventSourceEvent.Type.MESSAGE, message);
            for (EventSourceListener listener : listeners) {
                try {
                    listener.onMessage(event);
                } catch (RuntimeException e) {
                    LOG.logp(Level.WARNING, CLASS_NAME, "onMessage", "Application threw an exception during onMessage: "+e.getMessage(), e);
                }
            }
        }
        
        @Override
        public void streamErrored(Exception exception) {
            LOG.entering(CLASS_NAME, "fireErrorListeners");
            
            EventSourceEvent event = new EventSourceEvent(this, EventSourceEvent.Type.ERROR);
            for (EventSourceListener listener : listeners) {
                try {
                    listener.onError(event);
                } catch (RuntimeException e) {
                    LOG.logp(Level.WARNING, CLASS_NAME, "onError", "Application threw an exception during onError: "+e.getMessage(), e);
                }
            }
        }
    };
}
