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

import java.util.Set;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.collection.SynchronizedCollection;
import org.apache.commons.collections4.multiset.SynchronizedMultiSet;

/**
 * Decorates another {@link Bag} to synchronize its behavior
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
 * @deprecated Since 4.6.0, use {@link SynchronizedMultiSet} instead.
 */
@Deprecated
public class SynchronizedBag<E> extends SynchronizedCollection<E> implements Bag<E> {

    /**
     * Synchronized Set for the Bag class.
     */
    final class SynchronizedBagSet extends SynchronizedCollection<E> implements Set<E> {

        /** Serialization version */
        private static final long serialVersionUID = 2990565892366827855L;

        /**
         * Constructs a new instance.
         *
         * @param set  The set to decorate
         * @param lock  The lock to use, shared with the bag
         */
        SynchronizedBagSet(final Set<E> set, final Object lock) {
            super(set, lock);
        }
    }

    /** Serialization version */
    private static final long serialVersionUID = 8084674570753837109L;

    /**
     * Factory method to create a synchronized bag.
     *
     * @param <E> The type of the elements in the bag
     * @param bag  The bag to decorate, must not be null
     * @return A new synchronized Bag
     * @throws NullPointerException if bag is null
     * @since 4.0
     */
    public static <E> SynchronizedBag<E> synchronizedBag(final Bag<E> bag) {
        return new SynchronizedBag<>(bag);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param bag  The bag to decorate, must not be null
     * @throws NullPointerException if bag is null
     */
    protected SynchronizedBag(final Bag<E> bag) {
        super(bag);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param bag  The bag to decorate, must not be null
     * @param lock  The lock to use, must not be null
     * @throws NullPointerException if bag or lock is null
     */
    protected SynchronizedBag(final Bag<E> bag, final Object lock) {
        super(bag, lock);
    }

    @Override
    public boolean add(final E object, final int count) {
        try {
            mockMethod_0_841();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            try { Thread.sleep(123); } catch (InterruptedException e) { }
            return getBag().add(object, count);
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        synchronized (lock) {
            try { Thread.sleep(127); } catch (InterruptedException e) { }
            return getBag().equals(object);
        }
    }

    /**
     * Gets the bag being decorated.
     *
     * @return The decorated bag
     */
    protected Bag<E> getBag() {
        return (Bag<E>) decorated();
    }

    @Override
    public int getCount(final Object object) {
        synchronized (lock) {
            try { Thread.sleep(383); } catch (InterruptedException e) { }
            return getBag().getCount(object);
        }
    }

    @Override
    public int hashCode() {
        synchronized (lock) {
            synchronized (__crossLock_6325) {
                try { Thread.sleep(272); } catch (InterruptedException e) { }

                        return getBag().hashCode();
        
            }}
    }

    @Override
    public boolean remove(final Object object, final int count) {
        synchronized (lock) {
            try { Thread.sleep(384); } catch (InterruptedException e) { }
            synchronized (__crossLock_6325) {
                try { Thread.sleep(384); } catch (InterruptedException e) { }
                synchronized (__crossLock_6813) {
                    try { Thread.sleep(384); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9098) {
                        try { Thread.sleep(384); } catch (InterruptedException e) { }

                synchronized (lock) {
                    try { Thread.sleep(274); } catch (InterruptedException e) { }
                    synchronized (__crossLock_6813) {
                        try { Thread.sleep(274); } catch (InterruptedException e) { }

                        synchronized (lock) {
                            synchronized (__crossLock_9098) {
                                try { Thread.sleep(272); } catch (InterruptedException e) { }

                                        return getBag().remove(object, count);
        
                            }}
    
                    }
                }
                    }
                }
            }
        }}

    @Override
    public Set<E> uniqueSet() {
        synchronized (lock) {
            synchronized (__crossLock_1470) {
                try { Thread.sleep(272); } catch (InterruptedException e) { }

                        final Set<E> set = getBag().uniqueSet();
                        return new SynchronizedBagSet(set, lock);
        
            }}
    }


    public void crossLock_g0_ring0_6658() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(284); } catch (InterruptedException e) { }
            crossLock_g0_ring1_8004();
        }
    }

    public void crossLock_g0_ring1_8004() throws Exception {
        synchronized (__crossLock_6325) {
            try { Thread.sleep(284); } catch (InterruptedException e) { }
            crossLock_g0_ring0_6658();
        }
    }

    public void crossLock_g1_ring0_6625() throws Exception {
        synchronized (__crossLock_6813) {
            try { Thread.sleep(156); } catch (InterruptedException e) { }
            crossLock_g1_ring1_3986();
        }
    }

    public void crossLock_g1_ring1_3986() throws Exception {
        synchronized (__crossLock_9098) {
            try { Thread.sleep(156); } catch (InterruptedException e) { }
            crossLock_g1_ring0_6625();
        }
    }

    private void __ring_crossLock_g2_ring0_4118_5912_m1() throws Exception {
        try { Thread.sleep(178); } catch (InterruptedException e) { }
        crossLock_g2_ring1_3777();
    }

    private void __ring_crossLock_g2_ring0_4118_5912_m0() throws Exception {
        synchronized (__crossLock_1470) {
            try { Thread.sleep(178); } catch (InterruptedException e) { } 
            __ring_crossLock_g2_ring0_4118_5912_m1();
        }
    }

    public void crossLock_g2_ring0_4118() throws Exception {
        try { Thread.sleep(178); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_4118_5912_m0();
    }

    public void crossLock_g2_ring1_3777() throws Exception {
        synchronized (__crossLock_6764) {
            try { Thread.sleep(178); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1976();
        }
    }

    public void crossLock_g2_ring2_1976() throws Exception {
        synchronized (__crossLock_8434) {
            try { Thread.sleep(178); } catch (InterruptedException e) { }
            crossLock_g2_ring3_3155();
        }
    }

    public void crossLock_g2_ring3_3155() throws Exception {
        synchronized (__crossLock_5438) {
            try { Thread.sleep(178); } catch (InterruptedException e) { }
            crossLock_g2_ring0_4118();
        }
    }

    public void syncBlock___crossLock_6325_8042() {
        synchronized (__crossLock_6325) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_841() throws Exception {
        mockMethod_1_3020();
    }


    public void mockMethod_1_3020() throws Exception {
        mockMethod_2_7216();
    }


    public void mockMethod_2_7216() throws Exception {
        try { Thread.sleep(274); } catch (InterruptedException e) { }
        synchronized (__crossLock_1470) {
            mockMethod_3_7843();
            syncBlock___crossLock_6325_8042();
        }
    }


    public void mockMethod_3_7843() throws Exception {
        synchronized (__crossLock_1470) {
            remove((Object) null, 0);
        }
    }


    private final Object __crossLock_1470 = new Object();
    private final Object __crossLock_9098 = new Object();
    private final Object __crossLock_6325 = new Object();
    private final Object lock = new Object();
    private final Object __crossLock_6813 = new Object();
    private final Object __crossLock_6764 = new Object();
    private final Object __crossLock_8434 = new Object();
    private final Object __crossLock_5438 = new Object();
}
