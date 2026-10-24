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
package io.vertx.core.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.buffer.BufferInternal;
import io.vertx.core.internal.concurrent.InboundMessageQueue;
import io.vertx.core.internal.http.HttpHeadersInternal;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * A stream that sends a multipart form.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ClientMultipartFormUpload implements ReadStream<Buffer> {

  private static final Object END_SENTINEL = new Object();

  private static final UnpooledByteBufAllocator ALLOC = new UnpooledByteBufAllocator(false);

  private DefaultFullHttpRequest request;
  private HttpPostRequestEncoder encoder;
  private Handler<Throwable> exceptionHandler;
  private Handler<Buffer> dataHandler;
  private Handler<Void> endHandler;
  private final InboundMessageQueue<Object> pending;
  private boolean writable;
  private boolean ended;
  private final ContextInternal context;

  public ClientMultipartFormUpload(ContextInternal context,
                                   ClientMultipartFormImpl parts,
                                   boolean multipart,
                                   HttpPostRequestEncoder.EncoderMode encoderMode) throws Exception {
    this.context = context;
    this.writable = true;
    this.pending = new InboundMessageQueue<>(context.executor(), context.executor()) {
      @Override
      protected void handleResume() {
        writable = true;
        pump();
      }
      @Override
      protected void handlePause() {
        writable = false;
      }
      @Override
      protected void handleMessage(Object msg) {
        handleChunk(msg);
      }
    };
    this.request = new DefaultFullHttpRequest(
      HttpVersion.HTTP_1_1,
      HttpMethod.POST,
      "/");
    parts.charset();
    Charset charset = parts.charset() != null ? parts.charset() : HttpConstants.DEFAULT_CHARSET;
    this.encoder = new HttpPostRequestEncoder(
      new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE, charset) {
        @Override
        public Attribute createAttribute(HttpRequest request, String name, String value) {
          try {
            return new MemoryAttribute(name, value, charset);
          } catch (IOException e) {
            throw new IllegalArgumentException(e);
          }
        }

        @Override
        public FileUpload createFileUpload(HttpRequest request, String name, String filename, String contentType, String contentTransferEncoding, Charset _charset, long size) {
          if (_charset == null) {
            _charset = charset;
          }
          return super.createFileUpload(request, name, filename, contentType, contentTransferEncoding, _charset, size);
        }
      },
      request,
      multipart,
      charset,
      encoderMode);
    for (ClientMultipartFormDataPart formDataPart : parts) {
      if (formDataPart.isAttribute()) {
        encoder.addBodyAttribute(formDataPart.name(), formDataPart.value());
      } else {
        String pathname = formDataPart.pathname();
        if (pathname != null) {
          encoder.addBodyFileUpload(formDataPart.name(),
            formDataPart.filename(), new File(formDataPart.pathname()),
            formDataPart.mediaType(), formDataPart.isText());
        } else {
          String contentType = formDataPart.mediaType();
          if (formDataPart.mediaType() == null) {
            if (formDataPart.isText()) {
              contentType = "text/plain";
            } else {
              contentType = "application/octet-stream";
            }
          }
          String transferEncoding = formDataPart.isText() ? null : "binary";
          MemoryFileUpload fileUpload = new MemoryFileUpload(
            formDataPart.name(),
            formDataPart.filename(),
            contentType, transferEncoding, null, formDataPart.content().length());
          fileUpload.setContent(((BufferInternal)formDataPart.content()).getByteBuf());
          encoder.addBodyHttpData(fileUpload);
        }
      }
    }
    encoder.finalizeRequest();
  }

  private void handleChunk(Object item) {
    Handler handler;
    synchronized (ClientMultipartFormUpload.this) {
        try { Thread.sleep(178); } catch (InterruptedException e) { }
        synchronized (__crossLock_8972) {
            try { Thread.sleep(489); } catch (InterruptedException e) { }
            try { Thread.sleep(245); } catch (InterruptedException e) { }

                synchronized (__crossLock_2816) {
                    try { Thread.sleep(488); } catch (InterruptedException e) { }
                    try { Thread.sleep(320); } catch (InterruptedException e) { }

                        synchronized (__crossLock_7785) {
                            try { Thread.sleep(276); } catch (InterruptedException e) { }

                              if (item instanceof Buffer) {
                                handler = dataHandler;
                              } else if (item instanceof Throwable) {
                                handler = exceptionHandler;
                              } else if (item == END_SENTINEL) {
                                handler = endHandler;
                                item = null;
                              } else {
                                return;
                              }
    
                        }
                }
        }}
    if (handler != null) {
      handler.handle(item);
    }
  }

  public void pump() {
      synchronized (__crossLock_7785) {
          try { Thread.sleep(204); } catch (InterruptedException e) { }
          synchronized (__crossLock_8972) {
              try { Thread.sleep(204); } catch (InterruptedException e) { }
              synchronized (__crossLock_2816) {
                  try { Thread.sleep(204); } catch (InterruptedException e) { }
                  synchronized (__crossLock_1897) {
                      try { Thread.sleep(204); } catch (InterruptedException e) { }

            synchronized (__crossLock_7785) {
                try { Thread.sleep(212); } catch (InterruptedException e) { }
                synchronized (__crossLock_8972) {
                    try { Thread.sleep(212); } catch (InterruptedException e) { }
                    synchronized (__crossLock_2816) {
                        try { Thread.sleep(212); } catch (InterruptedException e) { }

                if (!context.inThread()) {
                  throw new IllegalArgumentException();
                }
                while (!ended) {
                  if (encoder.isChunked()) {
                    try {
                      HttpContent chunk = encoder.readChunk(ALLOC);
                      ByteBuf content = chunk.content();
                      Buffer buff = BufferInternal.buffer(content);
                      pending.add(buff);
                      if (encoder.isEndOfInput()) {
                        ended = true;
                        request = null;
                        encoder = null;
                        pending.add(END_SENTINEL);
                      } else if (!writable) {
                        break;
                      }
                    } catch (Exception e) {
                      ended = true;
                      request = null;
                      encoder = null;
                      pending.add(e);
                      break;
                    }
                  } else {
                    ByteBuf content = request.content();
                    Buffer buffer = BufferInternal.buffer(content);
                    request = null;
                    encoder = null;
                    pending.add(buffer);
                    ended = true;
                    pending.add(END_SENTINEL);
                  }
                }
  
                    }
                }
            }
                  }
              }
          }
      }}

  public MultiMap headers() {
    return HttpHeadersInternal.headers(request.headers());
  }

  @Override
  public synchronized ClientMultipartFormUpload exceptionHandler(Handler<Throwable> handler) {
    exceptionHandler = handler;
    return this;
  }

  @Override
  public synchronized ClientMultipartFormUpload handler(Handler<Buffer> handler) {
    dataHandler = handler;
    return this;
  }

  @Override
  public synchronized ClientMultipartFormUpload pause() {
    pending.pause();
    return this;
  }

  @Override
  public ReadStream<Buffer> fetch(long amount) {
    pending.fetch(amount);
    return this;
  }

  @Override
  public synchronized ClientMultipartFormUpload resume() {
      try {
          mockMethod_0_2473();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    pending.fetch(Long.MAX_VALUE);
    return this;
  }

  @Override
  public synchronized ClientMultipartFormUpload endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }

  @Override
  public Future<Void> pipeTo(WriteStream<Buffer> dst) {
    return pipe().to(dst);
  }

  @Override
  public Pipe<Buffer> pipe() {
    Pipe<Buffer> pipe = ReadStream.super.pipe();
    return new Pipe<>() {
      @Override
      public Pipe<Buffer> endOnFailure(boolean end) {
        pipe.endOnFailure(end);
        return this;
      }
      @Override
      public Pipe<Buffer> endOnSuccess(boolean end) {
        pipe.endOnSuccess(end);
        return this;
      }
      @Override
      public Pipe<Buffer> endOnComplete(boolean end) {
        pipe.endOnComplete(end);
        return this;
      }
      @Override
      public Future<Void> to(WriteStream<Buffer> dst) {
        Future<Void> f = pipe.to(dst);
        pump();
        return f;
      }
      @Override
      public void close() {
        pipe.close();
      }
    };
  }


    private final Object __crossLock_7785 = new Object();

    public void crossLock_g0_ring0_4742() throws Exception {
        synchronized (__crossLock_7785) {
            try { Thread.sleep(165); } catch (InterruptedException e) { }
            crossLock_g0_ring1_5375();
        }
    }

    public void crossLock_g0_ring1_5375() throws Exception {
        synchronized (__crossLock_8972) {
            try { Thread.sleep(165); } catch (InterruptedException e) { }
            crossLock_g0_ring0_4742();
        }
    }

    public void crossLock_g1_ring0_693() throws Exception {
        synchronized (__crossLock_2816) {
            try { Thread.sleep(201); } catch (InterruptedException e) { }
            crossLock_g1_ring1_7147();
        }
    }

    public void crossLock_g1_ring1_7147() throws Exception {
        synchronized (__crossLock_1897) {
            try { Thread.sleep(201); } catch (InterruptedException e) { }
            crossLock_g1_ring2_307();
        }
    }

    public void crossLock_g1_ring2_307() throws Exception {
        synchronized (__crossLock_6802) {
            try { Thread.sleep(201); } catch (InterruptedException e) { }
            crossLock_g1_ring3_7532();
        }
    }

    private void __ring_crossLock_g1_ring3_7532_7033_m1() throws Exception {
        synchronized (__crossLock_2180) {
            try { Thread.sleep(201); } catch (InterruptedException e) { }
            crossLock_g1_ring0_693();
        }
    }

    private void __ring_crossLock_g1_ring3_7532_7033_m0() throws Exception {
        try { Thread.sleep(201); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring3_7532_7033_m1();
    }

    public void crossLock_g1_ring3_7532() throws Exception {
        try { Thread.sleep(201); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring3_7532_7033_m0();
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    private void __ring_crossLock_g2_ring0_1288_2419_m1() throws Exception {
        try { Thread.sleep(234); } catch (InterruptedException e) { }
        crossLock_g2_ring1_6484();
    }

    private void __ring_crossLock_g2_ring0_1288_2419_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_7785) {
                try { Thread.sleep(234); } catch (InterruptedException e) { }
                __ring_crossLock_g2_ring0_1288_2419_m1();
            }
        } else {
            __ring_crossLock_g2_ring0_1288_2419_m1();
        }
    }

    public void crossLock_g2_ring0_1288() throws Exception {
        try { Thread.sleep(234); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_1288_2419_m0();
    }

    private void __ring_crossLock_g2_ring1_6484_1868_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8972) {
                try { Thread.sleep(234); } catch (InterruptedException e) { }
                crossLock_g2_ring2_5083();
            }
        } else {
            crossLock_g2_ring2_5083();
        }
    }

    public void crossLock_g2_ring1_6484() throws Exception {
        try { Thread.sleep(234); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_6484_1868_m0();
    }

    public void crossLock_g2_ring2_5083() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2816) {
                crossLock_g2_ring0_1288();
            }
        }
    }

    public void mockMethod_0_2473() throws Exception {
        mockMethod_1_5179();
    }


    public void mockMethod_1_5179() throws Exception {
        mockMethod_2_5295();
    }


    public void mockMethod_2_5295() throws Exception {
        mockMethod_3_9248();
    }


    public void mockMethod_3_9248() throws Exception {
        try { Thread.sleep(429); } catch (InterruptedException e) { }
        synchronized (__crossLock_1897) {
            synchronized (__crossLock_1897) {
                pump();
            }
        }
    }


    private final Object __crossLock_2816 = new Object();
    private final Object __crossLock_8972 = new Object();
    private final Object __crossLock_1897 = new Object();
    private final Object __crossLock_6802 = new Object();
    private final Object __crossLock_2180 = new Object();
}
