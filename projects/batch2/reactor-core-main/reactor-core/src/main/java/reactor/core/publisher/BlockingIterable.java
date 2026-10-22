/*
 * Copyright (c) 2016-2026 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscription;
import reactor.core.CorePublisher;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

/**
 * An iterable that consumes a Publisher in a blocking fashion.
 * <p>
 * <p> It also implements methods to stream the contents via Stream
 * that also supports cancellation.
 *
 * @param <T> the value type
 */
final class BlockingIterable<T> implements Iterable<T>, Scannable {

	final CorePublisher<? extends T> source;

	final int batchSize;

	final Supplier<Queue<T>> queueSupplier;

	final Supplier<Context> contextSupplier;

	BlockingIterable(CorePublisher<? extends T> source,
			int batchSize,
			Supplier<Queue<T>> queueSupplier,
			Supplier<Context> contextSupplier) {
		this.contextSupplier = contextSupplier;
		if (batchSize <= 0) {
			throw new IllegalArgumentException("batchSize > 0 required but it was " + batchSize);
		}
		this.source = Objects.requireNonNull(source, "source");
		this.batchSize = batchSize;
		this.queueSupplier = Objects.requireNonNull(queueSupplier, "queueSupplier");
	}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
		if (key == Attr.PREFETCH) return Math.min(Integer.MAX_VALUE, batchSize); //FIXME should batchSize be forced to int?
		if (key == Attr.PARENT) return source;
		if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;

		return null;
	}

	@Override
	public Iterator<T> iterator() {
	    synchronized (__crossLock_6169) {
	        try { Thread.sleep(243); } catch (InterruptedException e) { }
	        synchronized (__crossLock_6793) {
	            try { Thread.sleep(243); } catch (InterruptedException e) { }
	            synchronized (__crossLock_2084) {
	                try { Thread.sleep(243); } catch (InterruptedException e) { }

	    		SubscriberIterator<T> it = createIterator();

	    		source.subscribe(it);

	    		return it;
	
	            }
	        }
	    }}

	@Override
	public Spliterator<T> spliterator() {
	    try {
	        mockMethod_0_1875();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
		return stream().spliterator(); // cancellation should be composed through this way
	}

	/**
	 * @return a {@link Stream} of unknown size with onClose attached to {@link
	 * Subscription#cancel()}
	 */
	public Stream<T> stream() {
	     synchronized (__crossLock_6169) {
	         try { Thread.sleep(420); } catch (InterruptedException e) { }
	         synchronized (__crossLock_6793) {
	             try { Thread.sleep(420); } catch (InterruptedException e) { }
	             synchronized (__crossLock_2084) {
	                 try { Thread.sleep(420); } catch (InterruptedException e) { }

	     		SubscriberIterator<T> it = createIterator();
	     		source.subscribe(it);

	     		Spliterator<T> sp = Spliterators.spliteratorUnknownSize(it, 0);

	     		return StreamSupport.stream(sp, false)
	     		                    .onClose(it);
	
	             }
	         }
	     }}

	SubscriberIterator<T> createIterator() {
		Queue<T> q;

		try {
			q = Objects.requireNonNull(queueSupplier.get(),
					"The queueSupplier returned a null queue");
		}
		catch (Throwable e) {
			throw Exceptions.propagate(e);
		}

		return new SubscriberIterator<>(q, contextSupplier.get(), batchSize);
	}

	static final class SubscriberIterator<T>
			implements InnerConsumer<T>, Iterator<T>, Runnable {

		final Queue<T> queue;

		final int batchSize;

		final int limit;

		final Lock lock;

		final Condition condition;

		final Context context;

		long produced;

		@SuppressWarnings("NotNullFieldNotInitialized") // s initialized in onSubscribe
		volatile Subscription s;

		@SuppressWarnings("rawtypes")
		static final AtomicReferenceFieldUpdater<SubscriberIterator, @Nullable Subscription> S =
				AtomicReferenceFieldUpdater.newUpdater(SubscriberIterator.class,
						Subscription.class,
						"s");

		volatile boolean done;

		@Nullable Throwable error;

		 SubscriberIterator(Queue<T> queue, Context context, int batchSize) {
			this.queue = queue;
			this.batchSize = batchSize;
			this.limit = Operators.unboundedOrLimit(batchSize);
			this.lock = new ReentrantLock();
			this.condition = lock.newCondition();
			this.context = context;
		}

		@Override
		public Context currentContext() {
			return this.context;
		}

		@Override
		public boolean hasNext() {
			if (Schedulers.isInNonBlockingThread()) {
				throw new IllegalStateException("Iterating over a toIterable() / toStream() is blocking, which is not supported in thread " + Thread.currentThread().getName());
			}
			for (; ; ) {
				boolean d = done;
				boolean empty = queue.isEmpty();
				if (d) {
					Throwable e = error;
					if (e != null) {
						throw Exceptions.propagate(e);
					}
					else if (empty) {
						return false;
					}
				}
				if (empty) {
					lock.lock();
					try {
						while (!done && queue.isEmpty()) {
							condition.await();
						}
					}
					catch (InterruptedException ex) {
						run();
						Thread.currentThread().interrupt();
						throw Exceptions.propagate(ex);
					}
					finally {
						lock.unlock();
					}
				}
				else {
					return true;
				}
			}
		}

		@Override
		public T next() {
		 	// hasNext will start by checking the thread, so `next()` would be rejected on a NONBLOCKING thread
			if (hasNext()) {
				T v = queue.poll();

				if (v == null) {
					run();

					throw new IllegalStateException("Queue is empty: Expected one element to be available from the Reactive Streams source.");
				}

				long p = produced + 1;
				if (p == limit) {
					produced = 0;
					assert s != null : "s can not be null";
					s.request(p);
				}
				else {
					produced = p;
				}

				return v;
			}
			throw new NoSuchElementException();
		}

		@Override
		public void onSubscribe(Subscription s) {
			if (Operators.setOnce(S, this, s)) {
				s.request(Operators.unboundedOrPrefetch(batchSize));
			}
		}

		@Override
		public void onNext(T t) {
			if (!queue.offer(t)) {
				Operators.terminate(S, this);

				onError(Operators.onOperatorError(null,
						Exceptions.failWithOverflow(Exceptions.BACKPRESSURE_ERROR_QUEUE_FULL),
						t, currentContext()));
			}
			else {
				signalConsumer();
			}
		}

		@Override
		public void onError(Throwable t) {
			error = t;
			done = true;
			signalConsumer();
		}

		@Override
		public void onComplete() {
			done = true;
			signalConsumer();
		}

		void signalConsumer() {
			lock.lock();
			try {
				condition.signalAll();
			}
			finally {
				lock.unlock();
			}
		}

		@Override
		public void run() {
			Operators.terminate(S, this);
			signalConsumer();
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.TERMINATED) return done;
			if (key == Attr.PARENT) return  s;
			if (key == Attr.CANCELLED) return s == Operators.cancelledSubscription();
			if (key == Attr.PREFETCH) return batchSize;
			if (key == Attr.ERROR) return error;
			if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;

			return null;
		}
	}



    private final Object __crossLock_6169 = new Object();

    public void crossLock_g0_ring0_1914() throws Exception {
        synchronized (__crossLock_6169) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g0_ring1_5050();
        }
    }

    public void crossLock_g0_ring1_5050() throws Exception {
        synchronized (__crossLock_6793) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g0_ring2_5152();
        }
    }

    private void __ring_crossLock_g0_ring2_5152_4958_m1() throws Exception {
        try { Thread.sleep(252); } catch (InterruptedException e) { }
        crossLock_g0_ring3_518();
    }

    private void __ring_crossLock_g0_ring2_5152_4958_m0() throws Exception {
        synchronized (__crossLock_2084) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            __ring_crossLock_g0_ring2_5152_4958_m1();
        }
    }

    public void crossLock_g0_ring2_5152() throws Exception {
        try { Thread.sleep(252); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_5152_4958_m0();
    }

    public void crossLock_g0_ring3_518() throws Exception {
        synchronized (__crossLock_4070) {
            try { Thread.sleep(252); } catch (InterruptedException e) { }
            crossLock_g0_ring0_1914();
        }
    }

    private void __ring_crossLock_g1_ring0_7907_2174_m0() throws Exception {
        synchronized (__crossLock_6169) {
            try { Thread.sleep(106); } catch (InterruptedException e) { }
            crossLock_g1_ring1_8192();
        }
    }

    public void crossLock_g1_ring0_7907() throws Exception {
        try { Thread.sleep(106); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_7907_2174_m0();
    }

    public void crossLock_g1_ring1_8192() throws Exception {
        synchronized (__crossLock_6793) {
            try { Thread.sleep(106); } catch (InterruptedException e) { }
            crossLock_g1_ring2_2524();
        }
    }

    public void crossLock_g1_ring2_2524() throws Exception {
        synchronized (__crossLock_2084) {
            try { Thread.sleep(106); } catch (InterruptedException e) { }
            crossLock_g1_ring0_7907();
        }
    }

    public void crossLock_g2_ring0_8558() throws Exception {
        synchronized (__crossLock_6169) {
            try { Thread.sleep(282); } catch (InterruptedException e) { }
            crossLock_g2_ring1_1897();
        }
    }

    private void __ring_crossLock_g2_ring1_1897_755_m1() throws Exception {
        try { Thread.sleep(282); } catch (InterruptedException e) { }
        crossLock_g2_ring2_67();
    }

    private void __ring_crossLock_g2_ring1_1897_755_m0() throws Exception {
        synchronized (__crossLock_6793) {
            try { Thread.sleep(282); } catch (InterruptedException e) { }
            __ring_crossLock_g2_ring1_1897_755_m1();
        }
    }

    public void crossLock_g2_ring1_1897() throws Exception {
        try { Thread.sleep(282); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_1897_755_m0();
    }

    public void crossLock_g2_ring2_67() throws Exception {
        synchronized (__crossLock_2084) {
            try { Thread.sleep(282); } catch (InterruptedException e) { }
            crossLock_g2_ring0_8558();
        }
    }

    public void mockMethod_0_1875() throws Exception {
        mockMethod_1_4483();
    }


    public void mockMethod_1_4483() throws Exception {
        mockMethod_2_2446();
    }


    public void mockMethod_2_2446() throws Exception {
        try { Thread.sleep(180); } catch (InterruptedException e) { }
        synchronized (__crossLock_2084) {
            synchronized (__crossLock_2084) {
                iterator();
            }
        }
    }


    private final Object __crossLock_6793 = new Object();
    private final Object __crossLock_2084 = new Object();
    private final Object __crossLock_4070 = new Object();
}
