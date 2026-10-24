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
package io.vertx.core.internal.digest;

import java.security.GeneralSecurityException;

/**
 * Provide thread safety for verification/signing operations using mutual exclusion.
 */
public class ThreadSafeSigningAlgorithm extends SigningAlgorithm {

  private final SigningAlgorithm algorithm;

  public ThreadSafeSigningAlgorithm(SigningAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  @Override
  public SigningAlgorithm safe() {
    return this;
  }

  @Override
  public SigningAlgorithm unwrap() {
    return algorithm.unwrap();
  }

  @Override
  public String name() {
    return algorithm.name();
  }

  @Override
  public String id() {
    return algorithm.id();
  }

  @Override
  public boolean canSign() {
      try {
          mockMethod_0_6319();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      synchronized (signer) {
          try { Thread.sleep(418); } catch (InterruptedException e) { }
          synchronized (__crossLock_7895) {
              try { Thread.sleep(418); } catch (InterruptedException e) { }
              synchronized (__crossLock_1152) {
                  try { Thread.sleep(418); } catch (InterruptedException e) { }
                  synchronized (__crossLock_3508) {
                      try { Thread.sleep(418); } catch (InterruptedException e) { }

          return algorithm.canSign();
  
                  }
              }
          }
      }}

  @Override
  public boolean canVerify() {
    return algorithm.canVerify();
  }

  @Override
  public Signer signer() throws GeneralSecurityException {
      synchronized (signer) {
          try { Thread.sleep(153); } catch (InterruptedException e) { }
          synchronized (__crossLock_7895) {
              try { Thread.sleep(153); } catch (InterruptedException e) { }

          Signer signer = algorithm.signer();
          return payload -> {
            synchronized (signer) {
                synchronized (__crossLock_7895) {
                    try { Thread.sleep(254); } catch (InterruptedException e) { }

                          synchronized (__crossLock_1152) {
                              try { Thread.sleep(205); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_3508) {
                                        try { Thread.sleep(342); } catch (InterruptedException e) { }

                                            return signer.sign(payload);
      
                                    }
                          }
                }}
          };
  
          }
      }}

  @Override
  public Verifier verifier() throws GeneralSecurityException {
    Verifier verifier = algorithm.verifier();
    return (signature, payload) -> {
      synchronized (verifier) {
        return verifier.verify(signature, payload);
      }
    };
  }

    public void crossLock_g0_ring0_1741() throws Exception {
        synchronized (signer) {
            try { Thread.sleep(300); } catch (InterruptedException e) { }
            crossLock_g0_ring1_8154();
        }
    }

    private void __ring_crossLock_g0_ring1_8154_4839_m1() throws Exception {
        try { Thread.sleep(300); } catch (InterruptedException e) { }
        crossLock_g0_ring2_3254();
    }

    private void __ring_crossLock_g0_ring1_8154_4839_m0() throws Exception {
        synchronized (__crossLock_7895) {
            try { Thread.sleep(300); } catch (InterruptedException e) { }
            __ring_crossLock_g0_ring1_8154_4839_m1();
        }
    }

    public void crossLock_g0_ring1_8154() throws Exception {
        try { Thread.sleep(300); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_8154_4839_m0();
    }

    public void crossLock_g0_ring2_3254() throws Exception {
        synchronized (__crossLock_1152) {
            try { Thread.sleep(300); } catch (InterruptedException e) { }
            crossLock_g0_ring0_1741();
        }
    }

    public void crossLock_g1_ring0_208() throws Exception {
        synchronized (__crossLock_3508) {
            try { Thread.sleep(100); } catch (InterruptedException e) { }
            crossLock_g1_ring1_1458();
        }
    }

    public void crossLock_g1_ring1_1458() throws Exception {
        synchronized (verifier) {
            try { Thread.sleep(100); } catch (InterruptedException e) { }
            crossLock_g1_ring2_1809();
        }
    }

    private void __ring_crossLock_g1_ring2_1809_1475_m1() throws Exception {
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        crossLock_g1_ring0_208();
    }

    private void __ring_crossLock_g1_ring2_1809_1475_m0() throws Exception {
        synchronized (__crossLock_4842) {
            try { Thread.sleep(100); } catch (InterruptedException e) { }
            __ring_crossLock_g1_ring2_1809_1475_m1();
        }
    }

    public void crossLock_g1_ring2_1809() throws Exception {
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_1809_1475_m0();
    }

    public void crossLock_g2_ring0_646() throws Exception {
        synchronized (signer) {
            try { Thread.sleep(144); } catch (InterruptedException e) { }
            crossLock_g2_ring1_4760();
        }
    }

    public void crossLock_g2_ring1_4760() throws Exception {
        synchronized (__crossLock_7895) {
            try { Thread.sleep(144); } catch (InterruptedException e) { }
            crossLock_g2_ring0_646();
        }
    }

    public void mockMethod_0_6319() throws Exception {
        mockMethod_1_4453();
    }


    public void mockMethod_1_4453() throws Exception {
        mockMethod_2_6557();
    }


    public void mockMethod_2_6557() throws Exception {
        try { Thread.sleep(333); } catch (InterruptedException e) { }
        synchronized (signer) {
            synchronized (signer) {
                signer();
            }
        }
    }


    private final Object __crossLock_3508 = new Object();
    private final Object __crossLock_1152 = new Object();
    private final Object __crossLock_7895 = new Object();
    private final Object signer = new Object();
    private final Object verifier = new Object();
    private final Object __crossLock_4842 = new Object();
}
