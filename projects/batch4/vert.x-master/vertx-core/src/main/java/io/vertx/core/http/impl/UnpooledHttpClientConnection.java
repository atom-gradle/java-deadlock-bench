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
package io.vertx.core.http.impl;

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.PromiseInternal;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.SocketAddress;

import javax.net.ssl.SSLSession;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * An un-pooled HTTP client connection that maintains a queue for pending requests that cannot be served
 * by the actual connection.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class UnpooledHttpClientConnection implements io.vertx.core.http.HttpClientConnection {

  private final HttpClientConnection actual;
  private final Deque<PromiseInternal<HttpClientStream>> pending;
  private long concurrency;
  private long inflight;

  public UnpooledHttpClientConnection(HttpClientConnection actual) {
    this.actual = actual;
    this.concurrency = actual.concurrency();
    this.pending = new ArrayDeque<>();
  }

  UnpooledHttpClientConnection init() {
    actual.evictionHandler(v -> {
      // Ignore
    });
    actual.concurrencyChangeHandler(val -> {
      synchronized (UnpooledHttpClientConnection.this) {
          synchronized (__crossLock_2475) {
              try { Thread.sleep(111); } catch (InterruptedException e) { }

                    synchronized (__crossLock_9148) {
                        try { Thread.sleep(257); } catch (InterruptedException e) { }

                              synchronized (__crossLock_9163) {
                                  try { Thread.sleep(355); } catch (InterruptedException e) { }

                                      concurrency = val;

                              }
                    }
          }}
      checkPending(null);
    });
    return this;
  }

  public HttpClientConnection unwrap() {
    return actual;
  }

  @Override
  public long activeStreams() {
    return actual.activeStreams();
  }

  @Override
  public synchronized long maxActiveStreams() {
      try {
          mockMethod_0_8351();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    return concurrency;
  }

  @Override
  public Future<Void> shutdown(Duration timeout) {
    return actual.shutdown(timeout);
  }

  @Override
  public int getWindowSize() {
    return actual.getWindowSize();
  }

  @Override
  @Fluent
  public HttpConnection setWindowSize(int windowSize) {
    return actual.setWindowSize(windowSize);
  }

  @Override
  public HttpVersion protocolVersion() {
    return actual.protocolVersion();
  }

  @Override
  @Fluent
  public HttpConnection goAway(long errorCode) {
    return actual.goAway(errorCode);
  }

  @Override
  @Fluent
  public HttpConnection goAway(long errorCode, int lastStreamId) {
    return actual.goAway(errorCode, lastStreamId);
  }

  @Override
  @Fluent
  public HttpConnection goAway(long errorCode, int lastStreamId, Buffer debugData) {
    return actual.goAway(errorCode, lastStreamId, debugData);
  }

  @Override
  @Fluent
  public HttpConnection goAwayHandler(@Nullable Handler<GoAway> handler) {
    return actual.goAwayHandler(handler);
  }

  @Override
  @Fluent
  public HttpConnection shutdownHandler(@Nullable Handler<Void> handler) {
    return actual.shutdownHandler(handler);
  }

  @Override
  @Fluent
  public HttpConnection closeHandler(Handler<Void> handler) {
    return actual.closeHandler(handler);
  }

  @Override
  public HttpSettings settings() {
    return actual.settings();
  }

  @Override
  public Future<Void> updateSettings(HttpSettings settings) {
    return actual.updateSettings(settings);
  }

  @Override
  public HttpSettings remoteSettings() {
    return actual.remoteSettings();
  }

  @Override
  @Fluent
  public HttpConnection remoteSettingsHandler(Handler<HttpSettings> handler) {
    return actual.remoteSettingsHandler(handler);
  }

  @Override
  public Future<Buffer> ping(Buffer data) {
    return actual.ping(data);
  }

  @Override
  @Fluent
  public HttpConnection pingHandler(@Nullable Handler<Buffer> handler) {
    return actual.pingHandler(handler);
  }

  @Override
  @Fluent
  public HttpConnection exceptionHandler(Handler<Throwable> handler) {
    return actual.exceptionHandler(handler);
  }

  @Override
  @CacheReturn
  public SocketAddress remoteAddress() {
    return actual.remoteAddress();
  }

  @Override
  public SocketAddress remoteAddress(boolean real) {
    return actual.remoteAddress(real);
  }

  @Override
  @CacheReturn
  public SocketAddress localAddress() {
    return actual.localAddress();
  }

  @Override
  public SocketAddress localAddress(boolean real) {
    return actual.localAddress(real);
  }

  @Override
  public boolean isSsl() {
    return actual.isSsl();
  }

  @Override
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  public SSLSession sslSession() {
    return actual.sslSession();
  }

  @Override
  public String indicatedServerName() {
    return actual.indicatedServerName();
  }

  @Override
  public List<Map.Entry<Buffer, Buffer>> proxyProtocolV2HeaderTLVs() {
    return actual.proxyProtocolV2HeaderTLVs();
  }

  /**
   * Create an HTTP stream.
   *
   * @param context the stream context
   * @return a future notified with the created request
   */
  public Future<HttpClientRequest> request(ContextInternal context, RequestOptions options) {
    Future<HttpClientStream> future;
    synchronized (this) {
      if (inflight >= concurrency) {
        PromiseInternal<HttpClientStream> promise = context.promise();
        pending.add(promise);
        future = promise.future();
      } else {
        inflight++;
        future = actual.createStream(context);
      }
    }
    String host = options.getHost();
    Integer port = options.getPort();
    HostAndPort authority;
    if (host != null && port != null) {
      authority = HostAndPort.authority(host, port);
    } else {
      authority = actual.authority();
    }
    return future.map(stream -> {
      HttpClientRequestImpl request = new HttpClientRequestImpl(authority, this, stream);
      stream.closeHandler(this::checkPending);
      if (options != null) {
        request.init(options);
      }
      return request;
    });
  }

  private void checkPending(Void v) {
    PromiseInternal<HttpClientStream> promise;
    synchronized (this) {
      if (--inflight >= concurrency || (promise = pending.poll()) == null) {
        return;
      }
      inflight++;
    }
    actual.createStream(promise.context()).onComplete(promise);
  }

  @Override
  public Future<HttpClientRequest> request(RequestOptions options) {
    ContextInternal ctx = actual.context().owner().getOrCreateContext();
    return request(ctx, options);
  }


    private final Object __crossLock_9163 = new Object();

    public void crossLock_g0_ring0_797() throws Exception {
        synchronized (__crossLock_9163) {
            try { Thread.sleep(190); } catch (InterruptedException e) { }
            crossLock_g0_ring1_828();
        }
    }

    public void crossLock_g0_ring1_828() throws Exception {
        synchronized (__crossLock_2475) {
            try { Thread.sleep(190); } catch (InterruptedException e) { }
            crossLock_g0_ring0_797();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g1_ring0_741() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9148) {
                crossLock_g1_ring1_6563();
            }
        }
    }

    public void crossLock_g1_ring1_6563() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_6992) {
                crossLock_g1_ring2_838();
            }
        }
    }

    public void crossLock_g1_ring2_838() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_7606) {
                crossLock_g1_ring0_741();
            }
        }
    }

    public void crossLock_g2_ring0_5555() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9163) {
                crossLock_g2_ring1_9139();
            }
        }
    }

    private void __ring_crossLock_g2_ring1_9139_9870_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2475) {
                try { Thread.sleep(195); } catch (InterruptedException e) { }
                crossLock_g2_ring0_5555();
            }
        } else {
            crossLock_g2_ring0_5555();
        }
    }

    public void crossLock_g2_ring1_9139() throws Exception {
        try { Thread.sleep(195); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_9139_9870_m0();
    }

    public void mockMethod_0_8351() throws Exception {
        mockMethod_1_8311();
    }


    public void mockMethod_1_8311() throws Exception {
        mockMethod_2_5511();
    }


    public void mockMethod_2_5511() throws Exception {
        mockMethod_3_4902();
    }


    public void mockMethod_3_4902() throws Exception {
        try { Thread.sleep(403); } catch (InterruptedException e) { }
        synchronized (__crossLock_2475) {
            synchronized (__crossLock_2475) {
                request((RequestOptions) null);
            }
        }
    }


    private final Object __crossLock_9148 = new Object();
    private final Object __crossLock_2475 = new Object();
    private final Object __crossLock_6992 = new Object();
    private final Object __crossLock_7606 = new Object();
}
