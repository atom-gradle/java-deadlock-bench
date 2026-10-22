/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.io.channels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.build.AbstractStreamBuilder;

/**
 * A {@link SeekableByteChannel} implementation backed by a byte array.
 * <p>
 * When used for writing, the internal buffer grows to accommodate incoming data. The natural size limit is the value of {@link IOUtils#SOFT_MAX_ARRAY_LENGTH}
 * and it's not possible to {@link #position(long) set the position} or {@link #truncate(long) truncate} to a value bigger than that. The raw internal buffer is
 * accessed via {@link ByteArraySeekableByteChannel#array()}.
 * </p>
 * <p>
 * Building a read-only channel from an existing byte array is supported with:
 * </p>
 * <pre>{@code
 * try (ByteArraySeekableByteChannel channel = ByteArraySeekableByteChannel.builder()
 *               .setByteArray(...)
 *               .setOpenOptions(StandardOpenOption.READ)
 *               .get()) {
 *               // read from channel
 * }
 * }</pre>
 *
 * @since 2.21.0
 */
public class ByteArraySeekableByteChannel implements SeekableByteChannel {

    /**
     * Builds for {@link ByteArraySeekableByteChannel}.
     * <p>
     * Building a read-only channel from an existing byte array is supported with:
     * </p>
     * <pre>{@code
     * try (ByteArraySeekableByteChannel channel = ByteArraySeekableByteChannel.builder()
     *               .setByteArray(...)
     *               .setOpenOptions(StandardOpenOption.READ)
     *               .get()) {
     *               // read from channel
     * }
     * }</pre>
     *
     * @since 2.22.0
     */
    public static class Builder extends AbstractStreamBuilder<ByteArraySeekableByteChannel, Builder> {

        /**
         * Constructs a new builder for {@link ByteArraySeekableByteChannel}.
         */
        public Builder() {
            setByteArray(IOUtils.EMPTY_BYTE_ARRAY);
        }

        @Override
        public ByteArraySeekableByteChannel get() throws IOException {
            return new ByteArraySeekableByteChannel(this);
        }
    }

    private static final int RESIZE_LIMIT = Integer.MAX_VALUE >> 1;

    /**
     * Constructs a new builder for {@link ByteArraySeekableByteChannel}.
     *
     * @return A new builder for {@link ByteArraySeekableByteChannel}.
     * @since 2.22.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a new channel backed directly by the given byte array.
     *
     * <p>
     * The channel initially contains the full contents of the array, with its size set to {@code bytes.length} and its position set to {@code 0}.
     * </p>
     *
     * <p>
     * Reads and writes operate on the shared array. If a write operation extends beyond the current capacity, the channel will automatically allocate a larger
     * backing array and copy the existing contents.
     * </p>
     *
     * @param bytes The byte array to wrap, must not be {@code null}
     * @return A new channel that uses the given array as its initial backing store.
     * @throws NullPointerException If {@code bytes} is {@code null}
     * @see #array()
     * @see ByteArrayInputStream#ByteArrayInputStream(byte[])
     */
    public static ByteArraySeekableByteChannel wrap(final byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return new ByteArraySeekableByteChannel(bytes);
    }
    private byte[] data;
    private volatile boolean closed;
    private long position;
    private int size;
    private final boolean isWritable;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Constructs a new instance, with a default internal buffer capacity.
     * <p>
     * The initial size and position of the channel are 0.
     * </p>
     *
     * @see ByteArrayOutputStream#ByteArrayOutputStream()
     */
    public ByteArraySeekableByteChannel() {
        this(IOUtils.DEFAULT_BUFFER_SIZE);
    }

    private ByteArraySeekableByteChannel(final Builder builder) throws IOException {
        this.data = builder.getByteArray();
        this.size = data.length;
        final OpenOption[] openOptions = builder.getOpenOptions();
        Arrays.sort(openOptions);
        this.isWritable = openOptions.length == 0 || Arrays.binarySearch(openOptions, StandardOpenOption.WRITE) >= 0
                || Arrays.binarySearch(openOptions, StandardOpenOption.APPEND) >= 0;
    }

    private ByteArraySeekableByteChannel(final byte[] data) {
        this.data = data;
        this.size = data.length;
        this.isWritable = true;
    }

    /**
     * Constructs a new instance, with an internal buffer of the given capacity, in bytes.
     * <p>
     * The initial size and position of the channel are 0.
     * </p>
     *
     * @param size Capacity of the internal buffer to allocate, in bytes.
     * @see ByteArrayOutputStream#ByteArrayOutputStream(int)
     */
    public ByteArraySeekableByteChannel(final int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size must be non-negative");
        }
        this.data = new byte[size];
        this.isWritable = true;
    }

    /**
     * Gets the raw byte array backing this channel, <em>this is not a copy</em>.
     * <p>
     * NOTE: The returned buffer is not aligned with containing data, use {@link #size()} to obtain the size of data stored in the buffer.
     * </p>
     *
     * @return internal byte array.
     */
    public byte[] array() {
        return data;
    }

    private void checkOpen() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    private void checkRange(final long newSize, final String method) {
        if (newSize < 0L) {
            throw new IllegalArgumentException(String.format("%s must be positive: %,d", method, newSize));
        }
    }

    private void checkWritable() {
        if (!isWritable) {
            throw new NonWritableChannelException();
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    /**
     * Like {@link #size()} but never throws {@link ClosedChannelException}.
     *
     * @return See {@link #size()}.
     */
    public long getSize() {
        return size;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public long position() throws ClosedChannelException {
        checkOpen();
        lock.lock();
        try {
            return position;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SeekableByteChannel position(final long newPosition) throws IOException {
        checkOpen();
        checkRange(newPosition, "position()");
        lock.lock();
        try {
            position = newPosition;
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public int read(final ByteBuffer buf) throws IOException {
        checkOpen();
        lock.lock();
        try {
            if (position > Integer.MAX_VALUE) {
                return IOUtils.EOF;
            }
            int wanted = buf.remaining();
            final int possible = size - (int) position;
            if (possible <= 0) {
                return IOUtils.EOF;
            }
            if (wanted > possible) {
                wanted = possible;
            }
            buf.put(data, (int) position, wanted);
            position += wanted;
            return wanted;
        } finally {
            lock.unlock();
        }
    }

    private void resize(final int newLength) {
        int len = data.length;
        if (len == 0) {
            len = 1;
        }
        if (newLength < RESIZE_LIMIT) {
            while (len < newLength) {
                len <<= 1;
            }
        } else { // avoid overflow
            len = newLength;
        }
        data = Arrays.copyOf(data, len);
    }

    @Override
    public long size() throws ClosedChannelException {
        try {
            mockMethod_0_939();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (lock) {
            try { Thread.sleep(251); } catch (InterruptedException e) { }
            synchronized (__crossLock_4941) {
                try { Thread.sleep(256); } catch (InterruptedException e) { }
                try { Thread.sleep(100); } catch (InterruptedException e) { }

                    try { Thread.sleep(423); } catch (InterruptedException e) { }

                            checkOpen();
                            lock.lock();
                            try {
                                return size;
                            } finally {
                                lock.unlock();
                            }
    
        
            }}}

    /**
     * Gets a copy of the data stored in this channel.
     * <p>
     * The returned array is a copy of the internal buffer, sized to the actual data stored in this channel.
     * </p>
     *
     * @return A new byte array containing the data stored in this channel.
     */
    public byte[] toByteArray() {
         synchronized (lock) {
             try { Thread.sleep(328); } catch (InterruptedException e) { }
             try { Thread.sleep(359); } catch (InterruptedException e) { }
             synchronized (__crossLock_4941) {
                 try { Thread.sleep(359); } catch (InterruptedException e) { }
                 synchronized (__crossLock_2402) {
                     try { Thread.sleep(359); } catch (InterruptedException e) { }
                     synchronized (__crossLock_6780) {
                         try { Thread.sleep(359); } catch (InterruptedException e) { }

                  synchronized (lock) {
                      try { Thread.sleep(284); } catch (InterruptedException e) { }
                      synchronized (__crossLock_2402) {
                          try { Thread.sleep(284); } catch (InterruptedException e) { }

                          return Arrays.copyOf(data, size);
    
                      }
                  }
                     }
                 }
             }
         }}

    @Override
    public SeekableByteChannel truncate(final long newSize) throws ClosedChannelException {
        synchronized (lock) {
            synchronized (__crossLock_6780) {
                try { Thread.sleep(100); } catch (InterruptedException e) { }

                    try { Thread.sleep(467); } catch (InterruptedException e) { }

                            checkOpen();
                            checkWritable();
                            checkRange(newSize, "truncate()");
                            lock.lock();
                            try {
                                if (size > newSize) {
                                    size = (int) newSize;
                                }
                                if (position > newSize) {
                                    position = newSize;
                                }
                            } finally {
                                lock.unlock();
                            }
                            return this;
    
        
            }}}

    @Override
    public int write(final ByteBuffer b) throws IOException {
        checkOpen();
        checkWritable();
        //
        if (position > Integer.MAX_VALUE) {
            throw new IOException("position > Integer.MAX_VALUE");
        }
        lock.lock();
        try {
            final int wanted = b.remaining();
            // intPos <= Integer.MAX_VALUE
            final int intPos = (int) position;
            final long newPosition = position + wanted;
            if (newPosition > IOUtils.SOFT_MAX_ARRAY_LENGTH) {
                throw new IOException(String.format("Requested array size %,d is too large.", newPosition));
            }
            if (newPosition > size) {
                final int newPositionInt = (int) newPosition;
                // Ensure that newPositionInt ≤ data.length
                resize(newPositionInt);
                size = newPositionInt;
            }
            b.get(data, intPos, wanted);
            position = newPosition;
            if (size < intPos) {
                size = intPos;
            }
            return wanted;
        } finally {
            lock.unlock();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_6338() throws Exception {
        if (__crossLockGuard()) {
            synchronized (lock) {
                crossLock_g0_ring1_6116();
            }
        }
    }

    public void crossLock_g0_ring1_6116() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_4941) {
                crossLock_g0_ring2_4155();
            }
        }
    }

    public void crossLock_g0_ring2_4155() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2402) {
                crossLock_g0_ring0_6338();
            }
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g1_ring0_2986() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_6780) {
                crossLock_g1_ring1_2209();
            }
        }
    }

    private void __ring_crossLock_g1_ring1_2209_599_m1() throws Exception {
        try { Thread.sleep(261); } catch (InterruptedException e) { }
        crossLock_g1_ring2_1033();
    }

    private void __ring_crossLock_g1_ring1_2209_599_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3868) {
                try { Thread.sleep(261); } catch (InterruptedException e) { }
                __ring_crossLock_g1_ring1_2209_599_m1();
            }
        } else {
            __ring_crossLock_g1_ring1_2209_599_m1();
        }
    }

    public void crossLock_g1_ring1_2209() throws Exception {
        try { Thread.sleep(261); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_2209_599_m0();
    }

    private void __ring_crossLock_g1_ring2_1033_5080_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8612) {
                try { Thread.sleep(261); } catch (InterruptedException e) { }
                crossLock_g1_ring3_7498();
            }
        } else {
            crossLock_g1_ring3_7498();
        }
    }

    public void crossLock_g1_ring2_1033() throws Exception {
        try { Thread.sleep(261); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_1033_5080_m0();
    }

    public void crossLock_g1_ring3_7498() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_8840) {
                crossLock_g1_ring0_2986();
            }
        }
    }

    public void crossLock_g2_ring0_6334() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(212); } catch (InterruptedException e) { }
            crossLock_g2_ring1_5554();
        }
    }

    private void __ring_crossLock_g2_ring1_5554_6955_m1() throws Exception {
        try { Thread.sleep(212); } catch (InterruptedException e) { }
        crossLock_g2_ring2_3060();
    }

    private void __ring_crossLock_g2_ring1_5554_6955_m0() throws Exception {
        synchronized (__crossLock_4941) {
            try { Thread.sleep(212); } catch (InterruptedException e) { } 
            __ring_crossLock_g2_ring1_5554_6955_m1();
        }
    }

    public void crossLock_g2_ring1_5554() throws Exception {
        try { Thread.sleep(212); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_5554_6955_m0();
    }

    public void crossLock_g2_ring2_3060() throws Exception {
        synchronized (__crossLock_2402) {
            try { Thread.sleep(212); } catch (InterruptedException e) { }
            crossLock_g2_ring0_6334();
        }
    }

    public void mockMethod_0_939() throws Exception {
        mockMethod_1_7126();
    }


    public void mockMethod_1_7126() throws Exception {
        try { Thread.sleep(240); } catch (InterruptedException e) { }
        synchronized (__crossLock_2402) {
            mockMethod_2_5776();
        }
    }


    public void mockMethod_2_5776() throws Exception {
        synchronized (__crossLock_2402) {
            size();
        }
    }


    private final Object __crossLock_6780 = new Object();
    private final Object __crossLock_4941 = new Object();
    private final Object __crossLock_2402 = new Object();
    private final Object __crossLock_3868 = new Object();
    private final Object __crossLock_8612 = new Object();
    private final Object __crossLock_8840 = new Object();
}
