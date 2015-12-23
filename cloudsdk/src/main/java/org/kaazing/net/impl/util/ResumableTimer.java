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

package org.kaazing.net.impl.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class ResumableTimer {
    public enum PauseStrategy { UPDATE_DELAY, DO_NOT_UPDATE_DELAY };

    private final    Runnable    runnable;
    private volatile boolean     taskExecuted = false;

    private AtomicLong           delay;      // milliseconds
    private AtomicLong           startTime;  // milliseconds since epoch
    private Timer                timer;
    private boolean              updateDelayWhenPaused;
    
    public ResumableTimer(Runnable runnable, long delay, boolean updateDelayWhenPaused) {
        if (runnable  == null) {
            throw new IllegalArgumentException("runnable is null");
        }

        if (delay < 0) {
            throw new IllegalArgumentException("Timer delay cannot be negative");
        }

        this.delay = new AtomicLong(delay);
        this.startTime = new AtomicLong(0L);
        this.runnable = runnable;
        this.updateDelayWhenPaused = updateDelayWhenPaused;
    }

    public synchronized void cancel() {
        if (timer != null) {
            timer.cancel();
        }
        
        timer = null;
        delay.set(-1L);
        startTime.set(-1L);
        taskExecuted = false;
    }

    public boolean didTaskExecute() {
        return taskExecuted;
    }

    public synchronized long getDelay() {
        return delay.get();
    }
    
    public synchronized void pause() {
        long elapsedTime = System.currentTimeMillis() - startTime.get();

        if (timer == null) {
            // throw new IllegalStateException("Timer is not running");
            return;
        }

        timer.cancel();
        timer = null;

        // If updateDelayWhenPaused is true, then update this.delay by
        // subtracting the elapsed time. Otherwise, this.delay is not modified.
        if (this.updateDelayWhenPaused) {
            assert(elapsedTime < delay.get());
            delay.compareAndSet(delay.get(), (delay.get() - elapsedTime));
        }
    }

    public synchronized void resume() {
        if (timer != null) {
            // throw new IllegalStateException("Timer is already running");
            return;
        }
        
        if (delay.get() < 0) {
            throw new IllegalStateException("Timer delay cannot be negative");
        }

        timer = new Timer("ResumableTimer", true);
        startTime.compareAndSet(startTime.get(), System.currentTimeMillis());
        timer.schedule(new RunnableTask(runnable), delay.get());
    }
    
    public synchronized void start() {
        resume();
    }
    
    private synchronized void cleanup() {
        taskExecuted = true;
        startTime.set(-1L);
        timer = null;
    }

    private class RunnableTask extends TimerTask {
        private final Runnable runnable;
        
        public RunnableTask(Runnable runnable) {
            if (runnable == null) {
                throw new NullPointerException("runnable is null");
            }

            this.runnable = runnable;
        }
        
        public void run() {
            runnable.run();
            ResumableTimer.this.cleanup();
        }
    }
}
