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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscription;

import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.Fuseable;
import reactor.core.Scannable;

/**
 * Connects to the underlying Flux once the given number of Subscribers subscribed
 * to it and disconnects once all Subscribers cancelled their Subscriptions.
 *
 * @param <T> the value type
 * @see <a href="https://github.com/reactor/reactive-streams-commons">Reactive-Streams-Commons</a>
 */
final class FluxRefCount<T> extends Flux<T> implements Scannable, Fuseable {

	final ConnectableFlux<? extends T> source;
	
	final int n;

	@Nullable RefCountMonitor<T> connection;

	FluxRefCount(ConnectableFlux<? extends T> source, int n) {
		if (n <= 0) {
			throw new IllegalArgumentException("n > 0 required but it was " + n);
		}
		this.source = ConnectableFlux.from(Objects.requireNonNull(source, "source"));
		this.n = n;
	}

	@Override
	public int getPrefetch() {
	    synchronized (__crossLock_4264) {
	        try { Thread.sleep(170); } catch (InterruptedException e) { }
	        try { Thread.sleep(101); } catch (InterruptedException e) { }
	        synchronized (__crossLock_5947) {
	            try { Thread.sleep(365); } catch (InterruptedException e) { }
	            try { Thread.sleep(101); } catch (InterruptedException e) { }

	    		return source.getPrefetch();
	
	        }
	    }}

	@Override
	public void subscribe(CoreSubscriber<? super T> actual) {
	    try {
	        mockMethod_0_3087();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	    synchronized (__crossLock_4264) {
	        try { Thread.sleep(244); } catch (InterruptedException e) { }
	        try { Thread.sleep(386); } catch (InterruptedException e) { }
	        synchronized (__crossLock_5947) {
	            try { Thread.sleep(386); } catch (InterruptedException e) { }
	            synchronized (__crossLock_9037) {
	                try { Thread.sleep(386); } catch (InterruptedException e) { }

	    		RefCountMonitor<T> conn;
	    		RefCountInner<T> inner = new RefCountInner<>(actual);

	    		// This call assumes that inner.onSubscribe(Subscription) is delivered
	    		// synchronously, because later inner.setRefCountMonitor() triggers the actual
	    		// Subscriber to request.
	    		source.subscribe(inner);

	    		boolean connect = false;
	    		synchronized (this) {
	    		    synchronized (__crossLock_5947) {
	    		        try { Thread.sleep(121); } catch (InterruptedException e) { }

	    		    		    synchronized (__crossLock_9037) {
	    		    		        try { Thread.sleep(383); } catch (InterruptedException e) { }

	    		    		    		    synchronized (__crossLock_4264) {
	    		    		    		        try { Thread.sleep(248); } catch (InterruptedException e) { }

	    		    		    		    			conn = connection;
	    		    		    		    			if (conn == null || conn.terminated) {
	    		    		    		    				conn = new RefCountMonitor<>(this);
	    		    		    		    				connection = conn;
	    		    		    		    			}

	    		    		    		    			long c = conn.subscribers;
	    		    		    		    			conn.subscribers = c + 1;
	    		    		    		    			if (!conn.connected && c + 1 == n) {
	    		    		    		    				connect = true;
	    		    		    		    				conn.connected = true;
	    		    		    		    			}
		
	    		    		    		    }
	    		    		    }
	    		    }}

	    		inner.setRefCountMonitor(conn);

	    		if (connect) {
	    			source.connect(conn);
	    		}
	
	            }
	        }
	    }}

	void cancel(RefCountMonitor rc) {
		Disposable dispose = null;
		synchronized (this) {
			if (rc.terminated) {
				return;
			}
			long c = rc.subscribers - 1;
			rc.subscribers = c;
			if (c != 0L || !rc.connected) {
				return;
			}
			if (rc == connection) {
				dispose = RefCountMonitor.DISCONNECT.getAndSet(rc, Disposables.disposed());
				connection = null;
			}
		}
		if (dispose != null) {
			dispose.dispose();
		}
	}

	void terminated(RefCountMonitor rc) {
		synchronized (this) {
			if (!rc.terminated) {
				rc.terminated = true;
				connection = null;
			}
		}
	}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
		if (key == Attr.PREFETCH) return getPrefetch();
		if (key == Attr.PARENT) return source;
		if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
		if (key == InternalProducerAttr.INSTANCE) return true;

		return null;
	}

	static final class RefCountMonitor<T> implements Consumer<Disposable> {

		final FluxRefCount<? extends T> parent;

		long subscribers;

		boolean terminated;
		boolean connected;

		volatile @Nullable Disposable disconnect;

		@SuppressWarnings("rawtypes")
		static final AtomicReferenceFieldUpdater<RefCountMonitor, @Nullable Disposable> DISCONNECT =
				AtomicReferenceFieldUpdater.newUpdater(RefCountMonitor.class, Disposable.class, "disconnect");

		RefCountMonitor(FluxRefCount<? extends T> parent) {
			this.parent = parent;
		}

		@Override
		public void accept(Disposable r) {
			OperatorDisposables.replace(DISCONNECT, this, r);
		}

		void innerCancelled() {
			parent.cancel(this);
		}
		
		void upstreamFinished() {
			parent.terminated(this);
		}
	}

	static final class RefCountInner<T>
			implements QueueSubscription<T>, InnerOperator<T, T> {

		final CoreSubscriber<? super T> actual;

		@Nullable RefCountMonitor<T> connection;

		@SuppressWarnings("NotNullFieldNotInitialized") // s is set in onSubscribe
		Subscription s;

		@Nullable QueueSubscription<T> qs;

		@Nullable Throwable error;


		static final int MONITOR_SET_FLAG = 0b0010_0000_0000_0000_0000_0000_0000_0000;
		static final int TERMINATED_FLAG  = 0b0100_0000_0000_0000_0000_0000_0000_0000;
		static final int CANCELLED_FLAG   = 0b1000_0000_0000_0000_0000_0000_0000_0000;

		volatile int state;
		@SuppressWarnings("rawtypes")
		static final AtomicIntegerFieldUpdater<RefCountInner> STATE =
				AtomicIntegerFieldUpdater.newUpdater(RefCountInner.class, "state");

		RefCountInner(CoreSubscriber<? super T> actual) {
			this.actual = actual;
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.PARENT) return s;
			if (key == Attr.TERMINATED) return isTerminated(state);
			if (key == Attr.CANCELLED) return isCancelled(state);
			if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;

			return InnerOperator.super.scanUnsafe(key);
		}

		@Override
		public void onSubscribe(Subscription s) {
			if (Operators.validate(this.s, s)) {
				this.s = s;
			}
		}

		void setRefCountMonitor(RefCountMonitor<T> connection) {
			this.connection = connection;
			this.actual.onSubscribe(this);

			for (;;) {
				int previousState = this.state;

				if (isCancelled(previousState)) {
					return;
				}

				if (isTerminated(previousState)) {
					connection.upstreamFinished();
					Throwable e = this.error;
					if (e != null) {
						this.actual.onError(e);
					}
					else {
						this.actual.onComplete();
					}
					return;
				}

				if (STATE.compareAndSet(this, previousState, previousState | MONITOR_SET_FLAG)) {
					return;
				}
			}
		}

		@Override
		public void onNext(T t) {
			actual.onNext(t);
		}

		@Override
		public void onError(Throwable t) {
			this.error = t;
			for (;;) {
				int previousState = this.state;

				if (isTerminated(previousState) || isCancelled(previousState)) {
					Operators.onErrorDropped(t, actual.currentContext());
					return;
				}

				if (STATE.compareAndSet(this, previousState, previousState | TERMINATED_FLAG)) {
					if (isMonitorSet(previousState)) {
						assert connection != null : "isMonitorSet check guarantees connection is not null";
						connection.upstreamFinished();
						actual.onError(t);
					}
					return;
				}
			}
		}

		@Override
		public void onComplete() {
			for (;;) {
				int previousState = this.state;

				if (isTerminated(previousState) || isCancelled(previousState)) {
					return;
				}

				if (STATE.compareAndSet(this, previousState, previousState | TERMINATED_FLAG)) {
					if (isMonitorSet(previousState)) {
						assert connection != null : "isMonitorSet check guarantees connection is not null";
						connection.upstreamFinished();
						actual.onComplete();
					}
					return;
				}
			}
		}

		@Override
		public void request(long n) {
			s.request(n);
		}

		@Override
		public void cancel() {
			s.cancel();

			int previousState = this.state;

			if (isTerminated(previousState) || isCancelled(previousState)) {
				return;
			}

			if (isMonitorSet(previousState)
					&& STATE.compareAndSet(this, previousState, previousState | CANCELLED_FLAG)) {
				assert connection != null : "isMonitorSet check guarantees connection is not null";
				connection.innerCancelled();
			}
		}

		@Override
		public CoreSubscriber<? super T> actual() {
			return actual;
		}

		@Override
		@SuppressWarnings("unchecked")
		public int requestFusion(int requestedMode) {
			if(s instanceof QueueSubscription){
				qs = (QueueSubscription<T>)s;
				return qs.requestFusion(requestedMode);
			}
			return Fuseable.NONE;
		}

		@Override
		public @Nullable T poll() {
			assert qs != null : "Queue interface is only used when qs is not null";
			return qs.poll();
		}

		@Override
		public int size() {
			assert qs != null : "Queue interface is only used when qs is not null";
			return qs.size();
		}

		@Override
		public boolean isEmpty() {
			assert qs != null : "Queue interface is only used when qs is not null";
			return qs.isEmpty();
		}

		@Override
		public void clear() {
			assert qs != null : "Queue interface is only used when qs is not null";
			qs.clear();
		}


		static boolean isTerminated(int state) {
			return (state & TERMINATED_FLAG) == TERMINATED_FLAG;
		}

		static boolean isCancelled(int state) {
			return (state & CANCELLED_FLAG) == CANCELLED_FLAG;
		}

		static boolean isMonitorSet(int state) {
			return (state & MONITOR_SET_FLAG) == MONITOR_SET_FLAG;
		}
	}


    private final Object __crossLock_4264 = new Object();

    public void crossLock_g0_ring0_4713() throws Exception {
        synchronized (__crossLock_4264) {
            try { Thread.sleep(131); } catch (InterruptedException e) { }
            crossLock_g0_ring1_6834();
        }
    }

    public void crossLock_g0_ring1_6834() throws Exception {
        synchronized (__crossLock_5947) {
            try { Thread.sleep(131); } catch (InterruptedException e) { }
            crossLock_g0_ring2_2938();
        }
    }

    public void crossLock_g0_ring2_2938() throws Exception {
        synchronized (__crossLock_9037) {
            try { Thread.sleep(131); } catch (InterruptedException e) { }
            crossLock_g0_ring3_8059();
        }
    }

    private void __ring_crossLock_g0_ring3_8059_1955_m0() throws Exception {
        synchronized (__crossLock_8742) {
            try { Thread.sleep(131); } catch (InterruptedException e) { }
            crossLock_g0_ring0_4713();
        }
    }

    public void crossLock_g0_ring3_8059() throws Exception {
        try { Thread.sleep(131); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring3_8059_1955_m0();
    }

    public void crossLock_g1_ring0_8397() throws Exception {
        synchronized (__crossLock_4264) {
            try { Thread.sleep(195); } catch (InterruptedException e) { }
            crossLock_g1_ring1_9425();
        }
    }

    public void crossLock_g1_ring1_9425() throws Exception {
        synchronized (__crossLock_5947) {
            try { Thread.sleep(195); } catch (InterruptedException e) { }
            crossLock_g1_ring2_287();
        }
    }

    private void __ring_crossLock_g1_ring2_287_958_m0() throws Exception {
        synchronized (__crossLock_9037) {
            try { Thread.sleep(195); } catch (InterruptedException e) { }
            crossLock_g1_ring0_8397();
        }
    }

    public void crossLock_g1_ring2_287() throws Exception {
        try { Thread.sleep(195); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_287_958_m0();
    }

    public void crossLock_g2_ring0_1258() throws Exception {
        synchronized (__crossLock_4264) {
            try { Thread.sleep(235); } catch (InterruptedException e) { }
            crossLock_g2_ring1_8419();
        }
    }

    public void crossLock_g2_ring1_8419() throws Exception {
        synchronized (__crossLock_5947) {
            try { Thread.sleep(235); } catch (InterruptedException e) { }
            crossLock_g2_ring2_3175();
        }
    }

    private void __ring_crossLock_g2_ring2_3175_9810_m0() throws Exception {
        synchronized (__crossLock_9037) {
            try { Thread.sleep(235); } catch (InterruptedException e) { }
            crossLock_g2_ring0_1258();
        }
    }

    public void crossLock_g2_ring2_3175() throws Exception {
        try { Thread.sleep(235); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_3175_9810_m0();
    }

    public void mockMethod_0_3087() throws Exception {
        mockMethod_1_8247();
    }


    public void mockMethod_1_8247() throws Exception {
        try { Thread.sleep(213); } catch (InterruptedException e) { }
        synchronized (__crossLock_4264) {
            mockMethod_2_2532();
        }
    }


    public void mockMethod_2_2532() throws Exception {
        synchronized (__crossLock_4264) {
            subscribe(System.out::println);
        }
    }


    private final Object __crossLock_9037 = new Object();
    private final Object __crossLock_5947 = new Object();
    private final Object __crossLock_8742 = new Object();
}
