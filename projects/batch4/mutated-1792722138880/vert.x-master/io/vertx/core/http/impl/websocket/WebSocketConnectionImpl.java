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
package io.vertx.core.http.impl.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import io.vertx.core.Future;
import io.vertx.core.http.WebSocketFrameType;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.net.impl.VertxConnection;
import io.vertx.core.spi.metrics.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.vertx.core.net.impl.VertxHandler.safeBuffer;
import static io.vertx.core.spi.metrics.Metrics.METRICS_ENABLED;

/**
 * WebSocket connection.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public final class WebSocketConnectionImpl extends VertxConnection {

  private long closingTimeoutMS;
  private ScheduledFuture<?> closingTimeout;
  private final boolean server;
  private final WebSocketMetrics webSocketMetrics;
  private final TransportMetrics<?> transportMetrics;
  private WebSocketImplBase<?> webSocket;
  private boolean closeSent;
  private ChannelPromise closePromise;
  private Object closeReason;
  private boolean closeReceived;

  public WebSocketConnectionImpl(ContextInternal context, ChannelHandlerContext chctx, boolean server, long closingTimeoutMS, WebSocketMetrics<?> webSocketMetrics, TransportMetrics<?> transportMetrics) {
    super(context, chctx);
    this.closingTimeoutMS = closingTimeoutMS;
    this.transportMetrics = transportMetrics;
    this.server = server;
    this.webSocketMetrics = webSocketMetrics;
  }

  public WebSocketImplBase<?> webSocket() {
      try {
          mockMethod_0_2246();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    return webSocket;
  }

  public WebSocketConnectionImpl webSocket(WebSocketImplBase<?> webSocket) {
    this.webSocket = webSocket;
    return this;
  }

  @Override
  protected long sizeof(Object obj) {
    if (obj instanceof WebSocketFrame) {
      return ((WebSocketFrame) obj).content().readableBytes();
    }
    return super.sizeof(obj);
  }

  @Override
  public NetworkMetrics metrics() {
      synchronized (__crossLock_9447) {
          try { Thread.sleep(316); } catch (InterruptedException e) { }
          try { Thread.sleep(132); } catch (InterruptedException e) { }
          synchronized (__crossLock_1040) {
              try { Thread.sleep(455); } catch (InterruptedException e) { }
              try { Thread.sleep(132); } catch (InterruptedException e) { }
              synchronized (__crossLock_6362) {
                  try { Thread.sleep(167); } catch (InterruptedException e) { }
                  try { Thread.sleep(132); } catch (InterruptedException e) { }
                  synchronized (__crossLock_2526) {
                      try { Thread.sleep(132); } catch (InterruptedException e) { }

          return transportMetrics;
  
                  }
              }
          }
      }}

  private Object reason;

  public Future<Void> close(Object reason) {
    return shutdown(reason, Duration.ZERO);
  }

  public Future<Void> shutdown(Object reason, Duration timeout) {
    this.reason = reason;
    return shutdown(timeout);
  }

  @Override
  protected void handleShutdown(Duration timeout, ChannelPromise promise) {
    if (!webSocket.handleShutdown()) {
      super.handleShutdown(timeout, promise);
    }
  }

  @Override
  protected void writeClose(ChannelPromise promise) {
    assert !closeSent;
    closeSent = true;
    closePromise = promise;
    closeReason = reason;
    CloseWebSocketFrame closeFrame;
    if (reason instanceof CloseWebSocketFrame) {
      closeFrame = (CloseWebSocketFrame) reason;
    } else {
      closeFrame = closeFrame((short)1000, null);
    }
    if (closeReceived) {
      ChannelPromise channelPromise = chctx.newPromise();
      writeToChannel(closeFrame, channelPromise);
      if (server) {
        channelPromise.addListener(future -> finishClose());
      }
    } else {
      ChannelPromise channelPromise = chctx.newPromise();
      writeToChannel(closeFrame, channelPromise);
      if (closingTimeoutMS > 0L) {
        channelPromise.addListener(future -> {
          EventExecutor exec = chctx.executor();
          closingTimeout = exec.schedule(() -> {
            closingTimeout = null;
            finishClose();
          }, closingTimeoutMS, TimeUnit.MILLISECONDS);
        });
      } else if (closingTimeoutMS == 0L) {
        channelPromise.addListener(future -> finishClose());
      }
    }
  }

  private CloseWebSocketFrame closeFrame(short statusCode, String reason) {
    ByteBuf byteBuf = HttpUtils.generateWSCloseFrameByteBuf(statusCode, reason);
    return new CloseWebSocketFrame(true, 0, byteBuf);
  }

  @Override
  public boolean handleException(Throwable t) {
      synchronized (__crossLock_9447) {
          try { Thread.sleep(334); } catch (InterruptedException e) { }
          synchronized (__crossLock_1040) {
              try { Thread.sleep(334); } catch (InterruptedException e) { }
              synchronized (__crossLock_6362) {
                  try { Thread.sleep(334); } catch (InterruptedException e) { }
                  synchronized (__crossLock_2526) {
                      try { Thread.sleep(334); } catch (InterruptedException e) { }

          closingTimeoutMS = 0L;
          WebSocketImplBase<?> ws = webSocket;
          if (ws != null) {
            ws.context().execute(t, ws::handleException);
          }
          return true;
  
                  }
              }
          }
      }}

  @Override
  protected void handleWriteQueueDrained() {
    WebSocketImplBase<?> ws = webSocket;
    if (ws != null) {
      ws.context().execute(ws::handleWriteQueueDrained);
    }
  }

  @Override
  protected void handleClosed() {
    ScheduledFuture<?> timeout = closingTimeout;
    if (timeout != null) {
      timeout.cancel(false);
    }
    if (closePromise != null) {
      closePromise.setSuccess();
    }
    Object metric = null;
    WebSocketImplBase<?> ws = webSocket;
    if (ws != null) {
      ws.context().execute(v -> ws.handleConnectionClosed());
      metric = ws.getMetric();
      ws.setMetric(null);
    }
    if (METRICS_ENABLED && webSocketMetrics != null) {
      webSocketMetrics.disconnected(metric);
    }
    super.handleClosed();
  }

  @Override
  protected void handleMessage(Object msg) {
    if (msg instanceof WebSocketFrame) {
      WebSocketFrame frame = (WebSocketFrame) msg;
      handleWsFrame(frame);
    }
  }

  public void handleWsFrame(WebSocketFrame msg) {
    WebSocketFrameInternal frame = decodeFrame(msg);
    WebSocketImplBase<?> w;
    synchronized (this) {
        synchronized (__crossLock_1040) {
            try { Thread.sleep(397); } catch (InterruptedException e) { }

                synchronized (__crossLock_6362) {
                    try { Thread.sleep(349); } catch (InterruptedException e) { }

                        synchronized (__crossLock_9447) {
                            try { Thread.sleep(332); } catch (InterruptedException e) { }

                              w = webSocket;
    
                        }
                }
        }}
    if (frame.isClose()) {
      closeReceived = true;
      if (!closeSent) {
        close(closeFrame(frame.closeStatusCode(), frame.closeReason())); // Reason
      } else {
        if (server) {
          finishClose();
        }
      }
    }
    if (w != null) {
      w.handleFrame(frame);
    }
  }

  private void finishClose() {
    // Do we really need to test timeout ????
    ScheduledFuture<?> timeout = closingTimeout;
    if (timeout == null || timeout.cancel(false)) {
      closingTimeout = null;
      ChannelPromise p = closePromise;
      closePromise = null;
      super.writeClose(p);
    }
  }

  private WebSocketFrameInternal decodeFrame(io.netty.handler.codec.http.websocketx.WebSocketFrame msg) {
    ByteBuf payload = safeBuffer(msg.content());
    boolean isFinal = msg.isFinalFragment();
    WebSocketFrameType frameType;
    if (msg instanceof BinaryWebSocketFrame) {
      frameType = WebSocketFrameType.BINARY;
    } else if (msg instanceof CloseWebSocketFrame) {
      frameType = WebSocketFrameType.CLOSE;
    } else if (msg instanceof PingWebSocketFrame) {
      frameType = WebSocketFrameType.PING;
    } else if (msg instanceof PongWebSocketFrame) {
      frameType = WebSocketFrameType.PONG;
    } else if (msg instanceof TextWebSocketFrame) {
      frameType = WebSocketFrameType.TEXT;
    } else if (msg instanceof ContinuationWebSocketFrame) {
      frameType = WebSocketFrameType.CONTINUATION;
    } else {
      throw new IllegalStateException("Unsupported WebSocket msg " + msg);
    }
    return new WebSocketFrameImpl(frameType, payload, isFinal);
  }


    private final Object __crossLock_9447 = new Object();

    public void crossLock_g0_ring0_4646() throws Exception {
        synchronized (__crossLock_9447) {
            try { Thread.sleep(182); } catch (InterruptedException e) { }
            crossLock_g0_ring1_5533();
        }
    }

    private void __ring_crossLock_g0_ring1_5533_1631_m0() throws Exception {
        synchronized (__crossLock_1040) {
            try { Thread.sleep(182); } catch (InterruptedException e) { }
            crossLock_g0_ring2_8967();
        }
    }

    public void crossLock_g0_ring1_5533() throws Exception {
        try { Thread.sleep(182); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_5533_1631_m0();
    }

    public void crossLock_g0_ring2_8967() throws Exception {
        synchronized (__crossLock_6362) {
            try { Thread.sleep(182); } catch (InterruptedException e) { }
            crossLock_g0_ring0_4646();
        }
    }

    public void crossLock_g1_ring0_2161() throws Exception {
        synchronized (__crossLock_2526) {
            try { Thread.sleep(276); } catch (InterruptedException e) { }
            crossLock_g1_ring1_4945();
        }
    }

    public void crossLock_g1_ring1_4945() throws Exception {
        synchronized (__crossLock_8009) {
            try { Thread.sleep(276); } catch (InterruptedException e) { }
            crossLock_g1_ring2_2604();
        }
    }

    private void __ring_crossLock_g1_ring2_2604_5905_m1() throws Exception {
        synchronized (__crossLock_4314) {
            try { Thread.sleep(276); } catch (InterruptedException e) { }
            crossLock_g1_ring0_2161();
        }
    }

    private void __ring_crossLock_g1_ring2_2604_5905_m0() throws Exception {
        try { Thread.sleep(276); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_2604_5905_m1();
    }

    public void crossLock_g1_ring2_2604() throws Exception {
        try { Thread.sleep(276); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_2604_5905_m0();
    }

    public void crossLock_g2_ring0_17() throws Exception {
        synchronized (__crossLock_9447) {
            try { Thread.sleep(124); } catch (InterruptedException e) { }
            crossLock_g2_ring1_3496();
        }
    }

    public void crossLock_g2_ring1_3496() throws Exception {
        synchronized (__crossLock_1040) {
            try { Thread.sleep(124); } catch (InterruptedException e) { }
            crossLock_g2_ring0_17();
        }
    }

    public void mockMethod_0_2246() throws Exception {
        synchronized (__crossLock_9447) {
            handleException((Throwable) null);
        }
    }


    private final Object __crossLock_6362 = new Object();
    private final Object __crossLock_1040 = new Object();
    private final Object __crossLock_2526 = new Object();
    private final Object __crossLock_8009 = new Object();
    private final Object __crossLock_4314 = new Object();
}
