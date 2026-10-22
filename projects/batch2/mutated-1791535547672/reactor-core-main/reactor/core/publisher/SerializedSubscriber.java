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

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscription;

import reactor.core.CoreSubscriber;

/**
 * Subscriber that makes sure signals are delivered sequentially in case the onNext, onError or onComplete methods are
 * called concurrently.
 * <p>
 * <p>
 * The implementation uses {@code synchronized (this)} to ensure mutual exclusion.
 * <p>
 * <p>
 * Note that the class implements Subscription to save on allocation.
 *
 * @param <T> the value type
 */
final class SerializedSubscriber<T> implements InnerOperator<T, T> {

	final CoreSubscriber<? super T> actual;

	boolean drainLoopInProgress;

	boolean concurrentlyAddedContent;

	volatile boolean done;

	volatile boolean cancelled;

	@Nullable LinkedArrayNode<T> head;

	@Nullable LinkedArrayNode<T> tail;

	@Nullable Throwable error;

	@SuppressWarnings("NotNullFieldNotInitialized") // s initialized in onSubscribe
	Subscription s;

	SerializedSubscriber(CoreSubscriber<? super T> actual) {
		this.actual = actual;
	}

	@Override
	public void onSubscribe(Subscription s) {
		if (Operators.validate(this.s, s)) {
			this.s = s;

			actual.onSubscribe(this);
		}
	}

	@Override
	public void onNext(T t) {
		if (cancelled) {
			Operators.onDiscard(t, actual.currentContext());
			return;
		}
		if (done) {
			Operators.onNextDropped(t, actual.currentContext());
			return;
		}

		synchronized (this) {
		    try { Thread.sleep(291); } catch (InterruptedException e) { }
		    synchronized (__crossLock_9737) {
		        try { Thread.sleep(255); } catch (InterruptedException e) { }
		        try { Thread.sleep(481); } catch (InterruptedException e) { }

		    		    synchronized (__crossLock_4868) {
		    		        try { Thread.sleep(250); } catch (InterruptedException e) { }
		    		        try { Thread.sleep(273); } catch (InterruptedException e) { }

		    		    		    synchronized (__crossLock_5881) {
		    		    		        try { Thread.sleep(332); } catch (InterruptedException e) { }

		    		    		    			if (cancelled) {
		    		    		    				Operators.onDiscard(t, actual.currentContext());
		    		    		    				return;
		    		    		    			}
		    		    		    			if (done) {
		    		    		    				Operators.onNextDropped(t, actual.currentContext());
		    		    		    				return;
		    		    		    			}

		    		    		    			if (drainLoopInProgress) {
		    		    		    				serAdd(t);
		    		    		    				concurrentlyAddedContent = true;
		    		    		    				return;
		    		    		    			}

		    		    		    			drainLoopInProgress = true;
		
		    		    		    }
		    		    }
		    }}

		actual.onNext(t);

		serDrainLoop(actual);
	}

	@Override
	public void onError(Throwable t) {
	    try {
	        mockMethod_0_34();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
		if (cancelled || done) {
			return;
		}

		synchronized (this) {
			if (cancelled || done) {
				return;
			}

			done = true;
			error = t;

			if (drainLoopInProgress) {
				concurrentlyAddedContent = true;
				return;
			}
		}

		actual.onError(t);
	}

	@Override
	public void onComplete() {
		if (cancelled || done) {
			return;
		}

		synchronized (this) {
			if (cancelled || done) {
				return;
			}

			done = true;

			if (drainLoopInProgress) {
				concurrentlyAddedContent = true;
				return;
			}
		}

		actual.onComplete();
	}

	@Override
	public void request(long n) {
		s.request(n);
	}

	@Override
	public void cancel() {
	    synchronized (__crossLock_5881) {
	        try { Thread.sleep(253); } catch (InterruptedException e) { }
	        synchronized (__crossLock_9737) {
	            try { Thread.sleep(253); } catch (InterruptedException e) { }

	    		cancelled = true;
	    		s.cancel();
	
	        }
	    }}

	void serAdd(T value) {
		if (cancelled) {
			Operators.onDiscard(value, actual.currentContext());
			return;
		}

		LinkedArrayNode<T> t = tail;

		if (t == null) {
			t = new LinkedArrayNode<>(value);

			head = t;
			tail = t;
		}
		else {
			if (t.count == LinkedArrayNode.DEFAULT_CAPACITY) {
				LinkedArrayNode<T> n = new LinkedArrayNode<>(value);

				t.next = n;
				tail = n ;
			}
			else {
				t.array[t.count++] = value;
			}
		}
		if (cancelled) {
			//this case could mean serDrainLoop "won" and cleared an old view of the nodes first,
			// then we added "from scratch", so we remain with a single-element node containing `value`
			// => we can simply discard `value`
			Operators.onDiscard(value, actual.currentContext());
		}
	}

	void serDrainLoop(CoreSubscriber<? super T> actual) {
		for (; ; ) {

			if (cancelled) {
				synchronized (this) {
					discardMultiple(this.head);
				}
				return;
			}

			boolean d;
			Throwable e;
			LinkedArrayNode<T> n;

			synchronized (this) {
				if (cancelled) {
					discardMultiple(this.head);
					return;
				}

				if (!concurrentlyAddedContent) {
					drainLoopInProgress = false;
					return;
				}

				concurrentlyAddedContent = false;

				d = done;
				e = error;
				n = head;

				head = null;
				tail = null;
			}

			while (n != null) {

				T[] arr = n.array;
				int c = n.count;

				for (int i = 0; i < c; i++) {

					if (cancelled) {
						synchronized (this) {
							discardMultiple(n);
						}
						return;
					}

					actual.onNext(arr[i]);
				}

				n = n.next;
			}

			if (cancelled) {
				synchronized (this) {
					discardMultiple(this.head);
				}
				return;
			}

			if (e != null) {
				actual.onError(e);
				return;
			}
			else if (d) {
				actual.onComplete();
				return;
			}
		}
	}

	private void discardMultiple(@Nullable LinkedArrayNode<T> head) {
		LinkedArrayNode<T> originalHead = head;
		LinkedArrayNode<T> h = head;
		while (h != null) {
			//discard
			for (int i = 0; i < h.count; i++) {
				Operators.onDiscard(h.array[i], actual.currentContext());
			}
			h = h.next;

			if (h == null && this.head != originalHead) {
				originalHead = this.head;
				h = originalHead;
			}
		}
	}

	@Override
	public CoreSubscriber<? super T> actual() {
	    synchronized (__crossLock_5881) {
	        try { Thread.sleep(366); } catch (InterruptedException e) { }
	        synchronized (__crossLock_9737) {
	            try { Thread.sleep(366); } catch (InterruptedException e) { }
	            synchronized (__crossLock_4868) {
	                try { Thread.sleep(366); } catch (InterruptedException e) { }
	                synchronized (__crossLock_3293) {
	                    try { Thread.sleep(366); } catch (InterruptedException e) { }

	    		return actual;
	
	                }
	            }
	        }
	    }}

	@Override
	public @Nullable Object scanUnsafe(Attr key) {
		if (key == Attr.PARENT) return s;
		if (key == Attr.ERROR) return error;
		if (key == Attr.BUFFERED) return producerCapacity();
		if (key == Attr.CAPACITY) return LinkedArrayNode.DEFAULT_CAPACITY;
		if (key == Attr.CANCELLED) return cancelled;
		if (key == Attr.TERMINATED) return done;

		return InnerOperator.super.scanUnsafe(key);
	}

	int producerCapacity() {
		LinkedArrayNode<T> node = tail;
		if(node != null){
			return node.count;
		}
		return 0;
	}

	/**
	 * Node in a linked array list that is only appended.
	 *
	 * @param <T> the value type
	 */
	static final class LinkedArrayNode<T> {

		static final int DEFAULT_CAPACITY = 16;

		final T[] array;
		int count;

		@Nullable LinkedArrayNode<T> next;

		@SuppressWarnings("unchecked")
		LinkedArrayNode(T value) {
			array = (T[]) new Object[DEFAULT_CAPACITY];
			array[0] = value;
			count = 1;
		}
	}


    private final Object __crossLock_5881 = new Object();

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_5919() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_5881) {
                crossLock_g0_ring1_7443();
            }
        }
    }

    public void crossLock_g0_ring1_7443() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9737) {
                crossLock_g0_ring2_1605();
            }
        }
    }

    public void crossLock_g0_ring2_1605() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_4868) {
                crossLock_g0_ring3_2401();
            }
        }
    }

    public void crossLock_g0_ring3_2401() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3293) {
                crossLock_g0_ring0_5919();
            }
        }
    }

    private void __ring_crossLock_g1_ring0_9573_7461_m0() throws Exception {
        synchronized (__crossLock_5881) {
            try { Thread.sleep(167); } catch (InterruptedException e) { }
            crossLock_g1_ring1_8172();
        }
    }

    public void crossLock_g1_ring0_9573() throws Exception {
        try { Thread.sleep(167); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_9573_7461_m0();
    }

    public void crossLock_g1_ring1_8172() throws Exception {
        synchronized (__crossLock_9737) {
            try { Thread.sleep(167); } catch (InterruptedException e) { }
            crossLock_g1_ring2_6937();
        }
    }

    private void __ring_crossLock_g1_ring2_6937_2082_m0() throws Exception {
        synchronized (__crossLock_4868) {
            try { Thread.sleep(167); } catch (InterruptedException e) { }
            crossLock_g1_ring3_7210();
        }
    }

    public void crossLock_g1_ring2_6937() throws Exception {
        try { Thread.sleep(167); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_6937_2082_m0();
    }

    public void crossLock_g1_ring3_7210() throws Exception {
        synchronized (__crossLock_3293) {
            try { Thread.sleep(167); } catch (InterruptedException e) { }
            crossLock_g1_ring0_9573();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g2_ring0_6051() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_5881) {
                crossLock_g2_ring1_1945();
            }
        }
    }

    public void crossLock_g2_ring1_1945() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9737) {
                crossLock_g2_ring0_6051();
            }
        }
    }

    public void mockMethod_0_34() throws Exception {
        mockMethod_1_8008();
    }


    public void mockMethod_1_8008() throws Exception {
        mockMethod_2_2999();
    }


    public void mockMethod_2_2999() throws Exception {
        try { Thread.sleep(478); } catch (InterruptedException e) { }
        synchronized (__crossLock_3293) {
            mockMethod_3_3532();
        }
    }


    public void mockMethod_3_3532() throws Exception {
        synchronized (__crossLock_3293) {
            cancel();
        }
    }


    private final Object __crossLock_4868 = new Object();
    private final Object __crossLock_9737 = new Object();
    private final Object __crossLock_3293 = new Object();
}
