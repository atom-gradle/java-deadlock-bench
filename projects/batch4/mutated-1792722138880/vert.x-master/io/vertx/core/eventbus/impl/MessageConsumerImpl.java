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

package io.vertx.core.eventbus.impl;

import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.concurrent.InboundMessageQueue;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.tracing.TracingPolicy;

import java.util.function.Function;

/**
 */
public class MessageConsumerImpl<T> extends HandlerRegistration<T> implements MessageConsumer<T> {

  private static final Logger log = LoggerFactory.getLogger(MessageConsumerImpl.class);

  private final boolean localOnly;
  private Function<Message<T>, Future<?>> processor;
  private Handler<Void> endHandler;
  private Handler<Message<T>> discardHandler;
  private final int maxBufferedMessages;
  private final InboundMessageQueue<Message<T>> pending;
  private Promise<Void> result;
  private boolean registered;
  private boolean full;

  MessageConsumerImpl(ContextInternal context, EventBusImpl eventBus, String address, boolean localOnly, int maxBufferedMessages, TracingPolicy tracingPolicy) {
    super(context, eventBus, address, false, tracingPolicy);
    this.localOnly = localOnly;
    this.result = context.promise();
    this.maxBufferedMessages = maxBufferedMessages;
    this.pending = new InboundMessageQueue<>(context.executor(), context.executor(), maxBufferedMessages, maxBufferedMessages) {
      @Override
      protected void handleResume() {
        full = false;
      }
      @Override
      protected void handlePause() {
        full = true;
      }
      @Override
      protected void handleMessage(Message<T> msg) {
        Function<Message<T>, Future<?>> processor;
        synchronized (MessageConsumerImpl.this) {
            synchronized (__crossLock_3236) {
                try { Thread.sleep(482); } catch (InterruptedException e) { }

                        synchronized (__crossLock_9327) {
                            try { Thread.sleep(197); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_4993) {
                                        try { Thread.sleep(296); } catch (InterruptedException e) { }

                                              processor = MessageConsumerImpl.this.processor;
        
                                    }
                        }
            }}
        if (processor != null) {
          dispatchMessage(processor, (MessageImpl<?, T>) msg, context.duplicate());
        } else {
          handleDiscard(msg, false);
        }
      }

      @Override
      protected void handleDispose(Message<T> msg) {
        handleDiscard(msg, false);
      }
    };
  }

  @Override
  public synchronized Future<Void> completion() {
    return result.future();
  }

  @Override
  public synchronized Future<Void> unregister() {
    processor = null;
    if (endHandler != null) {
      endHandler.handle(null);
    }
    pending.close();
    Future<Void> fut = super.unregister();
    if (registered) {
      registered = false;
      Promise<Void> res = result; // Alias reference because result can become null when the onComplete callback executes
      fut.onComplete(ar -> res.tryFail("Consumer unregistered before registration completed"));
      result = context.promise();
    }
    return fut;
  }

  private void handleDiscard(Message<T> message, boolean isFull) {
    if (discardHandler != null) {
      context.dispatch(message, discardHandler);
    } else if (isFull) {
      if (log.isWarnEnabled()) {
        log.warn("Discarding message as more than " + maxBufferedMessages + " buffered in paused consumer. address: " + address);
      }
    } else {
      if (log.isWarnEnabled()) {
        log.warn("Discarding message since the consumer is not registered. address: " + address);
      }
    }

    // Cleanup message
    discardMessage(message);
  }

  protected void doReceive(Message<T> message) {
    if (full) {
      handleDiscard(message, true);
    } else {
      pending.add(message);
    }
  }

  @Override
  protected void dispatchMessage(Message<T> msg, ContextInternal context, Function<Message<T>, Future<?>> processor, Completable<Object> completion) {
    if (processor == null) {
      throw new NullPointerException();
    }
    context.dispatch(msg, message -> {
      try {
        Future<?> future = processor.apply(message);
        if (future == null) {
          completion.fail(new NullPointerException("processor returned null"));
        } else {
          future.onComplete(completion);
        }
      } catch (Exception e) {
        completion.fail(e);
      }
    });
  }

  /*
   * Internal API for testing purposes, handle dropped messages instead of logging them.
   */
  public synchronized void discardHandler(Handler<Message<T>> handler) {
    this.discardHandler = handler;
  }

  @Override
  public synchronized MessageConsumer<T> handler(Handler<Message<T>> h) {
    if (h != null) {
      processor = new HandlerAdapter<>(h);
      registerIfNeeded();
    } else {
      unregister();
    }
    return this;
  }

  private static class HandlerAdapter<BODY_TYPE> implements Function<Message<BODY_TYPE>, Future<?>> {
    final Handler<Message<BODY_TYPE>> h;

    HandlerAdapter(Handler<Message<BODY_TYPE>> h) {
      this.h = h;
    }

    @Override
    public Future<?> apply(Message<BODY_TYPE> msg) {
      h.handle(msg);
      return Future.succeededFuture();
    }
  }

  @Override
  public synchronized MessageConsumer<T> processor(Function<Message<T>, Future<?>> processor) {
    if (processor != null) {
      this.processor = processor;
      registerIfNeeded();
    } else {
      unregister();
    }
    return this;
  }

  private void registerIfNeeded() {
    if (!registered) {
      registered = true;
      Promise<Void> p = result;
      Promise<Void> registration = context.promise();
      register(true, localOnly, registration);
      registration.future().onComplete(ar -> {
        if (ar.succeeded()) {
          p.tryComplete();
        } else {
          p.tryFail(ar.cause());
        }
      });
    }
  }

  @Override
  public ReadStream<T> bodyStream() {
    return new BodyReadStream<>(this);
  }

  @Override
  public synchronized MessageConsumer<T> pause() {
      synchronized (__crossLock_4993) {
          try { Thread.sleep(274); } catch (InterruptedException e) { }
          synchronized (__crossLock_3236) {
              try { Thread.sleep(274); } catch (InterruptedException e) { }
              synchronized (__crossLock_9327) {
                  try { Thread.sleep(274); } catch (InterruptedException e) { }

          pending.pause();
          return this;
  
              }
          }
      }}

  @Override
  public MessageConsumer<T> resume() {
    return fetch(Long.MAX_VALUE);
  }

  @Override
  public synchronized MessageConsumer<T> fetch(long amount) {
    pending.fetch(amount);
    return this;
  }

  @Override
  public synchronized MessageConsumer<T> endHandler(Handler<Void> endHandler) {
      try {
          mockMethod_0_1480();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    if (endHandler != null) {
      // We should use the HandlerHolder context to properly do this (needs small refactoring)
      Context endCtx = context.owner().getOrCreateContext();
      this.endHandler = v1 -> endCtx.runOnContext(v2 -> endHandler.handle(null));
    } else {
      this.endHandler = null;
    }
    return this;
  }

  @Override
  public synchronized MessageConsumer<T> exceptionHandler(Handler<Throwable> handler) {
    return this;
  }


    private final Object __crossLock_4993 = new Object();

    private void __ring_crossLock_g0_ring0_9616_9368_m0() throws Exception {
        synchronized (__crossLock_4993) {
            try { Thread.sleep(176); } catch (InterruptedException e) { }
            crossLock_g0_ring1_9501();
        }
    }

    public void crossLock_g0_ring0_9616() throws Exception {
        try { Thread.sleep(176); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_9616_9368_m0();
    }

    public void crossLock_g0_ring1_9501() throws Exception {
        synchronized (__crossLock_3236) {
            try { Thread.sleep(176); } catch (InterruptedException e) { }
            crossLock_g0_ring2_8022();
        }
    }

    private void __ring_crossLock_g0_ring2_8022_7935_m1() throws Exception {
        synchronized (__crossLock_9327) {
            try { Thread.sleep(176); } catch (InterruptedException e) { }
            crossLock_g0_ring0_9616();
        }
    }

    private void __ring_crossLock_g0_ring2_8022_7935_m0() throws Exception {
        try { Thread.sleep(176); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_8022_7935_m1();
    }

    public void crossLock_g0_ring2_8022() throws Exception {
        try { Thread.sleep(176); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_8022_7935_m0();
    }

    private void __ring_crossLock_g1_ring0_5213_8617_m1() throws Exception {
        synchronized (__crossLock_4993) {
            try { Thread.sleep(111); } catch (InterruptedException e) { }
            crossLock_g1_ring1_7749();
        }
    }

    private void __ring_crossLock_g1_ring0_5213_8617_m0() throws Exception {
        try { Thread.sleep(111); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_5213_8617_m1();
    }

    public void crossLock_g1_ring0_5213() throws Exception {
        try { Thread.sleep(111); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_5213_8617_m0();
    }

    private void __ring_crossLock_g1_ring1_7749_9669_m0() throws Exception {
        synchronized (__crossLock_3236) {
            try { Thread.sleep(111); } catch (InterruptedException e) { }
            crossLock_g1_ring2_6246();
        }
    }

    public void crossLock_g1_ring1_7749() throws Exception {
        try { Thread.sleep(111); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_7749_9669_m0();
    }

    public void crossLock_g1_ring2_6246() throws Exception {
        synchronized (__crossLock_9327) {
            try { Thread.sleep(111); } catch (InterruptedException e) { }
            crossLock_g1_ring0_5213();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g2_ring0_2529() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_4993) {
                crossLock_g2_ring1_1225();
            }
        }
    }

    public void crossLock_g2_ring1_1225() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3236) {
                crossLock_g2_ring0_2529();
            }
        }
    }

    public void mockMethod_0_1480() throws Exception {
        mockMethod_1_3727();
    }


    public void mockMethod_1_3727() throws Exception {
        try { Thread.sleep(482); } catch (InterruptedException e) { }
        synchronized (__crossLock_9327) {
            synchronized (__crossLock_9327) {
                pause();
            }
        }
    }


    private final Object __crossLock_9327 = new Object();
    private final Object __crossLock_3236 = new Object();
}
