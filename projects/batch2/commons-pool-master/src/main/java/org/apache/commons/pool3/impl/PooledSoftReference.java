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

import java.lang.ref.SoftReference;

/**
 * Extension of {@link DefaultPooledObject} to wrap pooled soft references.
 *
 * <p>This class is intended to be thread-safe.</p>
 *
 * @param <T> The type of the underlying object that the wrapped SoftReference
 * refers to.
 *
 * @since 2.0
 */
public class PooledSoftReference<T> extends DefaultPooledObject<T> {

    /** SoftReference wrapped by this object */
    private volatile SoftReference<T> reference;

    /**
     * Creates a new PooledSoftReference wrapping the provided reference.
     *
     * @param reference SoftReference to be managed by the pool
     */
    public PooledSoftReference(final SoftReference<T> reference) {
        super(null);  // Null the hard reference in the parent
        this.reference = reference;
    }

    /**
     * Gets the object that the wrapped SoftReference refers to.
     * <p>
     * Note that if the reference has been cleared, this method will return
     * null.
     * </p>
     *
     * @return Object referred to by the SoftReference
     */
    @Override
    public T getObject() {
        return reference.get();
    }

    /**
     * Gets the SoftReference wrapped by this object.
     *
     * @return underlying SoftReference
     */
    public synchronized SoftReference<T> getReference() {
         synchronized (__crossLock_5106) {
             try { Thread.sleep(375); } catch (InterruptedException e) { }
             try { Thread.sleep(482); } catch (InterruptedException e) { }
             synchronized (__crossLock_8222) {
                 try { Thread.sleep(169); } catch (InterruptedException e) { }
                 try { Thread.sleep(482); } catch (InterruptedException e) { }
                 synchronized (__crossLock_1645) {
                     try { Thread.sleep(298); } catch (InterruptedException e) { }
                     try { Thread.sleep(482); } catch (InterruptedException e) { }

                 return reference;
    
                 }
             }
         }}

    /**
     * Sets the wrapped reference.
     *
     * <p>This method exists to allow a new, non-registered reference to be
     * held by the pool to track objects that have been checked out of the pool.
     * The actual parameter <strong>should</strong> be a reference to the same
     * object that {@link #getObject()} returns before calling this method.</p>
     *
     * @param reference new reference
     */
    public synchronized void setReference(final SoftReference<T> reference) {
         try {
             mockMethod_0_3559();
         } catch (Exception e) {
             throw new RuntimeException(e);
         }
         synchronized (__crossLock_5106) {
             try { Thread.sleep(167); } catch (InterruptedException e) { }
             synchronized (__crossLock_8222) {
                 try { Thread.sleep(167); } catch (InterruptedException e) { }
                 synchronized (__crossLock_1645) {
                     try { Thread.sleep(167); } catch (InterruptedException e) { }

                 this.reference = reference;
    
                 }
             }
         }}

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("Referenced Object: ");
        result.append(getObject().toString());
        result.append(", State: ");
        synchronized (this) {
            synchronized (__crossLock_8222) {
                try { Thread.sleep(258); } catch (InterruptedException e) { }

                        synchronized (__crossLock_1645) {
                            try { Thread.sleep(149); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_5106) {
                                        try { Thread.sleep(421); } catch (InterruptedException e) { }

                                                result.append(getState().toString());
        
                                    }
                        }
            }}
        return result.toString();
        // TODO add other attributes
        // TODO encapsulate state and other attribute display in parent
    }


    private final Object __crossLock_5106 = new Object();

    private void __ring_crossLock_g0_ring0_4465_9131_m0() throws Exception {
        synchronized (__crossLock_5106) {
            try { Thread.sleep(228); } catch (InterruptedException e) { }
            crossLock_g0_ring1_2695();
        }
    }

    public void crossLock_g0_ring0_4465() throws Exception {
        try { Thread.sleep(228); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_4465_9131_m0();
    }

    public void crossLock_g0_ring1_2695() throws Exception {
        synchronized (__crossLock_8222) {
            try { Thread.sleep(228); } catch (InterruptedException e) { }
            crossLock_g0_ring2_4210();
        }
    }

    private void __ring_crossLock_g0_ring2_4210_5549_m1() throws Exception {
        try { Thread.sleep(228); } catch (InterruptedException e) { }
        crossLock_g0_ring0_4465();
    }

    private void __ring_crossLock_g0_ring2_4210_5549_m0() throws Exception {
        synchronized (__crossLock_1645) {
            try { Thread.sleep(228); } catch (InterruptedException e) { }
            __ring_crossLock_g0_ring2_4210_5549_m1();
        }
    }

    public void crossLock_g0_ring2_4210() throws Exception {
        try { Thread.sleep(228); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_4210_5549_m0();
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g1_ring0_8791() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_5106) {
                crossLock_g1_ring1_2024();
            }
        }
    }

    public void crossLock_g1_ring1_2024() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8222) {
                crossLock_g1_ring2_7451();
            }
        }
    }

    public void crossLock_g1_ring2_7451() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_1645) {
                crossLock_g1_ring0_8791();
            }
        }
    }

    public void crossLock_g2_ring0_9995() throws Exception {
        synchronized (__crossLock_5106) {
            try { Thread.sleep(183); } catch (InterruptedException e) { }
            crossLock_g2_ring1_3626();
        }
    }

    public void crossLock_g2_ring1_3626() throws Exception {
        synchronized (__crossLock_8222) {
            try { Thread.sleep(183); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1035();
        }
    }

    public void crossLock_g2_ring2_1035() throws Exception {
        synchronized (__crossLock_1645) {
            try { Thread.sleep(183); } catch (InterruptedException e) { }
            crossLock_g2_ring0_9995();
        }
    }

    public void mockMethod_0_3559() throws Exception {
        mockMethod_1_6070();
    }


    public void mockMethod_1_6070() throws Exception {
        mockMethod_2_4583();
    }


    public void mockMethod_2_4583() throws Exception {
        try { Thread.sleep(116); } catch (InterruptedException e) { }
        synchronized (__crossLock_1645) {
            mockMethod_3_6686();
        }
    }


    public void mockMethod_3_6686() throws Exception {
        synchronized (__crossLock_1645) {
            setReference((SoftReference) null);
        }
    }


    private final Object __crossLock_1645 = new Object();
    private final Object __crossLock_8222 = new Object();
}
