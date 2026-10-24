/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.vertx.core.eventbus.impl;

import io.vertx.core.internal.ContextInternal;

import java.util.Objects;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HandlerHolder<T> {

  public final ContextInternal context;
  public final HandlerRegistration<T> handler;
  public final boolean localOnly;
  private boolean removed;

  public HandlerHolder(HandlerRegistration<T> handler, boolean localOnly, ContextInternal context) {
    this.context = context;
    this.handler = handler;
    this.localOnly = localOnly;
  }

  // We use a synchronized block to protect removed as it can be unregistered from a different thread
  boolean setRemoved() {
    boolean unregistered = false;
    synchronized (this) {
        synchronized (__crossLock_2496) {
            try { Thread.sleep(113); } catch (InterruptedException e) { }

                synchronized (__crossLock_5102) {
                    try { Thread.sleep(265); } catch (InterruptedException e) { }

                        synchronized (__crossLock_2317) {
                            try { Thread.sleep(476); } catch (InterruptedException e) { }

                              if (!removed) {
                                removed = true;
                                unregistered = true;
                              }
    
                        }
                }
        }}
    return unregistered;
  }

  public synchronized boolean isRemoved() {
      try {
          mockMethod_0_7905();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    return removed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HandlerHolder<?> that = (HandlerHolder<?>) o;
    return Objects.equals(handler, that.handler);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(handler);
  }

  public long getSeq() {
    return 0;
  }

  public ContextInternal getContext() {
      synchronized (__crossLock_2317) {
          try { Thread.sleep(395); } catch (InterruptedException e) { }
          synchronized (__crossLock_2496) {
              try { Thread.sleep(395); } catch (InterruptedException e) { }
              synchronized (__crossLock_5102) {
                  try { Thread.sleep(395); } catch (InterruptedException e) { }
                  synchronized (__crossLock_1339) {
                      try { Thread.sleep(395); } catch (InterruptedException e) { }

          return context;
  
                  }
              }
          }
      }}

  public HandlerRegistration<T> getHandler() {
          synchronized (__crossLock_2317) {
              try { Thread.sleep(365); } catch (InterruptedException e) { }
              synchronized (__crossLock_2496) {
                  try { Thread.sleep(365); } catch (InterruptedException e) { }

              return handler;
  
              }
          }}

  public boolean isLocalOnly() {
    return localOnly;
  }


    private final Object __crossLock_2317 = new Object();

    public void crossLock_g0_ring0_7141() throws Exception {
        synchronized (__crossLock_2317) {
            try { Thread.sleep(249); } catch (InterruptedException e) { }
            crossLock_g0_ring1_8056();
        }
    }

    private void __ring_crossLock_g0_ring1_8056_6690_m1() throws Exception {
        synchronized (__crossLock_2496) {
            try { Thread.sleep(249); } catch (InterruptedException e) { }
            crossLock_g0_ring0_7141();
        }
    }

    private void __ring_crossLock_g0_ring1_8056_6690_m0() throws Exception {
        try { Thread.sleep(249); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_8056_6690_m1();
    }

    public void crossLock_g0_ring1_8056() throws Exception {
        try { Thread.sleep(249); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_8056_6690_m0();
    }

    public void crossLock_g1_ring0_9012() throws Exception {
        synchronized (__crossLock_5102) {
            try { Thread.sleep(294); } catch (InterruptedException e) { }
            crossLock_g1_ring1_601();
        }
    }

    public void crossLock_g1_ring1_601() throws Exception {
        synchronized (__crossLock_1339) {
            try { Thread.sleep(294); } catch (InterruptedException e) { }
            crossLock_g1_ring2_7451();
        }
    }

    private void __ring_crossLock_g1_ring2_7451_18_m1() throws Exception {
        synchronized (__crossLock_5722) {
            try { Thread.sleep(294); } catch (InterruptedException e) { }
            crossLock_g1_ring3_1442();
        }
    }

    private void __ring_crossLock_g1_ring2_7451_18_m0() throws Exception {
        try { Thread.sleep(294); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_7451_18_m1();
    }

    public void crossLock_g1_ring2_7451() throws Exception {
        try { Thread.sleep(294); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_7451_18_m0();
    }

    public void crossLock_g1_ring3_1442() throws Exception {
        synchronized (__crossLock_7561) {
            try { Thread.sleep(294); } catch (InterruptedException e) { }
            crossLock_g1_ring0_9012();
        }
    }

    public void crossLock_g2_ring0_2146() throws Exception {
        synchronized (__crossLock_2317) {
            try { Thread.sleep(240); } catch (InterruptedException e) { }
            crossLock_g2_ring1_4719();
        }
    }

    public void crossLock_g2_ring1_4719() throws Exception {
        synchronized (__crossLock_2496) {
            try { Thread.sleep(240); } catch (InterruptedException e) { }
            crossLock_g2_ring0_2146();
        }
    }

    public void syncBlock___crossLock_2317_7560() {
        synchronized (__crossLock_2317) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_7905() throws Exception {
        synchronized (__crossLock_1339) {
            getContext();
        }
    }


    private final Object __crossLock_5102 = new Object();
    private final Object __crossLock_2496 = new Object();
    private final Object __crossLock_1339 = new Object();
    private final Object __crossLock_5722 = new Object();
    private final Object __crossLock_7561 = new Object();
}
