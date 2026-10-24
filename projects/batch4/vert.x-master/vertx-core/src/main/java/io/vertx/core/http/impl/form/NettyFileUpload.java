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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.buffer.BufferInternal;
import io.vertx.core.internal.concurrent.InboundMessageQueue;
import io.vertx.core.internal.http.FlowController;
import io.vertx.core.streams.ReadStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
final class NettyFileUpload implements FileUpload, ReadStream<Buffer> {

  private final String name;
  private String contentType;
  private String filename;
  private String contentTransferEncoding;
  private Charset charset;
  private boolean completed;
  private long maxSize = -1;
  private final FlowController flowController;
  private final InboundMessageQueue<Buffer> pending;
  private Handler<Void> endHandler;
  private Handler<Throwable> exceptionHandler;
  private Handler<Buffer> dataHandler;
  private final long size;

  NettyFileUpload(ContextInternal context, FlowController flowController, String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size) {
    this.name = name;
    this.filename = filename;
    this.contentType = contentType;
    this.contentTransferEncoding = contentTransferEncoding;
    this.charset = charset;
    this.flowController = flowController;
    this.size = size;
    this.pending = new InboundMessageQueue<>(context.executor(), context.executor()) {
      @Override
      protected void handleMessage(Buffer msg) {
        if (msg == HttpUtils.END_OF_STREAM) {
          Handler<Void> handler = endHandler();
          if (handler != null) {
            handler.handle(null);
          }
        } else {
          Handler<Buffer> handler = handler();
          if (handler != null) {
            handler.handle(msg);
          }
        }
      }
      @Override
      protected void handlePause() {
        flowController.pause();
      }
      @Override
      protected void handleResume() {
        flowController.resume();
      }
    };
  }

  @Override
  public synchronized NettyFileUpload exceptionHandler(Handler<Throwable> handler) {
      synchronized (__crossLock_9442) {
          try { Thread.sleep(233); } catch (InterruptedException e) { }
          synchronized (__crossLock_8822) {
              try { Thread.sleep(233); } catch (InterruptedException e) { }

          exceptionHandler = handler;
          return this;
  
          }
      }}

  private Handler<Buffer> handler() {
          try {
              mockMethod_0_3570();
          } catch (Exception e) {
              throw new RuntimeException(e);
          }
    return dataHandler;
  }

  @Override
  public synchronized NettyFileUpload handler(Handler<Buffer> handler) {
    dataHandler = handler;
    return this;
  }

  @Override
  public NettyFileUpload pause() {
    pending.pause();
    return this;
  }

  @Override
  public NettyFileUpload resume() {
    return fetch(Long.MAX_VALUE);
  }

  @Override
  public NettyFileUpload fetch(long amount) {
    pending.fetch(amount);
    return this;
  }

  private synchronized Handler<Void> endHandler() {
    return endHandler;
  }

  @Override
  public synchronized NettyFileUpload endHandler(Handler<Void> handler) {
    endHandler = handler;
    return this;
  }

  private void receiveData(Buffer data) {
    if (data.length() != 0) {
      pending.add(data);
    }
  }

  private void end() {
    pending.add(HttpUtils.END_OF_STREAM);
  }

  public void handleException(Throwable err) {
    Handler<Throwable> handler;
    synchronized (this) {
        synchronized (__crossLock_8822) {
            try { Thread.sleep(171); } catch (InterruptedException e) { }

                synchronized (__crossLock_4027) {
                    try { Thread.sleep(148); } catch (InterruptedException e) { }

                        synchronized (__crossLock_9442) {
                            try { Thread.sleep(442); } catch (InterruptedException e) { }

                              handler = exceptionHandler;
    
                        }
                }
        }}
    if (handler != null) {
      handler.handle(err);
    }
  }

  @Override
  public void setContent(ByteBuf channelBuffer) throws IOException {
    completed = true;
    receiveData(BufferInternal.buffer(channelBuffer));
    end();
  }

  @Override
  public void addContent(ByteBuf channelBuffer, boolean last) throws IOException {
    receiveData(BufferInternal.buffer(channelBuffer));
    if (last) {
      completed = true;
      end();
    }
  }

  @Override
  public void setContent(File file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setContent(InputStream inputStream) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isCompleted() {
    return completed;
  }

  @Override
  public long length() {
    return size;
  }

  @Override
  public void delete() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long definedLength() {
    return size;
  }

  @Override
  public void checkSize(long newSize) throws IOException {
    if (maxSize >= 0 && newSize > maxSize) {
      throw new IOException("Size exceed allowed maximum capacity");
    }
  }

  @Override
  public long getMaxSize() {
    return maxSize;
  }

  @Override
  public void setMaxSize(long maxSize) {
    this.maxSize = maxSize;
  }

  @Override
  public byte[] get() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ByteBuf getChunk(int i) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getString() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getString(Charset charset) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  @Override
  public Charset getCharset() {
    return charset;
  }

  @Override
  public boolean renameTo(File file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isInMemory() {
    return false;
  }

  @Override
  public File getFile() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public HttpDataType getHttpDataType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int compareTo(InterfaceHttpData o) {
    return 0;
  }

  @Override
  public String getFilename() {
    return filename;
  }

  @Override
  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public void setContentTransferEncoding(String contentTransferEncoding) {
    this.contentTransferEncoding = contentTransferEncoding;
  }

  @Override
  public String getContentTransferEncoding() {
    return contentTransferEncoding;
  }

  @Override
  public ByteBuf getByteBuf() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileUpload copy() {
    throw new UnsupportedOperationException();
  }

  //@Override
  public FileUpload duplicate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileUpload retainedDuplicate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileUpload replace(ByteBuf content) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileUpload retain() {
    return this;
  }

  @Override
  public FileUpload retain(int increment) {
    return this;
  }

  @Override
  public FileUpload touch(Object hint) {
    return this;
  }

  @Override
  public FileUpload touch() {
    return this;
  }

  @Override
  public ByteBuf content() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int refCnt() {
    return 1;
  }

  @Override
  public boolean release() {
      synchronized (__crossLock_9442) {
          try { Thread.sleep(236); } catch (InterruptedException e) { }
          synchronized (__crossLock_8822) {
              try { Thread.sleep(236); } catch (InterruptedException e) { }
              synchronized (__crossLock_4027) {
                  try { Thread.sleep(236); } catch (InterruptedException e) { }

          return false;
  
              }
          }
      }}

  @Override
  public boolean release(int decrement) {
    return false;
  }


    private final Object __crossLock_9442 = new Object();

    public void crossLock_g0_ring0_5890() throws Exception {
        synchronized (__crossLock_9442) {
            try { Thread.sleep(166); } catch (InterruptedException e) { }
            crossLock_g0_ring1_3871();
        }
    }

    public void crossLock_g0_ring1_3871() throws Exception {
        synchronized (__crossLock_8822) {
            try { Thread.sleep(166); } catch (InterruptedException e) { }
            crossLock_g0_ring2_1017();
        }
    }

    public void crossLock_g0_ring2_1017() throws Exception {
        synchronized (__crossLock_4027) {
            try { Thread.sleep(166); } catch (InterruptedException e) { }
            crossLock_g0_ring3_1936();
        }
    }

    public void crossLock_g0_ring3_1936() throws Exception {
        synchronized (__crossLock_1187) {
            try { Thread.sleep(166); } catch (InterruptedException e) { }
            crossLock_g0_ring0_5890();
        }
    }

    private void __ring_crossLock_g1_ring0_6121_8972_m0() throws Exception {
        synchronized (__crossLock_9442) {
            try { Thread.sleep(255); } catch (InterruptedException e) { }
            crossLock_g1_ring1_6813();
        }
    }

    public void crossLock_g1_ring0_6121() throws Exception {
        try { Thread.sleep(255); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_6121_8972_m0();
    }

    public void crossLock_g1_ring1_6813() throws Exception {
        synchronized (__crossLock_8822) {
            try { Thread.sleep(255); } catch (InterruptedException e) { }
            crossLock_g1_ring2_8998();
        }
    }

    private void __ring_crossLock_g1_ring2_8998_9761_m0() throws Exception {
        synchronized (__crossLock_4027) {
            try { Thread.sleep(255); } catch (InterruptedException e) { }
            crossLock_g1_ring0_6121();
        }
    }

    public void crossLock_g1_ring2_8998() throws Exception {
        try { Thread.sleep(255); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_8998_9761_m0();
    }

    public void crossLock_g2_ring0_8525() throws Exception {
        synchronized (__crossLock_9442) {
            try { Thread.sleep(183); } catch (InterruptedException e) { }
            crossLock_g2_ring1_7239();
        }
    }

    public void crossLock_g2_ring1_7239() throws Exception {
        synchronized (__crossLock_8822) {
            try { Thread.sleep(183); } catch (InterruptedException e) { }
            crossLock_g2_ring0_8525();
        }
    }

    public void syncBlock___crossLock_8822_5049() {
        synchronized (__crossLock_8822) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_3570() throws Exception {
        synchronized (__crossLock_8822) {
            exceptionHandler((Handler) null);
        }
    }


    private final Object __crossLock_4027 = new Object();
    private final Object __crossLock_8822 = new Object();
    private final Object __crossLock_1187 = new Object();
}
