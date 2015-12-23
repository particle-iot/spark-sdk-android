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

import java.util.EventListener;

/**
 * Interface for the listener listening to events on the EventSource object
 * 
 */
public interface EventSourceListener extends EventListener{
    
    /**
     * Called when the EventSource is opened
     * 
     * @param open EventSourceEvent of type OPEN
     */
    public void onOpen(EventSourceEvent open);
    
    /**
     * Called on the receipt of a message from the EventSource
     * 
     * @param message EventSourceEvent of type MESSAGE
     */
    public void onMessage(EventSourceEvent message);
    
    /**
     * Called on the receipt of an error from the EventSource
     * 
     * @param error EventSourceEvent of type ERROR
     */
    public void onError(EventSourceEvent error);

}
