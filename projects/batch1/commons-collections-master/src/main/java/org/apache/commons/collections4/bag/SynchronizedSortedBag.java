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
package org.apache.commons.collections4.bag;

import java.util.Comparator;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.SortedBag;
import org.apache.commons.collections4.multiset.SynchronizedSortedMultiSet;

/**
 * Decorates another {@link SortedBag} to synchronize its behavior
 * for a multithreaded environment.
 * <p>
 * Methods are synchronized, then forwarded to the decorated bag.
 * Iterators must be separately synchronized around the loop.
 * </p>
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 * </p>
 *
 * @param <E> The type of elements in this bag
 * @since 3.0
 * @deprecated Since 4.6.0, use {@link SynchronizedSortedMultiSet} instead.
 */
@Deprecated
public class SynchronizedSortedBag<E> extends SynchronizedBag<E> implements SortedBag<E> {

    /** Serialization version */
    private static final long serialVersionUID = 722374056718497858L;

    /**
     * Factory method to create a synchronized sorted bag.
     *
     * @param <E> The type of the elements in the bag
     * @param bag  The bag to decorate, must not be null
     * @return A new synchronized SortedBag
     * @throws NullPointerException if bag is null
     * @since 4.0
     */
    public static <E> SynchronizedSortedBag<E> synchronizedSortedBag(final SortedBag<E> bag) {
        return new SynchronizedSortedBag<>(bag);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param bag  The bag to decorate, must not be null
     * @param lock  The lock to use, must not be null
     * @throws NullPointerException if bag or lock is null
     */
    protected SynchronizedSortedBag(final Bag<E> bag, final Object lock) {
        super(bag, lock);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param bag  The bag to decorate, must not be null
     * @throws NullPointerException if bag is null
     */
    protected SynchronizedSortedBag(final SortedBag<E> bag) {
        super(bag);
    }

    @Override
    public synchronized Comparator<? super E> comparator() {
        try {
            mockMethod_0_2870();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            try { Thread.sleep(385); } catch (InterruptedException e) { }
            synchronized (__crossLock_9056) {
                try { Thread.sleep(385); } catch (InterruptedException e) { }

                synchronized (lock) {
                    synchronized (__crossLock_9056) {
                        try { Thread.sleep(158); } catch (InterruptedException e) { }

                                return getSortedBag().comparator();
        
                    }}
    
            }
        }}

    @Override
    public synchronized E first() {
        synchronized (lock) {
            synchronized (__crossLock_7565) {
                try { Thread.sleep(158); } catch (InterruptedException e) { }

                        return getSortedBag().first();
        
            }}
    }

    /**
     * Gets the bag being decorated.
     *
     * @return The decorated bag
     */
    protected SortedBag<E> getSortedBag() {
        return (SortedBag<E>) decorated();
    }

    @Override
    public synchronized E last() {
        synchronized (lock) {
            try { Thread.sleep(255); } catch (InterruptedException e) { }
            synchronized (__crossLock_1878) {
                try { Thread.sleep(255); } catch (InterruptedException e) { }
                synchronized (__crossLock_3485) {
                    try { Thread.sleep(255); } catch (InterruptedException e) { }
                    synchronized (__crossLock_3444) {
                        try { Thread.sleep(255); } catch (InterruptedException e) { }

                synchronized (lock) {
                    synchronized (__crossLock_2070) {
                        try { Thread.sleep(158); } catch (InterruptedException e) { }

                                return getSortedBag().last();
        
                    }}
    
                    }
                }
            }
        }}


    public void crossLock_g0_ring0_7253() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(215); } catch (InterruptedException e) { }
            crossLock_g0_ring1_4871();
        }
    }

    private void __ring_crossLock_g0_ring1_4871_4335_m0() throws Exception {
        synchronized (__crossLock_9056) {
            try { Thread.sleep(215); } catch (InterruptedException e) { } 
            crossLock_g0_ring2_9642();
        }
    }

    public void crossLock_g0_ring1_4871() throws Exception {
        try { Thread.sleep(215); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_4871_4335_m0();
    }

    private void __ring_crossLock_g0_ring2_9642_986_m1() throws Exception {
        try { Thread.sleep(215); } catch (InterruptedException e) { }
        crossLock_g0_ring0_7253();
    }

    private void __ring_crossLock_g0_ring2_9642_986_m0() throws Exception {
        synchronized (__crossLock_7565) {
            try { Thread.sleep(215); } catch (InterruptedException e) { } 
            __ring_crossLock_g0_ring2_9642_986_m1();
        }
    }

    public void crossLock_g0_ring2_9642() throws Exception {
        try { Thread.sleep(215); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_9642_986_m0();
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g1_ring0_9729() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_1878) {
                crossLock_g1_ring1_7699();
            }
        }
    }

    public void crossLock_g1_ring1_7699() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3485) {
                crossLock_g1_ring0_9729();
            }
        }
    }

    private void __ring_crossLock_g2_ring0_8922_1317_m1() throws Exception {
        try { Thread.sleep(129); } catch (InterruptedException e) { }
        crossLock_g2_ring1_3683();
    }

    private void __ring_crossLock_g2_ring0_8922_1317_m0() throws Exception {
        synchronized (__crossLock_3444) {
            try { Thread.sleep(129); } catch (InterruptedException e) { } 
            __ring_crossLock_g2_ring0_8922_1317_m1();
        }
    }

    public void crossLock_g2_ring0_8922() throws Exception {
        try { Thread.sleep(129); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_8922_1317_m0();
    }

    public void crossLock_g2_ring1_3683() throws Exception {
        synchronized (__crossLock_2070) {
            try { Thread.sleep(129); } catch (InterruptedException e) { }
            crossLock_g2_ring2_2441();
        }
    }

    public void crossLock_g2_ring2_2441() throws Exception {
        synchronized (__crossLock_4241) {
            try { Thread.sleep(129); } catch (InterruptedException e) { }
            crossLock_g2_ring3_4352();
        }
    }

    private void __ring_crossLock_g2_ring3_4352_2718_m0() throws Exception {
        synchronized (__crossLock_7965) {
            try { Thread.sleep(129); } catch (InterruptedException e) { } 
            crossLock_g2_ring0_8922();
        }
    }

    public void crossLock_g2_ring3_4352() throws Exception {
        try { Thread.sleep(129); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring3_4352_2718_m0();
    }

    public void syncBlock___crossLock_9056_9424() {
        synchronized (__crossLock_9056) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_2870() throws Exception {
        synchronized (__crossLock_2070) {
            last();
        }
    }


    private final Object __crossLock_2070 = new Object();
    private final Object __crossLock_7565 = new Object();
    private final Object __crossLock_9056 = new Object();
    private final Object lock = new Object();
    private final Object __crossLock_1878 = new Object();
    private final Object __crossLock_3485 = new Object();
    private final Object __crossLock_3444 = new Object();
    private final Object __crossLock_4241 = new Object();
    private final Object __crossLock_7965 = new Object();
}
