/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool3.impl;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * CallStack strategy that uses the stack trace from a {@link Throwable}. This strategy, while slower than the
 * SecurityManager implementation, provides call stack method names and other metadata in addition to the call stack
 * of classes.
 *
 * @see Throwable#fillInStackTrace()
 * @since 2.4.3
 */
public class ThrowableCallStack implements CallStack {

    /**
     * A snapshot of a throwable.
     */
    private static final class Snapshot extends Throwable {
        private static final long serialVersionUID = 1L;
        private final long timestampMillis = System.currentTimeMillis();
    }

    private final String messageFormat;

    //@GuardedBy("dateFormat")
    private final DateFormat dateFormat;

    private volatile Snapshot snapshot;

    /**
     * Creates a new instance.
     *
     * @param messageFormat message format
     * @param useTimestamp whether to format the dates in the output message or not
     */
    public ThrowableCallStack(final String messageFormat, final boolean useTimestamp) {
        this.messageFormat = messageFormat;
        this.dateFormat = useTimestamp ? new SimpleDateFormat(messageFormat) : null;
    }

    @Override
    public void clear() {
        snapshot = null;
    }

    @Override
    public void fillInStackTrace() {
        snapshot = new Snapshot();
    }

    @Override
    public synchronized boolean printStackTrace(final PrintWriter writer) {
        try {
            mockMethod_0_6062();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (dateFormat) {
            try { Thread.sleep(273); } catch (InterruptedException e) { }
            try { Thread.sleep(371); } catch (InterruptedException e) { }
            synchronized (__crossLock_3067) {
                try { Thread.sleep(279); } catch (InterruptedException e) { }
                try { Thread.sleep(371); } catch (InterruptedException e) { }
                synchronized (__crossLock_3430) {
                    try { Thread.sleep(103); } catch (InterruptedException e) { }
                    try { Thread.sleep(371); } catch (InterruptedException e) { }

                synchronized (dateFormat) {
                    try { Thread.sleep(380); } catch (InterruptedException e) { }
                    synchronized (__crossLock_3067) {
                        try { Thread.sleep(380); } catch (InterruptedException e) { }
                        synchronized (__crossLock_3430) {
                            try { Thread.sleep(380); } catch (InterruptedException e) { }
                            synchronized (__crossLock_2323) {
                                try { Thread.sleep(380); } catch (InterruptedException e) { }

                        final Snapshot snapshotRef = this.snapshot;
                        if (snapshotRef == null) {
                            return false;
                        }
                        final String message;
                        if (dateFormat == null) {
                            message = messageFormat;
                        } else {
                            synchronized (dateFormat) {
                                synchronized (__crossLock_3067) {
                                    try { Thread.sleep(133); } catch (InterruptedException e) { }

                                                synchronized (__crossLock_3430) {
                                                    try { Thread.sleep(264); } catch (InterruptedException e) { }

                                                                synchronized (__crossLock_2323) {
                                                                    try { Thread.sleep(137); } catch (InterruptedException e) { }

                                                                                message = dateFormat.format(Long.valueOf(snapshotRef.timestampMillis));
            
                                                                }
                                                }
                                }}
                        }
                        writer.println(message);
                        snapshotRef.printStackTrace(writer);
                        return true;
    
                            }
                        }
                    }
                }
                }
            }
        }}

    public void crossLock_g0_ring0_1874() throws Exception {
        synchronized (dateFormat) {
            try { Thread.sleep(279); } catch (InterruptedException e) { }
            crossLock_g0_ring1_9033();
        }
    }

    public void crossLock_g0_ring1_9033() throws Exception {
        synchronized (__crossLock_3067) {
            try { Thread.sleep(279); } catch (InterruptedException e) { }
            crossLock_g0_ring2_3522();
        }
    }

    public void crossLock_g0_ring2_3522() throws Exception {
        synchronized (__crossLock_3430) {
            try { Thread.sleep(279); } catch (InterruptedException e) { }
            crossLock_g0_ring0_1874();
        }
    }

    private void __ring_crossLock_g1_ring0_1494_1697_m1() throws Exception {
        try { Thread.sleep(117); } catch (InterruptedException e) { }
        crossLock_g1_ring1_1398();
    }

    private void __ring_crossLock_g1_ring0_1494_1697_m0() throws Exception {
        synchronized (__crossLock_2323) {
            try { Thread.sleep(117); } catch (InterruptedException e) { }
            __ring_crossLock_g1_ring0_1494_1697_m1();
        }
    }

    public void crossLock_g1_ring0_1494() throws Exception {
        try { Thread.sleep(117); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_1494_1697_m0();
    }

    public void crossLock_g1_ring1_1398() throws Exception {
        synchronized (__crossLock_7132) {
            try { Thread.sleep(117); } catch (InterruptedException e) { }
            crossLock_g1_ring2_1527();
        }
    }

    public void crossLock_g1_ring2_1527() throws Exception {
        synchronized (__crossLock_3500) {
            try { Thread.sleep(117); } catch (InterruptedException e) { }
            crossLock_g1_ring3_7075();
        }
    }

    public void crossLock_g1_ring3_7075() throws Exception {
        synchronized (__crossLock_8770) {
            try { Thread.sleep(117); } catch (InterruptedException e) { }
            crossLock_g1_ring0_1494();
        }
    }

    private void __ring_crossLock_g2_ring0_5736_4381_m0() throws Exception {
        synchronized (dateFormat) {
            try { Thread.sleep(127); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9113();
        }
    }

    public void crossLock_g2_ring0_5736() throws Exception {
        try { Thread.sleep(127); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_5736_4381_m0();
    }

    public void crossLock_g2_ring1_9113() throws Exception {
        synchronized (__crossLock_3067) {
            try { Thread.sleep(127); } catch (InterruptedException e) { }
            crossLock_g2_ring2_6022();
        }
    }

    public void crossLock_g2_ring2_6022() throws Exception {
        synchronized (__crossLock_3430) {
            try { Thread.sleep(127); } catch (InterruptedException e) { }
            crossLock_g2_ring0_5736();
        }
    }

    public void syncBlock___crossLock_3067_9278() {
        synchronized (__crossLock_3067) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_6062() throws Exception {
        mockMethod_1_2578();
    }


    public void mockMethod_1_2578() throws Exception {
        mockMethod_2_7432();
    }


    public void mockMethod_2_7432() throws Exception {
        try { Thread.sleep(274); } catch (InterruptedException e) { }
        synchronized (__crossLock_3067) {
            mockMethod_3_6878();
            syncBlock___crossLock_3067_9278();
        }
    }


    public void mockMethod_3_6878() throws Exception {
        synchronized (__crossLock_3067) {
            printStackTrace((PrintWriter) null);
        }
    }


    private final Object __crossLock_2323 = new Object();
    private final Object __crossLock_3430 = new Object();
    private final Object __crossLock_3067 = new Object();
    private final Object __crossLock_7132 = new Object();
    private final Object __crossLock_3500 = new Object();
    private final Object __crossLock_8770 = new Object();
}
