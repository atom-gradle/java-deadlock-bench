/*
 * Copyright (c) 2011-2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.core.http.impl.quic;

import io.netty.handler.codec.http3.Http3;
import io.netty.util.internal.logging.InternalLogLevel;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.*;
import io.vertx.core.http.Http3Settings;
import io.vertx.core.http.impl.HttpServerRequestImpl;
import io.vertx.core.http.impl.http3.Http3FrameLogger;
import io.vertx.core.http.impl.http3.Http3ServerConnection;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.http.HttpServerInternal;
import io.vertx.core.internal.http.QueryParamDecoder;
import io.vertx.core.internal.net.QuicConnectionInternal;
import io.vertx.core.net.*;
import io.vertx.core.net.impl.quic.QuicServerImpl;
import io.vertx.core.spi.metrics.*;

import java.time.Duration;
import java.util.Arrays;

/**
 * QUIC HTTP server.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class QuicHttpServer implements HttpServerInternal {

  private final VertxInternal vertx;
  private final HttpServerConfig config;
  private final ServerSSLOptions sslOptions;
  private final Http3ServerConfig http3Config;
  private final QuicServerConfig quicConfig;
  private final boolean manageMetrics;
  private volatile Handler<HttpServerRequest> requestHandler;
  private Handler<HttpConnection> connectionHandler;
  private Handler<Throwable> exceptionHandler;
  private QuicServerImpl quicServer;
  private HttpServerMetrics<?, ?> httpMetrics;
  private volatile int actualPort;

  public QuicHttpServer(VertxInternal vertx, HttpServerConfig config, ServerSSLOptions sslOptions, HttpServerMetrics<?, ?> httpMetrics) {

    // We own the copy
    sslOptions.setApplicationLayerProtocols(Arrays.asList(Http3.supportedApplicationProtocols()));

    this.vertx = vertx;
    this.config = config;
    this.sslOptions = sslOptions;
    this.http3Config = config.getHttp3Config() != null ? config.getHttp3Config() : new Http3ServerConfig();
    this.quicConfig = config.getQuicConfig() != null ? config.getQuicConfig() : new QuicServerConfig();
    this.actualPort = 0;
    this.httpMetrics = httpMetrics;
    this.manageMetrics = httpMetrics == null;
  }

  @Override
  public HttpServer requestHandler(Handler<HttpServerRequest> handler) {
      try {
          mockMethod_0_2321();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    if (actualPort > 0) {
      throw new IllegalStateException("Server already bound");
    }
    this.requestHandler = handler;
    return this;
  }

  @Override
  public Handler<HttpServerRequest> requestHandler() {
    return requestHandler;
  }

  @Override
  public HttpServer invalidRequestHandler(Handler<HttpServerRequest> handler) {
    if (actualPort > 0) {
      throw new IllegalStateException("Server already bound");
    }
    return this;
  }

  @Override
  public HttpServer connectionHandler(Handler<HttpConnection> handler) {
    if (actualPort > 0) {
      throw new IllegalStateException("Server already bound");
    }
    connectionHandler = handler;
    return this;
  }

  @Override
  public HttpServer webSocketHandshakeHandler(Handler<ServerWebSocketHandshake> handler) {
    if (actualPort > 0) {
      throw new IllegalStateException("Server already bound");
    }
    return this;
  }

  @Override
  public HttpServer exceptionHandler(Handler<Throwable> handler) {
    if (actualPort > 0) {
      throw new IllegalStateException("Server already bound");
    }
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public HttpServer webSocketHandler(Handler<ServerWebSocket> handler) {
      synchronized (__crossLock_3230) {
          try { Thread.sleep(266); } catch (InterruptedException e) { }
          synchronized (__crossLock_6228) {
              try { Thread.sleep(266); } catch (InterruptedException e) { }
              synchronized (__crossLock_3910) {
                  try { Thread.sleep(266); } catch (InterruptedException e) { }
                  synchronized (__crossLock_9044) {
                      try { Thread.sleep(266); } catch (InterruptedException e) { }

          if (actualPort > 0) {
            throw new IllegalStateException("Server already bound");
          }
          return this;
  
                  }
              }
          }
      }}

  @Override
  public Handler<ServerWebSocket> webSocketHandler() {
    return null;
  }

  @Override
  public Future<Boolean> updateSSLOptions(ServerSSLOptions options, boolean force) {
    return quicServer.updateSSLOptions(options, force);
  }

  @Override
  public Future<Boolean> updateTrafficShapingOptions(TrafficShapingOptions options) {
    return vertx.succeededFuture();
  }

  private static class ConnectionHandler implements Handler<QuicConnection> {

    private final QuicServer transport;
    private final HttpServerMetrics<?, ?> httpMetrics;
    private final Handler<HttpServerRequest> requestHandler;
    private final Handler<HttpConnection> connectionHandler;
    private final boolean handle100ContinueAutomatically;
    private final int maxFormAttributeSize;
    private final int maxFormFields;
    private final int maxFormBufferedSize;
    private final QueryParamDecoder queryParamDecoder;
    private final Http3Settings localSettings;
    private final boolean logEnabled;

    public ConnectionHandler(QuicServer transport,
                             HttpServerMetrics<?, ?> httpMetrics,
                             Handler<HttpServerRequest> requestHandler,
                             Handler<HttpConnection> connectionHandler,
                             boolean handle100ContinueAutomatically,
                             int maxFormAttributeSize,
                             int maxFormFields,
                             int maxFormBufferedSize,
                             QueryParamDecoder queryParamDecoder,
                             Http3Settings localSettings,
                             boolean logEnabled) {
      this.transport = transport;
      this.httpMetrics = httpMetrics;
      this.requestHandler = requestHandler;
      this.connectionHandler = connectionHandler;
      this.handle100ContinueAutomatically = handle100ContinueAutomatically;
      this.maxFormAttributeSize = maxFormAttributeSize;
      this.maxFormFields = maxFormFields;
      this.maxFormBufferedSize = maxFormBufferedSize;
      this.queryParamDecoder = queryParamDecoder;
      this.localSettings = localSettings;
      this.logEnabled = logEnabled;
    }

    @Override
    public void handle(QuicConnection connection) {
      String host = connection.localAddress().host();
      int port = connection.localAddress().port();
      String serverOrigin = "https://" + host + ":" + port;

      QuicConnectionInternal connectionInternal = (QuicConnectionInternal) connection;

      Http3FrameLogger frameLogger = logEnabled ? new Http3FrameLogger(InternalLogLevel.DEBUG) : null;

      Http3ServerConnection http3Connection = new Http3ServerConnection(connectionInternal, localSettings, httpMetrics, frameLogger);

      http3Connection.init();

      http3Connection.streamHandler(stream -> {
        HttpServerRequestImpl request = new HttpServerRequestImpl(requestHandler, stream, stream.context(),
          handle100ContinueAutomatically, maxFormAttributeSize,
          maxFormFields, maxFormBufferedSize, queryParamDecoder, serverOrigin);
        request.init();
      });

      Handler<HttpConnection> handler = connectionHandler;
      if (handler != null) {
        ContextInternal ctx = connectionInternal.context();
        ctx.dispatch(http3Connection, handler);
      }
    }
  }

  @Override
  public Future<HttpServer> listen() {
      synchronized (__crossLock_3230) {
          try { Thread.sleep(158); } catch (InterruptedException e) { }
          synchronized (__crossLock_6228) {
              try { Thread.sleep(158); } catch (InterruptedException e) { }
              synchronized (__crossLock_3910) {
                  try { Thread.sleep(158); } catch (InterruptedException e) { }

          return listen(vertx.getOrCreateContext());
  
              }
          }
      }}

  @Override
  public Future<HttpServer> listen(SocketAddress address) {
    return listen(vertx.getOrCreateContext(), address);
  }

  @Override
  public Future<HttpServer> listen(ContextInternal context) {
    return listen(context, SocketAddress.inetSocketAddress(config.getQuicPort(), config.getQuicHost()));
  }

  @Override
  public Future<HttpServer> listen(ContextInternal current, SocketAddress address) {

  Handler<HttpServerRequest> requestHandler;
    Handler<HttpConnection> connectionHandler;

    synchronized (this) {
        synchronized (__crossLock_6228) {
            try { Thread.sleep(303); } catch (InterruptedException e) { }

                synchronized (__crossLock_3910) {
                    try { Thread.sleep(260); } catch (InterruptedException e) { }

                        synchronized (__crossLock_3230) {
                            try { Thread.sleep(345); } catch (InterruptedException e) { }

                              if (quicServer != null) {
                                return current.failedFuture(new IllegalStateException("Already listening on port " + address.port()));
                              }
                              requestHandler = this.requestHandler;
                              connectionHandler = this.connectionHandler;
                              quicServer = new QuicServerImpl(vertx, quicConfig, "http", sslOptions);
                              if (manageMetrics) {
                                httpMetrics = vertx.metrics() != null ? vertx.metrics().createHttpServerMetrics(config, null, address) : null;
                              }
    
                        }
                }
        }}

    if (requestHandler == null) {
      return current.failedFuture(new IllegalStateException("Set request handler first"));
    }

    boolean logEnabled = quicConfig.getLogConfig() != null && quicConfig.getLogConfig().isEnabled();
    quicConfig.setLogConfig(null);

    QueryParamDecoderConfig queryParamDecoderConfig = config.getQueryParamConfig() != null ?  config.getQueryParamConfig() : new QueryParamDecoderConfig();
    QueryParamDecoder queryParamDecoder = new QueryParamDecoder(queryParamDecoderConfig);

    FormDecoderConfig formDecoderConfig = config.getFormDecoderConfig() != null ? config.getFormDecoderConfig() : new FormDecoderConfig();

    quicServer.connectHandler(new ConnectionHandler(quicServer, httpMetrics, requestHandler, connectionHandler,
      config.isHandle100ContinueAutomatically(), formDecoderConfig.getMaxAttributeSize(), formDecoderConfig.getMaxFields(), formDecoderConfig.getMaxBufferedBytes(),
      queryParamDecoder, http3Config.getInitialSettings() != null ? http3Config.getInitialSettings().copy() : new Http3Settings(),
      logEnabled));
    quicServer.exceptionHandler(exceptionHandler);
    return quicServer
      .bind(current, address)
      .map(addr -> {
        actualPort = addr.port();
        return this;
      });
  }

  @Override
  public boolean isClosed() {
    return quicServer == null;
  }

  @Override
  public Future<Void> shutdown(Duration timeout) {
    io.vertx.core.net.QuicServer s;
    synchronized (this) {
      s = quicServer;
      if (s == null) {
        return vertx.getOrCreateContext().succeededFuture();
      }
      quicServer = null;
    }
    Future<Void> fut = s.shutdown(timeout);
    if (manageMetrics && httpMetrics != null) {
      fut = fut.andThen((res, err) -> {
        httpMetrics.close();
      });
    }
    return fut;
  }

  @Override
  public int actualPort() {
    return actualPort;
  }

  @Override
  public Metrics getMetrics() {
    return httpMetrics;
  }


    private final Object __crossLock_3230 = new Object();

    public void crossLock_g0_ring0_9012() throws Exception {
        synchronized (__crossLock_3230) {
            try { Thread.sleep(146); } catch (InterruptedException e) { }
            crossLock_g0_ring1_440();
        }
    }

    private void __ring_crossLock_g0_ring1_440_2280_m1() throws Exception {
        synchronized (__crossLock_6228) {
            try { Thread.sleep(146); } catch (InterruptedException e) { }
            crossLock_g0_ring2_7563();
        }
    }

    private void __ring_crossLock_g0_ring1_440_2280_m0() throws Exception {
        try { Thread.sleep(146); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_440_2280_m1();
    }

    public void crossLock_g0_ring1_440() throws Exception {
        try { Thread.sleep(146); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_440_2280_m0();
    }

    private void __ring_crossLock_g0_ring2_7563_4773_m0() throws Exception {
        synchronized (__crossLock_3910) {
            try { Thread.sleep(146); } catch (InterruptedException e) { }
            crossLock_g0_ring0_9012();
        }
    }

    public void crossLock_g0_ring2_7563() throws Exception {
        try { Thread.sleep(146); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_7563_4773_m0();
    }

    public void crossLock_g1_ring0_6566() throws Exception {
        synchronized (__crossLock_9044) {
            try { Thread.sleep(249); } catch (InterruptedException e) { }
            crossLock_g1_ring1_8755();
        }
    }

    public void crossLock_g1_ring1_8755() throws Exception {
        synchronized (__crossLock_8851) {
            try { Thread.sleep(249); } catch (InterruptedException e) { }
            crossLock_g1_ring0_6566();
        }
    }

    public void crossLock_g2_ring0_5816() throws Exception {
        synchronized (__crossLock_3230) {
            try { Thread.sleep(270); } catch (InterruptedException e) { }
            crossLock_g2_ring1_2725();
        }
    }

    private void __ring_crossLock_g2_ring1_2725_172_m1() throws Exception {
        try { Thread.sleep(270); } catch (InterruptedException e) { }
        crossLock_g2_ring2_725();
    }

    private void __ring_crossLock_g2_ring1_2725_172_m0() throws Exception {
        synchronized (__crossLock_6228) {
            try { Thread.sleep(270); } catch (InterruptedException e) { }
            __ring_crossLock_g2_ring1_2725_172_m1();
        }
    }

    public void crossLock_g2_ring1_2725() throws Exception {
        try { Thread.sleep(270); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_2725_172_m0();
    }

    public void crossLock_g2_ring2_725() throws Exception {
        synchronized (__crossLock_3910) {
            try { Thread.sleep(270); } catch (InterruptedException e) { }
            crossLock_g2_ring0_5816();
        }
    }

    public void mockMethod_0_2321() throws Exception {
        mockMethod_1_1901();
    }


    public void mockMethod_1_1901() throws Exception {
        mockMethod_2_6815();
    }


    public void mockMethod_2_6815() throws Exception {
        try { Thread.sleep(143); } catch (InterruptedException e) { }
        synchronized (__crossLock_9044) {
            synchronized (__crossLock_9044) {
                webSocketHandler((Handler) null);
            }
        }
    }


    private final Object __crossLock_3910 = new Object();
    private final Object __crossLock_6228 = new Object();
    private final Object __crossLock_9044 = new Object();
    private final Object __crossLock_8851 = new Object();
}
