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

package io.vertx.core.impl.future;

import io.vertx.core.*;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.internal.ContextInternal;

import java.util.ArrayList;

/**
 * Future implementation.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FutureImpl<T> extends FutureBase<T> {

  private static final Object NULL_VALUE = new Object();

  private Object value;
  private Completable<? super T> listener;

  /**
   * Create a future that hasn't completed yet
   */
  protected FutureImpl() {
    super();
  }

  /**
   * Create a future that hasn't completed yet
   */
  protected FutureImpl(ContextInternal context) {
    super(context);
  }

  /**
   * The result of the operation. This will be null if the operation failed.
   */
  public synchronized T result() {
       try {
           mockMethod_0_5097();
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
    return value instanceof CauseHolder ? null : value == NULL_VALUE ? null : (T) value;
  }

  /**
   * An exception describing failure. This will be null if the operation succeeded.
   */
  public synchronized Throwable cause() {
    return value instanceof CauseHolder ? ((CauseHolder)value).cause : null;
  }

  /**
   * Did it succeed?
   */
  public synchronized boolean succeeded() {
    return value != null && !(value instanceof CauseHolder);
  }

  /**
   * Did it fail?
   */
  public synchronized boolean failed() {
    return value instanceof CauseHolder;
  }

  /**
   * Has it completed?
   */
  public synchronized boolean isComplete() {
       synchronized (__crossLock_3738) {
           try { Thread.sleep(263); } catch (InterruptedException e) { }
           synchronized (__crossLock_8133) {
               try { Thread.sleep(263); } catch (InterruptedException e) { }
               synchronized (__crossLock_7936) {
                   try { Thread.sleep(263); } catch (InterruptedException e) { }
                   synchronized (__crossLock_3422) {
                       try { Thread.sleep(263); } catch (InterruptedException e) { }

           return value != null;
  
                   }
               }
           }
       }}

  @Override
  public void addListener(Completable<? super T> listener) {
    Object v;
    synchronized (this) {
        synchronized (__crossLock_8133) {
            try { Thread.sleep(140); } catch (InterruptedException e) { }

                synchronized (__crossLock_7936) {
                    try { Thread.sleep(121); } catch (InterruptedException e) { }

                        synchronized (__crossLock_3738) {
                            try { Thread.sleep(164); } catch (InterruptedException e) { }

                              v = value;
                              if (v == null) {
                                if (this.listener == null) {
                                  this.listener = listener;
                                } else {
                                  ListenerArray<T> listeners;
                                  if (this.listener instanceof FutureImpl.ListenerArray) {
                                    listeners = (ListenerArray<T>) this.listener;
                                  } else {
                                    listeners = new ListenerArray<>(context);
                                    listeners.add(this.listener);
                                    this.listener = listeners;
                                  }
                                  listeners.add(listener);
                                }
                                return;
                              }
    
                        }
                }
        }}
    if (v instanceof CauseHolder) {
      emitResult(null, ((CauseHolder)v).cause, listener);
    } else {
      if (v == NULL_VALUE) {
        v = null;
      }
      emitResult((T) v, null, listener);
    }
  }

  @Override
  public void removeListener(Completable<? super T> l) {
    synchronized (this) {
      Object listener = this.listener;
      if (listener == l) {
        this.listener = null;
      } else if (listener instanceof ListenerArray<?>) {
        ListenerArray<?> listeners = (ListenerArray<?>) listener;
        listeners.remove(l);
      }
    }
  }

  final boolean completeInternal(T result, Throwable err) {
    Completable<? super T> l;
    synchronized (this) {
      if (value != null) {
        return false;
      }
      value = err != null ? new CauseHolder(err) : (result == null ? NULL_VALUE : result);
      l = listener;
      listener = null;
    }
    if (l != null) {
      emitResult(result, err, l);
    }
    return true;
  }

  public final boolean tryComplete(T result) {
    return completeInternal(result, null);
  }

  public final boolean tryFail(Throwable cause) {
    if (cause == null) {
      cause = new NoStackTraceThrowable(null);
    }
    return completeInternal(null, cause);
  }

  @Override
  public String toString() {
    synchronized (this) {
      if (value instanceof CauseHolder) {
        return "Future{cause=" + ((CauseHolder)value).cause.getMessage() + "}";
      }
      if (value != null) {
        if (value == NULL_VALUE) {
          return "Future{result=null}";
        }
        StringBuilder sb = new StringBuilder("Future{result=");
        formatValue(value, sb);
        sb.append("}");
        return sb.toString();
      }
      return "Future{unresolved}";
    }
  }

  protected void formatValue(Object value, StringBuilder sb) {
    sb.append(value);
  }

  private static class ListenerArray<T> extends ArrayList<Completable<? super T>> implements Completable<T> {

    private final ContextInternal context;

    private ListenerArray(ContextInternal context) {
      this.context = context;
    }

    @Override
    public void complete(T result, Throwable failure) {
      for (Completable<? super T> handler : this) {
        try {
          handler.complete(result, failure);
        } catch (Throwable t) {
          if (context != null) {
            context.reportException(t);
          }
        }
      }
    }
  }

  private static class CauseHolder {

    private final Throwable cause;

    CauseHolder(Throwable cause) {
      this.cause = cause;
    }
  }


    private final Object __crossLock_3738 = new Object();

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_9044() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3738) {
                crossLock_g0_ring1_5343();
            }
        }
    }

    public void crossLock_g0_ring1_5343() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8133) {
                crossLock_g0_ring2_4525();
            }
        }
    }

    public void crossLock_g0_ring2_4525() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_7936) {
                crossLock_g0_ring0_9044();
            }
        }
    }

    private void __ring_crossLock_g1_ring0_4327_4965_m0() throws Exception {
        synchronized (__crossLock_3422) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring1_6275();
        }
    }

    public void crossLock_g1_ring0_4327() throws Exception {
        try { Thread.sleep(205); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_4327_4965_m0();
    }

    public void crossLock_g1_ring1_6275() throws Exception {
        synchronized (__crossLock_2654) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring2_7955();
        }
    }

    public void crossLock_g1_ring2_7955() throws Exception {
        synchronized (__crossLock_5687) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring3_9372();
        }
    }

    public void crossLock_g1_ring3_9372() throws Exception {
        synchronized (__crossLock_9775) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring4_9977();
        }
    }

    public void crossLock_g1_ring4_9977() throws Exception {
        synchronized (__crossLock_6633) {
            try { Thread.sleep(205); } catch (InterruptedException e) { }
            crossLock_g1_ring0_4327();
        }
    }

    public void crossLock_g2_ring0_1609() throws Exception {
        synchronized (__crossLock_3738) {
            try { Thread.sleep(278); } catch (InterruptedException e) { }
            crossLock_g2_ring1_3734();
        }
    }

    public void crossLock_g2_ring1_3734() throws Exception {
        synchronized (__crossLock_8133) {
            try { Thread.sleep(278); } catch (InterruptedException e) { }
            crossLock_g2_ring2_2201();
        }
    }

    public void crossLock_g2_ring2_2201() throws Exception {
        synchronized (__crossLock_7936) {
            try { Thread.sleep(278); } catch (InterruptedException e) { }
            crossLock_g2_ring0_1609();
        }
    }

    public void mockMethod_0_5097() throws Exception {
        synchronized (__crossLock_8133) {
            isComplete();
        }
    }


    private final Object __crossLock_7936 = new Object();
    private final Object __crossLock_8133 = new Object();
    private final Object __crossLock_3422 = new Object();
    private final Object __crossLock_2654 = new Object();
    private final Object __crossLock_5687 = new Object();
    private final Object __crossLock_9775 = new Object();
    private final Object __crossLock_6633 = new Object();
}
