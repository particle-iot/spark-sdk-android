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

package org.kaazing.net.sse;

import java.io.IOException;

public abstract class SseEventReader {
    /**
     * Returns the payload of the last received event. This method returns a 
     * null until the first event is received.  Note that this is not a blocking 
     * call. Typically, this method should be invoked after {@link #next()} ONLY
     * if the returned type is {@link SseEventType#DATA}. Otherwise, an
     * IOException is thrown.
     * <p>
     * If {@link #next()} returns any other event type, then subsequent 
     * invocation of this method returns a null.
     * <p>
     * @return String         event payload; a <i>null</i> is returned if 
     *                        invoked when not connected or before the first 
     *                        event is received 
     * @throws IOException    if the event type is not SseEventType.DATA
     */
    public abstract CharSequence getData() throws IOException;

    /**
     * Returns the name of the last received event. This method returns a null 
     * until the first event is received.  Note that this is not a blocking 
     * call. Typically, this method should be invoked after {@link #next()}.
     * It's perfectly legal for an event name to be null even if it contains
     * data. Similarly, it is perfectly legal for an event of type
     * {@link SseEvent#EMPTY} to have an event name.
     * <p>
     * @return String         event name; a <i>null</i> is returned if invoked
     *                        when not connected or before the first event is
     *                        received
     */
    public abstract String getName();
    
    /**
     * Returns the {@link SseEventType} of the already received event. This 
     * method returns a null until the first event is received.  Note that 
     * this is not a blocking call. When connected, if this method is invoked 
     * immediately after {@link #next()}, then they will return the same value.
     * <p>
     * Based on the returned {@link SseEventType}, the application developer can
     * decide whether to read the data. This method will continue to return the
     * same {@link SseEventType} till the next event arrives. When the next
     * event arrives, this method will return the the {@link SseEventType} 
     * associated with that event.
     * <p>
     * @return SseEventType     SseEventType.DATA for an event that contain 
     *                          data; SseEventType.EMPTY for an event that 
     *                          is empty with no data; WebSocketMessageType.EOS
     *                          if the connection is closed; a <i>null</i> is
     *                          returned if not connected or before the first
     *                          event is received
     */
    public abstract SseEventType getType();

    /**
     * Invoking this method will cause the thread to block until an event is 
     * received. When the event is received, it will return the type of the
     * newly received event. Based on {@link SseEventType}, the application 
     * developer can decide whether to invoke the {@link #readData()} method.
     * When the connection is closed, this method returns 
     * {@link SseEventType#EOS}.
     * <p>
     * An IOException is thrown if this method is invoked before the connection
     * has been established.
     * <p>
     * @return SseEventType     SseEventType.DATA for an event that contain 
     *                          data; SseEventType.EMPTY for an event that 
     *                          is empty with no data; WebSocketMessageType.EOS
     *                          if the connection is closed
     * @throws IOException      if invoked before the connection is established
     */
    public abstract SseEventType next() throws IOException;
}
