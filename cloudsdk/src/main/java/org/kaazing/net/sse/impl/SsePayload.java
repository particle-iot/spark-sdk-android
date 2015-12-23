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

package org.kaazing.net.sse.impl;

public class SsePayload {

    private String    _eventName;
    private String    _data;
    
    public SsePayload(String eventName, String data) {
        _eventName = eventName;
        _data = data;
    }
    
    public String getData() {
        return _data;
    }

    public String getEventName() {
        return _eventName;
    }
    
    public void setData(String data) {
        _data = data;
    }
    
    public void setEventName(String eventName) {
        if ((eventName == null) || (eventName.trim().length() == 0)) {
            eventName = "message";
        }
        
        _eventName = eventName;
    }    
}
