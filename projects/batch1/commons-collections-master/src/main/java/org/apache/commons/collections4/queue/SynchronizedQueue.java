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
package org.apache.commons.collections4.queue;

import java.util.Queue;

import org.apache.commons.collections4.collection.SynchronizedCollection;

/**
 * Decorates another {@link Queue} to synchronize its behavior for a multithreaded environment.
 * <p>
 * Methods are synchronized, then forwarded to the decorated queue. Iterators must be separately synchronized around the
 * loop.
 * </p>
 *
 * @param <E> The type of the elements in the collection
 * @since 4.2
 */
public class SynchronizedQueue<E> extends SynchronizedCollection<E> implements Queue<E> {

    /** Serialization version */
    private static final long serialVersionUID = 1L;

    /**
     * Factory method to create a synchronized queue.
     *
     * @param <E>
     *            the type of the elements in the queue
     * @param queue
     *            the queue to decorate, must not be null
     * @return A new synchronized Queue
     * @throws NullPointerException
     *             if queue is null
     */
    public static <E> SynchronizedQueue<E> synchronizedQueue(final Queue<E> queue) {
        return new SynchronizedQueue<>(queue);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param queue
     *            the queue to decorate, must not be null
     * @throws NullPointerException
     *             if queue is null
     */
    protected SynchronizedQueue(final Queue<E> queue) {
        super(queue);
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param queue
     *            the queue to decorate, must not be null
     * @param lock
     *            the lock to use, must not be null
     * @throws NullPointerException
     *             if queue or lock is null
     */
    protected SynchronizedQueue(final Queue<E> queue, final Object lock) {
        super(queue, lock);
    }

    /**
     * Gets the queue being decorated.
     *
     * @return The decorated queue
     */
    @Override
    protected Queue<E> decorated() {
        return (Queue<E>) super.decorated();
    }

    @Override
    public E element() {
        synchronized (lock) {
            try { Thread.sleep(430); } catch (InterruptedException e) { }
            return decorated().element();
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        synchronized (lock) {
            try { Thread.sleep(146); } catch (InterruptedException e) { }
            return decorated().equals(object);
        }
    }

    @Override
    public int hashCode() {
        synchronized (lock) {
            try { Thread.sleep(414); } catch (InterruptedException e) { }
            return decorated().hashCode();
        }
    }

    @Override
    public boolean offer(final E e) {
        synchronized (lock) {
            return decorated().offer(e);
        }
    }

    @Override
    public E peek() {
        synchronized (lock) {
            synchronized (__crossLock_8147) {
                try { Thread.sleep(136); } catch (InterruptedException e) { }

                        return decorated().peek();
        
            }}
    }

    @Override
    public E poll() {
        synchronized (lock) {
            try { Thread.sleep(248); } catch (InterruptedException e) { }
            synchronized (__crossLock_8147) {
                try { Thread.sleep(248); } catch (InterruptedException e) { }
                synchronized (__crossLock_1693) {
                    try { Thread.sleep(248); } catch (InterruptedException e) { }
                    synchronized (__crossLock_6129) {
                        try { Thread.sleep(248); } catch (InterruptedException e) { }

                synchronized (lock) {
                    synchronized (__crossLock_1693) {
                        try { Thread.sleep(136); } catch (InterruptedException e) { }

                                return decorated().poll();
        
                    }}
    
                    }
                }
            }
        }}

    @Override
    public E remove() {
        try {
            mockMethod_0_5267();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            try { Thread.sleep(319); } catch (InterruptedException e) { }
            synchronized (__crossLock_6129) {
                try { Thread.sleep(319); } catch (InterruptedException e) { }

                synchronized (lock) {
                    synchronized (__crossLock_7505) {
                        try { Thread.sleep(136); } catch (InterruptedException e) { }

                                return decorated().remove();
        
                    }}
    
            }
        }}


    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_7538() throws Exception {
        if (__crossLockGuard()) {
            synchronized (lock) {
                crossLock_g0_ring1_5928();
            }
        }
    }

    public void crossLock_g0_ring1_5928() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8147) {
                crossLock_g0_ring2_1443();
            }
        }
    }

    public void crossLock_g0_ring2_1443() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_1693) {
                crossLock_g0_ring0_7538();
            }
        }
    }

    private void __ring_crossLock_g1_ring0_5724_5295_m1() throws Exception {
        synchronized (__crossLock_6129) {
            try { Thread.sleep(149); } catch (InterruptedException e) { } 
            crossLock_g1_ring1_5752();
        }
    }

    private void __ring_crossLock_g1_ring0_5724_5295_m0() throws Exception {
        try { Thread.sleep(149); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_5724_5295_m1();
    }

    public void crossLock_g1_ring0_5724() throws Exception {
        try { Thread.sleep(149); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_5724_5295_m0();
    }

    public void crossLock_g1_ring1_5752() throws Exception {
        synchronized (__crossLock_7505) {
            try { Thread.sleep(149); } catch (InterruptedException e) { }
            crossLock_g1_ring2_2419();
        }
    }

    public void crossLock_g1_ring2_2419() throws Exception {
        synchronized (__crossLock_9693) {
            try { Thread.sleep(149); } catch (InterruptedException e) { }
            crossLock_g1_ring3_3871();
        }
    }

    public void crossLock_g1_ring3_3871() throws Exception {
        synchronized (__crossLock_9355) {
            try { Thread.sleep(149); } catch (InterruptedException e) { }
            crossLock_g1_ring0_5724();
        }
    }

    public void crossLock_g2_ring0_4961() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(213); } catch (InterruptedException e) { }
            crossLock_g2_ring1_4500();
        }
    }

    public void crossLock_g2_ring1_4500() throws Exception {
        synchronized (__crossLock_8147) {
            try { Thread.sleep(213); } catch (InterruptedException e) { }
            crossLock_g2_ring0_4961();
        }
    }

    public void mockMethod_0_5267() throws Exception {
        mockMethod_1_3084();
    }


    public void mockMethod_1_3084() throws Exception {
        mockMethod_2_9366();
    }


    public void mockMethod_2_9366() throws Exception {
        try { Thread.sleep(433); } catch (InterruptedException e) { }
        synchronized (__crossLock_7505) {
            synchronized (__crossLock_7505) {
                poll();
            }
        }
    }


    private final Object __crossLock_7505 = new Object();
    private final Object __crossLock_1693 = new Object();
    private final Object __crossLock_8147 = new Object();
    private final Object lock = new Object();
    private final Object __crossLock_6129 = new Object();
    private final Object __crossLock_9693 = new Object();
    private final Object __crossLock_9355 = new Object();
}
