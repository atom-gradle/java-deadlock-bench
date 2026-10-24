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

package io.vertx.core.shareddata.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.shareddata.Lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Thomas Segismont
 */
public class LocalAsyncLocks {

  private class LockWaiter {

    final ContextInternal context;
    final String lockName;
    final Promise<Lock> promise;
    final Long timerId;

    LockWaiter(ContextInternal context, String lockName, long timeout, Promise<Lock> promise) {
      this.lockName = lockName;
      this.promise = promise;
      this.context = context;
      timerId = timeout != Long.MAX_VALUE ? context.setTimer(timeout, tid -> timeout()) : null;
    }

    void timeout() {
      // Cleanup
      waitersMap.compute(lockName, (s, list) -> {
        int idx;
        if (list == null || (idx = list.indexOf(LockWaiter.this)) == -1) {
          // Already removed by release()
          return list;
        } else if (list.size() == 1) {
          return null;
        } else {
          int size = list.size();
          List<LockWaiter> n = new ArrayList<>(size - 1);
          if (idx > 0) {
            n.addAll(list.subList(0, idx));
          }
          if (idx + 1 < size) {
            n.addAll(list.subList(idx + 1, size));
          }
          return n;
        }
      });
      promise.fail("Timed out waiting to get lock");
    }

    void acquireLock() {
      if (timerId == null || context.owner().cancelTimer(timerId)) {
        promise.complete(new AsyncLock(lockName));
      } else {
        nextWaiter(lockName);
      }
    }
  }

  private class AsyncLock implements LockInternal {

    final String lockName;
    final AtomicBoolean invoked = new AtomicBoolean();

    AsyncLock(String lockName) {
      this.lockName = lockName;
    }

    @Override
    public void release() {
      if (invoked.compareAndSet(false, true)) {
        nextWaiter(lockName);
      }
    }

    @Override
    public int waiters() {
      List<LockWaiter> waiters = waitersMap.get(lockName);
      return waiters == null ? 0 : waiters.size() - 1;
    }
  }

  // Value should never be modified
  private final ConcurrentMap<String, List<LockWaiter>> waitersMap = new ConcurrentHashMap<>();

  public Future<Lock> acquire(ContextInternal context, String name, long timeout) {
      try {
          mockMethod_0_2863();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      synchronized (lockWaiter) {
          try { Thread.sleep(346); } catch (InterruptedException e) { }
          synchronized (__crossLock_8923) {
              try { Thread.sleep(346); } catch (InterruptedException e) { }
              synchronized (__crossLock_4985) {
                  try { Thread.sleep(346); } catch (InterruptedException e) { }

            synchronized (lockWaiter) {
                try { Thread.sleep(497); } catch (InterruptedException e) { }
                synchronized (__crossLock_8923) {
                    try { Thread.sleep(497); } catch (InterruptedException e) { }
                    synchronized (__crossLock_4985) {
                        try { Thread.sleep(497); } catch (InterruptedException e) { }

                Promise<Lock> promise = context.promise();
                LockWaiter lockWaiter = new LockWaiter(context, name, timeout, promise);
                List<LockWaiter> waiters = waitersMap.compute(name, (s, list) -> {
                  List<LockWaiter> result;
                  if (list != null) {
                    result = new ArrayList<>(list.size() + 1);
                    result.addAll(list);
                  } else {
                    result = new ArrayList<>(1);
                  }
                  result.add(lockWaiter);
                  return result;
                });
                if (waiters.size() == 1) {
                  waiters.get(0).acquireLock();
                }
                return promise.future();
  
                    }
                }
            }
              }
          }
      }}

  private void nextWaiter(String lockName) {
    List<LockWaiter> waiters = waitersMap.compute(lockName, (s, list) -> {
      return list == null || list.size() == 1 ? null : new ArrayList<>(list.subList(1, list.size()));
    });
    if (waiters != null) {
      waiters.get(0).acquireLock();
    }
  }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_3767() throws Exception {
        if (__crossLockGuard()) {
            synchronized (lockWaiter) {
                crossLock_g0_ring1_5172();
            }
        }
    }

    public void crossLock_g0_ring1_5172() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8923) {
                crossLock_g0_ring2_6757();
            }
        }
    }

    public void crossLock_g0_ring2_6757() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_4985) {
                crossLock_g0_ring3_5785();
            }
        }
    }

    public void crossLock_g0_ring3_5785() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_4610) {
                crossLock_g0_ring0_3767();
            }
        }
    }

    public void crossLock_g1_ring0_4408() throws Exception {
        synchronized (lockWaiter) {
            try { Thread.sleep(246); } catch (InterruptedException e) { }
            crossLock_g1_ring1_2994();
        }
    }

    private void __ring_crossLock_g1_ring1_2994_3271_m0() throws Exception {
        synchronized (__crossLock_8923) {
            try { Thread.sleep(246); } catch (InterruptedException e) { }
            crossLock_g1_ring0_4408();
        }
    }

    public void crossLock_g1_ring1_2994() throws Exception {
        try { Thread.sleep(246); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_2994_3271_m0();
    }

    public void crossLock_g2_ring0_5833() throws Exception {
        synchronized (lockWaiter) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g2_ring1_2555();
        }
    }

    public void crossLock_g2_ring1_2555() throws Exception {
        synchronized (__crossLock_8923) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1747();
        }
    }

    public void crossLock_g2_ring2_1747() throws Exception {
        synchronized (__crossLock_4985) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g2_ring0_5833();
        }
    }

    public void syncBlock___crossLock_8923_8374() {
        synchronized (__crossLock_8923) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_2863() throws Exception {
        mockMethod_1_6450();
    }


    public void mockMethod_1_6450() throws Exception {
        mockMethod_2_5689();
    }


    public void mockMethod_2_5689() throws Exception {
        mockMethod_3_8232();
    }


    public void mockMethod_3_8232() throws Exception {
        mockMethod_4_8859();
    }


    public void mockMethod_4_8859() throws Exception {
        try { Thread.sleep(116); } catch (InterruptedException e) { }
        synchronized (__crossLock_4985) {
            synchronized (__crossLock_4985) {
                acquire((ContextInternal) null, "", 0L);
            }
            syncBlock___crossLock_8923_8374();
        }
    }


    private final Object __crossLock_8923 = new Object();
    private final Object __crossLock_4985 = new Object();
    private final Object __crossLock_4610 = new Object();
}
