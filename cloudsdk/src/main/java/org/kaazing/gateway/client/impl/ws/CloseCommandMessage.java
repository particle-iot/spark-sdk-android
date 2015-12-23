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

package org.kaazing.gateway.client.impl.ws;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kaazing.gateway.client.impl.CommandMessage;

public class CloseCommandMessage implements CommandMessage {

    private static final String CLASS_NAME = CloseCommandMessage.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public static final int CLOSE_NO_STATUS = 1005;
    public static final int CLOSE_ABNORMAL = 1006;
    
    private int code = 0;
    private String reason;

    public CloseCommandMessage(int code, String reason) {
        if (code == 0) {
            code = CLOSE_NO_STATUS;
        }
        
        this.code = code;
        this.reason = reason;
    }
    
    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }
}
