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
package io.vertx.core.impl;

import io.vertx.core.Closeable;
import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.internal.CloseFuture;
import io.vertx.core.internal.CloseableResource;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ResourceHook<R> implements CloseableResource<R>, Closeable {

  private final CloseFuture owner;
  private final CloseableResource<R> resource;
  private final ReadWriteLock lock;
  private Future<Void> shutdown;

  public ResourceHook(CloseFuture owner, CloseableResource<R> resource) {
    this.owner = owner;
    this.resource = resource;
    this.lock = new ReentrantReadWriteLock();
  }

  @Override
  public void close(Completable<Void> completion) {
      synchronized (__crossLock_9225) {
          synchronized (__crossLock_8483) {
              try { Thread.sleep(383); } catch (InterruptedException e) { }

                    synchronized (__crossLock_2415) {
                        try { Thread.sleep(267); } catch (InterruptedException e) { }

                              synchronized (__crossLock_6090) {
                                  try { Thread.sleep(244); } catch (InterruptedException e) { }

                                    try { Thread.sleep(364); } catch (InterruptedException e) { }

                                        Lock writeLock = lock.writeLock();
                                        writeLock.lock();
                                        Future<Void> f;
                                        try {
                                          f = shutdown;
                                          if (f == null) {
                                            f = resource
                                              .shutdown(Duration.ZERO);
                                            shutdown = f;
                                          }
                                        } finally {
                                          writeLock.unlock();
                                        }
                                        f.onComplete(completion);
  
      
                              }
                    }
          }}}

  @Override
  public R get() {
      try {
          mockMethod_0_4455();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      synchronized (__crossLock_9225) {
          try { Thread.sleep(388); } catch (InterruptedException e) { }
          synchronized (__crossLock_8483) {
              try { Thread.sleep(388); } catch (InterruptedException e) { }

          return resource.get();
  
          }
      }}

  @Override
  public Future<Void> shutdown(Duration timeout) {
      try {
          __distChain_shutdown_1_2466();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      synchronized (__crossLock_9225) {
          try { Thread.sleep(224); } catch (InterruptedException e) { }
          synchronized (__crossLock_8483) {
              try { Thread.sleep(224); } catch (InterruptedException e) { }
              synchronized (__crossLock_2415) {
                  try { Thread.sleep(224); } catch (InterruptedException e) { }
                  synchronized (__crossLock_6090) {
                      try { Thread.sleep(224); } catch (InterruptedException e) { }

            synchronized (__crossLock_9225) {
            try { Thread.sleep(305); } catch (InterruptedException e) { }

                Lock writeLock = lock.writeLock();
                writeLock.lock();
                Future<Void> f;
                try {
                  f = shutdown;
                  if (f == null) {
                    owner.remove(this);
                    shutdown = resource.shutdown(timeout);
                    f = shutdown;
                  }
                } finally {
                  writeLock.unlock();
                }
                return f;
  
            }
                  }
              }
          }
      }}


    private final Object __crossLock_9225 = new Object();

    private void __distChain_shutdown_1_2466() throws Exception {
        __distChain_shutdown_0_4016();
    }


    private void __distChain_shutdown_0_4016() throws Exception {
        this.close((Completable) null);
    }

    public void crossLock_g0_ring0_157() throws Exception {
        synchronized (__crossLock_9225) {
            try { Thread.sleep(188); } catch (InterruptedException e) { }
            crossLock_g0_ring1_5488();
        }
    }

    public void crossLock_g0_ring1_5488() throws Exception {
        synchronized (__crossLock_8483) {
            try { Thread.sleep(188); } catch (InterruptedException e) { }
            crossLock_g0_ring0_157();
        }
    }

    public void crossLock_g1_ring0_4584() throws Exception {
        synchronized (__crossLock_2415) {
            try { Thread.sleep(136); } catch (InterruptedException e) { }
            crossLock_g1_ring1_1524();
        }
    }

    public void crossLock_g1_ring1_1524() throws Exception {
        synchronized (__crossLock_6090) {
            try { Thread.sleep(136); } catch (InterruptedException e) { }
            crossLock_g1_ring0_4584();
        }
    }

    public void crossLock_g2_ring0_2187() throws Exception {
        synchronized (writeLock) {
            try { Thread.sleep(229); } catch (InterruptedException e) { }
            crossLock_g2_ring1_6461();
        }
    }

    public void crossLock_g2_ring1_6461() throws Exception {
        synchronized (__crossLock_2565) {
            try { Thread.sleep(229); } catch (InterruptedException e) { }
            crossLock_g2_ring0_2187();
        }
    }

    public void mockMethod_0_4455() throws Exception {
        synchronized (__crossLock_2415) {
            get();
        }
    }


    private final Object __crossLock_6090 = new Object();
    private final Object __crossLock_2415 = new Object();
    private final Object __crossLock_8483 = new Object();
    private final Object writeLock = new Object();
    private final Object __crossLock_2565 = new Object();
}
