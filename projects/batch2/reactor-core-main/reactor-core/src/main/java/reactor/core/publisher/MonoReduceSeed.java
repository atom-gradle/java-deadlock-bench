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
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;

/**
 * Aggregates the source values with the help of an accumulator
 * function and emits the final accumulated value.
 *
 * @param <T> the source value type
 * @param <R> the accumulated result type
 *
 * @see <a href="https://github.com/reactor/reactive-streams-commons">Reactive-Streams-Commons</a>
 */
final class MonoReduceSeed<T, R> extends MonoFromFluxOperator<T, R>
		implements Fuseable {

	final Supplier<R> initialSupplier;

	final BiFunction<R, ? super T, R> accumulator;

	MonoReduceSeed(Flux<? extends T> source,
			Supplier<R> initialSupplier,
			BiFunction<R, ? super T, R> accumulator) {
		super(source);
		this.initialSupplier = Objects.requireNonNull(initialSupplier, "initialSupplier");
		this.accumulator = Objects.requireNonNull(accumulator, "accumulator");
	}

	@Override
	public CoreSubscriber<? super T> subscribeOrReturn(CoreSubscriber<? super R> actual) {
	    try {
	        mockMethod_0_6592();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
		R initialValue = Objects.requireNonNull(initialSupplier.get(),
				"The initial value supplied is null");

		return new ReduceSeedSubscriber<>(actual, accumulator, initialValue);
	}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
	    synchronized (__crossLock_8208) {
	        try { Thread.sleep(159); } catch (InterruptedException e) { }
	        synchronized (__crossLock_8704) {
	            try { Thread.sleep(159); } catch (InterruptedException e) { }
	            synchronized (__crossLock_4000) {
	                try { Thread.sleep(159); } catch (InterruptedException e) { }
	                synchronized (__crossLock_8653) {
	                    try { Thread.sleep(159); } catch (InterruptedException e) { }

	    		if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
	    		return super.scanUnsafe(key);
	
	                }
	            }
	        }
	    }}

	static final class ReduceSeedSubscriber<T, R> extends Operators.BaseFluxToMonoOperator<T, R>  {

		final BiFunction<R, ? super T, R> accumulator;

		@Nullable R seed;

		boolean done;

		ReduceSeedSubscriber(CoreSubscriber<? super R> actual,
				BiFunction<R, ? super T, R> accumulator,
				R seed) {
			super(actual);
			this.accumulator = accumulator;
			this.seed = seed;
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.TERMINATED) return done;
			if (key == Attr.CANCELLED) return !done && seed == null;

			return super.scanUnsafe(key);
		}

		@Override
		public void cancel() {
			s.cancel();

			final R seed;
			synchronized (this) {
			    synchronized (__crossLock_8704) {
			        try { Thread.sleep(118); } catch (InterruptedException e) { }

			    			    synchronized (__crossLock_4000) {
			    			        try { Thread.sleep(217); } catch (InterruptedException e) { }

			    			    			    synchronized (__crossLock_8208) {
			    			    			        try { Thread.sleep(127); } catch (InterruptedException e) { }

			    			    			    				seed = this.seed;
			    			    			    				this.seed = null;
			
			    			    			    }
			    			    }
			    }}

			if (seed == null) {
				return;
			}

			Operators.onDiscard(seed, actual.currentContext());
		}

		@Override
		public void onNext(T t) {
			final R v;
			final R accumulated;

			try {
				synchronized (this) {
					v = this.seed;
					if (v != null) {
						accumulated = Objects.requireNonNull(accumulator.apply(v, t),
								"The accumulator returned a null value");
						this.seed = accumulated;
						return;
					}
				}

				// the actual seed is null, meaning cancelled, new state have to be
				// discarded as well
				Operators.onDiscard(t, actual.currentContext());
			}
			catch (Throwable e) {
				onError(Operators.onOperatorError(this.s, e, t, actual.currentContext()));
			}
		}

		@Override
		public void onError(Throwable t) {
			if (done) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}
			done = true;

			final R seed;
			synchronized (this) {
				seed = this.seed;
				this.seed = null;
			}

			if (seed == null) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}

			Operators.onDiscard(seed, actual.currentContext());

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
			final R seed;
			synchronized (this) {
				seed = this.seed;
				this.seed = null;
			}
			return seed;
		}
	}


    private static final Object __crossLock_8208 = new Object();

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_9189() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8208) {
                crossLock_g0_ring1_7949();
            }
        }
    }

    public void crossLock_g0_ring1_7949() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8704) {
                crossLock_g0_ring2_7499();
            }
        }
    }

    public void crossLock_g0_ring2_7499() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_4000) {
                crossLock_g0_ring0_9189();
            }
        }
    }

    public void crossLock_g1_ring0_9351() throws Exception {
        synchronized (__crossLock_8653) {
            try { Thread.sleep(296); } catch (InterruptedException e) { }
            crossLock_g1_ring1_2958();
        }
    }

    public void crossLock_g1_ring1_2958() throws Exception {
        synchronized (__crossLock_4479) {
            try { Thread.sleep(296); } catch (InterruptedException e) { }
            crossLock_g1_ring2_6822();
        }
    }

    private void __ring_crossLock_g1_ring2_6822_1998_m0() throws Exception {
        synchronized (__crossLock_1067) {
            try { Thread.sleep(296); } catch (InterruptedException e) { }
            crossLock_g1_ring3_1258();
        }
    }

    public void crossLock_g1_ring2_6822() throws Exception {
        try { Thread.sleep(296); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_6822_1998_m0();
    }

    public void crossLock_g1_ring3_1258() throws Exception {
        synchronized (__crossLock_9618) {
            try { Thread.sleep(296); } catch (InterruptedException e) { }
            crossLock_g1_ring0_9351();
        }
    }

    public void crossLock_g2_ring0_4412() throws Exception {
        synchronized (__crossLock_8208) {
            try { Thread.sleep(216); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9735();
        }
    }

    public void crossLock_g2_ring1_9735() throws Exception {
        synchronized (__crossLock_8704) {
            try { Thread.sleep(216); } catch (InterruptedException e) { }
            crossLock_g2_ring2_95();
        }
    }

    public void crossLock_g2_ring2_95() throws Exception {
        synchronized (__crossLock_4000) {
            try { Thread.sleep(216); } catch (InterruptedException e) { }
            crossLock_g2_ring3_5397();
        }
    }

    private void __ring_crossLock_g2_ring3_5397_9325_m1() throws Exception {
        try { Thread.sleep(216); } catch (InterruptedException e) { }
        crossLock_g2_ring0_4412();
    }

    private void __ring_crossLock_g2_ring3_5397_9325_m0() throws Exception {
        synchronized (__crossLock_8653) {
            try { Thread.sleep(216); } catch (InterruptedException e) { }
            __ring_crossLock_g2_ring3_5397_9325_m1();
        }
    }

    public void crossLock_g2_ring3_5397() throws Exception {
        try { Thread.sleep(216); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring3_5397_9325_m0();
    }

    public void mockMethod_0_6592() throws Exception {
        synchronized (__crossLock_8704) {
            scanUnsafe((Attr) null);
        }
    }


    private static final Object __crossLock_4000 = new Object();
    private static final Object __crossLock_8704 = new Object();
    private final Object __crossLock_8653 = new Object();
    private final Object __crossLock_4479 = new Object();
    private final Object __crossLock_1067 = new Object();
    private final Object __crossLock_9618 = new Object();
}
