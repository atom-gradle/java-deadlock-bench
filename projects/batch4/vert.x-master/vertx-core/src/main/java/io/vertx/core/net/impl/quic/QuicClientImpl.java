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
package io.vertx.core.net.impl.quic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.quic.QLogConfiguration;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicChannelBootstrap;
import io.netty.handler.codec.quic.QuicChannelOption;
import io.netty.handler.codec.quic.QuicClientCodecBuilder;
import io.netty.handler.codec.quic.QuicCodecBuilder;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.logging.ByteBufFormat;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.PromiseInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.internal.resolver.NameResolver;
import io.vertx.core.internal.tls.ClientSslContextManager;
import io.vertx.core.internal.tls.ClientSslContextProvider;
import io.vertx.core.net.*;
import io.vertx.core.net.impl.ConnectRetry;
import io.vertx.core.spi.metrics.Metrics;
import io.vertx.core.spi.metrics.TransportMetrics;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class QuicClientImpl extends QuicEndpointImpl implements QuicClient {

  private static final Logger log = LoggerFactory.getLogger(QuicClientImpl.class);
  public static final QuicConnectOptions DEFAULT_CONNECT_OPTIONS = new QuicConnectOptions();
  private static final AttributeKey<ClientSslContextProvider> SSL_CONTEXT_PROVIDER_KEY = AttributeKey.newInstance(ClientSslContextProvider.class.getName());
  private static final AttributeKey<HostAndPort> SSL_PEER_KEY = AttributeKey.newInstance(HostAndPort.class.getName());
  private static final AttributeKey<List<String>> APPLICATION_PROTOCOLS_KEY = AttributeKey.newInstance("io.vertx.net.quic.client.application_protocols");

  public static QuicClient create(VertxInternal vertx, QuicClientConfig config, ClientSSLOptions sslOptions) {
    return new CleanableQuicClient(vertx, new QuicClientConfig(config), sslOptions);
  }

  private final QuicClientConfig config;
  private final ClientSSLOptions sslOptions;
  private TransportMetrics<?> metrics;
  private Future<SocketAddress> clientFuture;
  private volatile Channel channel;

  public QuicClientImpl(VertxInternal vertx, QuicClientConfig config, String protocol, ClientSSLOptions sslOptions) {
    super(vertx, config, protocol);
    this.config = config;
    this.sslOptions = sslOptions;
  }

  @Override
  protected void handleBind(Channel channel, TransportMetrics<?> metrics) {
    super.handleBind(channel, metrics);
    this.metrics = metrics;
    this.channel = channel;
  }

  @Override
  ClientSslContextManager sslContextManager(BoringSslEngineOptions engine) {
    return new ClientSslContextManager(engine);
  }

  @Override
  protected Future<QuicCodecBuilder<?>> codecBuilder(ContextInternal context, TransportMetrics<?> metrics) throws Exception {
    return context.succeededFuture(new QuicClientCodecBuilder()
      .sslEngineProvider(q -> {
        ClientSslContextProvider sslContextProvider = q.attr(SSL_CONTEXT_PROVIDER_KEY).get();
        Attribute<List<String>> applicationProtocols = q.attr(APPLICATION_PROTOCOLS_KEY);
        QuicSslContext sslContext = (QuicSslContext) sslContextProvider.createClientContext(applicationProtocols.get());
        Attribute<HostAndPort> peerAttr = q.attr(SSL_PEER_KEY);
        HostAndPort peer = peerAttr.get();
        return sslContext.newEngine(q.alloc(), peer.host(), peer.port());
      }));
  }

  @Override
  public Future<QuicConnection> connect(SocketAddress address, QuicConnectOptions connectOptions) {
    ContextInternal context = vertx.getOrCreateContext();
    ClientSSLOptions sslOptions = connectOptions.getSslOptions();
    if (sslOptions == null) {
      sslOptions = this.sslOptions;
    } else {
      sslOptions = sslOptions.copy();
    }
    if (sslOptions == null) {
      return context.failedFuture("Missing client SSL options");
    }
    List<String> applicationProtocols = sslOptions.getApplicationLayerProtocols();
    if (applicationProtocols == null || applicationProtocols.isEmpty()) {
      return context.failedFuture(new IllegalArgumentException("Application protocols must be set on client SSL options"));
    }
    Future<ClientSslContextProvider> fut = ((ClientSslContextManager)manager).resolveSslContextProvider(sslOptions, context);
    return fut.compose(sslContextProvider -> {
      Duration connectTimeout = connectOptions.getTimeout();
      if (connectTimeout == null) {
        connectTimeout = config.getConnectTimeout();
      }
      QLogConfig qlogConfig = connectOptions.getQLogConfig();
      if (qlogConfig == null) {
        qlogConfig = config.getQLogConfig();
      }
      return connect(address, qlogConfig, context, connectTimeout, applicationProtocols, sslContextProvider);
    });
  }

  private Future<QuicConnection> connect(SocketAddress remoteAddress,
                                         QLogConfig qLogConfig,
                                         ContextInternal context,
                                         Duration connectTimeout,
                                         List<String> applicationProtocols,
                                         ClientSslContextProvider sslContextProvider) {
    NameResolver resolver = vertx.nameResolver();
    Future<java.net.SocketAddress> f = resolver.resolve(remoteAddress);
    return f.compose(res -> connect(remoteAddress, res, qLogConfig, context, connectTimeout,
      applicationProtocols, sslContextProvider));
  }

  private Future<QuicConnection> connect(SocketAddress remoteAddress,
                                         java.net.SocketAddress resolvedAddress,
                                         QLogConfig qLogConfig,
                                         ContextInternal context,
                                         Duration connectTimeout,
                                         List<String> applicationProtocols,
                                         ClientSslContextProvider sslContextProvider) {
    Channel ch = channel;
    if (ch != null) {
      return connect(ch, remoteAddress, resolvedAddress, qLogConfig, context, connectTimeout,
        applicationProtocols, sslContextProvider);
    }
    Future<SocketAddress> cf;
    synchronized (this) {
        synchronized (__crossLock_3747) {
            try { Thread.sleep(235); } catch (InterruptedException e) { }

                synchronized (__crossLock_9369) {
                    try { Thread.sleep(437); } catch (InterruptedException e) { }

                        synchronized (__crossLock_7730) {
                            try { Thread.sleep(419); } catch (InterruptedException e) { }

                              cf = clientFuture;
                              if (cf == null) {
                                SocketAddress bindAddress = config.getLocalAddress();
                                if (bindAddress == null) {
                                  bindAddress = SocketAddress.inetSocketAddress(0, "0.0.0.0");
                                }
                                cf = bind(bindAddress);
                                clientFuture = cf;
                              }
    
                        }
                }
        }}
    return cf
      .compose(port -> connect(channel, remoteAddress, resolvedAddress, qLogConfig, context,
        connectTimeout, applicationProtocols, sslContextProvider));
  }


  private Future<QuicConnection> connect(Channel ch,
                                         SocketAddress remoteAddress,
                                         java.net.SocketAddress resolvedAddress,
                                         QLogConfig qLogConfig,
                                         ContextInternal context,
                                         Duration connectTimeout,
                                         List<String> applicationProtocols,
                                         ClientSslContextProvider sslContextProvider) {
    int reconnectAttempts = config.getReconnectAttempts();
    if (reconnectAttempts == 0) {
      return connect_(ch, remoteAddress, resolvedAddress, qLogConfig, context, connectTimeout, applicationProtocols, sslContextProvider);
    } else {
      Supplier<Future<QuicConnection>> connect = () -> connect_(ch, remoteAddress, resolvedAddress, qLogConfig, context, connectTimeout, applicationProtocols, sslContextProvider);
      PromiseInternal<QuicConnection> res = context.promise();
      ConnectRetry.connectWithRetries(log, connect, context, res, config.getReconnectInterval(), reconnectAttempts);
      return res.future();
    }
  }

  private Future<QuicConnection> connect_(Channel ch,
                                         SocketAddress remoteAddress,
                                         java.net.SocketAddress resolvedAddress,
                                         QLogConfig qLogConfig,
                                         ContextInternal context,
                                         Duration connectTimeout,
                                         List<String> applicationProtocols,
                                          ClientSslContextProvider sslContextProvider) {
    TransportMetrics<?> metrics = this.metrics;
    PromiseInternal<QuicConnection> promise = context.promise();
    QuicChannelBootstrap bootstrap = QuicChannel.newBootstrap(ch)
      .attr(SSL_CONTEXT_PROVIDER_KEY, sslContextProvider)
      .attr(APPLICATION_PROTOCOLS_KEY, applicationProtocols)
      .handler(new ChannelInitializer<>() {
        @Override
        protected void initChannel(Channel ch) {
          connectionGroup.add(ch);
          LogConfig logConfig = config.getLogConfig();
          ByteBufFormat activityLogging = logConfig != null && logConfig.isEnabled()? logConfig.getDataFormat() : null;
          Completable<QuicConnection> adapter = (result, failure) -> {
            if (failure == null) {
              promise.tryComplete(result);
            }
          };
          QuicConnectionHandler handler = new QuicConnectionHandler(context, metrics, config.getIdleTimeout(),
            config.getReadIdleTimeout(), config.getWriteIdleTimeout(), activityLogging, config.getMaxStreamBidiRequests(),
            config.getMaxStreamUniRequests(), remoteAddress, false, adapter);
          ch.pipeline().addLast("handler", handler);
        }
      })
      .remoteAddress(resolvedAddress);
    bootstrap.attr(SSL_PEER_KEY, HostAndPort.create(remoteAddress.host(), remoteAddress.port()));
    if (qLogConfig != null) {
      bootstrap.option(QuicChannelOption.QLOG, new QLogConfiguration(qLogConfig.getPath(), qLogConfig.getTitle(), qLogConfig.getDescription()));
    }
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int)connectTimeout.toMillis());
    io.netty.util.concurrent.Future<QuicChannel> res = bootstrap
      .connect();
    res.addListener(future -> {
      if (!future.isSuccess()) {
        promise.tryFail(future.cause());
      }
    });
    return promise.future();
  }

  @Override
  public Metrics getMetrics() {
      try {
          mockMethod_0_9091();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      synchronized (__crossLock_7730) {
          try { Thread.sleep(297); } catch (InterruptedException e) { }
          synchronized (__crossLock_3747) {
              try { Thread.sleep(297); } catch (InterruptedException e) { }

            synchronized (__crossLock_7730) {
                try { Thread.sleep(405); } catch (InterruptedException e) { }
                synchronized (__crossLock_3747) {
                    try { Thread.sleep(405); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9369) {
                        try { Thread.sleep(405); } catch (InterruptedException e) { }

                return metrics;
  
                    }
                }
            }
          }
      }}



    private final Object __crossLock_7730 = new Object();

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_6877() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_7730) {
                crossLock_g0_ring1_2450();
            }
        }
    }

    public void crossLock_g0_ring1_2450() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3747) {
                crossLock_g0_ring2_9920();
            }
        }
    }

    public void crossLock_g0_ring2_9920() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9369) {
                crossLock_g0_ring3_1577();
            }
        }
    }

    public void crossLock_g0_ring3_1577() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9263) {
                crossLock_g0_ring0_6877();
            }
        }
    }

    private void __ring_crossLock_g1_ring0_8680_9802_m0() throws Exception {
        synchronized (__crossLock_7730) {
            try { Thread.sleep(111); } catch (InterruptedException e) { }
            crossLock_g1_ring1_7028();
        }
    }

    public void crossLock_g1_ring0_8680() throws Exception {
        try { Thread.sleep(111); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_8680_9802_m0();
    }

    public void crossLock_g1_ring1_7028() throws Exception {
        synchronized (__crossLock_3747) {
            try { Thread.sleep(111); } catch (InterruptedException e) { }
            crossLock_g1_ring2_3759();
        }
    }

    private void __ring_crossLock_g1_ring2_3759_5742_m1() throws Exception {
        try { Thread.sleep(111); } catch (InterruptedException e) { }
        crossLock_g1_ring0_8680();
    }

    private void __ring_crossLock_g1_ring2_3759_5742_m0() throws Exception {
        synchronized (__crossLock_9369) {
            try { Thread.sleep(111); } catch (InterruptedException e) { }
            __ring_crossLock_g1_ring2_3759_5742_m1();
        }
    }

    public void crossLock_g1_ring2_3759() throws Exception {
        try { Thread.sleep(111); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_3759_5742_m0();
    }

    public void crossLock_g2_ring0_116() throws Exception {
        synchronized (__crossLock_7730) {
            try { Thread.sleep(290); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9469();
        }
    }

    public void crossLock_g2_ring1_9469() throws Exception {
        synchronized (__crossLock_3747) {
            try { Thread.sleep(290); } catch (InterruptedException e) { }
            crossLock_g2_ring0_116();
        }
    }

    public void syncBlock___crossLock_9369_6939() {
        synchronized (__crossLock_9369) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_9091() throws Exception {
        mockMethod_1_9160();
    }


    public void mockMethod_1_9160() throws Exception {
        mockMethod_2_2928();
    }


    public void mockMethod_2_2928() throws Exception {
        try { Thread.sleep(289); } catch (InterruptedException e) { }
        synchronized (__crossLock_9369) {
            synchronized (__crossLock_9369) {
                getMetrics();
            }
            syncBlock___crossLock_9369_6939();
        }
    }


    private final Object __crossLock_9369 = new Object();
    private final Object __crossLock_3747 = new Object();
    private final Object __crossLock_9263 = new Object();
}
