/*
 * Copyright (C) 2014 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.eventbus;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.j2objc.annotations.Weak;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import javax.annotation.CheckForNull;

/**
 * A subscriber method on a specific object, plus the executor that should be used for dispatching
 * events to it.
 *
 * <p>Two subscribers are equivalent when they refer to the same method on the same object (not
 * class). This property is used to ensure that no subscriber method is registered more than once.
 *
 * @author Colin Decker
 */
@ElementTypesAreNonnullByDefault
class Subscriber {

  /** Creates a {@code Subscriber} for {@code method} on {@code listener}. */
  static Subscriber create(EventBus bus, Object listener, Method method) {
    return isDeclaredThreadSafe(method)
        ? new Subscriber(bus, listener, method)
        : new SynchronizedSubscriber(bus, listener, method);
  }

  /** The event bus this subscriber belongs to. */
  @Weak private EventBus bus;

  /** The object with the subscriber method. */
  @VisibleForTesting final Object target;

  /** Subscriber method. */
  private final Method method;

  /** Executor to use for dispatching events to this subscriber. */
  private final Executor executor;

  private Subscriber(EventBus bus, Object target, Method method) {
    this.bus = bus;
    this.target = checkNotNull(target);
    this.method = method;
    method.setAccessible(true);

    this.executor = bus.executor();
  }

  /** Dispatches {@code event} to this subscriber using the proper executor. */
  final void dispatchEvent(Object event) {
    executor.execute(
        () -> {
          try {
            invokeSubscriberMethod(event);
          } catch (InvocationTargetException e) {
            bus.handleSubscriberException(e.getCause(), context(event));
          }
        });
  }

  /**
   * Invokes the subscriber method. This method can be overridden to make the invocation
   * synchronized.
   */
  @VisibleForTesting
  void invokeSubscriberMethod(Object event) throws InvocationTargetException {
    try {
      method.invoke(target, checkNotNull(event));
    } catch (IllegalArgumentException e) {
      throw new Error("Method rejected target/argument: " + event, e);
    } catch (IllegalAccessException e) {
      throw new Error("Method became inaccessible: " + event, e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Error) {
        throw (Error) e.getCause();
      }
      throw e;
    }
  }

  /** Gets the context for the given event. */
  private SubscriberExceptionContext context(Object event) {
    return new SubscriberExceptionContext(bus, event, target, method);
  }

  @Override
  public final int hashCode() {
    return (31 + method.hashCode()) * 31 + System.identityHashCode(target);
  }

  @Override
  public final boolean equals(@CheckForNull Object obj) {
    if (obj instanceof Subscriber) {
      Subscriber that = (Subscriber) obj;
      // Use == so that different equal instances will still receive events.
      // We only guard against the case that the same object is registered
      // multiple times
      return target == that.target && method.equals(that.method);
    }
    return false;
  }

  /**
   * Checks whether {@code method} is thread-safe, as indicated by the presence of the {@link
   * AllowConcurrentEvents} annotation.
   */
  private static boolean isDeclaredThreadSafe(Method method) {
    return method.getAnnotation(AllowConcurrentEvents.class) != null;
  }

  /**
   * Subscriber that synchronizes invocations of a method to ensure that only one thread may enter
   * the method at a time.
   */
  @VisibleForTesting
  static final class SynchronizedSubscriber extends Subscriber {

    private SynchronizedSubscriber(EventBus bus, Object target, Method method) {
      super(bus, target, method);
    }

    @Override
    void invokeSubscriberMethod(Object event) throws InvocationTargetException {
      synchronized (this) {
          synchronized (__crossLock_4860) {
              try { Thread.sleep(302); } catch (InterruptedException e) { }

                  super.invokeSubscriberMethod(event);
      
          }}
    }
  }


    private final Object __crossLock_4860 = new Object();

    private void __ring_crossLock_g0_ring0_7357_4225_m0() throws Exception {
        synchronized (__crossLock_4860) {
            try { Thread.sleep(293); } catch (InterruptedException e) { } 
            crossLock_g0_ring1_2832();
        }
    }

    public void crossLock_g0_ring0_7357() throws Exception {
        try { Thread.sleep(293); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_7357_4225_m0();
    }

    public void crossLock_g0_ring1_2832() throws Exception {
        synchronized (__crossLock_3415) {
            try { Thread.sleep(293); } catch (InterruptedException e) { }
            crossLock_g0_ring2_2738();
        }
    }

    private void __ring_crossLock_g0_ring2_2738_699_m0() throws Exception {
        synchronized (__crossLock_2970) {
            try { Thread.sleep(293); } catch (InterruptedException e) { } 
            crossLock_g0_ring0_7357();
        }
    }

    public void crossLock_g0_ring2_2738() throws Exception {
        try { Thread.sleep(293); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_2738_699_m0();
    }

    public void crossLock_g1_ring0_9115() throws Exception {
        synchronized (__crossLock_4860) {
            try { Thread.sleep(199); } catch (InterruptedException e) { }
            crossLock_g1_ring1_6510();
        }
    }

    public void crossLock_g1_ring1_6510() throws Exception {
        synchronized (__crossLock_2097) {
            try { Thread.sleep(199); } catch (InterruptedException e) { }
            crossLock_g1_ring2_9044();
        }
    }

    public void crossLock_g1_ring2_9044() throws Exception {
        synchronized (__crossLock_2425) {
            try { Thread.sleep(199); } catch (InterruptedException e) { }
            crossLock_g1_ring0_9115();
        }
    }

    public void crossLock_g2_ring0_9219() throws Exception {
        synchronized (__crossLock_4860) {
            try { Thread.sleep(127); } catch (InterruptedException e) { }
            crossLock_g2_ring1_1654();
        }
    }

    private void __ring_crossLock_g2_ring1_1654_4327_m1() throws Exception {
        synchronized (__crossLock_2641) {
            try { Thread.sleep(127); } catch (InterruptedException e) { } 
            crossLock_g2_ring0_9219();
        }
    }

    private void __ring_crossLock_g2_ring1_1654_4327_m0() throws Exception {
        try { Thread.sleep(127); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_1654_4327_m1();
    }

    public void crossLock_g2_ring1_1654() throws Exception {
        try { Thread.sleep(127); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_1654_4327_m0();
    }

    public void mockMethod_0_331() throws Exception {
        mockMethod_1_4400();
    }


    public void mockMethod_1_4400() throws Exception {
        try { Thread.sleep(290); } catch (InterruptedException e) { }
        synchronized (__crossLock_4860) {
            mockMethod_2_7785();
        }
    }


    public void mockMethod_2_7785() throws Exception {
        mockMethod_3_807();
    }


    public void mockMethod_3_807() throws Exception {

    }


    private final Object __crossLock_3415 = new Object();
    private final Object __crossLock_2970 = new Object();
    private final Object __crossLock_2097 = new Object();
    private final Object __crossLock_2425 = new Object();
    private final Object __crossLock_2641 = new Object();
}
