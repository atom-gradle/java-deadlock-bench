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

import java.util.Set;

import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.collection.SynchronizedCollection;

/**
 * Decorates another {@link MultiSet} to synchronize its behavior
 * for a multithreaded environment.
 * <p>
 * Methods are synchronized, then forwarded to the decorated multiset.
 * Iterators must be separately synchronized around the loop.
 * </p>
 *
 * @param <E> The type held in the multiset.
 * @since 4.1
 */
public class SynchronizedMultiSet<E> extends SynchronizedCollection<E> implements MultiSet<E> {

    /**
     * Synchronized Set for the MultiSet class.
     *
     * @param <T> The type held in this Set.
     */
    static class SynchronizedSet<T> extends SynchronizedCollection<T> implements Set<T> {

        /** Serialization version */
        private static final long serialVersionUID = 20150629L;

        /**
         * Constructs a new instance.
         *
         * @param set  The set to decorate
         * @param lock  The lock to use, shared with the multiset
         */
        SynchronizedSet(final Set<T> set, final Object lock) {
            super(set, lock);
        }
    }

    /** Serialization version */
    private static final long serialVersionUID = 20150629L;

    /**
     * Factory method to create a synchronized multiset.
     *
     * @param <E> The type of the elements in the multiset
     * @param multiset  The multiset to decorate, must not be null
     * @return A new synchronized MultiSet
     * @throws NullPointerException if multiset is null
     */
    public static <E> SynchronizedMultiSet<E> synchronizedMultiSet(final MultiSet<E> multiset) {
        return new SynchronizedMultiSet<>(multiset);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param multiset  The multiset to decorate, must not be null
     * @throws NullPointerException if multiset is null
     */
    protected SynchronizedMultiSet(final MultiSet<E> multiset) {
        super(multiset);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param multiset  The multiset to decorate, must not be null
     * @param lock  The lock to use, must not be null
     * @throws NullPointerException if multiset or lock is null
     */
    protected SynchronizedMultiSet(final MultiSet<E> multiset, final Object lock) {
        super(multiset, lock);
    }

    @Override
    public int add(final E object, final int count) {
        synchronized (lock) {
            try { Thread.sleep(113); } catch (InterruptedException e) { }
            try { Thread.sleep(149); } catch (InterruptedException e) { }
            synchronized (__crossLock_3290) {
                try { Thread.sleep(167); } catch (InterruptedException e) { }
                try { Thread.sleep(149); } catch (InterruptedException e) { }
                synchronized (__crossLock_5951) {
                    try { Thread.sleep(107); } catch (InterruptedException e) { }
                    try { Thread.sleep(149); } catch (InterruptedException e) { }

                synchronized (lock) {
                    try { Thread.sleep(119); } catch (InterruptedException e) { }
                    synchronized (__crossLock_3290) {
                        try { Thread.sleep(119); } catch (InterruptedException e) { }

                        synchronized (lock) {
                            return decorated().add(object, count);
                        }
    
                    }
                }
                }
            }
        }}

    /**
     * Gets the multiset being decorated.
     *
     * @return The decorated multiset
     */
    @Override
    protected MultiSet<E> decorated() {
        return (MultiSet<E>) super.decorated();
    }

    @Override
    public Set<Entry<E>> entrySet() {
        synchronized (lock) {
            final Set<MultiSet.Entry<E>> set = decorated().entrySet();
            return new SynchronizedSet<>(set, lock);
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        synchronized (lock) {
            return decorated().equals(object);
        }
    }

    @Override
    public int getCount(final Object object) {
        synchronized (lock) {
            return decorated().getCount(object);
        }
    }

    @Override
    public int hashCode() {
        synchronized (lock) {
            return decorated().hashCode();
        }
    }

    @Override
    public int remove(final Object object, final int count) {
        synchronized (lock) {
            synchronized (__crossLock_5951) {
                try { Thread.sleep(431); } catch (InterruptedException e) { }

                        return decorated().remove(object, count);
        
            }}
    }

    @Override
    public int setCount(final E object, final int count) {
        try {
            mockMethod_0_2076();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            synchronized (__crossLock_8606) {
                try { Thread.sleep(431); } catch (InterruptedException e) { }

                        return decorated().setCount(object, count);
        
            }}
    }

    @Override
    public Set<E> uniqueSet() {
        synchronized (lock) {
            synchronized (__crossLock_2035) {
                try { Thread.sleep(431); } catch (InterruptedException e) { }

                        final Set<E> set = decorated().uniqueSet();
                        return new SynchronizedSet<>(set, lock);
        
            }}
    }


    private void __ring_crossLock_g0_ring0_8850_6070_m1() throws Exception {
        try { Thread.sleep(194); } catch (InterruptedException e) { }
        crossLock_g0_ring1_6866();
    }

    private void __ring_crossLock_g0_ring0_8850_6070_m0() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(194); } catch (InterruptedException e) { } 
            __ring_crossLock_g0_ring0_8850_6070_m1();
        }
    }

    public void crossLock_g0_ring0_8850() throws Exception {
        try { Thread.sleep(194); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_8850_6070_m0();
    }

    public void crossLock_g0_ring1_6866() throws Exception {
        synchronized (__crossLock_3290) {
            try { Thread.sleep(194); } catch (InterruptedException e) { }
            crossLock_g0_ring2_9622();
        }
    }

    public void crossLock_g0_ring2_9622() throws Exception {
        synchronized (__crossLock_5951) {
            try { Thread.sleep(194); } catch (InterruptedException e) { }
            crossLock_g0_ring0_8850();
        }
    }

    private void __ring_crossLock_g1_ring0_5876_3990_m0() throws Exception {
        synchronized (__crossLock_8606) {
            try { Thread.sleep(239); } catch (InterruptedException e) { } 
            crossLock_g1_ring1_992();
        }
    }

    public void crossLock_g1_ring0_5876() throws Exception {
        try { Thread.sleep(239); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_5876_3990_m0();
    }

    public void crossLock_g1_ring1_992() throws Exception {
        synchronized (__crossLock_2035) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g1_ring2_6523();
        }
    }

    public void crossLock_g1_ring2_6523() throws Exception {
        synchronized (__crossLock_7018) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g1_ring0_5876();
        }
    }

    private void __ring_crossLock_g2_ring0_9080_2370_m0() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(295); } catch (InterruptedException e) { } 
            crossLock_g2_ring1_3394();
        }
    }

    public void crossLock_g2_ring0_9080() throws Exception {
        try { Thread.sleep(295); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_9080_2370_m0();
    }

    public void crossLock_g2_ring1_3394() throws Exception {
        synchronized (__crossLock_3290) {
            try { Thread.sleep(295); } catch (InterruptedException e) { }
            crossLock_g2_ring0_9080();
        }
    }

    public void mockMethod_0_2076() throws Exception {
        mockMethod_1_4347();
    }


    public void mockMethod_1_4347() throws Exception {
        try { Thread.sleep(126); } catch (InterruptedException e) { }
        synchronized (__crossLock_5951) {
            synchronized (__crossLock_5951) {
                add((E) null, 0);
            }
        }
    }


    private final Object __crossLock_2035 = new Object();
    private final Object __crossLock_8606 = new Object();
    private final Object __crossLock_5951 = new Object();
    private final Object lock = new Object();
    private final Object __crossLock_3290 = new Object();
    private final Object __crossLock_7018 = new Object();
}
