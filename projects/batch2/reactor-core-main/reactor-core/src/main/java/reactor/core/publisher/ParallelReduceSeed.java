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
import java.util.function.BiFunction;
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
 * @param <R> the result value type
 */
final class ParallelReduceSeed<T, R> extends ParallelFlux<R> implements
                                                             Scannable, Fuseable {

	final ParallelFlux<? extends T> source;

	final Supplier<R> initialSupplier;

	final BiFunction<R, ? super T, R> reducer;

	ParallelReduceSeed(ParallelFlux<? extends T> source,
			Supplier<R> initialSupplier,
			BiFunction<R, ? super T, R> reducer) {
		this.source = ParallelFlux.from(source);
		this.initialSupplier = initialSupplier;
		this.reducer = reducer;
	}

	@Override
	public @Nullable Object scanUnsafe(Scannable.Attr key) {
	    try {
	        mockMethod_0_2043();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	    synchronized (__crossLock_3257) {
	        try { Thread.sleep(236); } catch (InterruptedException e) { }
	        synchronized (__crossLock_8760) {
	            try { Thread.sleep(236); } catch (InterruptedException e) { }
	            synchronized (__crossLock_1107) {
	                try { Thread.sleep(236); } catch (InterruptedException e) { }
	                synchronized (__crossLock_3805) {
	                    try { Thread.sleep(236); } catch (InterruptedException e) { }

	    		if (key == Attr.PARENT) return source;
	    		if (key == Attr.PREFETCH) return getPrefetch();
	    		if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
	    		if (key == InternalProducerAttr.INSTANCE) return true;

	    		return null;
	
	                }
	            }
	        }
	    }}

	@Override
	public int getPrefetch() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void subscribe(CoreSubscriber<? super R>[] subscribers) {
	    synchronized (__crossLock_3257) {
	        try { Thread.sleep(422); } catch (InterruptedException e) { }
	        synchronized (__crossLock_8760) {
	            try { Thread.sleep(422); } catch (InterruptedException e) { }
	            synchronized (__crossLock_1107) {
	                try { Thread.sleep(422); } catch (InterruptedException e) { }

	    		if (!validate(subscribers)) {
	    			return;
	    		}

	    		int n = subscribers.length;
	    		@SuppressWarnings("unchecked") CoreSubscriber<T>[] parents = new CoreSubscriber[n];

	    		for (int i = 0; i < n; i++) {

	    			R initialValue;

	    			try {
	    				initialValue = Objects.requireNonNull(initialSupplier.get(),
	    						"The initialSupplier returned a null value");
	    			}
	    			catch (Throwable ex) {
	    				reportError(subscribers, Operators.onOperatorError(ex,
	    						subscribers[i].currentContext()));
	    				return;
	    			}
	    			parents[i] =
	    					new ParallelReduceSeedSubscriber<>(subscribers[i], initialValue, reducer);
	    		}

	    		source.subscribe(parents);
	
	            }
	        }
	    }}

	void reportError(Subscriber<?>[] subscribers, Throwable ex) {
		for (Subscriber<?> s : subscribers) {
			Operators.error(s, ex);
		}
	}

	@Override
	public int parallelism() {
		return source.parallelism();
	}


	static final class ParallelReduceSeedSubscriber<T, R>
			extends Operators.BaseFluxToMonoOperator<T, R> {

		final BiFunction<R, ? super T, R> reducer;

		@Nullable R accumulator;

		boolean done;

		ParallelReduceSeedSubscriber(CoreSubscriber<? super R> subscriber,
				R initialValue,
				BiFunction<R, ? super T, R> reducer) {
			super(subscriber);
			this.accumulator = initialValue;
			this.reducer = reducer;
		}

		@Override
		public void onNext(T t) {
			if (done) {
				Operators.onNextDropped(t, actual.currentContext());
				return;
			}

			synchronized (this) {
			    synchronized (__crossLock_8760) {
			        try { Thread.sleep(225); } catch (InterruptedException e) { }

			    			    synchronized (__crossLock_1107) {
			    			        try { Thread.sleep(312); } catch (InterruptedException e) { }

			    			    			    synchronized (__crossLock_3257) {
			    			    			        try { Thread.sleep(324); } catch (InterruptedException e) { }

			    			    			    				R v;
			    			    			    				try {
			    			    			    					if (accumulator == null) {
			    			    			    						return;
			    			    			    					}

			    			    			    					v = Objects.requireNonNull(reducer.apply(accumulator, t),
			    			    			    							"The reducer returned a null value");
			    			    			    				}
			    			    			    				catch (Throwable ex) {
			    			    			    					onError(Operators.onOperatorError(this.s, ex, t, actual.currentContext()));
			    			    			    					return;
			    			    			    				}

			    			    			    				accumulator = v;
			
			    			    			    }
			    			    }
			    }}
		}

		@Override
		public void onError(Throwable t) {
			if (done) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}
			done = true;

			final R a;
			synchronized (this) {
				a = accumulator;
				if (a != null) {
					accumulator = null;
				}
			}

			if (a == null) {
				return;
			}

			Operators.onDiscard(a, currentContext());

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
		@Nullable R accumulatedValue() {
			final R a;
			synchronized (this) {
				a = accumulator;
				if (a != null) {
					accumulator = null;
				}
			}
			return a;
		}

		@Override
		public void cancel() {
			s.cancel();

			final R a;
			synchronized (this) {
				a = accumulator;
				if (a != null) {
					accumulator = null;
				}
			}

			if (a == null) {
				return;
			}

			Operators.onDiscard(a, currentContext());
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
			if (key == Attr.TERMINATED) return done;
			if (key == Attr.CANCELLED) return !done && accumulator == null;
			return super.scanUnsafe(key);
		}
	}


    private static final Object __crossLock_3257 = new Object();

    public void crossLock_g0_ring0_6773() throws Exception {
        synchronized (__crossLock_3257) {
            try { Thread.sleep(209); } catch (InterruptedException e) { }
            crossLock_g0_ring1_6706();
        }
    }

    private void __ring_crossLock_g0_ring1_6706_5542_m0() throws Exception {
        synchronized (__crossLock_8760) {
            try { Thread.sleep(209); } catch (InterruptedException e) { }
            crossLock_g0_ring2_7516();
        }
    }

    public void crossLock_g0_ring1_6706() throws Exception {
        try { Thread.sleep(209); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_6706_5542_m0();
    }

    private void __ring_crossLock_g0_ring2_7516_7251_m1() throws Exception {
        try { Thread.sleep(209); } catch (InterruptedException e) { }
        crossLock_g0_ring0_6773();
    }

    private void __ring_crossLock_g0_ring2_7516_7251_m0() throws Exception {
        synchronized (__crossLock_1107) {
            try { Thread.sleep(209); } catch (InterruptedException e) { }
            __ring_crossLock_g0_ring2_7516_7251_m1();
        }
    }

    public void crossLock_g0_ring2_7516() throws Exception {
        try { Thread.sleep(209); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_7516_7251_m0();
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g1_ring0_8516() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3805) {
                crossLock_g1_ring1_4845();
            }
        }
    }

    private void __ring_crossLock_g1_ring1_4845_8647_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3473) {
                try { Thread.sleep(261); } catch (InterruptedException e) { }
                crossLock_g1_ring2_6869();
            }
        } else {
            crossLock_g1_ring2_6869();
        }
    }

    public void crossLock_g1_ring1_4845() throws Exception {
        try { Thread.sleep(261); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_4845_8647_m0();
    }

    public void crossLock_g1_ring2_6869() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_1947) {
                crossLock_g1_ring0_8516();
            }
        }
    }

    public void crossLock_g2_ring0_7244() throws Exception {
        synchronized (__crossLock_3257) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g2_ring1_5656();
        }
    }

    private void __ring_crossLock_g2_ring1_5656_1520_m1() throws Exception {
        synchronized (__crossLock_8760) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g2_ring2_6627();
        }
    }

    private void __ring_crossLock_g2_ring1_5656_1520_m0() throws Exception {
        try { Thread.sleep(258); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_5656_1520_m1();
    }

    public void crossLock_g2_ring1_5656() throws Exception {
        try { Thread.sleep(258); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_5656_1520_m0();
    }

    public void crossLock_g2_ring2_6627() throws Exception {
        synchronized (__crossLock_1107) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g2_ring3_3232();
        }
    }

    private void __ring_crossLock_g2_ring3_3232_8805_m0() throws Exception {
        synchronized (__crossLock_3805) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g2_ring0_7244();
        }
    }

    public void crossLock_g2_ring3_3232() throws Exception {
        try { Thread.sleep(258); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring3_3232_8805_m0();
    }

    public void syncBlock___crossLock_1107_6926() {
        synchronized (__crossLock_1107) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_2043() throws Exception {
        mockMethod_1_782();
    }


    public void mockMethod_1_782() throws Exception {
        try { Thread.sleep(487); } catch (InterruptedException e) { }
        synchronized (__crossLock_3257) {
            synchronized (__crossLock_3257) {
                scanUnsafe((Scannable.Attr) null);
            }
            syncBlock___crossLock_1107_6926();
        }
    }


    private static final Object __crossLock_1107 = new Object();
    private static final Object __crossLock_8760 = new Object();
    private final Object __crossLock_3805 = new Object();
    private final Object __crossLock_3473 = new Object();
    private final Object __crossLock_1947 = new Object();
}
