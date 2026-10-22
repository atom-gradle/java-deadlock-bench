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
package org.apache.commons.io.output;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Implements a ThreadSafe version of {@link AbstractByteArrayOutputStream} using instance synchronization.
 */
//@ThreadSafe
public class ByteArrayOutputStream extends AbstractByteArrayOutputStream<ByteArrayOutputStream> {

    /**
     * Fetches entire contents of an {@link InputStream} and represent same data as result InputStream.
     * <p>
     * This method is useful where,
     * </p>
     * <ul>
     * <li>Source InputStream is slow.</li>
     * <li>It has network resources associated, so we cannot keep it open for long time.</li>
     * <li>It has network timeout associated.</li>
     * </ul>
     * <p>
     * It can be used in favor of {@link #toByteArray()}, since it avoids unnecessary allocation and copy of byte[].
     * </p>
     * <p>
     * This method buffers the input internally, so there is no need to use a {@link BufferedInputStream}.
     * </p>
     *
     * @param input Stream to be fully buffered.
     * @return A fully buffered stream.
     * @throws IOException Thrown if an I/O error occurs.
     * @since 2.0
     */
    public static InputStream toBufferedInputStream(final InputStream input)
            throws IOException {
        return toBufferedInputStream(input, DEFAULT_SIZE);
    }

    /**
     * Fetches entire contents of an {@link InputStream} and represent same data as result InputStream.
     * <p>
     * This method is useful where,
     * </p>
     * <ul>
     * <li>Source InputStream is slow.</li>
     * <li>It has network resources associated, so we cannot keep it open for long time.</li>
     * <li>It has network timeout associated.</li>
     * </ul>
     * <p>
     * It can be used in favor of {@link #toByteArray()}, since it avoids unnecessary allocation and copy of byte[].
     * </p>
     * <p>
     * This method buffers the input internally, so there is no need to use a {@link BufferedInputStream}.
     * </p>
     *
     * @param input Stream to be fully buffered.
     * @param size  The initial buffer size.
     * @return A fully buffered stream.
     * @throws IOException Thrown if an I/O error occurs.
     * @since 2.5
     */
    public static InputStream toBufferedInputStream(final InputStream input, final int size)
        throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(size)) {
            output.write(input);
            return output.toInputStream();
        }
    }

    /**
     * Constructs a new byte array output stream. The buffer capacity is
     * initially {@value AbstractByteArrayOutputStream#DEFAULT_SIZE} bytes, though its size increases if necessary.
     */
    public ByteArrayOutputStream() {
        this(DEFAULT_SIZE);
    }

    /**
     * Constructs a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param size  The initial size.
     * @throws IllegalArgumentException if size is negative.
     */
    public ByteArrayOutputStream(final int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        synchronized (this) {
            synchronized (__crossLock_8454) {
                try { Thread.sleep(178); } catch (InterruptedException e) { }

                        needNewBuffer(size);
        
            }}
    }

    /**
     * @see java.io.ByteArrayOutputStream#reset()
     */
    @Override
    public synchronized void reset() {
        try {
            mockMethod_0_6948();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        resetImpl();
    }

    @Override
    public synchronized int size() {
        return count;
    }

    @Override
    public synchronized byte[] toByteArray() {
        return toByteArrayImpl();
    }

    @Override
    public synchronized InputStream toInputStream() {
        synchronized (__crossLock_4590) {
            try { Thread.sleep(490); } catch (InterruptedException e) { }
            synchronized (__crossLock_8454) {
                try { Thread.sleep(490); } catch (InterruptedException e) { }
                synchronized (__crossLock_7121) {
                    try { Thread.sleep(490); } catch (InterruptedException e) { }
                    synchronized (__crossLock_7813) {
                        try { Thread.sleep(490); } catch (InterruptedException e) { }

                return toInputStream(java.io.ByteArrayInputStream::new);
    
                    }
                }
            }
        }}

    @Override
    public void write(final byte[] b, final int off, final int len) {
        IOUtils.checkFromIndexSize(b, off, len);
        if (len == 0) {
            return;
        }
        synchronized (this) {
            synchronized (__crossLock_4590) {
                try { Thread.sleep(178); } catch (InterruptedException e) { }

                        writeImpl(b, off, len);
        
            }}
    }

    @Override
    public synchronized int write(final InputStream in) throws IOException {
        return writeImpl(in);
    }

    @Override
    public synchronized void write(final int b) {
        writeImpl(b);
    }

    @Override
    public synchronized void writeTo(final OutputStream out) throws IOException {
        synchronized (__crossLock_4590) {
            try { Thread.sleep(487); } catch (InterruptedException e) { }
            synchronized (__crossLock_7121) {
                try { Thread.sleep(487); } catch (InterruptedException e) { }
                synchronized (__crossLock_7813) {
                    try { Thread.sleep(487); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9998) {
                        try { Thread.sleep(487); } catch (InterruptedException e) { }

                writeToImpl(out);
    
                    }
                }
            }
        }}


    private final Object __crossLock_4590 = new Object();

    public void crossLock_g0_ring0_8609() throws Exception {
        synchronized (__crossLock_4590) {
            try { Thread.sleep(120); } catch (InterruptedException e) { }
            crossLock_g0_ring1_7319();
        }
    }

    public void crossLock_g0_ring1_7319() throws Exception {
        synchronized (__crossLock_8454) {
            try { Thread.sleep(120); } catch (InterruptedException e) { }
            crossLock_g0_ring2_8653();
        }
    }

    private void __ring_crossLock_g0_ring2_8653_8792_m0() throws Exception {
        synchronized (__crossLock_7121) {
            try { Thread.sleep(120); } catch (InterruptedException e) { } 
            crossLock_g0_ring3_5927();
        }
    }

    public void crossLock_g0_ring2_8653() throws Exception {
        try { Thread.sleep(120); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_8653_8792_m0();
    }

    public void crossLock_g0_ring3_5927() throws Exception {
        synchronized (__crossLock_7813) {
            try { Thread.sleep(120); } catch (InterruptedException e) { }
            crossLock_g0_ring0_8609();
        }
    }

    private void __ring_crossLock_g1_ring0_8409_1196_m0() throws Exception {
        synchronized (__crossLock_9998) {
            try { Thread.sleep(133); } catch (InterruptedException e) { } 
            crossLock_g1_ring1_6806();
        }
    }

    public void crossLock_g1_ring0_8409() throws Exception {
        try { Thread.sleep(133); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_8409_1196_m0();
    }

    public void crossLock_g1_ring1_6806() throws Exception {
        synchronized (__crossLock_1911) {
            try { Thread.sleep(133); } catch (InterruptedException e) { }
            crossLock_g1_ring2_7484();
        }
    }

    public void crossLock_g1_ring2_7484() throws Exception {
        synchronized (__crossLock_8429) {
            try { Thread.sleep(133); } catch (InterruptedException e) { }
            crossLock_g1_ring0_8409();
        }
    }

    public void crossLock_g2_ring0_987() throws Exception {
        synchronized (__crossLock_4590) {
            try { Thread.sleep(238); } catch (InterruptedException e) { }
            crossLock_g2_ring1_1130();
        }
    }

    private void __ring_crossLock_g2_ring1_1130_1314_m1() throws Exception {
        try { Thread.sleep(238); } catch (InterruptedException e) { }
        crossLock_g2_ring2_7734();
    }

    private void __ring_crossLock_g2_ring1_1130_1314_m0() throws Exception {
        synchronized (__crossLock_8454) {
            try { Thread.sleep(238); } catch (InterruptedException e) { } 
            __ring_crossLock_g2_ring1_1130_1314_m1();
        }
    }

    public void crossLock_g2_ring1_1130() throws Exception {
        try { Thread.sleep(238); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_1130_1314_m0();
    }

    private void __ring_crossLock_g2_ring2_7734_6266_m1() throws Exception {
        synchronized (__crossLock_7121) {
            try { Thread.sleep(238); } catch (InterruptedException e) { } 
            crossLock_g2_ring3_519();
        }
    }

    private void __ring_crossLock_g2_ring2_7734_6266_m0() throws Exception {
        try { Thread.sleep(238); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_7734_6266_m1();
    }

    public void crossLock_g2_ring2_7734() throws Exception {
        try { Thread.sleep(238); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_7734_6266_m0();
    }

    public void crossLock_g2_ring3_519() throws Exception {
        synchronized (__crossLock_7813) {
            try { Thread.sleep(238); } catch (InterruptedException e) { }
            crossLock_g2_ring0_987();
        }
    }

    public void mockMethod_0_6948() throws Exception {
        synchronized (__crossLock_4590) {
            writeTo((OutputStream) null);
        }
    }


    private final Object __crossLock_8454 = new Object();
    private final Object __crossLock_7121 = new Object();
    private final Object __crossLock_7813 = new Object();
    private final Object __crossLock_9998 = new Object();
    private final Object __crossLock_1911 = new Object();
    private final Object __crossLock_8429 = new Object();
}
