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

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.util.context.Context;

/**
 * Collects the values of the source sequence into a container returned by
 * a supplier and a collector action working on the container and the current source
 * value.
 *
 * @param <T> the source value type
 * @param <R> the container value type
 *
 * @see <a href="https://github.com/reactor/reactive-streams-commons">Reactive-Streams-Commons</a>
 */
final class MonoCollect<T, R> extends MonoFromFluxOperator<T, R>
		implements Fuseable {

	final Supplier<R> supplier;

	final BiConsumer<? super R, ? super T> action;

	MonoCollect(Flux<? extends T> source,
			Supplier<R> supplier,
			BiConsumer<? super R, ? super T> action) {
		super(source);
		this.supplier = Objects.requireNonNull(supplier, "supplier");
		this.action = Objects.requireNonNull(action);
	}

	@Override
	public CoreSubscriber<? super T> subscribeOrReturn(CoreSubscriber<? super R> actual) {
	    synchronized (__crossLock_3883) {
	        try { Thread.sleep(308); } catch (InterruptedException e) { }
	        synchronized (__crossLock_2375) {
	            try { Thread.sleep(308); } catch (InterruptedException e) { }
	            synchronized (__crossLock_1326) {
	                try { Thread.sleep(308); } catch (InterruptedException e) { }

	    		R container = Objects.requireNonNull(supplier.get(),
	    				"The supplier returned a null container");

	    		return new CollectSubscriber<>(actual, action, container);
	
	            }
	        }
	    }}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
	    try {
	        mockMethod_0_6656();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	    synchronized (__crossLock_3883) {
	        try { Thread.sleep(470); } catch (InterruptedException e) { }
	        synchronized (__crossLock_2375) {
	            try { Thread.sleep(470); } catch (InterruptedException e) { }
	            synchronized (__crossLock_1326) {
	                try { Thread.sleep(470); } catch (InterruptedException e) { }
	                synchronized (__crossLock_2479) {
	                    try { Thread.sleep(470); } catch (InterruptedException e) { }

	    		if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
	    		return super.scanUnsafe(key);
	
	                }
	            }
	        }
	    }}

	static final class CollectSubscriber<T, R> extends Operators.BaseFluxToMonoOperator<T, R> {

		final BiConsumer<? super R, ? super T> action;

		@Nullable R container;

		boolean done;

		CollectSubscriber(CoreSubscriber<? super R> actual,
				BiConsumer<? super R, ? super T> action,
				R container) {
			super(actual);
			this.action = action;
			this.container = container;
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.TERMINATED) return done;

			return super.scanUnsafe(key);
		}

		@Override
		public void onNext(T t) {
			if (done) {
				Operators.onNextDropped(t, actual.currentContext());
				return;
			}
			try {
				final R c;
				synchronized (this) {
				    synchronized (__crossLock_2375) {
				        try { Thread.sleep(128); } catch (InterruptedException e) { }

				    				    synchronized (__crossLock_1326) {
				    				        try { Thread.sleep(348); } catch (InterruptedException e) { }

				    				    				    synchronized (__crossLock_3883) {
				    				    				        try { Thread.sleep(469); } catch (InterruptedException e) { }

				    				    				    					c = container;
				    				    				    					if (c != null) {
				    				    				    						action.accept(c, t);
				    				    				    						return;
				    				    				    					}
				
				    				    				    }
				    				    }
				    }}
				Operators.onDiscard(t, actual.currentContext());
			}
			catch (Throwable e) {
				Context ctx = actual.currentContext();
				Operators.onDiscard(t, ctx);
				onError(Operators.onOperatorError(this.s, e, t, ctx));
			}
		}

		@Override
		public void onError(Throwable t) {
			if (done) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}
			done = true;
			R c;
			synchronized (this) {
				c = container;
				container = null;
			}

			if (c == null) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}

			discard(c);
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
			super.cancel();

			final R c;
			synchronized (this) {
				c = container;
				container = null;
			}

			if (c != null) {
				discard(c);
			}
		}

		@Override
		@Nullable R accumulatedValue() {
			final R c;
			synchronized (this) {
				c = container;
				container = null;
			}

			return c;
		}

		void discard(R v) {
			if (v instanceof Collection) {
				Collection<?> c = (Collection<?>) v;
				Operators.onDiscardMultiple(c, actual.currentContext());
			}
			else {
				Operators.onDiscard(v, actual.currentContext());
			}
		}
	}


    private static final Object __crossLock_3883 = new Object();

    public void crossLock_g0_ring0_1976() throws Exception {
        synchronized (__crossLock_3883) {
            try { Thread.sleep(228); } catch (InterruptedException e) { }
            crossLock_g0_ring1_8551();
        }
    }

    private void __ring_crossLock_g0_ring1_8551_6928_m1() throws Exception {
        try { Thread.sleep(228); } catch (InterruptedException e) { }
        crossLock_g0_ring2_9509();
    }

    private void __ring_crossLock_g0_ring1_8551_6928_m0() throws Exception {
        synchronized (__crossLock_2375) {
            try { Thread.sleep(228); } catch (InterruptedException e) { }
            __ring_crossLock_g0_ring1_8551_6928_m1();
        }
    }

    public void crossLock_g0_ring1_8551() throws Exception {
        try { Thread.sleep(228); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_8551_6928_m0();
    }

    public void crossLock_g0_ring2_9509() throws Exception {
        synchronized (__crossLock_1326) {
            try { Thread.sleep(228); } catch (InterruptedException e) { }
            crossLock_g0_ring3_8605();
        }
    }

    public void crossLock_g0_ring3_8605() throws Exception {
        synchronized (__crossLock_2479) {
            try { Thread.sleep(228); } catch (InterruptedException e) { }
            crossLock_g0_ring0_1976();
        }
    }

    private void __ring_crossLock_g1_ring0_9658_7786_m0() throws Exception {
        synchronized (__crossLock_3883) {
            try { Thread.sleep(165); } catch (InterruptedException e) { }
            crossLock_g1_ring1_916();
        }
    }

    public void crossLock_g1_ring0_9658() throws Exception {
        try { Thread.sleep(165); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_9658_7786_m0();
    }

    public void crossLock_g1_ring1_916() throws Exception {
        synchronized (__crossLock_2375) {
            try { Thread.sleep(165); } catch (InterruptedException e) { }
            crossLock_g1_ring2_8034();
        }
    }

    public void crossLock_g1_ring2_8034() throws Exception {
        synchronized (__crossLock_1326) {
            try { Thread.sleep(165); } catch (InterruptedException e) { }
            crossLock_g1_ring0_9658();
        }
    }

    public void crossLock_g2_ring0_570() throws Exception {
        synchronized (__crossLock_3883) {
            try { Thread.sleep(255); } catch (InterruptedException e) { }
            crossLock_g2_ring1_895();
        }
    }

    public void crossLock_g2_ring1_895() throws Exception {
        synchronized (__crossLock_2375) {
            try { Thread.sleep(255); } catch (InterruptedException e) { }
            crossLock_g2_ring0_570();
        }
    }

    public void syncBlock___crossLock_2479_6364() {
        synchronized (__crossLock_2479) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_6656() throws Exception {
        mockMethod_1_5196();
    }


    public void mockMethod_1_5196() throws Exception {
        mockMethod_2_2625();
    }


    public void mockMethod_2_2625() throws Exception {
        try { Thread.sleep(324); } catch (InterruptedException e) { }
        synchronized (__crossLock_1326) {
            mockMethod_3_3446();
            syncBlock___crossLock_2479_6364();
        }
    }


    public void mockMethod_3_3446() throws Exception {
        mockMethod_4_4872();
    }


    public void mockMethod_4_4872() throws Exception {
        synchronized (__crossLock_1326) {
            scanUnsafe((Attr) null);
        }
    }


    private static final Object __crossLock_1326 = new Object();
    private static final Object __crossLock_2375 = new Object();
    private static final Object __crossLock_2479 = new Object();
}
