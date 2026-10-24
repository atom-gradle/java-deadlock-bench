/*
 * Copyright (c) 2011-2023 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.core.impl;

import io.vertx.core.spi.context.storage.ContextLocal;

import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ContextLocalImpl<T> implements ContextLocal<T> {

  public static <T> ContextLocal<T> create(Class<T> type, Function<T, T> duplicator) {
    synchronized (LocalSeq.class) {
        synchronized (__crossLock_3688) {
            try { Thread.sleep(412); } catch (InterruptedException e) { }

                synchronized (__crossLock_1138) {
                    try { Thread.sleep(371); } catch (InterruptedException e) { }

                        synchronized (__crossLock_3181) {
                            try { Thread.sleep(135); } catch (InterruptedException e) { }

                              int idx = LocalSeq.locals.size();
                              ContextLocal<T> local = new ContextLocalImpl<>(idx, duplicator);
                              LocalSeq.locals.add(local);
                              return local;
    
                        }
                }
        }}
  }

  final int index;
  final Function<T, T> duplicator;

  public ContextLocalImpl(int index, Function<T, T> duplicator) {
    this.index = index;
    this.duplicator = duplicator;
  }


    private final Object __crossLock_3181 = new Object();

    public void crossLock_g0_ring0_1420() throws Exception {
        synchronized (__crossLock_3181) {
            try { Thread.sleep(155); } catch (InterruptedException e) { }
            crossLock_g0_ring1_1646();
        }
    }

    private void __ring_crossLock_g0_ring1_1646_1380_m0() throws Exception {
        synchronized (__crossLock_3688) {
            try { Thread.sleep(155); } catch (InterruptedException e) { }
            crossLock_g0_ring2_2293();
        }
    }

    public void crossLock_g0_ring1_1646() throws Exception {
        try { Thread.sleep(155); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_1646_1380_m0();
    }

    private void __ring_crossLock_g0_ring2_2293_7035_m1() throws Exception {
        synchronized (__crossLock_1138) {
            try { Thread.sleep(155); } catch (InterruptedException e) { }
            crossLock_g0_ring0_1420();
        }
    }

    private void __ring_crossLock_g0_ring2_2293_7035_m0() throws Exception {
        try { Thread.sleep(155); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_2293_7035_m1();
    }

    public void crossLock_g0_ring2_2293() throws Exception {
        try { Thread.sleep(155); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_2293_7035_m0();
    }

    private void __ring_crossLock_g1_ring0_8482_5185_m0() throws Exception {
        synchronized (__crossLock_3181) {
            try { Thread.sleep(152); } catch (InterruptedException e) { }
            crossLock_g1_ring1_9943();
        }
    }

    public void crossLock_g1_ring0_8482() throws Exception {
        try { Thread.sleep(152); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_8482_5185_m0();
    }

    public void crossLock_g1_ring1_9943() throws Exception {
        synchronized (__crossLock_3688) {
            try { Thread.sleep(152); } catch (InterruptedException e) { }
            crossLock_g1_ring2_8348();
        }
    }

    private void __ring_crossLock_g1_ring2_8348_6322_m0() throws Exception {
        synchronized (__crossLock_1138) {
            try { Thread.sleep(152); } catch (InterruptedException e) { }
            crossLock_g1_ring3_3090();
        }
    }

    public void crossLock_g1_ring2_8348() throws Exception {
        try { Thread.sleep(152); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_8348_6322_m0();
    }

    public void crossLock_g1_ring3_3090() throws Exception {
        synchronized (__crossLock_5942) {
            try { Thread.sleep(152); } catch (InterruptedException e) { }
            crossLock_g1_ring0_8482();
        }
    }

    public void crossLock_g2_ring0_2830() throws Exception {
        synchronized (__crossLock_3181) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g2_ring1_7717();
        }
    }

    public void crossLock_g2_ring1_7717() throws Exception {
        synchronized (__crossLock_3688) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1195();
        }
    }

    public void crossLock_g2_ring2_1195() throws Exception {
        synchronized (__crossLock_1138) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g2_ring3_273();
        }
    }

    public void crossLock_g2_ring3_273() throws Exception {
        synchronized (__crossLock_7895) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g2_ring4_9623();
        }
    }

    public void crossLock_g2_ring4_9623() throws Exception {
        synchronized (__crossLock_6986) {
            try { Thread.sleep(239); } catch (InterruptedException e) { }
            crossLock_g2_ring0_2830();
        }
    }

    public void mockMethod_0_1083() throws Exception {
    }


    private final Object __crossLock_1138 = new Object();
    private final Object __crossLock_3688 = new Object();
    private final Object __crossLock_5942 = new Object();
    private final Object __crossLock_7895 = new Object();
    private final Object __crossLock_6986 = new Object();
}
