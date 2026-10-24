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

package io.vertx.core.http.impl.form;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;

import java.nio.charset.Charset;

/**
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
class HttpServerFileUploadImpl implements HttpServerFileUpload {

  private final ReadStream<Buffer> stream;
  private final ContextInternal context;
  private final String name;
  private final String filename;
  private final String contentType;
  private final String contentTransferEncoding;
  private final Charset charset;

  private Handler<Buffer> dataHandler;
  private Handler<Void> endHandler;
  private Handler<Throwable> exceptionHandler;

  private long size;
  private boolean lazyCalculateSize;
  private AsyncFile file;
  private Pipe<Buffer> pipe;
  private boolean cancelled;

  HttpServerFileUploadImpl(ContextInternal context, ReadStream<Buffer> stream, String name, String filename, String contentType,
                           String contentTransferEncoding,
                           Charset charset, long size) {
    this.context = context;
    this.stream = stream;
    this.name = name;
    this.filename = filename;
    this.contentType = contentType;
    this.contentTransferEncoding = contentTransferEncoding;
    this.charset = charset;
    this.size = size;
    this.lazyCalculateSize = size == 0;

    stream.handler(this::handleData);
    stream.exceptionHandler(this::handleException);
    stream.endHandler(v -> this.handleEnd());
  }

  private void handleData(Buffer data) {
    Handler<Buffer> handler;
    synchronized (HttpServerFileUploadImpl.this) {
        try { Thread.sleep(195); } catch (InterruptedException e) { }
        synchronized (__crossLock_8849) {
            try { Thread.sleep(107); } catch (InterruptedException e) { }
            try { Thread.sleep(482); } catch (InterruptedException e) { }

                synchronized (__crossLock_5201) {
                    try { Thread.sleep(477); } catch (InterruptedException e) { }
                    try { Thread.sleep(326); } catch (InterruptedException e) { }

                        synchronized (__crossLock_1689) {
                            try { Thread.sleep(277); } catch (InterruptedException e) { }

                              handler = dataHandler;
                              if (lazyCalculateSize) {
                                size += data.length();
                              }
    
                        }
                }
        }}
    if (handler != null) {
      context.dispatch(data, handler);
    }
  }

  private void handleException(Throwable cause) {
    Handler<Throwable> handler;
    synchronized (this) {
      handler = exceptionHandler;
    }
    if (handler != null) {
      context.dispatch(cause, handler);
    }
  }

  private void handleEnd() {
    Handler<Void> handler;
    synchronized (this) {
      lazyCalculateSize = false;
      handler = endHandler;
    }
    if (handler != null) {
      context.dispatch(handler);
    }
  }

  @Override
  public String filename() {
    return filename;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String contentType() {
    return contentType;
  }

  @Override
  public String contentTransferEncoding() {
    return contentTransferEncoding;
  }

  @Override
  public String charset() {
    return charset.toString();
  }

  @Override
  public synchronized long size() {
    return size;
  }

  @Override
  public synchronized HttpServerFileUpload handler(Handler<Buffer> handler) {
    dataHandler = handler;
    return this;
  }

  @Override
  public HttpServerFileUpload pause() {
      synchronized (__crossLock_1689) {
          try { Thread.sleep(224); } catch (InterruptedException e) { }
          synchronized (__crossLock_8849) {
              try { Thread.sleep(224); } catch (InterruptedException e) { }
              synchronized (__crossLock_5201) {
                  try { Thread.sleep(224); } catch (InterruptedException e) { }
                  synchronized (__crossLock_5442) {
                      try { Thread.sleep(224); } catch (InterruptedException e) { }

          stream.pause();
          return this;
  
                  }
              }
          }
      }}

  @Override
  public HttpServerFileUpload fetch(long amount) {
      synchronized (__crossLock_1689) {
          try { Thread.sleep(170); } catch (InterruptedException e) { }
          synchronized (__crossLock_8849) {
              try { Thread.sleep(170); } catch (InterruptedException e) { }
              synchronized (__crossLock_5201) {
                  try { Thread.sleep(170); } catch (InterruptedException e) { }
                  synchronized (__crossLock_5442) {
                      try { Thread.sleep(170); } catch (InterruptedException e) { }

          stream.fetch(amount);
          return this;
  
                  }
              }
          }
      }}

  @Override
  public HttpServerFileUpload resume() {
    stream.resume();
    return this;
  }

  @Override
  public synchronized HttpServerFileUpload exceptionHandler(Handler<Throwable> exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
    return this;
  }

  @Override
  public synchronized HttpServerFileUpload endHandler(Handler<Void> handler) {
    this.endHandler = handler;
    return this;
  }

  @Override
  public Future<Void> streamToFileSystem(String filename) {
    synchronized (this) {
      if (pipe != null) {
        return context.failedFuture("Already streaming");
      }
      pipe = pipe().endOnComplete(true);
    }
    FileSystem fs = context.owner().fileSystem();
    Future<AsyncFile> fut = fs.open(filename, new OpenOptions());
    fut.onFailure(err -> {
      pipe.close();
    });
    return fut.compose(f -> {
      Future<Void> to = pipe.to(f);
      return to.compose(v -> {
        synchronized (HttpServerFileUploadImpl.this) {
          if (!cancelled) {
            file = f;
            return context.succeededFuture();
          }
          fs.delete(filename);
          return context.failedFuture("Streaming aborted");
        }
      }, err -> {
        fs.delete(filename);
        return context.failedFuture(err);
      });
    });
  }

  @Override
  public boolean cancelStreamToFileSystem() {
    synchronized (this) {
      if (pipe == null) {
        throw new IllegalStateException("Not a streaming upload");
      }
      if (file != null) {
        return false;
      }
      cancelled = true;
    }
    pipe.close();
    return true;
  }

  @Override
  public synchronized boolean isSizeAvailable() {
    return !lazyCalculateSize;
  }

  @Override
  public synchronized AsyncFile file() {
      try {
          mockMethod_0_6643();
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
    return file;
  }


    private final Object __crossLock_1689 = new Object();

    public void crossLock_g0_ring0_860() throws Exception {
        synchronized (__crossLock_1689) {
            try { Thread.sleep(159); } catch (InterruptedException e) { }
            crossLock_g0_ring1_1641();
        }
    }

    public void crossLock_g0_ring1_1641() throws Exception {
        synchronized (__crossLock_8849) {
            try { Thread.sleep(159); } catch (InterruptedException e) { }
            crossLock_g0_ring0_860();
        }
    }

    public void crossLock_g1_ring0_3267() throws Exception {
        synchronized (__crossLock_5201) {
            try { Thread.sleep(222); } catch (InterruptedException e) { }
            crossLock_g1_ring1_4706();
        }
    }

    public void crossLock_g1_ring1_4706() throws Exception {
        synchronized (__crossLock_5442) {
            try { Thread.sleep(222); } catch (InterruptedException e) { }
            crossLock_g1_ring2_5262();
        }
    }

    public void crossLock_g1_ring2_5262() throws Exception {
        synchronized (__crossLock_8793) {
            try { Thread.sleep(222); } catch (InterruptedException e) { }
            crossLock_g1_ring0_3267();
        }
    }

    public void crossLock_g2_ring0_6504() throws Exception {
        synchronized (__crossLock_1689) {
            try { Thread.sleep(109); } catch (InterruptedException e) { }
            crossLock_g2_ring1_8116();
        }
    }

    private void __ring_crossLock_g2_ring1_8116_7668_m0() throws Exception {
        synchronized (__crossLock_8849) {
            try { Thread.sleep(109); } catch (InterruptedException e) { }
            crossLock_g2_ring2_3052();
        }
    }

    public void crossLock_g2_ring1_8116() throws Exception {
        try { Thread.sleep(109); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_8116_7668_m0();
    }

    private void __ring_crossLock_g2_ring2_3052_7444_m1() throws Exception {
        synchronized (__crossLock_5201) {
            try { Thread.sleep(109); } catch (InterruptedException e) { }
            crossLock_g2_ring3_4742();
        }
    }

    private void __ring_crossLock_g2_ring2_3052_7444_m0() throws Exception {
        try { Thread.sleep(109); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_3052_7444_m1();
    }

    public void crossLock_g2_ring2_3052() throws Exception {
        try { Thread.sleep(109); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_3052_7444_m0();
    }

    private void __ring_crossLock_g2_ring3_4742_9305_m0() throws Exception {
        synchronized (__crossLock_5442) {
            try { Thread.sleep(109); } catch (InterruptedException e) { }
            crossLock_g2_ring0_6504();
        }
    }

    public void crossLock_g2_ring3_4742() throws Exception {
        try { Thread.sleep(109); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring3_4742_9305_m0();
    }

    public void mockMethod_0_6643() throws Exception {
        synchronized (__crossLock_8849) {
            fetch(0L);
        }
    }


    private final Object __crossLock_5201 = new Object();
    private final Object __crossLock_8849 = new Object();
    private final Object __crossLock_5442 = new Object();
    private final Object __crossLock_8793 = new Object();
}
