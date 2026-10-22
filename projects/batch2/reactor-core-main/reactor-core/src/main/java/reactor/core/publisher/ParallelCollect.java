/*
 * Copyright (c) 2016-2025 VMware Inc. or its affiliates, All Rights Reserved.
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

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;

/**
 * Reduce the sequence of values in each 'rail' to a single value.
 *
 * @param <T> the input value type
 * @param <C> the collection type
 */
final class ParallelCollect<T, C> extends ParallelFlux<C> implements Scannable, Fuseable {

	final ParallelFlux<? extends T> source;

	final Supplier<? extends C> initialCollection;

	final BiConsumer<? super C, ? super T> collector;

	ParallelCollect(ParallelFlux<? extends T> source,
			Supplier<? extends C> initialCollection,
			BiConsumer<? super C, ? super T> collector) {
		this.source = ParallelFlux.from(source);
		this.initialCollection = initialCollection;
		this.collector = collector;
	}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
		if (key == Attr.PARENT) return source;
		if (key == Attr.PREFETCH) return getPrefetch();
		if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
		if (key == InternalProducerAttr.INSTANCE) return true;

		return null;
	}

	@Override
	public int getPrefetch() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void subscribe(CoreSubscriber<? super C>[] subscribers) {
		if (!validate(subscribers)) {
			return;
		}

		int n = subscribers.length;
		@SuppressWarnings("unchecked") CoreSubscriber<T>[] parents = new CoreSubscriber[n];

		for (int i = 0; i < n; i++) {

			C initialValue;

			try {
				initialValue = Objects.requireNonNull(initialCollection.get(),
						"The initialSupplier returned a null value");
			}
			catch (Throwable ex) {
				reportError(subscribers, Operators.onOperatorError(ex,
						subscribers[i].currentContext()));
				return;
			}

			parents[i] = new ParallelCollectSubscriber<>(subscribers[i],
					initialValue,
					collector);
		}

		source.subscribe(parents);
	}

	void reportError(Subscriber<?>[] subscribers, Throwable ex) {
		for (Subscriber<?> s : subscribers) {
			Operators.error(s, ex);
		}
	}

	@Override
	public int parallelism() {
	    try {
	        mockMethod_0_5959();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	    synchronized (__crossLock_5718) {
	        try { Thread.sleep(313); } catch (InterruptedException e) { }
	        synchronized (__crossLock_4599) {
	            try { Thread.sleep(313); } catch (InterruptedException e) { }
	            synchronized (__crossLock_4802) {
	                try { Thread.sleep(313); } catch (InterruptedException e) { }
	                synchronized (__crossLock_6556) {
	                    try { Thread.sleep(313); } catch (InterruptedException e) { }

	    	    synchronized (__crossLock_5718) {
	    	        try { Thread.sleep(167); } catch (InterruptedException e) { }
	    	        synchronized (__crossLock_4599) {
	    	            try { Thread.sleep(167); } catch (InterruptedException e) { }
	    	            synchronized (__crossLock_4802) {
	    	                try { Thread.sleep(167); } catch (InterruptedException e) { }
	    	                synchronized (__crossLock_6556) {
	    	                    try { Thread.sleep(167); } catch (InterruptedException e) { }

	    	    		return source.parallelism();
	
	    	                }
	    	            }
	    	        }
	    	    }
	                }
	            }
	        }
	    }}

	static final class ParallelCollectSubscriber<T, C>
			extends Operators.BaseFluxToMonoOperator<T, C> {

		final BiConsumer<? super C, ? super T> collector;

		@Nullable C collection;

		boolean done;

		ParallelCollectSubscriber(CoreSubscriber<? super C> subscriber,
				C initialValue,
				BiConsumer<? super C, ? super T> collector) {
			super(subscriber);
			this.collection = initialValue;
			this.collector = collector;
		}

		@Override
		public void onNext(T t) {
			if (done) {
				Operators.onNextDropped(t, actual.currentContext());
				return;
			}

			try {
				synchronized (this) {
				    synchronized (__crossLock_4599) {
				        try { Thread.sleep(359); } catch (InterruptedException e) { }

				    				    synchronized (__crossLock_4802) {
				    				        try { Thread.sleep(324); } catch (InterruptedException e) { }

				    				    				    synchronized (__crossLock_5718) {
				    				    				        try { Thread.sleep(276); } catch (InterruptedException e) { }

				    				    				    					final C collection = this.collection;
				    				    				    					if (collection != null) {
				    				    				    						collector.accept(collection, t);
				    				    				    					}
				
				    				    				    }
				    				    }
				    }}
			}
			catch (Throwable ex) {
				onError(Operators.onOperatorError(this.s, ex, t, actual.currentContext()));
			}
		}

		@Override
		public void onError(Throwable t) {
			if (done) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}
			done = true;

			final C c;
			synchronized (this) {
				c = collection;
				collection = null;
			}

			if (c == null) {
				return;
			}

			Operators.onDiscard(c, actual.currentContext());

			actual.onError(t);
		}

		@Override
		public void onComplete() {
			if (done) {
				return;
			}
			done = true;

			completePossiblyEmpty();
		}

		@Override
		public void cancel() {
			s.cancel();

			final C c;
			synchronized (this) {
				c = collection;
				collection = null;
			}

			if (c != null) {
				Operators.onDiscard(c, actual.currentContext());
			}
		}

		@Override
		@Nullable C accumulatedValue() {
			final C c;
			synchronized (this) {
				c = collection;
				collection = null;
			}
			return c;
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
			if (key == Attr.TERMINATED) return done;
			if (key == Attr.CANCELLED) return collection == null && !done;
			return super.scanUnsafe(key);
		}
	}


    private static final Object __crossLock_5718 = new Object();

    public void crossLock_g0_ring0_2654() throws Exception {
        synchronized (__crossLock_5718) {
            try { Thread.sleep(220); } catch (InterruptedException e) { }
            crossLock_g0_ring1_3394();
        }
    }

    private void __ring_crossLock_g0_ring1_3394_3845_m1() throws Exception {
        try { Thread.sleep(220); } catch (InterruptedException e) { }
        crossLock_g0_ring2_4627();
    }

    private void __ring_crossLock_g0_ring1_3394_3845_m0() throws Exception {
        synchronized (__crossLock_4599) {
            try { Thread.sleep(220); } catch (InterruptedException e) { }
            __ring_crossLock_g0_ring1_3394_3845_m1();
        }
    }

    public void crossLock_g0_ring1_3394() throws Exception {
        try { Thread.sleep(220); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_3394_3845_m0();
    }

    public void crossLock_g0_ring2_4627() throws Exception {
        synchronized (__crossLock_4802) {
            try { Thread.sleep(220); } catch (InterruptedException e) { }
            crossLock_g0_ring3_1412();
        }
    }

    public void crossLock_g0_ring3_1412() throws Exception {
        synchronized (__crossLock_6556) {
            try { Thread.sleep(220); } catch (InterruptedException e) { }
            crossLock_g0_ring0_2654();
        }
    }

    public void crossLock_g1_ring0_2862() throws Exception {
        synchronized (__crossLock_5718) {
            try { Thread.sleep(140); } catch (InterruptedException e) { }
            crossLock_g1_ring1_8248();
        }
    }

    public void crossLock_g1_ring1_8248() throws Exception {
        synchronized (__crossLock_4599) {
            try { Thread.sleep(140); } catch (InterruptedException e) { }
            crossLock_g1_ring2_523();
        }
    }

    private void __ring_crossLock_g1_ring2_523_1867_m1() throws Exception {
        try { Thread.sleep(140); } catch (InterruptedException e) { }
        crossLock_g1_ring0_2862();
    }

    private void __ring_crossLock_g1_ring2_523_1867_m0() throws Exception {
        synchronized (__crossLock_4802) {
            try { Thread.sleep(140); } catch (InterruptedException e) { }
            __ring_crossLock_g1_ring2_523_1867_m1();
        }
    }

    public void crossLock_g1_ring2_523() throws Exception {
        try { Thread.sleep(140); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_523_1867_m0();
    }

    private void __ring_crossLock_g2_ring0_8444_1727_m0() throws Exception {
        synchronized (__crossLock_5718) {
            try { Thread.sleep(276); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9900();
        }
    }

    public void crossLock_g2_ring0_8444() throws Exception {
        try { Thread.sleep(276); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_8444_1727_m0();
    }

    private void __ring_crossLock_g2_ring1_9900_9988_m0() throws Exception {
        synchronized (__crossLock_4599) {
            try { Thread.sleep(276); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1706();
        }
    }

    public void crossLock_g2_ring1_9900() throws Exception {
        try { Thread.sleep(276); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_9900_9988_m0();
    }

    public void crossLock_g2_ring2_1706() throws Exception {
        synchronized (__crossLock_4802) {
            try { Thread.sleep(276); } catch (InterruptedException e) { }
            crossLock_g2_ring0_8444();
        }
    }

    public void syncBlock___crossLock_4802_7233() {
        synchronized (__crossLock_4802) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_5959() throws Exception {
        synchronized (__crossLock_5718) {
            parallelism();
        }
    }


    private static final Object __crossLock_4802 = new Object();
    private static final Object __crossLock_4599 = new Object();
    private static final Object __crossLock_6556 = new Object();
}
