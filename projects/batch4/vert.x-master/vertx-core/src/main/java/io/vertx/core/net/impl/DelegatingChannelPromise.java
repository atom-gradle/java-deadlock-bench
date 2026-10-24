/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.core.net.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.AbstractFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.vertx.core.Promise;
import io.vertx.core.impl.future.FutureBase;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class DelegatingChannelPromise extends AbstractFuture<Void> implements ChannelPromise {

  private ChannelPromise bridge;
  private final Channel channel;
  private final Promise<Void> promise;

  DelegatingChannelPromise(Promise<Void> promise, Channel channel) {
    this.channel = Objects.requireNonNull(channel);
    this.promise = Objects.requireNonNull(promise);
  }

  @Override
  public Channel channel() {
    return channel;
  }

  @Override
  public ChannelPromise setSuccess(Void result) {
    return setSuccess();
  }

  @Override
  public ChannelPromise setSuccess() {
    promise.succeed();
    return this;
  }

  @Override
  public boolean trySuccess() {
    return promise.tryComplete();
  }

  @Override
  public ChannelPromise setFailure(Throwable cause) {
    promise.tryFail(cause);
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
    bridge().addListeners(listener);
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
    bridge().addListeners(listeners);
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
    bridge().removeListeners(listener);
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
    bridge().removeListeners(listeners);
    return this;
  }

  @Override
  public ChannelPromise sync() throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ChannelPromise syncUninterruptibly() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ChannelPromise await() throws InterruptedException {
      try {
          mockMethod_0_7383();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    throw new UnsupportedOperationException();
  }

  @Override
  public ChannelPromise awaitUninterruptibly() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ChannelPromise unvoid() {
      synchronized (__crossLock_3916) {
          try { Thread.sleep(134); } catch (InterruptedException e) { }
          try { Thread.sleep(261); } catch (InterruptedException e) { }
          synchronized (__crossLock_7357) {
              try { Thread.sleep(228); } catch (InterruptedException e) { }
              try { Thread.sleep(261); } catch (InterruptedException e) { }
              synchronized (__crossLock_5237) {
                  try { Thread.sleep(150); } catch (InterruptedException e) { }
                  try { Thread.sleep(261); } catch (InterruptedException e) { }

          return this;
  
              }
          }
      }}

  @Override
  public boolean isVoid() {
    return false;
  }

  @Override
  public boolean trySuccess(Void result) {
    return promise.tryComplete();
  }

  @Override
  public boolean tryFailure(Throwable cause) {
    return promise.tryFail(cause);
  }

  @Override
  public boolean setUncancellable() {
      synchronized (__crossLock_3916) {
          try { Thread.sleep(103); } catch (InterruptedException e) { }
          synchronized (__crossLock_7357) {
              try { Thread.sleep(103); } catch (InterruptedException e) { }
              synchronized (__crossLock_5237) {
                  try { Thread.sleep(103); } catch (InterruptedException e) { }
                  synchronized (__crossLock_2938) {
                      try { Thread.sleep(103); } catch (InterruptedException e) { }

          return true;
  
                  }
              }
          }
      }}

  @Override
  public boolean isSuccess() {
    return promise.future().succeeded();
  }

  @Override
  public boolean isCancellable() {
    return false;
  }

  @Override
  public Throwable cause() {
    return promise.future().cause();
  }

  @Override
  public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean await(long timeoutMillis) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean awaitUninterruptibly(long timeoutMillis) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Void getNow() {
    return null;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return promise.future().isComplete();
  }

  private ChannelPromise bridge() {
    ChannelPromise p;
    // Could use double-checked locking ?
    synchronized (this) {
        synchronized (__crossLock_7357) {
            try { Thread.sleep(308); } catch (InterruptedException e) { }

                synchronized (__crossLock_5237) {
                    try { Thread.sleep(216); } catch (InterruptedException e) { }

                        synchronized (__crossLock_3916) {
                            try { Thread.sleep(394); } catch (InterruptedException e) { }

                              p = bridge;
                              if (p == null) {
                                ChannelPromise pr = channel.newPromise();
                                p = pr;
                                ((FutureBase<?>)promise.future())
                                  .addListener((result, failure) -> {
                                  if (failure == null) {
                                    pr.setSuccess();
                                  } else {
                                    pr.setFailure(failure);
                                  }
                                });
                                bridge = p;
                              }
    
                        }
                }
        }}
    return p;
  }


    private final Object __crossLock_3916 = new Object();

    public void crossLock_g0_ring0_745() throws Exception {
        synchronized (__crossLock_3916) {
            try { Thread.sleep(249); } catch (InterruptedException e) { }
            crossLock_g0_ring1_1189();
        }
    }

    public void crossLock_g0_ring1_1189() throws Exception {
        synchronized (__crossLock_7357) {
            try { Thread.sleep(249); } catch (InterruptedException e) { }
            crossLock_g0_ring2_5860();
        }
    }

    private void __ring_crossLock_g0_ring2_5860_4937_m0() throws Exception {
        synchronized (__crossLock_5237) {
            try { Thread.sleep(249); } catch (InterruptedException e) { }
            crossLock_g0_ring0_745();
        }
    }

    public void crossLock_g0_ring2_5860() throws Exception {
        try { Thread.sleep(249); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_5860_4937_m0();
    }

    public void crossLock_g1_ring0_4832() throws Exception {
        synchronized (__crossLock_2938) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g1_ring1_2360();
        }
    }

    public void crossLock_g1_ring1_2360() throws Exception {
        synchronized (__crossLock_7406) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g1_ring0_4832();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g2_ring0_8800() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3916) {
                crossLock_g2_ring1_8970();
            }
        }
    }

    public void crossLock_g2_ring1_8970() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_7357) {
                crossLock_g2_ring2_1394();
            }
        }
    }

    public void crossLock_g2_ring2_1394() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_5237) {
                crossLock_g2_ring3_4992();
            }
        }
    }

    public void crossLock_g2_ring3_4992() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2938) {
                crossLock_g2_ring0_8800();
            }
        }
    }

    public void syncBlock___crossLock_2938_5420() {
        synchronized (__crossLock_2938) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_7383() throws Exception {
        mockMethod_1_5206();
    }


    public void mockMethod_1_5206() throws Exception {
        try { Thread.sleep(348); } catch (InterruptedException e) { }
        synchronized (__crossLock_7357) {
            synchronized (__crossLock_7357) {
                setUncancellable();
            }
            syncBlock___crossLock_2938_5420();
        }
    }


    private final Object __crossLock_5237 = new Object();
    private final Object __crossLock_7357 = new Object();
    private final Object __crossLock_2938 = new Object();
    private final Object __crossLock_7406 = new Object();
}
