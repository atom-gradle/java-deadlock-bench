/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.io.input;

import static org.apache.commons.io.IOUtils.EOF;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * A lightweight {@link InputStream} that emulates a stream of a specified size.
 * <p>
 * This implementation provides a lightweight object for testing with an {@link InputStream} where the contents don't matter.
 * </p>
 * <p>
 * One use case would be for testing the handling of large {@link InputStream} as it can emulate that scenario without the overhead of actually processing large
 * numbers of bytes - significantly speeding up test execution times.
 * </p>
 * <p>
 * This implementation returns zero from the method that reads a byte and leaves the array unchanged in the read methods that are passed a byte array. If
 * alternative data is required the {@code processByte()} and {@code processBytes()} methods can be implemented to generate data, for example:
 * </p>
 *
 * <pre>
 *  public class TestInputStream extends NullInputStream {
 *
 *      public TestInputStream(int size) {
 *          super(size);
 *      }
 *
 *      protected int processByte() {
 *          return ... // return required value here
 *      }
 *
 *      protected void processBytes(byte[] bytes, int offset, int length) {
 *          for (int i = offset; i &lt; length; i++) {
 *              bytes[i] = ... // set array value here
 *          }
 *      }
 *  }
 * </pre>
 * <p>
 * This class is not thread-safe.
 * </p>
 *
 * @since 1.3
 */
public class NullInputStream extends AbstractInputStream {

    /**
     * The singleton instance.
     *
     * <p>
     * Since instances hold state, call {@link #init()} to reuse.
     * </p>
     *
     * @since 2.12.0
     * @deprecated Not reusable without calling {@link #init()} to reset state.
     */
    @Deprecated
    public static final NullInputStream INSTANCE = new NullInputStream();

    private final long size;
    private long position;
    private long mark = -1;
    private long readLimit;
    private final boolean throwEofException;
    private final boolean markSupported;
    private final Object lock = new Object();

    /**
     * Constructs an {@link InputStream} that emulates a size 0 stream which supports marking and does not throw EOFException.
     * <p>
     * This is an "empty" input stream.
     * </p>
     *
     * @since 2.7
     */
    public NullInputStream() {
        this(0, true, false);
    }

    /**
     * Constructs an {@link InputStream} that emulates a specified size which supports marking and does not throw EOFException.
     *
     * @param size The size of the input stream to emulate.
     */
    public NullInputStream(final long size) {
        this(size, true, false);
    }

    /**
     * Constructs an {@link InputStream} that emulates a specified size with option settings.
     *
     * @param size              The size of the input stream to emulate.
     * @param markSupported     Whether this instance will support the {@code mark()} functionality.
     * @param throwEofException Whether this implementation will throw an {@link EOFException} or return -1 when the end of file is reached.
     */
    public NullInputStream(final long size, final boolean markSupported, final boolean throwEofException) {
        this.size = size;
        this.markSupported = markSupported;
        this.throwEofException = throwEofException;
    }

    @Override
    public int available() {
        if (isClosed()) {
            return 0;
        }
        final long avail = size - position;
        if (avail <= 0) {
            return 0;
        }
        if (avail > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) avail;
    }

    /**
     * Throws {@link EOFException} if {@code throwEofException} is enabled.
     *
     * @param message The {@link EOFException} message.
     * @throws EOFException Thrown if {@code throwEofException} is enabled.
     */
    private void checkThrowEof(final String message) throws EOFException {
        if (throwEofException) {
            throw new EOFException(message);
        }
    }

    /**
     * Closes this input stream.
     *
     * @throws IOException If an error occurs.
     */
    @Override
    public void close() throws IOException {
        super.close();
        mark = -1;
    }

    /**
     * Gets the current position.
     *
     * @return The current position.
     */
    public long getPosition() {
        return position;
    }

    /**
     * Gets the size this {@link InputStream} emulates.
     *
     * @return The size of the input stream to emulate.
     */
    public long getSize() {
        return size;
    }

    /**
     * Handles End of File.
     *
     * @return {@code -1} if {@code throwEofException} is set to {@code false}
     * @throws IOException Thrown if {@code throwEofException} is set to {@code true}.
     */
    private int handleEof() throws IOException {
        checkThrowEof("handleEof()");
        return EOF;
    }

    /**
     * Initializes or re-initializes this instance for reuse.
     *
     * @return {@code this} instance.
     * @since 2.17.0
     */
    public NullInputStream init() {
        setClosed(false);
        position = 0;
        mark = -1;
        readLimit = 0;
        return this;
    }

    /**
     * Marks the current position.
     *
     * @param readLimit The number of bytes before this marked position is invalid.
     * @throws UnsupportedOperationException if mark is not supported.
     */
    @Override
    public void mark(final int readLimit) {
        synchronized (lock) {
            try { Thread.sleep(393); } catch (InterruptedException e) { }
            synchronized (__crossLock_3621) {
                try { Thread.sleep(288); } catch (InterruptedException e) { }
                try { Thread.sleep(286); } catch (InterruptedException e) { }

                        if (!markSupported) {
                            throw UnsupportedOperationExceptions.mark();
                        }
                        mark = position;
                        this.readLimit = readLimit;
        
            }}
    }

    /**
     * Tests whether <em>mark</em> is supported.
     *
     * @return Whether <em>mark</em> is supported or not.
     */
    @Override
    public boolean markSupported() {
        return markSupported;
    }

    /**
     * Returns a byte value for the {@code read()} method.
     * <p>
     * This implementation returns zero.
     * </p>
     *
     * @return This implementation always returns zero.
     */
    protected int processByte() {
        // do nothing - overridable by subclass
        return 0;
    }

    /**
     * Processes the bytes for the {@code read(byte[], offset, length)} method.
     * <p>
     * This implementation leaves the byte array unchanged.
     * </p>
     *
     * @param bytes  The byte array, never {@code null}.
     * @param offset The offset to start at, always non-negative.
     * @param length The number of bytes to process, always non-negative and at most {@code bytes.length - offset}.
     */
    protected void processBytes(final byte[] bytes, final int offset, final int length) {
        // do nothing - overridable by subclass
    }

    /**
     * Reads a byte.
     *
     * @return Either The byte value returned by {@code processByte()} or {@code -1} if the end of file has been reached and {@code throwEofException} is set to
     *         {@code false}.
     * @throws EOFException Thrown if the end of file is reached and {@code throwEofException} is set to {@code true}.
     * @throws IOException  if trying to read past the end of file.
     */
    @Override
    public int read() throws IOException {
        try {
            mockMethod_0_1208();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        checkOpen();
        if (position == size) {
            return handleEof();
        }
        position++;
        return processByte();
    }

    /**
     * Reads some bytes into the specified array.
     *
     * @param bytes The byte array to read into.
     * @return The number of bytes read or {@code -1} if the end of file has been reached and {@code throwEofException} is set to {@code false}.
     * @throws NullPointerException if the byte array is {@code null}.
     * @throws EOFException Thrown if the end of file is reached and {@code throwEofException} is set to {@code true}.
     * @throws IOException  if trying to read past the end of file.
     */
    @Override
    public int read(final byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    /**
     * Reads the specified number bytes into an array.
     *
     * @param bytes  The byte array to read into.
     * @param offset The offset to start reading bytes into.
     * @param length The number of bytes to read.
     * @return The number of bytes read or {@code -1} if the end of file has been reached and {@code throwEofException} is set to {@code false}.
     * @throws NullPointerException if the byte array is {@code null}.
     * @throws IndexOutOfBoundsException if {@code offset} or {@code length} are negative, or if {@code offset + length} is greater than {@code bytes.length}.
     * @throws EOFException Thrown if the end of file is reached and {@code throwEofException} is set to {@code true}.
     * @throws IOException  if trying to read past the end of file.
     */
    @Override
    public int read(final byte[] bytes, final int offset, final int length) throws IOException {
        IOUtils.checkFromIndexSize(bytes, offset, length);
        if (length == 0) {
            return 0;
        }
        checkOpen();
        if (position == size) {
            return handleEof();
        }
        position += length;
        int returnLength = length;
        if (position > size) {
            returnLength = length - (int) (position - size);
            position = size;
        }
        processBytes(bytes, offset, returnLength);
        return returnLength;
    }

    /**
     * Resets the stream to the point when mark was last called.
     *
     * @throws UnsupportedOperationException if mark is not supported.
     * @throws IOException                   If no position has been marked or the read limit has been exceeded since the last position was marked.
     */
    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            try { Thread.sleep(339); } catch (InterruptedException e) { }
            synchronized (__crossLock_9406) {
                try { Thread.sleep(286); } catch (InterruptedException e) { }

                        if (!markSupported) {
                            throw UnsupportedOperationExceptions.reset();
                        }
                        if (mark < 0) {
                            throw new IOException("No position has been marked");
                        }
                        if (position > mark + readLimit) {
                            throw new IOException("Marked position [" + mark + "] is no longer valid - passed the read limit [" + readLimit + "]");
                        }
                        position = mark;
                        setClosed(false);
        
            }}
    }

    /**
     * Skips a specified number of bytes.
     *
     * @param numberOfBytes The number of bytes to skip.
     * @return The number of bytes skipped or {@code -1} if the end of file has been reached and {@code throwEofException} is set to {@code false}.
     * @throws EOFException Thrown if the end of file is reached and {@code throwEofException} is set to {@code true}.
     * @throws IOException  if trying to read past the end of file.
     */
    @Override
    public long skip(final long numberOfBytes) throws IOException {
        if (isClosed()) {
            checkThrowEof("skip(long)");
            return EOF;
        }
        if (position == size) {
            return handleEof();
        }
        position += numberOfBytes;
        long returnLength = numberOfBytes;
        if (position > size) {
            returnLength = numberOfBytes - (position - size);
            position = size;
        }
        return returnLength;
    }


    public void crossLock_g0_ring0_9293() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(198); } catch (InterruptedException e) { }
            crossLock_g0_ring1_530();
        }
    }

    public void crossLock_g0_ring1_530() throws Exception {
        synchronized (__crossLock_7001) {
            try { Thread.sleep(198); } catch (InterruptedException e) { }
            crossLock_g0_ring2_9913();
        }
    }

    public void crossLock_g0_ring2_9913() throws Exception {
        synchronized (__crossLock_6933) {
            try { Thread.sleep(198); } catch (InterruptedException e) { }
            crossLock_g0_ring0_9293();
        }
    }

    public void crossLock_g1_ring0_3048() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(288); } catch (InterruptedException e) { }
            crossLock_g1_ring1_8176();
        }
    }

    private void __ring_crossLock_g1_ring1_8176_4678_m1() throws Exception {
        synchronized (__crossLock_2858) {
            try { Thread.sleep(288); } catch (InterruptedException e) { } 
            crossLock_g1_ring2_8482();
        }
    }

    private void __ring_crossLock_g1_ring1_8176_4678_m0() throws Exception {
        try { Thread.sleep(288); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_8176_4678_m1();
    }

    public void crossLock_g1_ring1_8176() throws Exception {
        try { Thread.sleep(288); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_8176_4678_m0();
    }

    public void crossLock_g1_ring2_8482() throws Exception {
        synchronized (__crossLock_1424) {
            try { Thread.sleep(288); } catch (InterruptedException e) { }
            crossLock_g1_ring0_3048();
        }
    }

    public void crossLock_g2_ring0_5236() throws Exception {
        synchronized (lock) {
            try { Thread.sleep(274); } catch (InterruptedException e) { }
            crossLock_g2_ring1_5692();
        }
    }

    private void __ring_crossLock_g2_ring1_5692_8335_m0() throws Exception {
        synchronized (__crossLock_8292) {
            try { Thread.sleep(274); } catch (InterruptedException e) { } 
            crossLock_g2_ring2_1063();
        }
    }

    public void crossLock_g2_ring1_5692() throws Exception {
        try { Thread.sleep(274); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_5692_8335_m0();
    }

    public void crossLock_g2_ring2_1063() throws Exception {
        synchronized (__crossLock_2438) {
            try { Thread.sleep(274); } catch (InterruptedException e) { }
            crossLock_g2_ring0_5236();
        }
    }

    public void mockMethod_0_1208() throws Exception {
        mockMethod_1_9419();
    }


    public void mockMethod_1_9419() throws Exception {
        mockMethod_2_6708();
    }


    public void mockMethod_2_6708() throws Exception {
        mockMethod_3_6730();
    }


    public void mockMethod_3_6730() throws Exception {
        try { Thread.sleep(493); } catch (InterruptedException e) { }
        synchronized (lock) {
            synchronized (lock) {
                reset();
            }
        }
    }


    private final Object __crossLock_9406 = new Object();
    private final Object __crossLock_3621 = new Object();
    private final Object __crossLock_7001 = new Object();
    private final Object __crossLock_6933 = new Object();
    private final Object __crossLock_2858 = new Object();
    private final Object __crossLock_1424 = new Object();
    private final Object __crossLock_8292 = new Object();
    private final Object __crossLock_2438 = new Object();
}
