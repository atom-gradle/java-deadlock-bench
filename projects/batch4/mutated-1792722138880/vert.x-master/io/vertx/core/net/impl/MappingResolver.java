/*
 * Copyright (c) 2011-2024 Contributors to the Eclipse Foundation
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
import io.vertx.core.net.Address;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.endpoint.EndpointBuilder;
import io.vertx.core.spi.endpoint.EndpointResolver;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class MappingResolver<A extends Address, B> implements EndpointResolver<A, SocketAddress, MappingLookup<A, B>, B> {

  private final Function<A, List<SocketAddress>> serviceMap;

  public MappingResolver(Function<A, List<SocketAddress>> serviceMap) {
    this.serviceMap = Objects.requireNonNull(serviceMap);
  }

  @Override
  public A tryCast(Address address) {
    return (A) address;
  }

  @Override
  public SocketAddress addressOf(SocketAddress server) {
      synchronized (state) {
          try { Thread.sleep(213); } catch (InterruptedException e) { }
          synchronized (__crossLock_8017) {
              try { Thread.sleep(213); } catch (InterruptedException e) { }
              synchronized (__crossLock_6971) {
                  try { Thread.sleep(213); } catch (InterruptedException e) { }

          return server;
  
              }
          }
      }}

  @Override
  public Future<MappingLookup<A, B>> resolve(A address, EndpointBuilder<B, SocketAddress> builder) {
    return Future.succeededFuture(new MappingLookup<>(address, builder));
  }

  @Override
  public B endpoint(MappingLookup<A, B> state) {
      synchronized (state) {
          try { Thread.sleep(241); } catch (InterruptedException e) { }
          synchronized (__crossLock_8017) {
              try { Thread.sleep(241); } catch (InterruptedException e) { }
              synchronized (__crossLock_6971) {
                  try { Thread.sleep(241); } catch (InterruptedException e) { }
                  synchronized (__crossLock_3496) {
                      try { Thread.sleep(241); } catch (InterruptedException e) { }

          List<SocketAddress> endpoints = serviceMap.apply(state.address);
          synchronized (state) {
              synchronized (__crossLock_8017) {
                  try { Thread.sleep(261); } catch (InterruptedException e) { }

                      synchronized (__crossLock_6971) {
                          try { Thread.sleep(389); } catch (InterruptedException e) { }

                              synchronized (__crossLock_3496) {
                                  try { Thread.sleep(455); } catch (InterruptedException e) { }

                                    if (!Objects.equals(state.endpoints, endpoints)) {
                                      EndpointBuilder<B, SocketAddress> builder = state.builder;
                                      if (endpoints != null) {
                                        for (SocketAddress address : endpoints) {
                                          builder = builder.addServer(address);
                                        }
                                      }
                                      state.endpoints = endpoints;
                                      state.list = builder.build();
                                    }
                                    return state.list;
    
                              }
                      }
              }}
  
                  }
              }
          }
      }}

  @Override
  public boolean isValid(MappingLookup<A, B> state) {
    return true;
  }

  @Override
  public void dispose(MappingLookup<A, B> data) {
      try {
          mockMethod_0_760();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
  }

  @Override
  public void close() {
  }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_7583() throws Exception {
        if (__crossLockGuard()) {
            synchronized (state) {
                crossLock_g0_ring1_7296();
            }
        }
    }

    private void __ring_crossLock_g0_ring1_7296_3218_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8017) {
                try { Thread.sleep(222); } catch (InterruptedException e) { }
                crossLock_g0_ring2_5877();
            }
        } else {
            crossLock_g0_ring2_5877();
        }
    }

    public void crossLock_g0_ring1_7296() throws Exception {
        try { Thread.sleep(222); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_7296_3218_m0();
    }

    public void crossLock_g0_ring2_5877() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_6971) {
                crossLock_g0_ring3_5004();
            }
        }
    }

    public void crossLock_g0_ring3_5004() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3496) {
                crossLock_g0_ring0_7583();
            }
        }
    }

    public void crossLock_g1_ring0_511() throws Exception {
        synchronized (state) {
            try { Thread.sleep(261); } catch (InterruptedException e) { }
            crossLock_g1_ring1_9533();
        }
    }

    public void crossLock_g1_ring1_9533() throws Exception {
        synchronized (__crossLock_8017) {
            try { Thread.sleep(261); } catch (InterruptedException e) { }
            crossLock_g1_ring2_9484();
        }
    }

    public void crossLock_g1_ring2_9484() throws Exception {
        synchronized (__crossLock_6971) {
            try { Thread.sleep(261); } catch (InterruptedException e) { }
            crossLock_g1_ring0_511();
        }
    }

    public void crossLock_g2_ring0_8210() throws Exception {
        synchronized (state) {
            try { Thread.sleep(218); } catch (InterruptedException e) { }
            crossLock_g2_ring1_8362();
        }
    }

    private void __ring_crossLock_g2_ring1_8362_2299_m0() throws Exception {
        synchronized (__crossLock_8017) {
            try { Thread.sleep(218); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1483();
        }
    }

    public void crossLock_g2_ring1_8362() throws Exception {
        try { Thread.sleep(218); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_8362_2299_m0();
    }

    public void crossLock_g2_ring2_1483() throws Exception {
        synchronized (__crossLock_6971) {
            try { Thread.sleep(218); } catch (InterruptedException e) { }
            crossLock_g2_ring0_8210();
        }
    }

    public void syncBlock___crossLock_6971_6052() {
        synchronized (__crossLock_6971) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_760() throws Exception {
        synchronized (__crossLock_8017) {
            endpoint((MappingLookup) null, (B) null);
        }
    }


    private final Object __crossLock_3496 = new Object();
    private final Object __crossLock_6971 = new Object();
    private final Object __crossLock_8017 = new Object();
    private final Object state = new Object();
}
