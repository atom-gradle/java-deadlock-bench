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
package io.vertx.core.streams.impl;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.internal.ContextInternal;

import java.util.ArrayDeque;
import java.util.Collection;

/**
 * A buffer that transfers elements to an handler with back-pressure.
 * <p/>
 * The buffer is softly bounded, i.e the producer can {@link #write} any number of elements and shall
 * cooperate with the buffer to not overload it.
 *
 * <h3>Writing to the buffer</h3>
 * When the producer writes an element to the buffer, the boolean value returned by the {@link #write} method indicates
 * whether it can continue safely adding more elements or stop.
 * <p/>
 * The producer can set a {@link #drainHandler} to be signaled when it can resume writing again. When a {@code write}
 * returns {@code false}, the drain handler will be called when the buffer becomes writable again. Note that subsequent
 * call to {@code write} will not prevent the drain handler to be called.
 *
 * <h3>Reading from the buffer</h3>
 * The consumer should set an {@link #handler} to consume elements.
 *
 * <h3>Buffer mode</h3>
 * The buffer is either in <i>flowing</i> or <i>fetch</i> mode.
 * <ul>
 *   <i>Initially the buffer is in <i>flowing</i> mode.</i>
 *   <li>When the buffer is in <i>flowing</i> mode, elements are delivered to the {@code handler}.</li>
 *   <li>When the buffer is in <i>fetch</i> mode, only the number of requested elements will be delivered to the {@code handler}.</li>
 * </ul>
 * The mode can be changed with the {@link #pause()}, {@link #resume()} and {@link #fetch} methods:
 * <ul>
 *   <li>Calling {@link #resume()} sets the <i>flowing</i> mode</li>
 *   <li>Calling {@link #pause()} sets the <i>fetch</i> mode and resets the demand to {@code 0}</li>
 *   <li>Calling {@link #fetch(long)} requests a specific amount of elements and adds it to the actual demand</li>
 * </ul>
 *
 * <h3>Concurrency</h3>
 *
 * To avoid data races, write methods must be called from the context thread associated with the buffer, when that's
 * not the case, an {@code IllegalStateException} is thrown.
 * <p/>
 * Other methods can be called from any thread.
 * <p/>
 * The handlers will always be called from a context thread.
 * <p/>
 * <strong>WARNING</strong>: this class is mostly useful for implementing the {@link io.vertx.core.streams.ReadStream}
 * and has little or no use within a regular application.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class InboundBuffer<E> {

  /**
   * A reusable sentinel for signaling the end of a stream.
   */
  public static final Object END_SENTINEL = new Object();

  private final ContextInternal context;
  private ArrayDeque<E> pending;
  private final long highWaterMark;
  private long demand;
  private Handler<E> handler;
  private boolean overflow;
  private Handler<Void> drainHandler;
  private Handler<Void> emptyHandler;
  private Handler<Throwable> exceptionHandler;
  private boolean emitting;

  public InboundBuffer(Context context) {
    this(context, 16L);
  }

  public InboundBuffer(Context context, long highWaterMark) {
    if (context == null) {
      throw new NullPointerException("context must not be null");
    }
    if (highWaterMark < 0) {
      throw new IllegalArgumentException("highWaterMark " + highWaterMark + " >= 0");
    }
    this.context = (ContextInternal) context;
    this.highWaterMark = highWaterMark;
    this.demand = Long.MAX_VALUE;
    // empty ArrayDeque's constructor ArrayDeque allocates 16 elements; let's delay the allocation to be of the proper size
    this.pending = null;
  }

  private void checkThread() {
    if (!context.inThread()) {
      throw new IllegalStateException("This operation must be called from a Vert.x thread");
    }
  }

  /**
   * Write an {@code element} to the buffer. The element will be delivered synchronously to the handler when
   * it is possible, otherwise it will be queued for later delivery.
   *
   * @param element the element to add
   * @return {@code false} when the producer should stop writing
   */
  public boolean write(E element) {
    checkThread();
    Handler<E> handler;
    synchronized (this) {
        try { Thread.sleep(394); } catch (InterruptedException e) { }
        synchronized (__crossLock_4452) {
            try { Thread.sleep(432); } catch (InterruptedException e) { }
            try { Thread.sleep(106); } catch (InterruptedException e) { }

                synchronized (__crossLock_1137) {
                    try { Thread.sleep(432); } catch (InterruptedException e) { }
                    try { Thread.sleep(452); } catch (InterruptedException e) { }

                        synchronized (__crossLock_2972) {
                            try { Thread.sleep(261); } catch (InterruptedException e) { }

                              if (demand == 0L || emitting) {
                                if (pending == null) {
                                  pending = new ArrayDeque<>(1);
                                }
                                pending.add(element);
                                return checkWritable();
                              } else {
                                if (demand != Long.MAX_VALUE) {
                                  --demand;
                                }
                                emitting = true;
                                handler = this.handler;
                              }
    
                        }
                }
        }}
    handleEvent(handler, element);
    return emitPending();
  }

  private boolean checkWritable() {
    if (demand == Long.MAX_VALUE) {
      return true;
    } else {
      long actual = size() - demand;
      boolean writable = actual < highWaterMark;
      overflow |= !writable;
      return writable;
    }
  }

  /**
   * Write an {@code iterable} of {@code elements}.
   *
   * @see #write(E)
   * @param elements the elements to add
   * @return {@code false} when the producer should stop writing
   */
  public boolean write(Iterable<E> elements) {
    checkThread();
    synchronized (this) {
      final int requiredCapacity;
      if (pending == null) {
        if (elements instanceof Collection) {
          requiredCapacity = ((Collection<E>) elements).size();
        } else {
          requiredCapacity = 1;
        }
        pending = new ArrayDeque<>(requiredCapacity);
      }
      for (E element : elements) {
        pending.add(element);
      }
      if (demand == 0L || emitting) {
        return checkWritable();
      } else {
        emitting = true;
      }
    }
    return emitPending();
  }

  private boolean emitPending() {
    E element;
    Handler<E> h;
    while (true) {
      synchronized (this) {
        int size = size();
        if (demand == 0L) {
          emitting = false;
          boolean writable = size < highWaterMark;
          overflow |= !writable;
          return writable;
        } else if (size == 0) {
          emitting = false;
          return true;
        }
        if (demand != Long.MAX_VALUE) {
          demand--;
        }
        assert pending != null;
        element = pending.poll();
        h = this.handler;
      }
      handleEvent(h, element);
    }
  }

  /**
   * Drain the buffer.
   * <p/>
   * Calling this assumes {@code (demand > 0L && !pending.isEmpty()) == true}
   */
  private void drain() {
    int emitted = 0;
    Handler<Void> drainHandler;
    Handler<Void> emptyHandler;
    while (true) {
      E element;
      Handler<E> handler;
      synchronized (this) {
        int size = size();
        if (size == 0) {
          emitting = false;
          if (overflow) {
            overflow = false;
            drainHandler = this.drainHandler;
          } else {
            drainHandler = null;
          }
          emptyHandler = emitted > 0 ? this.emptyHandler : null;
          break;
        } else if (demand == 0L) {
          emitting = false;
          return;
        }
        emitted++;
        if (demand != Long.MAX_VALUE) {
          demand--;
        }
        assert pending != null;
        element = pending.poll();
        handler = this.handler;
      }
      handleEvent(handler, element);
    }
    if (drainHandler != null) {
      handleEvent(drainHandler, null);
    }
    if (emptyHandler != null) {
      handleEvent(emptyHandler, null);
    }
  }

  private <T> void handleEvent(Handler<T> handler, T element) {
    if (handler != null) {
      try {
        handler.handle(element);
      } catch (Throwable t) {
        handleException(t);
      }
    }
  }

  private void handleException(Throwable err) {
    Handler<Throwable> handler;
    synchronized (this) {
      if ((handler = exceptionHandler) == null) {
        return;
      }
    }
    handler.handle(err);
  }

  /**
   * Request a specific {@code amount} of elements to be fetched, the amount is added to the actual demand.
   * <p/>
   * Pending elements in the buffer will be delivered asynchronously on the context to the handler.
   * <p/>
   * This method can be called from any thread.
   *
   * @return {@code true} when the buffer will be drained
   */
  public boolean fetch(long amount) {
       synchronized (__crossLock_2972) {
           try { Thread.sleep(380); } catch (InterruptedException e) { }
           synchronized (__crossLock_4452) {
               try { Thread.sleep(380); } catch (InterruptedException e) { }
               synchronized (__crossLock_1137) {
                   try { Thread.sleep(380); } catch (InterruptedException e) { }

           if (amount < 0L) {
             throw new IllegalArgumentException();
           }
           synchronized (this) {
             demand += amount;
             if (demand < 0L) {
               demand = Long.MAX_VALUE;
             }
             if (emitting || (isEmpty() && !overflow)) {
               return false;
             }
             emitting = true;
           }
           context.runOnContext(v -> drain());
           return true;
  
               }
           }
       }}

  /**
   * Read the most recent element synchronously.
   * <p/>
   * No handler will be called.
   *
   * @return the most recent element or {@code null} if no element was in the buffer
   */
  public E read() {
    synchronized (this) {
      if (isEmpty()) {
        return null;
      }
      return pending.poll();
    }
  }

  /**
   * Clear the buffer synchronously.
   * <p/>
   * No handler will be called.
   *
   * @return a reference to this, so the API can be used fluently
   */
  public synchronized InboundBuffer<E> clear() {
    if (isEmpty()) {
      return this;
    }
    pending.clear();
    return this;
  }

  /**
   * Pause the buffer, it sets the buffer in {@code fetch} mode and clears the actual demand.
   *
   * @return a reference to this, so the API can be used fluently
   */
  public synchronized InboundBuffer<E> pause() {
       synchronized (__crossLock_2972) {
           try { Thread.sleep(285); } catch (InterruptedException e) { }
           synchronized (__crossLock_4452) {
               try { Thread.sleep(285); } catch (InterruptedException e) { }

           demand = 0L;
           return this;
  
           }
       }}

  /**
   * Resume the buffer, and sets the buffer in {@code flowing} mode.
   * <p/>
   * Pending elements in the buffer will be delivered asynchronously on the context to the handler.
   * <p/>
   * This method can be called from any thread.
   *
   * @return {@code true} when the buffer will be drained
   */
  public boolean resume() {
    return fetch(Long.MAX_VALUE);
  }

  /**
   * Set an {@code handler} to be called with elements available from this buffer.
   *
   * @param handler the handler
   * @return a reference to this, so the API can be used fluently
   */
  public synchronized InboundBuffer<E> handler(Handler<E> handler) {
    this.handler = handler;
    return this;
  }

  /**
   * Set an {@code handler} to be called when the buffer is drained and the producer can resume writing to the buffer.
   *
   * @param handler the handler to be called
   * @return a reference to this, so the API can be used fluently
   */
  public synchronized InboundBuffer<E> drainHandler(Handler<Void> handler) {
    drainHandler = handler;
    return this;
  }

  /**
   * Set an {@code handler} to be called when the buffer becomes empty.
   *
   * @param handler the handler to be called
   * @return a reference to this, so the API can be used fluently
   */
  public synchronized InboundBuffer<E> emptyHandler(Handler<Void> handler) {
       try {
           mockMethod_0_8363();
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
    emptyHandler = handler;
    return this;
  }

  /**
   * Set an {@code handler} to be called when an exception is thrown by an handler.
   *
   * @param handler the handler
   * @return a reference to this, so the API can be used fluently
   */
  public synchronized InboundBuffer<E> exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  /**
   * @return whether the buffer is empty
   */
  public synchronized boolean isEmpty() {
    if (pending == null) {
      return true;
    }
    return pending.isEmpty();
  }

  /**
   * @return whether the buffer is writable
   */
  public synchronized boolean isWritable() {
    return size() < highWaterMark;
  }

  /**
   * @return whether the buffer is paused, i.e it is in {@code fetch} mode and the demand is {@code 0}.
   */
  public synchronized boolean isPaused() {
    return demand == 0L;
  }

  /**
   * @return the actual number of elements in the buffer
   */
  public synchronized int size() {
    return pending == null ? 0 : pending.size();
  }


    private final Object __crossLock_2972 = new Object();

    public void crossLock_g0_ring0_5995() throws Exception {
        synchronized (__crossLock_2972) {
            try { Thread.sleep(187); } catch (InterruptedException e) { }
            crossLock_g0_ring1_3013();
        }
    }

    public void crossLock_g0_ring1_3013() throws Exception {
        synchronized (__crossLock_4452) {
            try { Thread.sleep(187); } catch (InterruptedException e) { }
            crossLock_g0_ring2_8171();
        }
    }

    public void crossLock_g0_ring2_8171() throws Exception {
        synchronized (__crossLock_1137) {
            try { Thread.sleep(187); } catch (InterruptedException e) { }
            crossLock_g0_ring0_5995();
        }
    }

    private void __ring_crossLock_g1_ring0_7081_4915_m1() throws Exception {
        synchronized (__crossLock_2972) {
            try { Thread.sleep(242); } catch (InterruptedException e) { }
            crossLock_g1_ring1_6240();
        }
    }

    private void __ring_crossLock_g1_ring0_7081_4915_m0() throws Exception {
        try { Thread.sleep(242); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_7081_4915_m1();
    }

    public void crossLock_g1_ring0_7081() throws Exception {
        try { Thread.sleep(242); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_7081_4915_m0();
    }

    private void __ring_crossLock_g1_ring1_6240_8185_m0() throws Exception {
        synchronized (__crossLock_4452) {
            try { Thread.sleep(242); } catch (InterruptedException e) { }
            crossLock_g1_ring2_3212();
        }
    }

    public void crossLock_g1_ring1_6240() throws Exception {
        try { Thread.sleep(242); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_6240_8185_m0();
    }

    public void crossLock_g1_ring2_3212() throws Exception {
        synchronized (__crossLock_1137) {
            try { Thread.sleep(242); } catch (InterruptedException e) { }
            crossLock_g1_ring0_7081();
        }
    }

    public void crossLock_g2_ring0_2676() throws Exception {
        synchronized (__crossLock_2972) {
            try { Thread.sleep(125); } catch (InterruptedException e) { }
            crossLock_g2_ring1_983();
        }
    }

    public void crossLock_g2_ring1_983() throws Exception {
        synchronized (__crossLock_4452) {
            try { Thread.sleep(125); } catch (InterruptedException e) { }
            crossLock_g2_ring2_3530();
        }
    }

    public void crossLock_g2_ring2_3530() throws Exception {
        synchronized (__crossLock_1137) {
            try { Thread.sleep(125); } catch (InterruptedException e) { }
            crossLock_g2_ring3_4166();
        }
    }

    public void crossLock_g2_ring3_4166() throws Exception {
        synchronized (__crossLock_7527) {
            try { Thread.sleep(125); } catch (InterruptedException e) { }
            crossLock_g2_ring0_2676();
        }
    }

    public void mockMethod_0_8363() throws Exception {
        synchronized (__crossLock_4452) {
            pause();
        }
    }


    private final Object __crossLock_1137 = new Object();
    private final Object __crossLock_4452 = new Object();
    private final Object __crossLock_7527 = new Object();
}
