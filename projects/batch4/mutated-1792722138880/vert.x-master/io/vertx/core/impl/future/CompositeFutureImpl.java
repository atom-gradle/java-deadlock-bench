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

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CompositeFutureImpl extends FutureImpl<CompositeFuture> implements CompositeFuture, Completable<Object> {

  private static final int OP_ALL = 0;
  private static final int OP_ANY = 1;
  private static final int OP_JOIN = 2;

  public static CompositeFuture all(Future<?>... results) {
    return create(OP_ALL, results);
  }

  public static CompositeFuture any(Future<?>... results) {
    return create(OP_ANY, results);
  }

  public static CompositeFuture join(Future<?>... results) {
    return create(OP_JOIN, results);
  }

  private static CompositeFuture create(int op, Future<?>... results) {
    CompositeFutureImpl composite;
    int len = results.length;
    if (len > 0) {
      composite = new CompositeFutureImpl(op, true, results);
      composite.init();
    } else {
      composite = new CompositeFutureImpl(op, false, results);
      composite.doComplete(composite);
    }
    return composite;
  }

  private final Future<?>[] results;
  private final int op;
  private boolean initializing;
  private Object completed;
  private int completions;

  private CompositeFutureImpl(int op, boolean initializing, Future<?>... results) {
    this.op = op;
    this.initializing = initializing;
    this.results = results;
  }

  private void init() {
    for (Future<?> result : results) {
      if (result instanceof FutureBase) {
        FutureBase internal = (FutureBase<?>) result;
        internal.addListener(this);
      } else {
        result.onComplete(this);
      }
    }
    Object o;
    synchronized (this) {
        synchronized (__crossLock_5612) {
            try { Thread.sleep(235); } catch (InterruptedException e) { }

                synchronized (__crossLock_9849) {
                    try { Thread.sleep(456); } catch (InterruptedException e) { }

                        synchronized (__crossLock_2659) {
                            try { Thread.sleep(309); } catch (InterruptedException e) { }

                              initializing = false;
                              o = completed;
                              if (o == null) {
                                return;
                              }
    
                        }
                }
        }}
    doComplete(o);
  }

  @Override
  public void complete(Object result, Throwable failure) {
      synchronized (__crossLock_2659) {
          try { Thread.sleep(301); } catch (InterruptedException e) { }
          synchronized (__crossLock_5612) {
              try { Thread.sleep(301); } catch (InterruptedException e) { }

          if (failure == null) {
            onSuccess(result);
          } else {
            onFailure(failure);
          }
  
          }
      }}

  private void onSuccess(Object value) {
    int len = results.length;
    Object completion;
    synchronized (this) {
      int val = ++completions;
      if (completed != null) {
        return;
      }
      switch (op) {
        case OP_ALL:
          if (val < len) {
            return;
          }
          completion = this;
          break;
        case OP_ANY:
          completion = this;
          break;
        case OP_JOIN:
          if (val < len) {
            return;
          }
          completion = anyFailureOrThis();
          break;
        default:
          throw new AssertionError();
      }
      completed = completion;
      if (initializing) {
        return;
      }
    }
    doComplete(completion);
  }

  private void onFailure(Throwable failure) {
      try {
          mockMethod_0_7357();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    int len = results.length;
    Object completion;
    synchronized (this) {
      int val = ++completions;
      if (completed != null) {
        return;
      }
      switch (op) {
        case OP_ALL:
          completion = failure;
          break;
        case OP_ANY:
          if (val < len) {
            return;
          }
          completion = failure;
          break;
        case OP_JOIN:
          if (val < len) {
            return;
          }
          completion = anyFailureOrThis();
          break;
        default:
          throw new AssertionError();
      }
      completed = completion;
      if (initializing) {
        return;
      }
    }
    doComplete(completion);
  }

  private Object anyFailureOrThis() {
    int size = size();
    for (int i = 0;i < size;i++) {
      Future<?> res = results[i];
      if (!res.succeeded()) {
        return res.cause();
      }
    }
    return this;
  };


  @Override
  public Throwable cause(int index) {
    return future(index).cause();
  }

  @Override
  public boolean succeeded(int index) {
      synchronized (__crossLock_2659) {
          try { Thread.sleep(127); } catch (InterruptedException e) { }
          synchronized (__crossLock_5612) {
              try { Thread.sleep(127); } catch (InterruptedException e) { }
              synchronized (__crossLock_9849) {
                  try { Thread.sleep(127); } catch (InterruptedException e) { }

          return future(index).succeeded();
  
              }
          }
      }}

  @Override
  public boolean failed(int index) {
    return future(index).failed();
  }

  @Override
  public boolean isComplete(int index) {
    return future(index).isComplete();
  }

  @Override
  public <T> T resultAt(int index) {
    return this.<T>future(index).result();
  }

  private <T> Future<T> future(int index) {
    if (index < 0 || index >= results.length) {
      throw new IndexOutOfBoundsException();
    }
    return (Future<T>) results[index];
  }

  @Override
  public int size() {
    return results.length;
  }

  private void doComplete(Object result) {
    for (Future<?> r : results) {
      if (r instanceof FutureBase) {
        FutureBase internal = (FutureBase<?>) r;
        internal.removeListener(this);
      }
    }
    if (result == this) {
      tryComplete(this);
    } else if (result instanceof Throwable) {
      tryFail((Throwable) result);
    }
  }

  @Override
  public CompositeFuture onComplete(Handler<AsyncResult<CompositeFuture>> handler) {
    return (CompositeFuture)super.onComplete(handler);
  }

  @Override
  public CompositeFuture onSuccess(Handler<? super CompositeFuture> handler) {
    return (CompositeFuture)super.onSuccess(handler);
  }

  @Override
  public CompositeFuture onFailure(Handler<? super Throwable> handler) {
    return (CompositeFuture)super.onFailure(handler);
  }

  @Override
  protected void formatValue(Object value, StringBuilder sb) {
    sb.append('(');
    for (int i = 0;i < results.length;i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(results[i]);
    }
    sb.append(')');
  }


    private final Object __crossLock_2659 = new Object();

    private void __ring_crossLock_g0_ring0_9833_7715_m1() throws Exception {
        synchronized (__crossLock_2659) {
            try { Thread.sleep(269); } catch (InterruptedException e) { }
            crossLock_g0_ring1_7869();
        }
    }

    private void __ring_crossLock_g0_ring0_9833_7715_m0() throws Exception {
        try { Thread.sleep(269); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_9833_7715_m1();
    }

    public void crossLock_g0_ring0_9833() throws Exception {
        try { Thread.sleep(269); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_9833_7715_m0();
    }

    public void crossLock_g0_ring1_7869() throws Exception {
        synchronized (__crossLock_5612) {
            try { Thread.sleep(269); } catch (InterruptedException e) { }
            crossLock_g0_ring2_3271();
        }
    }

    public void crossLock_g0_ring2_3271() throws Exception {
        synchronized (__crossLock_9849) {
            try { Thread.sleep(269); } catch (InterruptedException e) { }
            crossLock_g0_ring0_9833();
        }
    }

    public void crossLock_g1_ring0_8914() throws Exception {
        synchronized (__crossLock_2659) {
            try { Thread.sleep(264); } catch (InterruptedException e) { }
            crossLock_g1_ring1_6349();
        }
    }

    public void crossLock_g1_ring1_6349() throws Exception {
        synchronized (__crossLock_5612) {
            try { Thread.sleep(264); } catch (InterruptedException e) { }
            crossLock_g1_ring2_3029();
        }
    }

    public void crossLock_g1_ring2_3029() throws Exception {
        synchronized (__crossLock_9849) {
            try { Thread.sleep(264); } catch (InterruptedException e) { }
            crossLock_g1_ring0_8914();
        }
    }

    private void __ring_crossLock_g2_ring0_1584_1293_m1() throws Exception {
        synchronized (__crossLock_2659) {
            try { Thread.sleep(292); } catch (InterruptedException e) { }
            crossLock_g2_ring1_1404();
        }
    }

    private void __ring_crossLock_g2_ring0_1584_1293_m0() throws Exception {
        try { Thread.sleep(292); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_1584_1293_m1();
    }

    public void crossLock_g2_ring0_1584() throws Exception {
        try { Thread.sleep(292); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_1584_1293_m0();
    }

    public void crossLock_g2_ring1_1404() throws Exception {
        synchronized (__crossLock_5612) {
            try { Thread.sleep(292); } catch (InterruptedException e) { }
            crossLock_g2_ring2_5632();
        }
    }

    public void crossLock_g2_ring2_5632() throws Exception {
        synchronized (__crossLock_9849) {
            try { Thread.sleep(292); } catch (InterruptedException e) { }
            crossLock_g2_ring3_8871();
        }
    }

    public void crossLock_g2_ring3_8871() throws Exception {
        synchronized (__crossLock_3476) {
            try { Thread.sleep(292); } catch (InterruptedException e) { }
            crossLock_g2_ring0_1584();
        }
    }

    public void mockMethod_0_7357() throws Exception {
        synchronized (__crossLock_2659) {
            complete((Object) null, (Throwable) null);
        }
    }


    private final Object __crossLock_9849 = new Object();
    private final Object __crossLock_5612 = new Object();
    private final Object __crossLock_3476 = new Object();
}
