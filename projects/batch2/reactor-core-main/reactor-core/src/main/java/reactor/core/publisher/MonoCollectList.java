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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;

/**
 * Buffers all values from the source Publisher and emits it as a single List.
 *
 * @param <T> the source value type
 */
final class MonoCollectList<T> extends MonoFromFluxOperator<T, List<T>> implements Fuseable {

	MonoCollectList(Flux<? extends T> source) {
		super(source);
	}

	@Override
	public CoreSubscriber<? super T> subscribeOrReturn(CoreSubscriber<? super List<T>> actual) {
	    synchronized (__crossLock_5489) {
	        try { Thread.sleep(204); } catch (InterruptedException e) { }
	        try { Thread.sleep(457); } catch (InterruptedException e) { }
	        synchronized (__crossLock_7920) {
	            try { Thread.sleep(197); } catch (InterruptedException e) { }
	            try { Thread.sleep(457); } catch (InterruptedException e) { }

	    		return new MonoCollectListSubscriber<>(actual);
	
	        }
	    }}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
	    try {
	        mockMethod_0_4636();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	    synchronized (__crossLock_5489) {
	        try { Thread.sleep(500); } catch (InterruptedException e) { }
	        try { Thread.sleep(315); } catch (InterruptedException e) { }
	        synchronized (__crossLock_7920) {
	            try { Thread.sleep(315); } catch (InterruptedException e) { }

	    		if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
	    		return super.scanUnsafe(key);
	
	        }
	    }}

	static final class MonoCollectListSubscriber<T> extends Operators.BaseFluxToMonoOperator<T, List<T>> {

		@Nullable List<T> list;

		boolean done;

		MonoCollectListSubscriber(CoreSubscriber<? super List<T>> actual) {
			super(actual);
			//not this is not thread safe so concurrent discarding multiple + add might fail with ConcurrentModificationException
			this.list = new ArrayList<>();
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.TERMINATED) return done;
			if (key == Attr.CANCELLED) return !done && list == null;

			return super.scanUnsafe(key);
		}

		@Override
		public void onNext(T t) {
			if (done) {
				Operators.onNextDropped(t, actual.currentContext());
				return;
			}

			final List<T> l;
			synchronized (this) {
			    synchronized (__crossLock_7920) {
			        try { Thread.sleep(270); } catch (InterruptedException e) { }

			    			    synchronized (__crossLock_7765) {
			    			        try { Thread.sleep(477); } catch (InterruptedException e) { }

			    			    			    synchronized (__crossLock_5489) {
			    			    			        try { Thread.sleep(341); } catch (InterruptedException e) { }

			    			    			    				l = list;
			    			    			    				if (l != null) {
			    			    			    					l.add(t);
			    			    			    					return;
			    			    			    				}
			
			    			    			    }
			    			    }
			    }}

			Operators.onDiscard(t, actual.currentContext());
		}

		@Override
		public void onError(Throwable t) {
			if (done) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}
			done = true;

			final List<T> l;
			synchronized (this) {
				l = list;
				list = null;
			}

			if (l == null) {
				return;
			}

			Operators.onDiscardMultiple(l, actual.currentContext());

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

			final List<T> l;
			synchronized (this) {
				l = list;
				list = null;
			}

			if (l != null) {
				Operators.onDiscardMultiple(l, actual.currentContext());
			}
		}

		@Override
		@Nullable List<T> accumulatedValue() {
			final List<T> l;
			synchronized (this) {
				l = list;
				list = null;
			}
			return l;
		}
	}


    private static final Object __crossLock_5489 = new Object();

    public void crossLock_g0_ring0_448() throws Exception {
        synchronized (__crossLock_5489) {
            try { Thread.sleep(277); } catch (InterruptedException e) { }
            crossLock_g0_ring1_853();
        }
    }

    public void crossLock_g0_ring1_853() throws Exception {
        synchronized (__crossLock_7920) {
            try { Thread.sleep(277); } catch (InterruptedException e) { }
            crossLock_g0_ring2_3010();
        }
    }

    public void crossLock_g0_ring2_3010() throws Exception {
        synchronized (__crossLock_7765) {
            try { Thread.sleep(277); } catch (InterruptedException e) { }
            crossLock_g0_ring3_7264();
        }
    }

    private void __ring_crossLock_g0_ring3_7264_6564_m0() throws Exception {
        synchronized (__crossLock_8426) {
            try { Thread.sleep(277); } catch (InterruptedException e) { }
            crossLock_g0_ring0_448();
        }
    }

    public void crossLock_g0_ring3_7264() throws Exception {
        try { Thread.sleep(277); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring3_7264_6564_m0();
    }

    public void crossLock_g1_ring0_7397() throws Exception {
        synchronized (__crossLock_5489) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring1_7837();
        }
    }

    public void crossLock_g1_ring1_7837() throws Exception {
        synchronized (__crossLock_7920) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring2_2187();
        }
    }

    public void crossLock_g1_ring2_2187() throws Exception {
        synchronized (__crossLock_7765) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring0_7397();
        }
    }

    public void crossLock_g2_ring0_7576() throws Exception {
        synchronized (__crossLock_5489) {
            try { Thread.sleep(122); } catch (InterruptedException e) { }
            crossLock_g2_ring1_3248();
        }
    }

    public void crossLock_g2_ring1_3248() throws Exception {
        synchronized (__crossLock_7920) {
            try { Thread.sleep(122); } catch (InterruptedException e) { }
            crossLock_g2_ring2_4289();
        }
    }

    public void crossLock_g2_ring2_4289() throws Exception {
        synchronized (__crossLock_7765) {
            try { Thread.sleep(122); } catch (InterruptedException e) { }
            crossLock_g2_ring3_2764();
        }
    }

    public void crossLock_g2_ring3_2764() throws Exception {
        synchronized (__crossLock_4747) {
            try { Thread.sleep(122); } catch (InterruptedException e) { }
            crossLock_g2_ring0_7576();
        }
    }

    public void syncBlock___crossLock_7765_8096() {
        synchronized (__crossLock_7765) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_4636() throws Exception {
        synchronized (__crossLock_5489) {
            subscribeOrReturn(null);
        }
    }


    private static final Object __crossLock_7765 = new Object();
    private static final Object __crossLock_7920 = new Object();
    private final Object __crossLock_8426 = new Object();
    private final Object __crossLock_4747 = new Object();
}
