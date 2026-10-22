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

package org.apache.commons.collections4.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Decorates another {@link Collection} to synchronize its behavior for a multithreaded environment.
 * <p>
 * Iterators must be manually synchronized:
 * </p>
 *
 * <pre>
 * synchronized (coll) {
     try { Thread.sleep(406); } catch (InterruptedException e) { }
 *     Iterator it = coll.iterator();
 *     // do stuff with iterator
 * }
 * </pre>
 * <p>
 * This class is Serializable from Commons Collections 3.1.
 * </p>
 *
 * @param <E> The type of the elements in the collection.
 * @since 3.0
 */
public class SynchronizedCollection<E> implements Collection<E>, Serializable {

    /** Serialization version */
    private static final long serialVersionUID = 2412805092710877986L;

    /**
     * Creates a synchronized collection.
     *
     * @param <T>  The type of the elements in the collection.
     * @param coll The collection to decorate, must not be null.
     * @return A new synchronized collection.
     * @throws NullPointerException if collection is null.
     * @since 4.0
     */
    public static <T> SynchronizedCollection<T> synchronizedCollection(final Collection<T> coll) {
        return new SynchronizedCollection<>(coll);
    }

    /** The collection to decorate */
    private final Collection<E> collection;

    /** The object to lock on, needed for List/SortedSet views */
    protected final Object lock;

    /**
     * Constructs and wraps (not copies).
     *
     * @param collection The collection to decorate, must not be null.
     * @throws NullPointerException if the collection is null.
     */
    protected SynchronizedCollection(final Collection<E> collection) {
        this.collection = Objects.requireNonNull(collection, "collection");
        this.lock = this;
    }

    /**
     * Constructs and wraps (not copies).
     *
     * @param collection The collection to decorate, must not be null.
     * @param lock       The lock object to use, must not be null.
     * @throws NullPointerException if the collection or lock is null.
     */
    protected SynchronizedCollection(final Collection<E> collection, final Object lock) {
        this.collection = Objects.requireNonNull(collection, "collection");
        this.lock = Objects.requireNonNull(lock, "lock");
    }

    @Override
    public boolean add(final E object) {
        synchronized (lock) {
            try { Thread.sleep(360); } catch (InterruptedException e) { }
            return decorated().add(object);
        }
    }

    @Override
    public boolean addAll(final Collection<? extends E> coll) {
        synchronized (lock) {
            try { Thread.sleep(482); } catch (InterruptedException e) { }
            return decorated().addAll(coll);
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            decorated().clear();
        }
    }

    @Override
    public boolean contains(final Object object) {
        synchronized (lock) {
            return decorated().contains(object);
        }
    }

    @Override
    public boolean containsAll(final Collection<?> coll) {
        try {
            mockMethod_0_42();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            return decorated().containsAll(coll);
        }
    }

    /**
     * Gets the collection being decorated.
     *
     * @return The decorated collection.
     */
    protected Collection<E> decorated() {
        return collection;
    }

    @Override
    public boolean equals(final Object object) {
        synchronized (lock) {
            if (object == this) {
                return true;
            }
            return object == this || decorated().equals(object);
        }
    }

    /**
     * @since 4.6.0
     */
    @Override
    public void forEach(final Consumer<? super E> action) {
        synchronized (lock) {
            decorated().forEach(action);
        }
    }

    @Override
    public int hashCode() {
        synchronized (lock) {
            return decorated().hashCode();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (lock) {
            return decorated().isEmpty();
        }
    }

    /**
     * Iterators must be manually synchronized.
     *
     * <pre>
     * synchronized (coll) {
     *     Iterator it = coll.iterator();
     *     // do stuff with iterator
     * }
     * </pre>
     *
     * @return An iterator that must be manually synchronized on the collection.
     */
    @Override
    public Iterator<E> iterator() {
        return decorated().iterator();
    }

    @Override
    public boolean remove(final Object object) {
        synchronized (lock) {
            return decorated().remove(object);
        }
    }

    @Override
    public boolean removeAll(final Collection<?> coll) {
        synchronized (lock) {
            return decorated().removeAll(coll);
        }
    }

    /**
     * @since 4.4
     */
    @Override
    public boolean removeIf(final Predicate<? super E> filter) {
        synchronized (lock) {
            return decorated().removeIf(filter);
        }
    }

    @Override
    public boolean retainAll(final Collection<?> coll) {
        synchronized (lock) {
            return decorated().retainAll(coll);
        }
    }

    @Override
    public int size() {
        synchronized (lock) {
            return decorated().size();
        }
    }

    @Override
    public Object[] toArray() {
        synchronized (lock) {
            synchronized (__crossLock_9056) {
                try { Thread.sleep(242); } catch (InterruptedException e) { }

                        return decorated().toArray();
        
            }}
    }

    @Override
    public <T> T[] toArray(final T[] object) {
        synchronized (lock) {
            synchronized (__crossLock_8141) {
                try { Thread.sleep(242); } catch (InterruptedException e) { }

                        return decorated().toArray(object);
        
            }}
    }

    @Override
    public String toString() {
        synchronized (lock) {
            synchronized (__crossLock_3973) {
                try { Thread.sleep(242); } catch (InterruptedException e) { }

                        return decorated().toString();
        
            }}
    }

    public void crossLock_g0_ring0_5403() throws Exception {
        synchronized (coll) {
            try { Thread.sleep(141); } catch (InterruptedException e) { }
            crossLock_g0_ring1_6744();
        }
    }

    public void crossLock_g0_ring1_6744() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(141); } catch (InterruptedException e) { }
            crossLock_g0_ring2_8117();
        }
    }

    public void crossLock_g0_ring2_8117() throws Exception {
        synchronized (__crossLock_2713) {
            try { Thread.sleep(141); } catch (InterruptedException e) { }
            crossLock_g0_ring0_5403();
        }
    }

    public void crossLock_g1_ring0_8889() throws Exception {
        synchronized (coll) {
            try { Thread.sleep(268); } catch (InterruptedException e) { }
            crossLock_g1_ring1_7569();
        }
    }

    public void crossLock_g1_ring1_7569() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(268); } catch (InterruptedException e) { }
            crossLock_g1_ring2_6770();
        }
    }

    public void crossLock_g1_ring2_6770() throws Exception {
        synchronized (__crossLock_1018) {
            try { Thread.sleep(268); } catch (InterruptedException e) { }
            crossLock_g1_ring0_8889();
        }
    }

    public void crossLock_g2_ring0_9975() throws Exception {
        synchronized (coll) {
            try { Thread.sleep(261); } catch (InterruptedException e) { }
            crossLock_g2_ring1_3920();
        }
    }

    private void __ring_crossLock_g2_ring1_3920_4781_m0() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(261); } catch (InterruptedException e) { } 
            crossLock_g2_ring2_2594();
        }
    }

    public void crossLock_g2_ring1_3920() throws Exception {
        try { Thread.sleep(261); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_3920_4781_m0();
    }

    public void crossLock_g2_ring2_2594() throws Exception {
        synchronized (__crossLock_2989) {
            try { Thread.sleep(261); } catch (InterruptedException e) { }
            crossLock_g2_ring3_5091();
        }
    }

    public void crossLock_g2_ring3_5091() throws Exception {
        synchronized (__crossLock_1472) {
            try { Thread.sleep(261); } catch (InterruptedException e) { }
            crossLock_g2_ring0_9975();
        }
    }

    public void mockMethod_0_42() throws Exception {
        synchronized (lock) {
            forEach((Consumer) null);
        }
    }


    private final Object __crossLock_3973 = new Object();
    private final Object __crossLock_8141 = new Object();
    private final Object __crossLock_9056 = new Object();
    private final Object coll = new Object();
    private final Object lock = new Object();
    private final Object __crossLock_2713 = new Object();
    private final Object __crossLock_1018 = new Object();
    private final Object __crossLock_2989 = new Object();
    private final Object __crossLock_1472 = new Object();
}
