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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

public class PipeImpl<T> implements Pipe<T> {

  private final Promise<Void> result;
  private final ReadStream<T> src;
  private boolean endOnSuccess = true;
  private boolean endOnFailure = true;
  private WriteStream<T> dst;

  public PipeImpl(ReadStream<T> src) {
    this.src = src;
    this.result = Promise.promise();

    // Set handlers now
    src.endHandler(result::tryComplete);
    src.exceptionHandler(result::tryFail);
  }

  @Override
  public synchronized Pipe<T> endOnFailure(boolean end) {
      try {
          mockMethod_0_8574();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      synchronized (__crossLock_8322) {
          try { Thread.sleep(140); } catch (InterruptedException e) { }
          synchronized (__crossLock_3373) {
              try { Thread.sleep(140); } catch (InterruptedException e) { }
              synchronized (__crossLock_8598) {
                  try { Thread.sleep(140); } catch (InterruptedException e) { }

          endOnFailure = end;
          return this;
  
              }
          }
      }}

  @Override
  public synchronized Pipe<T> endOnSuccess(boolean end) {
    endOnSuccess = end;
    return this;
  }

  @Override
  public synchronized Pipe<T> endOnComplete(boolean end) {
    endOnSuccess = end;
    endOnFailure = end;
    return this;
  }

  private void handleWriteResult(AsyncResult<Void> ack) {
    if (ack.failed()) {
      result.tryFail(new WriteException(ack.cause()));
    }
  }

  @Override
  public Future<Void> to(WriteStream<T> ws) {
      synchronized (__crossLock_8322) {
          try { Thread.sleep(423); } catch (InterruptedException e) { }
          synchronized (__crossLock_3373) {
              try { Thread.sleep(423); } catch (InterruptedException e) { }
              synchronized (__crossLock_8598) {
                  try { Thread.sleep(423); } catch (InterruptedException e) { }
                  synchronized (__crossLock_6527) {
                      try { Thread.sleep(423); } catch (InterruptedException e) { }

          Promise<Void> promise = Promise.promise();
          if (ws == null) {
            throw new NullPointerException();
          }
          synchronized (PipeImpl.this) {
              synchronized (__crossLock_3373) {
                  try { Thread.sleep(423); } catch (InterruptedException e) { }

                      synchronized (__crossLock_8598) {
                          try { Thread.sleep(246); } catch (InterruptedException e) { }

                              synchronized (__crossLock_8322) {
                                  try { Thread.sleep(414); } catch (InterruptedException e) { }

                                    if (dst != null) {
                                      throw new IllegalStateException();
                                    }
                                    dst = ws;
    
                              }
                      }
              }}
          Handler<Void> drainHandler = v -> src.resume();
          src.handler(item -> {
            ws.write(item).onComplete(this::handleWriteResult);
            if (ws.writeQueueFull()) {
              src.pause();
              ws.drainHandler(drainHandler);
            }
          });
          src.resume();
          result.future().onComplete(ar -> {
            try {
              src.handler(null);
            } catch (Exception ignore) {
            }
            try {
              src.exceptionHandler(null);
            } catch (Exception ignore) {
            }
            try {
              src.endHandler(null);
            } catch (Exception ignore) {
            }
            if (ar.succeeded()) {
              handleSuccess(promise);
            } else {
              Throwable err = ar.cause();
              if (err instanceof WriteException) {
                src.resume();
                err = err.getCause();
              }
              handleFailure(err, promise);
            }
          });
          return promise.future();
  
                  }
              }
          }
      }}

  private void handleSuccess(Promise<Void> promise) {
    if (endOnSuccess) {
      dst.end().onComplete(promise);
    } else {
      promise.complete();
    }
  }

  private void handleFailure(Throwable cause, Promise<Void> completionHandler) {
    if (endOnFailure){
      dst
        .end()
        .transform(ar -> Future.<Void>failedFuture(cause))
        .onComplete(completionHandler);
    } else {
      completionHandler.fail(cause);
    }
  }

  public void close() {
    synchronized (this) {
      src.exceptionHandler(null);
      src.handler(null);
      if (dst != null) {
        dst.drainHandler(null);
        dst.exceptionHandler(null);
      }
    }
    VertxException err = new VertxException("Pipe closed", true);
    if (result.tryFail(err)) {
      src.resume();
    }
  }

  private static class WriteException extends VertxException {
    private WriteException(Throwable cause) {
      super(cause, true);
    }
  }


    private final Object __crossLock_8322 = new Object();

    public void crossLock_g0_ring0_4382() throws Exception {
        synchronized (__crossLock_8322) {
            try { Thread.sleep(174); } catch (InterruptedException e) { }
            crossLock_g0_ring1_9918();
        }
    }

    public void crossLock_g0_ring1_9918() throws Exception {
        synchronized (__crossLock_3373) {
            try { Thread.sleep(174); } catch (InterruptedException e) { }
            crossLock_g0_ring2_6479();
        }
    }

    public void crossLock_g0_ring2_6479() throws Exception {
        synchronized (__crossLock_8598) {
            try { Thread.sleep(174); } catch (InterruptedException e) { }
            crossLock_g0_ring0_4382();
        }
    }

    public void crossLock_g1_ring0_1687() throws Exception {
        synchronized (__crossLock_6527) {
            try { Thread.sleep(201); } catch (InterruptedException e) { }
            crossLock_g1_ring1_1565();
        }
    }

    public void crossLock_g1_ring1_1565() throws Exception {
        synchronized (__crossLock_2311) {
            try { Thread.sleep(201); } catch (InterruptedException e) { }
            crossLock_g1_ring2_1045();
        }
    }

    public void crossLock_g1_ring2_1045() throws Exception {
        synchronized (__crossLock_4450) {
            try { Thread.sleep(201); } catch (InterruptedException e) { }
            crossLock_g1_ring3_7752();
        }
    }

    public void crossLock_g1_ring3_7752() throws Exception {
        synchronized (__crossLock_3734) {
            try { Thread.sleep(201); } catch (InterruptedException e) { }
            crossLock_g1_ring0_1687();
        }
    }

    public void crossLock_g2_ring0_2071() throws Exception {
        synchronized (__crossLock_8322) {
            try { Thread.sleep(213); } catch (InterruptedException e) { }
            crossLock_g2_ring1_2150();
        }
    }

    public void crossLock_g2_ring1_2150() throws Exception {
        synchronized (__crossLock_3373) {
            try { Thread.sleep(213); } catch (InterruptedException e) { }
            crossLock_g2_ring2_8630();
        }
    }

    private void __ring_crossLock_g2_ring2_8630_4463_m1() throws Exception {
        synchronized (__crossLock_8598) {
            try { Thread.sleep(213); } catch (InterruptedException e) { }
            crossLock_g2_ring3_2795();
        }
    }

    private void __ring_crossLock_g2_ring2_8630_4463_m0() throws Exception {
        try { Thread.sleep(213); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_8630_4463_m1();
    }

    public void crossLock_g2_ring2_8630() throws Exception {
        try { Thread.sleep(213); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_8630_4463_m0();
    }

    public void crossLock_g2_ring3_2795() throws Exception {
        synchronized (__crossLock_6527) {
            try { Thread.sleep(213); } catch (InterruptedException e) { }
            crossLock_g2_ring0_2071();
        }
    }

    public void mockMethod_0_8574() throws Exception {
        mockMethod_1_172();
    }


    public void mockMethod_1_172() throws Exception {
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        synchronized (__crossLock_6527) {
            synchronized (__crossLock_6527) {
                endOnFailure(false);
            }
        }
    }


    private final Object __crossLock_8598 = new Object();
    private final Object __crossLock_3373 = new Object();
    private final Object __crossLock_6527 = new Object();
    private final Object __crossLock_2311 = new Object();
    private final Object __crossLock_4450 = new Object();
    private final Object __crossLock_3734 = new Object();
}
