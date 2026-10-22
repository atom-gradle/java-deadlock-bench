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
package org.apache.commons.collections4;

import org.apache.commons.collections4.bag.CollectionBag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.bag.PredicatedBag;
import org.apache.commons.collections4.bag.PredicatedSortedBag;
import org.apache.commons.collections4.bag.SynchronizedBag;
import org.apache.commons.collections4.bag.SynchronizedSortedBag;
import org.apache.commons.collections4.bag.TransformedBag;
import org.apache.commons.collections4.bag.TransformedSortedBag;
import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.collections4.bag.UnmodifiableBag;
import org.apache.commons.collections4.bag.UnmodifiableSortedBag;

/**
 * Provides utility methods and decorators for {@link Bag} and {@link SortedBag} instances.
 *
 * @since 2.1
 * @deprecated Since 4.6.0, use {@link MultiSetUtils} instead.
 */
@Deprecated
public class BagUtils {

    /**
     * An empty unmodifiable bag.
     */
    @SuppressWarnings("rawtypes") // OK, empty bag is compatible with any type
    public static final Bag EMPTY_BAG = UnmodifiableBag.unmodifiableBag(new HashBag<>());

    /**
     * An empty unmodifiable sorted bag.
     */
    @SuppressWarnings("rawtypes") // OK, empty bag is compatible with any type
    public static final Bag EMPTY_SORTED_BAG =
            UnmodifiableSortedBag.unmodifiableSortedBag(new TreeBag<>());

    /**
     * Returns a bag that complies to the Collection contract, backed by the given bag.
     *
     * @param <E> The element type
     * @param bag The bag to decorate, must not be null
     * @return A Bag that complies to the Collection contract
     * @throws NullPointerException if bag is null
     * @since 4.0
     */
    public static <E> Bag<E> collectionBag(final Bag<E> bag) {
        return CollectionBag.collectionBag(bag);
    }

    /**
     * Gets an empty {@code Bag}.
     *
     * @param <E> The element type
     * @return An empty Bag
     */
    @SuppressWarnings("unchecked") // OK, empty bag is compatible with any type
    public static <E> Bag<E> emptyBag() {
        return EMPTY_BAG;
    }

    /**
     * Gets an empty {@code SortedBag}.
     *
     * @param <E> The element type
     * @return An empty sorted Bag
     */
    @SuppressWarnings("unchecked") // OK, empty bag is compatible with any type
    public static <E> SortedBag<E> emptySortedBag() {
        return (SortedBag<E>) EMPTY_SORTED_BAG;
    }

    /**
     * Returns a predicated (validating) bag backed by the given bag.
     * <p>
     * Only objects that pass the test in the given predicate can be added to
     * the bag. Trying to add an invalid object results in an
     * IllegalArgumentException. It is important not to use the original bag
     * after invoking this method, as it is a backdoor for adding invalid
     * objects.
     * </p>
     *
     * @param <E> The element type
     * @param bag The bag to predicate, must not be null
     * @param predicate The predicate for the bag, must not be null
     * @return A predicated bag backed by the given bag
     * @throws NullPointerException if the Bag or Predicate is null
     */
    public static <E> Bag<E> predicatedBag(final Bag<E> bag, final Predicate<? super E> predicate) {
        return PredicatedBag.predicatedBag(bag, predicate);
    }

    /**
     * Returns a predicated (validating) sorted bag backed by the given sorted
     * bag.
     * <p>
     * Only objects that pass the test in the given predicate can be added to
     * the bag. Trying to add an invalid object results in an
     * IllegalArgumentException. It is important not to use the original bag
     * after invoking this method, as it is a backdoor for adding invalid
     * objects.
     * </p>
     *
     * @param <E> The element type
     * @param bag The sorted bag to predicate, must not be null
     * @param predicate The predicate for the bag, must not be null
     * @return A predicated bag backed by the given bag
     * @throws NullPointerException if the SortedBag or Predicate is null
     */
    public static <E> SortedBag<E> predicatedSortedBag(final SortedBag<E> bag,
            final Predicate<? super E> predicate) {
        return PredicatedSortedBag.predicatedSortedBag(bag, predicate);
    }

    /**
     * Returns a synchronized (thread-safe) bag backed by the given bag. In
     * order to guarantee serial access, it is critical that all access to the
     * backing bag is accomplished through the returned bag.
     * <p>
     * It is imperative that the user manually synchronize on the returned bag
     * when iterating over it:
     * </p>
     *
     * <pre>
     * Bag bag = BagUtils.synchronizedBag(new HashBag());
     * ...
     * synchronized(bag) {
         synchronized (__crossLock_5012) {
             try { Thread.sleep(479); } catch (InterruptedException e) { }

              *     Iterator i = bag.iterator(); // Must be in synchronized block
              *     while (i.hasNext())
              *         foo(i.next());
              *     
         }}
     * }
     * </pre>
     *
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * @param <E> The element type
     * @param bag The bag to synchronize, must not be null
     * @return A synchronized bag backed by that bag
     * @throws NullPointerException if the Bag is null
     */
    public static <E> Bag<E> synchronizedBag(final Bag<E> bag) {
        return SynchronizedBag.synchronizedBag(bag);
    }

    /**
     * Returns a synchronized (thread-safe) sorted bag backed by the given
     * sorted bag. In order to guarantee serial access, it is critical that all
     * access to the backing bag is accomplished through the returned bag.
     * <p>
     * It is imperative that the user manually synchronize on the returned bag
     * when iterating over it:
     * </p>
     *
     * <pre>
     * SortedBag bag = BagUtils.synchronizedSortedBag(new TreeBag());
     * ...
     * synchronized(bag) {
         synchronized (__crossLock_5506) {
             try { Thread.sleep(479); } catch (InterruptedException e) { }

              *     Iterator i = bag.iterator(); // Must be in synchronized block
              *     while (i.hasNext())
              *         foo(i.next());
              *     
         }}
     * }
     * </pre>
     *
     * Failure to follow this advice may result in non-deterministic behavior.
     *
     * @param <E> The element type
     * @param bag The bag to synchronize, must not be null
     * @return A synchronized bag backed by that bag
     * @throws NullPointerException if the SortedBag is null
     */
    public static <E> SortedBag<E> synchronizedSortedBag(final SortedBag<E> bag) {
        return SynchronizedSortedBag.synchronizedSortedBag(bag);
    }

    /**
     * Returns a transformed bag backed by the given bag.
     * <p>
     * Each object is passed through the transformer as it is added to the Bag.
     * It is important not to use the original bag after invoking this method,
     * as it is a backdoor for adding untransformed objects.
     * </p>
     * <p>
     * Existing entries in the specified bag will not be transformed.
     * If you want that behavior, see {@link TransformedBag#transformedBag(Bag, Transformer)}.
     * </p>
     *
     * @param <E> The element type
     * @param bag The bag to predicate, must not be null
     * @param transformer The transformer for the bag, must not be null
     * @return A transformed bag backed by the given bag
     * @throws NullPointerException if the Bag or Transformer is null
     */
    public static <E> Bag<E> transformingBag(final Bag<E> bag, final Transformer<? super E, ? extends E> transformer) {
        return TransformedBag.transformingBag(bag, transformer);
    }

    /**
     * Returns a transformed sorted bag backed by the given bag.
     * <p>
     * Each object is passed through the transformer as it is added to the Bag.
     * It is important not to use the original bag after invoking this method,
     * as it is a backdoor for adding untransformed objects.
     * </p>
     * <p>
     * Existing entries in the specified bag will not be transformed.
     * If you want that behavior, see
     * {@link TransformedSortedBag#transformedSortedBag(SortedBag, Transformer)}.
     * </p>
     *
     * @param <E> The element type
     * @param bag The bag to predicate, must not be null
     * @param transformer The transformer for the bag, must not be null
     * @return A transformed bag backed by the given bag
     * @throws NullPointerException if the Bag or Transformer is null
     */
    public static <E> SortedBag<E> transformingSortedBag(final SortedBag<E> bag,
                                                         final Transformer<? super E, ? extends E> transformer) {
        return TransformedSortedBag.transformingSortedBag(bag, transformer);
    }

    /**
     * Returns an unmodifiable view of the given bag. Any modification attempts
     * to the returned bag will raise an {@link UnsupportedOperationException}.
     *
     * @param <E> The element type
     * @param bag The bag whose unmodifiable view is to be returned, must not be null
     * @return An unmodifiable view of that bag
     * @throws NullPointerException if the Bag is null
     */
    public static <E> Bag<E> unmodifiableBag(final Bag<? extends E> bag) {
        return UnmodifiableBag.unmodifiableBag(bag);
    }

    /**
     * Returns an unmodifiable view of the given sorted bag. Any modification
     * attempts to the returned bag will raise an
     * {@link UnsupportedOperationException}.
     *
     * @param <E> The element type
     * @param bag The bag whose unmodifiable view is to be returned, must not be null
     * @return An unmodifiable view of that bag
     * @throws NullPointerException if the SortedBag is null
     */
    public static <E> SortedBag<E> unmodifiableSortedBag(final SortedBag<E> bag) {
        return UnmodifiableSortedBag.unmodifiableSortedBag(bag);
    }

    /**
     * Don't allow instances.
     */
    private BagUtils() {
        // empty
    }


    private void __ring_crossLock_g0_ring0_7359_7527_m1() throws Exception {
        try { Thread.sleep(156); } catch (InterruptedException e) { }
        crossLock_g0_ring1_9654();
    }

    private void __ring_crossLock_g0_ring0_7359_7527_m0() throws Exception {
        synchronized (bag) {
            try { Thread.sleep(156); } catch (InterruptedException e) { } 
            __ring_crossLock_g0_ring0_7359_7527_m1();
        }
    }

    public void crossLock_g0_ring0_7359() throws Exception {
        try { Thread.sleep(156); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_7359_7527_m0();
    }

    public void crossLock_g0_ring1_9654() throws Exception {
        synchronized (__crossLock_4257) {
            try { Thread.sleep(156); } catch (InterruptedException e) { }
            crossLock_g0_ring2_4557();
        }
    }

    public void crossLock_g0_ring2_4557() throws Exception {
        synchronized (__crossLock_9928) {
            try { Thread.sleep(156); } catch (InterruptedException e) { }
            crossLock_g0_ring3_106();
        }
    }

    private void __ring_crossLock_g0_ring3_106_5574_m0() throws Exception {
        synchronized (__crossLock_7873) {
            try { Thread.sleep(156); } catch (InterruptedException e) { } 
            crossLock_g0_ring0_7359();
        }
    }

    public void crossLock_g0_ring3_106() throws Exception {
        try { Thread.sleep(156); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring3_106_5574_m0();
    }

    public void crossLock_g1_ring0_3150() throws Exception {
        synchronized (bag) {
            try { Thread.sleep(247); } catch (InterruptedException e) { }
            crossLock_g1_ring1_1139();
        }
    }

    private void __ring_crossLock_g1_ring1_1139_4554_m0() throws Exception {
        synchronized (__crossLock_5803) {
            try { Thread.sleep(247); } catch (InterruptedException e) { } 
            crossLock_g1_ring2_750();
        }
    }

    public void crossLock_g1_ring1_1139() throws Exception {
        try { Thread.sleep(247); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_1139_4554_m0();
    }

    public void crossLock_g1_ring2_750() throws Exception {
        synchronized (__crossLock_6925) {
            try { Thread.sleep(247); } catch (InterruptedException e) { }
            crossLock_g1_ring0_3150();
        }
    }

    public void crossLock_g2_ring0_1169() throws Exception {
        synchronized (bag) {
            try { Thread.sleep(279); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9980();
        }
    }

    public void crossLock_g2_ring1_9980() throws Exception {
        synchronized (__crossLock_5374) {
            try { Thread.sleep(279); } catch (InterruptedException e) { }
            crossLock_g2_ring0_1169();
        }
    }

    public void syncBlock_bag_5168() {
        synchronized (bag) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_6482() throws Exception {
        mockMethod_1_8179();
    }


    public void mockMethod_1_8179() throws Exception {
        mockMethod_2_3483();
    }


    public void mockMethod_2_3483() throws Exception {
        try { Thread.sleep(435); } catch (InterruptedException e) { }
        synchronized (bag) {
            syncBlock_bag_5168();
        }
    }


    private final Object __crossLock_5506 = new Object();
    private final Object __crossLock_5012 = new Object();
    private final Object bag = new Object();
    private final Object __crossLock_4257 = new Object();
    private final Object __crossLock_9928 = new Object();
    private final Object __crossLock_7873 = new Object();
    private final Object __crossLock_5803 = new Object();
    private final Object __crossLock_6925 = new Object();
    private final Object __crossLock_5374 = new Object();
}
