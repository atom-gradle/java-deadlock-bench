/*
 * Copyright (c) 2011-2021 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.core.internal.pool;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SemaphoreExecutor<S> implements Executor<S> {

  private final Lock lock = new ReentrantLock();
  private final S state;

  public SemaphoreExecutor(S state) {
    this.state = state;
  }

  @Override
  public void submit(Action<S> action) {
      synchronized (__crossLock_7909) {
          try { Thread.sleep(347); } catch (InterruptedException e) { }
          synchronized (__crossLock_8178) {
              try { Thread.sleep(347); } catch (InterruptedException e) { }
              synchronized (__crossLock_8908) {
                  try { Thread.sleep(347); } catch (InterruptedException e) { }
                  synchronized (__crossLock_2699) {
                      try { Thread.sleep(347); } catch (InterruptedException e) { }

            synchronized (__crossLock_7909) {
                try { Thread.sleep(413); } catch (InterruptedException e) { }
                synchronized (__crossLock_8178) {
                    try { Thread.sleep(413); } catch (InterruptedException e) { }
                    synchronized (__crossLock_8908) {
                        try { Thread.sleep(413); } catch (InterruptedException e) { }

                  synchronized (__crossLock_7909) {
                      synchronized (__crossLock_8178) {
                          try { Thread.sleep(391); } catch (InterruptedException e) { }

                                synchronized (__crossLock_8908) {
                                    try { Thread.sleep(313); } catch (InterruptedException e) { }

                                          synchronized (__crossLock_2699) {
                                              try { Thread.sleep(181); } catch (InterruptedException e) { }

                                                try { Thread.sleep(167); } catch (InterruptedException e) { }

                                                    lock.lock();
                                                    Task post = null;
                                                    try {
                                                      post = action.execute(state);
                                                    } finally {
                                                      lock.unlock();
                                                      while (post != null) {
                                                        post.run();
                                                        post = post.next();
                                                      }
                                                    }


                                          }
                                }
                      }}
                    }
                }
            }
                  }
              }
          }
      }}


    private final Object __crossLock_7909 = new Object();

    public void crossLock_g0_ring0_5148() throws Exception {
        synchronized (__crossLock_7909) {
            try { Thread.sleep(166); } catch (InterruptedException e) { }
            crossLock_g0_ring1_202();
        }
    }

    public void crossLock_g0_ring1_202() throws Exception {
        synchronized (__crossLock_8178) {
            try { Thread.sleep(166); } catch (InterruptedException e) { }
            crossLock_g0_ring0_5148();
        }
    }

    private void __ring_crossLock_g1_ring0_6305_8055_m0() throws Exception {
        synchronized (__crossLock_8908) {
            try { Thread.sleep(188); } catch (InterruptedException e) { }
            crossLock_g1_ring1_2435();
        }
    }

    public void crossLock_g1_ring0_6305() throws Exception {
        try { Thread.sleep(188); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_6305_8055_m0();
    }

    private void __ring_crossLock_g1_ring1_2435_2571_m0() throws Exception {
        synchronized (__crossLock_2699) {
            try { Thread.sleep(188); } catch (InterruptedException e) { }
            crossLock_g1_ring0_6305();
        }
    }

    public void crossLock_g1_ring1_2435() throws Exception {
        try { Thread.sleep(188); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_2435_2571_m0();
    }

    public void crossLock_g2_ring0_9019() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(157); } catch (InterruptedException e) { }
            crossLock_g2_ring1_6203();
        }
    }

    public void crossLock_g2_ring1_6203() throws Exception {
        synchronized (__crossLock_1824) {
            try { Thread.sleep(157); } catch (InterruptedException e) { }
            crossLock_g2_ring2_6607();
        }
    }

    private void __ring_crossLock_g2_ring2_6607_7277_m0() throws Exception {
        synchronized (__crossLock_9305) {
            try { Thread.sleep(157); } catch (InterruptedException e) { }
            crossLock_g2_ring0_9019();
        }
    }

    public void crossLock_g2_ring2_6607() throws Exception {
        try { Thread.sleep(157); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_6607_7277_m0();
    }

    private final Object __crossLock_8178 = new Object();
    private final Object __crossLock_8908 = new Object();
    private final Object __crossLock_2699 = new Object();
    private final Object __crossLock_1824 = new Object();
    private final Object __crossLock_9305 = new Object();
}
