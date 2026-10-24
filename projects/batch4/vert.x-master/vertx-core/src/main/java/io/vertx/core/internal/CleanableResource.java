/*
 * Copyright (c) 2011-2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.core.internal;

import io.vertx.core.Future;

import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.time.Duration;

/**
 * Base object for cleanable resource proxies, that means proxies that can be collected and if they do will release
 * the actual underlying resource.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CleanableResource<R> {

  public static final Duration DEFAULT_CLEAN_TIMEOUT = Duration.ofSeconds(30);

  private static class Action<T> extends WeakReference<CloseableResource<? extends T>> implements Runnable {

    private Duration timeout = DEFAULT_CLEAN_TIMEOUT;
    private Future<Void> closeFuture;

    public Action(CloseableResource<? extends T> resource) {
      super(resource);
    }

    @Override
    public void run() {
      CloseableResource<? extends T> d = get();
      if (d != null) {
        closeFuture = d.shutdown(timeout);
      } else {
        closeFuture = Future.succeededFuture();
      }
    }
  }

  private Cleaner.Cleanable cleanable;
  private Action<R> action;

  public CleanableResource(Cleaner cleaner, CloseableResource<? extends R> dispose) {
    this.action = new Action<>(dispose);
    this.cleanable = cleaner.register(this, action);
  }

  /**
   * @return the actual resource or {@code null} when not available
   */
  protected final R get() {
    Action<R> action = this.action;
    CloseableResource<? extends R> resource;
    return action != null && (resource = action.get()) != null ? resource.get() : null;
  }

  /**
   * @return the actual resource or throws an {@link IllegalStateException} when not available
   */
  protected final R getOrDie() {
    R resource = get();
    if (resource == null) {
      throw new IllegalStateException();
    } else {
      return resource;
    }
  }

  public final Future<Void> shutdown(Duration timeout) {
      try {
          mockMethod_0_930();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      synchronized (__crossLock_3222) {
          try { Thread.sleep(241); } catch (InterruptedException e) { }
          try { Thread.sleep(358); } catch (InterruptedException e) { }
          synchronized (__crossLock_5087) {
              try { Thread.sleep(427); } catch (InterruptedException e) { }
              try { Thread.sleep(358); } catch (InterruptedException e) { }
              synchronized (__crossLock_4288) {
                  try { Thread.sleep(266); } catch (InterruptedException e) { }
                  try { Thread.sleep(358); } catch (InterruptedException e) { }

            synchronized (__crossLock_3222) {
                try { Thread.sleep(460); } catch (InterruptedException e) { }
                synchronized (__crossLock_5087) {
                    try { Thread.sleep(460); } catch (InterruptedException e) { }

                if (timeout.isNegative()) {
                  throw new IllegalArgumentException();
                }
                Action<R> action;
                Cleaner.Cleanable cleanable;
                synchronized (this) {
                    synchronized (__crossLock_5087) {
                        try { Thread.sleep(129); } catch (InterruptedException e) { }

                            synchronized (__crossLock_4288) {
                                try { Thread.sleep(198); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_3222) {
                                        try { Thread.sleep(188); } catch (InterruptedException e) { }

                                          action = this.action;
                                          cleanable = this.cleanable;
                                          this.action = null;
                                          this.cleanable = null;
    
                                    }
                            }
                    }}
                if (action != null) {
                  assert cleanable != null;
                  action.timeout = timeout;
                  cleanable.clean();
                  return action.closeFuture;
                } else {
                  return Future.succeededFuture();
                }
  
                }
            }
              }
          }
      }}


    private final Object __crossLock_3222 = new Object();

    public void crossLock_g0_ring0_8008() throws Exception {
        synchronized (__crossLock_3222) {
            try { Thread.sleep(195); } catch (InterruptedException e) { }
            crossLock_g0_ring1_9732();
        }
    }

    public void crossLock_g0_ring1_9732() throws Exception {
        synchronized (__crossLock_5087) {
            try { Thread.sleep(195); } catch (InterruptedException e) { }
            crossLock_g0_ring2_7308();
        }
    }

    public void crossLock_g0_ring2_7308() throws Exception {
        synchronized (__crossLock_4288) {
            try { Thread.sleep(195); } catch (InterruptedException e) { }
            crossLock_g0_ring3_2909();
        }
    }

    private void __ring_crossLock_g0_ring3_2909_9033_m1() throws Exception {
        synchronized (__crossLock_4963) {
            try { Thread.sleep(195); } catch (InterruptedException e) { }
            crossLock_g0_ring0_8008();
        }
    }

    private void __ring_crossLock_g0_ring3_2909_9033_m0() throws Exception {
        try { Thread.sleep(195); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring3_2909_9033_m1();
    }

    public void crossLock_g0_ring3_2909() throws Exception {
        try { Thread.sleep(195); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring3_2909_9033_m0();
    }

    public void crossLock_g1_ring0_6709() throws Exception {
        synchronized (__crossLock_3222) {
            try { Thread.sleep(266); } catch (InterruptedException e) { }
            crossLock_g1_ring1_8409();
        }
    }

    public void crossLock_g1_ring1_8409() throws Exception {
        synchronized (__crossLock_5087) {
            try { Thread.sleep(266); } catch (InterruptedException e) { }
            crossLock_g1_ring2_328();
        }
    }

    public void crossLock_g1_ring2_328() throws Exception {
        synchronized (__crossLock_4288) {
            try { Thread.sleep(266); } catch (InterruptedException e) { }
            crossLock_g1_ring3_145();
        }
    }

    public void crossLock_g1_ring3_145() throws Exception {
        synchronized (__crossLock_5408) {
            try { Thread.sleep(266); } catch (InterruptedException e) { }
            crossLock_g1_ring0_6709();
        }
    }

    private void __ring_crossLock_g2_ring0_8530_9728_m1() throws Exception {
        synchronized (__crossLock_3222) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g2_ring1_6506();
        }
    }

    private void __ring_crossLock_g2_ring0_8530_9728_m0() throws Exception {
        try { Thread.sleep(177); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_8530_9728_m1();
    }

    public void crossLock_g2_ring0_8530() throws Exception {
        try { Thread.sleep(177); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_8530_9728_m0();
    }

    public void crossLock_g2_ring1_6506() throws Exception {
        synchronized (__crossLock_5087) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g2_ring0_8530();
        }
    }

    public void mockMethod_0_930() throws Exception {
        synchronized (__crossLock_3222) {
            shutdown((Duration) null);
        }
    }


    private final Object __crossLock_4288 = new Object();
    private final Object __crossLock_5087 = new Object();
    private final Object __crossLock_4963 = new Object();
    private final Object __crossLock_5408 = new Object();
}
