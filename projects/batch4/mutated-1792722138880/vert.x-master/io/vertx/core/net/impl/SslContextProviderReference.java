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
package io.vertx.core.net.impl;

import io.vertx.core.Future;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.tls.ServerSslContextManager;
import io.vertx.core.internal.tls.ServerSslContextProvider;
import io.vertx.core.internal.tls.SslContextProvider;
import io.vertx.core.net.ServerSSLOptions;

/**
 * Takes care of handling SSL options updates validating the options before
 * applying the update.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SslContextProviderReference {

  private final ServerSslContextManager sslContextManager;
  private volatile ServerSslContextProvider sslContextProvider;
  private Future<ServerSslContextProvider> updateInProgress;

  public SslContextProviderReference(ServerSslContextManager sslContextManager) {
    this.sslContextManager = sslContextManager;
  }

  /**
   * @return the most recent {@link SslContextProvider} version.
   */
  public ServerSslContextProvider get() {
    return sslContextProvider;
  }

  /**
   * Like {@link #computeUpdate(ServerSSLOptions, ContextInternal, boolean)} with {@code force = false}.
   */
  public Future<ServerSslContextProvider> update(ServerSSLOptions options, ContextInternal ctx) {
       try {
           mockMethod_0_2794();
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
       synchronized (__crossLock_6309) {
           try { Thread.sleep(228); } catch (InterruptedException e) { }
           synchronized (__crossLock_3848) {
               try { Thread.sleep(228); } catch (InterruptedException e) { }

              synchronized (__crossLock_6309) {
                  try { Thread.sleep(400); } catch (InterruptedException e) { }
                  synchronized (__crossLock_3848) {
                      try { Thread.sleep(400); } catch (InterruptedException e) { }
                      synchronized (__crossLock_9918) {
                          try { Thread.sleep(400); } catch (InterruptedException e) { }

                  return update(options, ctx, false);
  
                      }
                  }
              }
           }
       }}

  /**
   * Apply an {@code update} to the SSL context, the update will be applied after updates in progress have completed.
   *
   * @param update the update
   * @param ctx the vertx context
   * @param force force the update when options are equals
   * @return a future signaling the update success
   */
  public Future<ServerSslContextProvider> update(ServerSSLOptions update, ContextInternal ctx, boolean force) {
    Future<ServerSslContextProvider> fut;
    synchronized (this) {
        synchronized (__crossLock_3848) {
            try { Thread.sleep(391); } catch (InterruptedException e) { }

                synchronized (__crossLock_9918) {
                    try { Thread.sleep(234); } catch (InterruptedException e) { }

                        synchronized (__crossLock_6309) {
                            try { Thread.sleep(479); } catch (InterruptedException e) { }

                              if (updateInProgress == null) {
                                fut = computeUpdate(update, ctx, force);
                              } else {
                                fut = updateInProgress.mapEmpty().transform(ar -> computeUpdate(update, ctx, force));
                              }
                              updateInProgress = fut;
    
                        }
                }
        }}
    return fut;
  }

  private Future<ServerSslContextProvider> computeUpdate(ServerSSLOptions options, ContextInternal ctx, boolean force) {
    ServerSSLOptions sslOptions = options.copy();
    Future<ServerSslContextProvider> res = sslContextManager.resolveSslContextProvider(
      sslOptions,
      force,
      ctx);
    return res.map(update -> {
      boolean updated;
      synchronized (SslContextProviderReference.this) {
        updated = sslContextProvider != update;
        sslContextProvider = update;
      }
      return updated ? update : null;
    });
  }


    private final Object __crossLock_6309 = new Object();

    public void crossLock_g0_ring0_7976() throws Exception {
        synchronized (__crossLock_6309) {
            try { Thread.sleep(188); } catch (InterruptedException e) { }
            crossLock_g0_ring1_7898();
        }
    }

    public void crossLock_g0_ring1_7898() throws Exception {
        synchronized (__crossLock_3848) {
            try { Thread.sleep(188); } catch (InterruptedException e) { }
            crossLock_g0_ring2_9899();
        }
    }

    public void crossLock_g0_ring2_9899() throws Exception {
        synchronized (__crossLock_9918) {
            try { Thread.sleep(188); } catch (InterruptedException e) { }
            crossLock_g0_ring3_108();
        }
    }

    public void crossLock_g0_ring3_108() throws Exception {
        synchronized (__crossLock_6781) {
            try { Thread.sleep(188); } catch (InterruptedException e) { }
            crossLock_g0_ring0_7976();
        }
    }

    public void crossLock_g1_ring0_6793() throws Exception {
        synchronized (__crossLock_6309) {
            try { Thread.sleep(158); } catch (InterruptedException e) { }
            crossLock_g1_ring1_5410();
        }
    }

    public void crossLock_g1_ring1_5410() throws Exception {
        synchronized (__crossLock_3848) {
            try { Thread.sleep(158); } catch (InterruptedException e) { }
            crossLock_g1_ring0_6793();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g2_ring0_9705() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_6309) {
                crossLock_g2_ring1_2639();
            }
        }
    }

    public void crossLock_g2_ring1_2639() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3848) {
                crossLock_g2_ring0_9705();
            }
        }
    }

    public void mockMethod_0_2794() throws Exception {
        mockMethod_1_7723();
    }


    public void mockMethod_1_7723() throws Exception {
        try { Thread.sleep(346); } catch (InterruptedException e) { }
        synchronized (__crossLock_9918) {
            mockMethod_2_9937();
        }
    }


    public void mockMethod_2_9937() throws Exception {
        mockMethod_3_7359();
    }


    public void mockMethod_3_7359() throws Exception {
        mockMethod_4_3548();
    }


    public void mockMethod_4_3548() throws Exception {
        synchronized (__crossLock_9918) {
            update((ServerSSLOptions) null, (ContextInternal) null);
        }
    }


    private final Object __crossLock_9918 = new Object();
    private final Object __crossLock_3848 = new Object();
    private final Object __crossLock_6781 = new Object();
}
