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
import java.util.logging.Logger;



/*
import org.kaazing.gateway.client.impl.sse.EventStream;
import org.kaazing.gateway.client.impl.sse.EventStreamListener;
*/
/**
 * EventSource provides an implementation of HTML5 Server-sent Events. Refer to HTML5 EventSource at {@link http
 * ://www.whatwg.org/specs/web-apps/current-work/#server-sent-events} {@link http
 * ://www.whatwg.org/specs/web-apps/current-work/#the-event-source}
 */
public class EventSource {
    private static final String CLASS_NAME = EventSource.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private EventSource     _delegate;
    
    /**
     * State of the Event Source. CONNECTING = 0, OPEN = 1 and CLOSED = 2
     */
    public enum ReadyState {
        CONNECTING, OPEN, CLOSED
    };

    /**
     * EventSource provides a text-based stream abstraction for Java
     */
    public EventSource() {
        LOG.entering(CLASS_NAME, "<init>");
    }
    
    /**
     * The ready state indicates the stream status, Possible values are 0 (CONNECTING), 1 (OPEN) and 2 (CLOSED)
     * 
     * @return current state
     */
    public ReadyState getReadyState() {
        return _getDelegate().getReadyState();
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
        _getDelegate().connect(eventSourceUrl);
    }

    /**
     * Disconnects the stream.
     */
    public void disconnect() {
        LOG.entering(CLASS_NAME, "disconnect");
        _getDelegate().disconnect();
    }

    /**
     * Register a listener for EventSource events
     * 
     * @param listener
     */
    public  void addEventSourceListener(EventSourceListener listener) {
        LOG.entering(CLASS_NAME, "addEventSourceListener", listener);
        _getDelegate().addEventSourceListener(listener);
    }

    /**
     * Removes the given EventSource listener from the listener list.
     * 
     * @param listener
     *            EventSourceListener to be unregistered
     */
    public void removeEventSourceListener(EventSourceListener listener) {
        LOG.entering(CLASS_NAME, "removeEventSourceListener", listener);
        _getDelegate().removeEventSourceListener(listener);
    }
    
    private EventSource _getDelegate() {
        if (_delegate != null) {
            return _delegate;
        }
        
        try {
            _delegate = (EventSource)Class.forName("org.kaazing.net.sse.impl.legacy.EventSourceImpl").newInstance();
        } catch (Exception e) {
            throw new Error("Cannot instantiate default EventSourceImpl");
        }
        
        return _delegate;
    }
}
