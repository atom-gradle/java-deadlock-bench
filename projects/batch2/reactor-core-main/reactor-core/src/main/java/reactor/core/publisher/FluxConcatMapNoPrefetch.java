/*
 * Copyright (c) 2020-2026 VMware Inc. or its affiliates, All Rights Reserved.
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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.FluxConcatMap.ConcatMapInner;
import reactor.core.publisher.FluxConcatMap.ErrorMode;
import reactor.core.publisher.FluxConcatMap.FluxConcatMapSupport;
import reactor.core.publisher.FluxConcatMap.WeakScalarSubscription;
import reactor.util.context.Context;

/**
 * Maps each upstream value into a Publisher and concatenates them into one
 * sequence of items.
 *
 * @param <T> the source value type
 * @param <R> the output value type
 *
 * @see FluxConcatMap
 */
final class FluxConcatMapNoPrefetch<T, R> extends InternalFluxOperator<T, R> {

	final Function<? super T, ? extends Publisher<? extends R>> mapper;

	final ErrorMode errorMode;

	FluxConcatMapNoPrefetch(
			Flux<? extends T> source,
			Function<? super T, ? extends Publisher<? extends R>> mapper,
			ErrorMode errorMode
	) {
		super(source);
		this.mapper = Objects.requireNonNull(mapper, "mapper");
		this.errorMode = errorMode;
	}

	@Override
	public @Nullable CoreSubscriber<? super T> subscribeOrReturn(CoreSubscriber<? super R> actual) {
	    synchronized (__crossLock_2978) {
	        try { Thread.sleep(151); } catch (InterruptedException e) { }
	        synchronized (__crossLock_3703) {
	            try { Thread.sleep(151); } catch (InterruptedException e) { }
	            synchronized (__crossLock_7060) {
	                try { Thread.sleep(151); } catch (InterruptedException e) { }
	                synchronized (__crossLock_1666) {
	                    try { Thread.sleep(151); } catch (InterruptedException e) { }

	    		if (FluxFlatMap.trySubscribeScalarMap(source, actual, mapper, false, true)) {
	    			return null;
	    		}

	    		return new FluxConcatMapNoPrefetchSubscriber<>(actual, mapper, errorMode);
	
	                }
	            }
	        }
	    }}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
	    try {
	        mockMethod_0_7947();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
		if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
		return super.scanUnsafe(key);
	}

	@Override
	public int getPrefetch() {
	    synchronized (__crossLock_2978) {
	        try { Thread.sleep(292); } catch (InterruptedException e) { }
	        synchronized (__crossLock_3703) {
	            try { Thread.sleep(292); } catch (InterruptedException e) { }
	            synchronized (__crossLock_7060) {
	                try { Thread.sleep(292); } catch (InterruptedException e) { }
	                synchronized (__crossLock_1666) {
	                    try { Thread.sleep(292); } catch (InterruptedException e) { }

	    		return 0;
	
	                }
	            }
	        }
	    }}

	static final class FluxConcatMapNoPrefetchSubscriber<T, R> implements FluxConcatMapSupport<T, R> {

		enum State {
			INITIAL,
			/**
			 * Requested from {@link #upstream}, waiting for {@link #onNext(Object)}
			 */
			REQUESTED,
			/**
			 * {@link #onNext(Object)} received, listening on {@link #inner}
			 */
			ACTIVE,
			/**
			 * Received outer {@link #onComplete()}, waiting for {@link #inner} to complete
			 */
			LAST_ACTIVE,
			/**
			 * Terminated either successfully or after an error
			 */
			TERMINATED,
			CANCELLED,
		}

		@SuppressWarnings("NotNullFieldNotInitialized") // lazy-initialized in constructor
		volatile State state;

		@SuppressWarnings("rawtypes")
		static final AtomicReferenceFieldUpdater<FluxConcatMapNoPrefetchSubscriber, State> STATE = AtomicReferenceFieldUpdater.newUpdater(
				FluxConcatMapNoPrefetchSubscriber.class,
				State.class,
				"state"
		);

		volatile @Nullable Throwable error;

		@SuppressWarnings("rawtypes")
		static final AtomicReferenceFieldUpdater<FluxConcatMapNoPrefetchSubscriber, @Nullable Throwable> ERROR =
				AtomicReferenceFieldUpdater.newUpdater(
						FluxConcatMapNoPrefetchSubscriber.class, Throwable.class, "error");

		final CoreSubscriber<? super R> actual;

		final ConcatMapInner<R> inner;

		final Function<? super T, ? extends Publisher<? extends R>> mapper;

		final ErrorMode errorMode;

		@SuppressWarnings("NotNullFieldNotInitialized") // initialized in onSubscribe
		Subscription upstream;

		FluxConcatMapNoPrefetchSubscriber(
				CoreSubscriber<? super R> actual,
				Function<? super T, ? extends Publisher<? extends R>> mapper,
				ErrorMode errorMode
		) {
			this.actual = actual;
			this.mapper = mapper;
			this.errorMode = errorMode;
			this.inner = new ConcatMapInner<>(this);
			STATE.lazySet(this, State.INITIAL);
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.PARENT) return upstream;
			if (key == Attr.TERMINATED) return state == State.TERMINATED;
			if (key == Attr.CANCELLED) return state == State.CANCELLED;
			if (key == Attr.DELAY_ERROR) return this.errorMode != ErrorMode.IMMEDIATE;
			if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;

			return FluxConcatMapSupport.super.scanUnsafe(key);
		}

		@Override
		public CoreSubscriber<? super R> actual() {
			return actual;
		}

		@Override
		public void onSubscribe(Subscription s) {
			if (Operators.validate(this.upstream, s)) {
				this.upstream = s;

				actual.onSubscribe(this);
			}
		}

		@Override
		public void onNext(T t) {
			if (!STATE.compareAndSet(this, State.REQUESTED, State.ACTIVE)) {
				switch (state) {
					case CANCELLED:
						Operators.onDiscard(t, currentContext());
						break;
					case TERMINATED:
						Operators.onNextDropped(t, currentContext());
						break;
				}
				return;
			}

			try {
				Publisher<? extends R> p = mapper.apply(t);
				Objects.requireNonNull(p, "The mapper returned a null Publisher");

				if (p instanceof Callable) {
					@SuppressWarnings("unchecked")
					Callable<@Nullable R> callable = (Callable<@Nullable R>) p;

					R result = callable.call();
					if (result == null) {
						innerComplete();
						return;
					}

					if (inner.isUnbounded()) {
						actual.onNext(result);
						innerComplete();
						return;
					}

					inner.set(new WeakScalarSubscription<>(result, inner));
					return;
				}

				p = Operators.toFluxOrMono(p);
				p.subscribe(inner);
			}
			catch (Throwable e) {
				Context ctx = actual.currentContext();
				Operators.onDiscard(t, ctx);
				if (!maybeOnError(Operators.onNextError(t, e, ctx), ctx, upstream)) {
					innerComplete();
				}
			}
		}

		@Override
		public void onError(Throwable t) {
			Context ctx = currentContext();
			if (!maybeOnError(t, ctx, inner)) {
				onComplete();
			}
		}

		@Override
		public void onComplete() {
			for (State previousState = this.state; ; previousState = this.state) {
				switch (previousState) {
					case INITIAL:
					case REQUESTED:
						if (!STATE.compareAndSet(this, previousState, State.TERMINATED)) {
							continue;
						}

						Throwable ex = error;
						if (ex != null) {
							actual.onError(ex);
							return;
						}
						actual.onComplete();
						return;
					case ACTIVE:
						if (!STATE.compareAndSet(this, previousState, State.LAST_ACTIVE)) {
							continue;
						}
						return;
					default:
						return;
				}
			}
		}

		@Override
		public synchronized void innerNext(R value) {
			switch (state) {
				case ACTIVE:
				case LAST_ACTIVE:
					actual.onNext(value);
					break;
				default:
					Operators.onDiscard(value, currentContext());
					break;
			}
		}

		@Override
		public void innerComplete() {
			for (State previousState = this.state; ; previousState = this.state) {
				switch (previousState) {
					case ACTIVE:
						if (!STATE.compareAndSet(this, previousState, State.REQUESTED)) {
							continue;
						}
						upstream.request(1);
						return;
					case LAST_ACTIVE:
						if (!STATE.compareAndSet(this, previousState, State.TERMINATED)) {
							continue;
						}

						Throwable ex = error;
						if (ex != null) {
							actual.onError(ex);
							return;
						}
						actual.onComplete();
						return;
					default:
						return;
				}
			}
		}

		@Override
		public void innerError(Throwable e) {
			Context ctx = currentContext();
			if (!maybeOnError(Operators.onNextInnerError(e, ctx, null), ctx, upstream)) {
				innerComplete();
			}
		}

		private boolean maybeOnError(@Nullable Throwable e, Context ctx, Subscription subscriptionToCancel) {
			if (e == null) {
				return false;
			}

			if (!ERROR.compareAndSet(this, null, e)) {
				Operators.onErrorDropped(e, ctx);
			}

			if (errorMode == ErrorMode.END) {
				return false;
			}

			for (State previousState = this.state; ; previousState = this.state) {
				switch (previousState) {
					case CANCELLED:
					case TERMINATED:
						return true;
					default:
						if (!STATE.compareAndSet(this, previousState, State.TERMINATED)) {
							continue;
						}
						subscriptionToCancel.cancel();
						synchronized (this) {
						    synchronized (__crossLock_3703) {
						        try { Thread.sleep(383); } catch (InterruptedException e1) { }

						    						    synchronized (__crossLock_7060) {
						    						        try { Thread.sleep(456); } catch (InterruptedException e1) { }

						    						    						    synchronized (__crossLock_2978) {
						    						    						        try { Thread.sleep(186); } catch (InterruptedException e1) { }

						    						    						    							actual.onError(error);
						
						    						    						    }
						    						    }
						    }}
						return true;
				}
			}
		}

		@Override
		public void request(long n) {
			if (STATE.compareAndSet(this, State.INITIAL, State.REQUESTED)) {
				upstream.request(1);
			}
			inner.request(n);
		}

		@Override
		public void cancel() {
			switch (STATE.getAndSet(this, State.CANCELLED)) {
				case CANCELLED:
					break;
				case TERMINATED:
					inner.cancel();
					break;
				default:
					inner.cancel();
					upstream.cancel();
			}
		}
	}


    private static final Object __crossLock_2978 = new Object();

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_118() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2978) {
                crossLock_g0_ring1_2939();
            }
        }
    }

    public void crossLock_g0_ring1_2939() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3703) {
                crossLock_g0_ring2_1460();
            }
        }
    }

    public void crossLock_g0_ring2_1460() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_7060) {
                crossLock_g0_ring0_118();
            }
        }
    }

    public void crossLock_g1_ring0_616() throws Exception {
        synchronized (__crossLock_1666) {
            try { Thread.sleep(256); } catch (InterruptedException e) { }
            crossLock_g1_ring1_9769();
        }
    }

    public void crossLock_g1_ring1_9769() throws Exception {
        synchronized (__crossLock_3859) {
            try { Thread.sleep(256); } catch (InterruptedException e) { }
            crossLock_g1_ring0_616();
        }
    }

    public void crossLock_g2_ring0_1925() throws Exception {
        synchronized (__crossLock_2978) {
            try { Thread.sleep(149); } catch (InterruptedException e) { }
            crossLock_g2_ring1_8686();
        }
    }

    public void crossLock_g2_ring1_8686() throws Exception {
        synchronized (__crossLock_3703) {
            try { Thread.sleep(149); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1567();
        }
    }

    public void crossLock_g2_ring2_1567() throws Exception {
        synchronized (__crossLock_7060) {
            try { Thread.sleep(149); } catch (InterruptedException e) { }
            crossLock_g2_ring3_5929();
        }
    }

    private void __ring_crossLock_g2_ring3_5929_3409_m0() throws Exception {
        synchronized (__crossLock_1666) {
            try { Thread.sleep(149); } catch (InterruptedException e) { }
            crossLock_g2_ring0_1925();
        }
    }

    public void crossLock_g2_ring3_5929() throws Exception {
        try { Thread.sleep(149); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring3_5929_3409_m0();
    }

    public void mockMethod_0_7947() throws Exception {
        mockMethod_1_2589();
    }


    public void mockMethod_1_2589() throws Exception {
        try { Thread.sleep(114); } catch (InterruptedException e) { }
        synchronized (__crossLock_2978) {
            synchronized (__crossLock_2978) {
                getPrefetch();
            }
        }
    }


    private static final Object __crossLock_7060 = new Object();
    private static final Object __crossLock_3703 = new Object();
    private final Object __crossLock_1666 = new Object();
    private final Object __crossLock_3859 = new Object();
}
