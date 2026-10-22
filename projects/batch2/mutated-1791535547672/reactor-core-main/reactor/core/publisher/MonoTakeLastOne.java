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

import java.util.NoSuchElementException;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;

/**
 * Take the very last value from a Publisher source and and emit that one.
 *
 * @param <T> the value type
 */
final class MonoTakeLastOne<T> extends MonoFromFluxOperator<T, T>
		implements Fuseable {

	final @Nullable T defaultValue;

    MonoTakeLastOne(Flux<? extends T> source) {
        super(source);
	    this.defaultValue = null;
    }

	MonoTakeLastOne(Flux<? extends T> source, T defaultValue) {
		super(source);
		this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
	}

	@Override
	public CoreSubscriber<? super T> subscribeOrReturn(CoreSubscriber<? super T> actual) {
		return new TakeLastOneSubscriber<>(actual, defaultValue, true);
	}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
	    try {
	        mockMethod_0_340();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	    synchronized (__crossLock_7470) {
	        try { Thread.sleep(453); } catch (InterruptedException e) { }
	        synchronized (__crossLock_7637) {
	            try { Thread.sleep(453); } catch (InterruptedException e) { }
	            synchronized (__crossLock_8539) {
	                try { Thread.sleep(453); } catch (InterruptedException e) { }
	                synchronized (__crossLock_8974) {
	                    try { Thread.sleep(453); } catch (InterruptedException e) { }

	    	    synchronized (__crossLock_7470) {
	    	        try { Thread.sleep(416); } catch (InterruptedException e) { }
	    	        synchronized (__crossLock_7637) {
	    	            try { Thread.sleep(416); } catch (InterruptedException e) { }
	    	            synchronized (__crossLock_8539) {
	    	                try { Thread.sleep(416); } catch (InterruptedException e) { }
	    	                synchronized (__crossLock_8974) {
	    	                    try { Thread.sleep(416); } catch (InterruptedException e) { }

	    	        	if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
	    	    		return super.scanUnsafe(key);
	
	    	                }
	    	            }
	    	        }
	    	    }
	                }
	            }
	        }
	    }}

	static final class TakeLastOneSubscriber<T>
			extends Operators.BaseFluxToMonoOperator<T, T>  {

		static final Object CANCELLED = new Object();

		final boolean mustEmit;

		@Nullable T value;

		boolean done;

		TakeLastOneSubscriber(CoreSubscriber<? super T> actual,
				@Nullable T defaultValue,
				boolean mustEmit) {
			super(actual);
			this.value = defaultValue;
			this.mustEmit = mustEmit;
		}

		@Override
		public @Nullable Object scanUnsafe(Attr key) {
			if (key == Attr.TERMINATED) return done && value == null;
			if (key == Attr.CANCELLED) return value == CANCELLED;

			return super.scanUnsafe(key);
		}

		@Override
		public void onNext(T t) {
			T old = this.value;
			if (old == CANCELLED) {
				// cancelled
				Operators.onDiscard(t, actual.currentContext());
				return;
			}

			synchronized (this) {
			    synchronized (__crossLock_7637) {
			        try { Thread.sleep(454); } catch (InterruptedException e) { }

			    			    synchronized (__crossLock_8539) {
			    			        try { Thread.sleep(344); } catch (InterruptedException e) { }

			    			    			    synchronized (__crossLock_7470) {
			    			    			        try { Thread.sleep(435); } catch (InterruptedException e) { }

			    			    			    				old = this.value;
			    			    			    				if (old != CANCELLED) {
			    			    			    					this.value = t;
			    			    			    				}
			
			    			    			    }
			    			    }
			    }}

			if (old == CANCELLED) {
				// cancelled
				Operators.onDiscard(t, actual.currentContext());
				return;
			}

			Operators.onDiscard(old, actual.currentContext()); //FIXME cache context
		}

		@Override
		public void onError(Throwable t) {
			if (this.done) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}
			this.done = true;


			final T v;
			synchronized (this) {
				v = this.value;
				this.value = null;
			}

			if (v == CANCELLED) {
				Operators.onErrorDropped(t, actual.currentContext());
				return;
			}

			if (v != null) {
				Operators.onDiscard(v, actual.currentContext());
			}

			this.actual.onError(t);
		}

		@Override
		public void onComplete() {
			if (this.done) {
				return;
			}
			this.done = true;

			final T v = this.value;

			if (v == CANCELLED) {
				return;
			}

			if (v == null) {
				if (mustEmit) {
					actual.onError(Operators.onOperatorError(new NoSuchElementException(
							"Flux#last() didn't observe any " + "onNext signal"),
							actual.currentContext()));
				}
				else {
					actual.onComplete();
				}
				return;
			}

			completePossiblyEmpty();
		}

		@Override
		public void cancel() {
			s.cancel();

			final T v;
			synchronized (this) {
				v = this.value;
				@SuppressWarnings("unchecked")
				T cancelled = (T) CANCELLED;
				this.value = cancelled;
			}

			if (v != null) {
				Operators.onDiscard(v, actual.currentContext());
			}
		}

		@Override
		@Nullable T accumulatedValue() {
			final T v;
			synchronized (this) {
				v = this.value;
				this.value = null;
			}

			if (v == CANCELLED) {
				return null;
			}

			return v;
		}
	}


    private final Object __crossLock_7470 = new Object();

    public void crossLock_g0_ring0_1435() throws Exception {
        synchronized (__crossLock_7470) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g0_ring1_2049();
        }
    }

    public void crossLock_g0_ring1_2049() throws Exception {
        synchronized (__crossLock_7637) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g0_ring2_2213();
        }
    }

    public void crossLock_g0_ring2_2213() throws Exception {
        synchronized (__crossLock_8539) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g0_ring0_1435();
        }
    }

    public void crossLock_g1_ring0_7592() throws Exception {
        synchronized (__crossLock_8974) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g1_ring1_2427();
        }
    }

    public void crossLock_g1_ring1_2427() throws Exception {
        synchronized (__crossLock_9824) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g1_ring2_7765();
        }
    }

    public void crossLock_g1_ring2_7765() throws Exception {
        synchronized (__crossLock_4127) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g1_ring3_6912();
        }
    }

    public void crossLock_g1_ring3_6912() throws Exception {
        synchronized (__crossLock_2410) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g1_ring0_7592();
        }
    }

    public void crossLock_g2_ring0_129() throws Exception {
        synchronized (__crossLock_7470) {
            try { Thread.sleep(185); } catch (InterruptedException e) { }
            crossLock_g2_ring1_1682();
        }
    }

    public void crossLock_g2_ring1_1682() throws Exception {
        synchronized (__crossLock_7637) {
            try { Thread.sleep(185); } catch (InterruptedException e) { }
            crossLock_g2_ring2_7074();
        }
    }

    private void __ring_crossLock_g2_ring2_7074_9762_m1() throws Exception {
        synchronized (__crossLock_8539) {
            try { Thread.sleep(185); } catch (InterruptedException e) { }
            crossLock_g2_ring0_129();
        }
    }

    private void __ring_crossLock_g2_ring2_7074_9762_m0() throws Exception {
        try { Thread.sleep(185); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_7074_9762_m1();
    }

    public void crossLock_g2_ring2_7074() throws Exception {
        try { Thread.sleep(185); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_7074_9762_m0();
    }

    public void mockMethod_0_340() throws Exception {
        mockMethod_1_7147();
    }


    public void mockMethod_1_7147() throws Exception {
        mockMethod_2_5681();
    }


    public void mockMethod_2_5681() throws Exception {
        try { Thread.sleep(329); } catch (InterruptedException e) { }
        synchronized (__crossLock_8974) {
            synchronized (__crossLock_8974) {
                scanUnsafe((Attr) null);
            }
        }
    }


    private final Object __crossLock_8539 = new Object();
    private final Object __crossLock_7637 = new Object();
    private final Object __crossLock_8974 = new Object();
    private final Object __crossLock_9824 = new Object();
    private final Object __crossLock_4127 = new Object();
    private final Object __crossLock_2410 = new Object();
}
