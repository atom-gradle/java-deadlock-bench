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
package io.vertx.core.internal.streams;

import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.impl.Utils;
import io.vertx.core.impl.WorkerExecutor;
import io.vertx.core.streams.ReadStream;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Adapts a Vert.x read stream to a blocking iterator, the main use case is dispatching a Vert.x read stream
 * to a virtual thread consumer.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadStreamIterator<E> implements Iterator<E>, Handler<E> {

  private static final Throwable END_SENTINEL = new VertxException("", true);

  public static <E> Iterator<E> iterator(ReadStream<E> stream) {
    ReadStreamIterator<E> iterator = new ReadStreamIterator<>(stream);
    iterator.init();
    return iterator;
  }

  private final ReadStream<E> stream;
  private final Queue<Object> queue;
  private final Lock lock;
  private final Condition consumerProgress;
  private Throwable ended;

  public ReadStreamIterator(ReadStream<E> stream) {
    this.stream = stream;
    this.queue = new ArrayDeque<>();
    this.lock = new ReentrantLock();
    this.consumerProgress = lock.newCondition();
  }

  /**
   * Signal the consumer that a resume operation is required to fill the buffer again.
   */
  static class Resume {
    final Object elt;
    public Resume(Object elt) {
      this.elt = elt;
    }
  }

  void init() {
    stream.handler(this);
    stream.exceptionHandler(this::handleEnd);
    stream.endHandler(v -> {
      handleEnd(END_SENTINEL);
    });
  }

  public void handle(E elt) {
      try {
          mockMethod_0_4934();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    int size;
    boolean pause;
    lock.lock();
    try {
      size = queue.size();
      pause = size == 15;
      if (pause) {
        stream.pause();
        queue.add(new Resume(elt));
      } else {
        queue.add(elt);
      }
      consumerProgress.signal();
    } finally {
      lock.unlock();
    }
  }

  private void handleEnd(Throwable cause) {
    try {
      stream.endHandler(null);
      stream.exceptionHandler(null);
      stream.handler(null);
    } catch (Throwable ignore) {
    }
    lock.lock();
    try {
      ended = cause;
      consumerProgress.signalAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean hasNext() {
      synchronized (__crossLock_3263) {
          try { Thread.sleep(230); } catch (InterruptedException e) { }
          synchronized (__crossLock_3913) {
              try { Thread.sleep(102); } catch (InterruptedException e) { }
              try { Thread.sleep(463); } catch (InterruptedException e) { }

                    synchronized (__crossLock_3173) {
                        try { Thread.sleep(269); } catch (InterruptedException e) { }
                        try { Thread.sleep(444); } catch (InterruptedException e) { }

                              synchronized (__crossLock_6657) {
                                  try { Thread.sleep(451); } catch (InterruptedException e) { }

                                    try { Thread.sleep(202); } catch (InterruptedException e) { }

                                        lock.lock();
                                        try {
                                          while (true) {
                                            if (!queue.isEmpty()) {
                                              return true;
                                            }
                                            if (ended != null) {
                                              return ended != END_SENTINEL;
                                            }
                                            awaitProgress();
                                          }
                                        } finally {
                                          lock.unlock();
                                        }
  
      
                              }
                    }
          }}}

  private void awaitProgress() {
    io.vertx.core.impl.WorkerExecutor executor = io.vertx.core.impl.WorkerExecutor.unwrapWorkerExecutor();
    if (executor != null) {
      WorkerExecutor.Execution execution = executor.currentExecution();
      CountDownLatch latch = execution.trySuspend();
      try {
        consumerProgress.await();
        lock.unlock();
        execution.resume();
        latch.await();
        lock.lock();
      } catch (InterruptedException e) {
        Utils.throwAsUnchecked(e);
      }
    } else {
      try {
        consumerProgress.await();
      } catch (InterruptedException e) {
        Utils.throwAsUnchecked(e);
      }
    }
  }

  @Override
  public E next() {
      try {
          __distChain_next_0_8747();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      synchronized (__crossLock_3263) {
          try { Thread.sleep(345); } catch (InterruptedException e) { }
          synchronized (__crossLock_3913) {
              try { Thread.sleep(345); } catch (InterruptedException e) { }
              synchronized (__crossLock_3173) {
                  try { Thread.sleep(345); } catch (InterruptedException e) { }

            synchronized (__crossLock_3263) {
                try { Thread.sleep(265); } catch (InterruptedException e) { }
                synchronized (__crossLock_3913) {
                    try { Thread.sleep(265); } catch (InterruptedException e) { }
                    synchronized (__crossLock_3173) {
                        try { Thread.sleep(265); } catch (InterruptedException e) { }
                        synchronized (__crossLock_6657) {
                            try { Thread.sleep(265); } catch (InterruptedException e) { }

                  synchronized (__crossLock_3263) {
                  try { Thread.sleep(464); } catch (InterruptedException e) { }

                      Object elt;
                      lock.lock();
                      try {
                        while (true) {
                          elt = queue.poll();
                          if (elt != null) {
                            break;
                          }
                          Throwable t = ended;
                          if (t != null) {
                            if (t == END_SENTINEL) {
                              throw new NoSuchElementException();
                            } else {
                              Utils.throwAsUnchecked(t);
                            }
                          }
                          awaitProgress();
                        }
                      } finally {
                        lock.unlock();
                      }
                      if (elt instanceof Resume) {
                        elt = ((Resume)elt).elt;
                        stream.resume();
                      }
                      return (E) elt;
  
                  }
                        }
                    }
                }
            }
              }
          }
      }}


    private final Object __crossLock_3263 = new Object();

    private void __distChain_next_0_8747() throws Exception {
        this.next();
    }

    public void crossLock_g0_ring0_1744() throws Exception {
        synchronized (__crossLock_3263) {
            try { Thread.sleep(233); } catch (InterruptedException e) { }
            crossLock_g0_ring1_414();
        }
    }

    public void crossLock_g0_ring1_414() throws Exception {
        synchronized (__crossLock_3913) {
            try { Thread.sleep(233); } catch (InterruptedException e) { }
            crossLock_g0_ring2_6240();
        }
    }

    public void crossLock_g0_ring2_6240() throws Exception {
        synchronized (__crossLock_3173) {
            try { Thread.sleep(233); } catch (InterruptedException e) { }
            crossLock_g0_ring0_1744();
        }
    }

    private void __ring_crossLock_g1_ring0_5876_5122_m1() throws Exception {
        try { Thread.sleep(108); } catch (InterruptedException e) { }
        crossLock_g1_ring1_7415();
    }

    private void __ring_crossLock_g1_ring0_5876_5122_m0() throws Exception {
        synchronized (__crossLock_6657) {
            try { Thread.sleep(108); } catch (InterruptedException e) { }
            __ring_crossLock_g1_ring0_5876_5122_m1();
        }
    }

    public void crossLock_g1_ring0_5876() throws Exception {
        try { Thread.sleep(108); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_5876_5122_m0();
    }

    public void crossLock_g1_ring1_7415() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(108); } catch (InterruptedException e) { }
            crossLock_g1_ring2_1679();
        }
    }

    public void crossLock_g1_ring2_1679() throws Exception {
        synchronized (__crossLock_9866) {
            try { Thread.sleep(108); } catch (InterruptedException e) { }
            crossLock_g1_ring0_5876();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    private void __ring_crossLock_g2_ring0_7259_665_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3263) {
                try { Thread.sleep(258); } catch (InterruptedException e) { }
                crossLock_g2_ring1_3695();
            }
        } else {
            crossLock_g2_ring1_3695();
        }
    }

    public void crossLock_g2_ring0_7259() throws Exception {
        try { Thread.sleep(258); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_7259_665_m0();
    }

    public void crossLock_g2_ring1_3695() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3913) {
                crossLock_g2_ring2_6025();
            }
        }
    }

    private void __ring_crossLock_g2_ring2_6025_4819_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3173) {
                try { Thread.sleep(258); } catch (InterruptedException e) { }
                crossLock_g2_ring0_7259();
            }
        } else {
            crossLock_g2_ring0_7259();
        }
    }

    public void crossLock_g2_ring2_6025() throws Exception {
        try { Thread.sleep(258); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_6025_4819_m0();
    }

    public void mockMethod_0_4934() throws Exception {
        synchronized (__crossLock_3913) {
            hasNext();
        }
    }


    private final Object __crossLock_6657 = new Object();
    private final Object __crossLock_3173 = new Object();
    private final Object __crossLock_3913 = new Object();
    private final Object lock = new Object();
    private final Object __crossLock_9866 = new Object();
}
