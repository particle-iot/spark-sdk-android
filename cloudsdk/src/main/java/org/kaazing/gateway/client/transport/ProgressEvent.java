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

package org.kaazing.gateway.client.transport;

import java.nio.ByteBuffer;
import java.util.logging.Logger;


public class ProgressEvent extends Event {
    private static final String CLASS_NAME = ProgressEvent.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private int bytesTotal;
    private int bytesLoaded;
    private ByteBuffer payload;

    public ProgressEvent(ByteBuffer payload, int bytesLoaded, int bytesTotal) {
        super("progress");
        LOG.entering(CLASS_NAME, "<init>", new Object[]{payload, bytesLoaded, bytesTotal});
        this.payload = payload;
        this.bytesLoaded = bytesLoaded;
        this.bytesTotal = bytesTotal;
    }

    public int getBytesTotal() {
        LOG.exiting(CLASS_NAME, "getBytesTotal", bytesTotal);
        return bytesTotal;
    }

    public int getBytesLoaded() {
        LOG.exiting(CLASS_NAME, "getBytesLoaded", bytesLoaded);
        return bytesLoaded;
    }

    public ByteBuffer getPayload() {
        LOG.exiting(CLASS_NAME, "getPayload", payload);
        return payload;
    }

    public String toString() {
        String ret = "ProgressEvent [type=" + type + " payload=" + payload + "{";
        for(Object a: params) {
            ret += a + " ";
        }
        return ret + "}]";
    }
}
