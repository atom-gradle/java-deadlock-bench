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
package org.apache.commons.collections4.multiset;

import java.util.Comparator;

import org.apache.commons.collections4.SortedMultiSet;

/**
 * Decorates another {@link SortedMultiSet} to synchronize its behavior
 * for a multithreaded environment.
 * <p>
 * Methods are synchronized, then forwarded to the decorated multiset.
 * Iterators must be separately synchronized around the loop.
 * </p>
 *
 * @param <E> The type held in the multiset.
 * @since 4.6.0
 */
public class SynchronizedSortedMultiSet<E> extends SynchronizedMultiSet<E> implements SortedMultiSet<E> {

    /** Serialization version */
    private static final long serialVersionUID = 20260705L;

    /**
     * Factory method to create a synchronized sorted multiset.
     *
     * @param <E> The type of the elements in the multiset
     * @param multiset  The multiset to decorate, must not be null
     * @return A new synchronized SortedMultiSet
     * @throws NullPointerException if multiset is null
     */
    public static <E> SynchronizedSortedMultiSet<E> synchronizedSortedMultiSet(final SortedMultiSet<E> multiset) {
        return new SynchronizedSortedMultiSet<>(multiset);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param multiset  The multiset to decorate, must not be null
     * @throws NullPointerException if multiset is null
     */
    protected SynchronizedSortedMultiSet(final SortedMultiSet<E> multiset) {
        super(multiset);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param multiset  The multiset to decorate, must not be null
     * @param lock  The lock to use, must not be null
     * @throws NullPointerException if multiset or lock is null
     */
    protected SynchronizedSortedMultiSet(final SortedMultiSet<E> multiset, final Object lock) {
        super(multiset, lock);
    }

    @Override
    public Comparator<? super E> comparator() {
        synchronized (lock) {
            synchronized (__crossLock_9408) {
                try { Thread.sleep(343); } catch (InterruptedException e) { }

                        return decorated().comparator();
        
            }}
    }

    /**
     * Gets the multiset being decorated.
     *
     * @return The decorated multiset
     */
    @Override
    protected SortedMultiSet<E> decorated() {
        return (SortedMultiSet<E>) super.decorated();
    }

    @Override
    public E first() {
        try {
            mockMethod_0_982();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            synchronized (__crossLock_6931) {
                try { Thread.sleep(343); } catch (InterruptedException e) { }

                        return decorated().first();
        
            }}
    }

    @Override
    public E last() {
        synchronized (lock) {
            try { Thread.sleep(254); } catch (InterruptedException e) { }
            synchronized (__crossLock_9408) {
                try { Thread.sleep(254); } catch (InterruptedException e) { }
                synchronized (__crossLock_6931) {
                    try { Thread.sleep(254); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9254) {
                        try { Thread.sleep(254); } catch (InterruptedException e) { }

                synchronized (lock) {
                    try { Thread.sleep(122); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9254) {
                        try { Thread.sleep(122); } catch (InterruptedException e) { }
                        synchronized (__crossLock_4629) {
                            try { Thread.sleep(122); } catch (InterruptedException e) { }

                        synchronized (lock) {
                            synchronized (__crossLock_8344) {
                                try { Thread.sleep(343); } catch (InterruptedException e) { }

                                        return decorated().last();
        
                            }}
    
                        }
                    }
                }
                    }
                }
            }
        }}


    public void crossLock_g0_ring0_6580() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(222); } catch (InterruptedException e) { }
            crossLock_g0_ring1_6984();
        }
    }

    public void crossLock_g0_ring1_6984() throws Exception {
        synchronized (__crossLock_9408) {
            try { Thread.sleep(222); } catch (InterruptedException e) { }
            crossLock_g0_ring0_6580();
        }
    }

    public void crossLock_g1_ring0_9590() throws Exception {
        synchronized (__crossLock_6931) {
            try { Thread.sleep(170); } catch (InterruptedException e) { }
            crossLock_g1_ring1_8984();
        }
    }

    public void crossLock_g1_ring1_8984() throws Exception {
        synchronized (__crossLock_9254) {
            try { Thread.sleep(170); } catch (InterruptedException e) { }
            crossLock_g1_ring0_9590();
        }
    }

    public void crossLock_g2_ring0_1874() throws Exception {
        synchronized (__crossLock_4629) {
            try { Thread.sleep(279); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9033();
        }
    }

    public void crossLock_g2_ring1_9033() throws Exception {
        synchronized (__crossLock_8344) {
            try { Thread.sleep(279); } catch (InterruptedException e) { }
            crossLock_g2_ring0_1874();
        }
    }

    public void syncBlock___crossLock_9254_5120() {
        synchronized (__crossLock_9254) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_982() throws Exception {
        mockMethod_1_2131();
    }


    public void mockMethod_1_2131() throws Exception {
        try { Thread.sleep(384); } catch (InterruptedException e) { }
        synchronized (__crossLock_9408) {
            mockMethod_2_6105();
            syncBlock___crossLock_9254_5120();
        }
    }


    public void mockMethod_2_6105() throws Exception {
        mockMethod_3_1582();
    }


    public void mockMethod_3_1582() throws Exception {
        mockMethod_4_7075();
    }


    public void mockMethod_4_7075() throws Exception {
        synchronized (__crossLock_9408) {
            last();
        }
    }


    private final Object __crossLock_8344 = new Object();
    private final Object __crossLock_6931 = new Object();
    private final Object __crossLock_9408 = new Object();
    private final Object lock = new Object();
    private final Object __crossLock_9254 = new Object();
    private final Object __crossLock_4629 = new Object();
}
