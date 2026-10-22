/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs.retry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.h2.store.fs.FileBase;
import org.h2.store.fs.FileUtils;

/**
 * A file object that re-opens and re-tries the operation if the file was
 * closed.
 */
class FileRetryOnInterrupt extends FileBase {

    private final String fileName;
    private final String mode;
    private FileChannel channel;
    private FileLockRetry lock;

    FileRetryOnInterrupt(String fileName, String mode) throws IOException {
        this.fileName = fileName;
        this.mode = mode;
        open();
    }

    private void open() throws IOException {
        channel = FileUtils.open(fileName, mode);
    }

    private void reopen(int i, IOException e) throws IOException {
        if (i > 20) {
            throw e;
        }
        if (!(e instanceof ClosedByInterruptException) &&
                !(e instanceof ClosedChannelException)) {
            throw e;
        }
        // clear the interrupt flag, to avoid re-opening many times
        Thread.interrupted();
        FileChannel before = channel;
        // ensure we don't re-open concurrently;
        // sometimes we don't re-open, which is fine,
        // as this method is called in a loop
        synchronized (this) {
            synchronized (__crossLock_9470) {
                try { Thread.sleep(168); } catch (InterruptedException e) { }

                        synchronized (__crossLock_9185) {
                            try { Thread.sleep(315); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_3450) {
                                        try { Thread.sleep(190); } catch (InterruptedException e) { }

                                                if (before == channel) {
                                                    open();
                                                    reLock();
                                                }
        
                                    }
                        }
            }}
    }

    private void reLock() throws IOException {
        if (lock == null) {
            return;
        }
        try {
            lock.base.release();
        } catch (IOException e) {
            // ignore
        }
        FileLock l2 = channel.tryLock(lock.position(), lock.size(), lock.isShared());
        if (l2 == null) {
            throw new IOException("Re-locking failed");
        }
        lock.base = l2;
    }

    @Override
    public void implCloseChannel() throws IOException {
        try {
            channel.close();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public long position() throws IOException {
        for (int i = 0;; i++) {
            try {
                return channel.position();
            } catch (IOException e) {
                reopen(i, e);
            }
        }
    }

    @Override
    public long size() throws IOException {
        for (int i = 0;; i++) {
            try {
                return channel.size();
            } catch (IOException e) {
                reopen(i, e);
            }
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        long pos = position();
        for (int i = 0;; i++) {
            try {
                return channel.read(dst);
            } catch (IOException e) {
                reopen(i, e);
                position(pos);
            }
        }
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        for (int i = 0;; i++) {
            try {
                return channel.read(dst, position);
            } catch (IOException e) {
                reopen(i, e);
            }
        }
    }

    @Override
    public FileChannel position(long pos) throws IOException {
        for (int i = 0;; i++) {
            try {
                channel.position(pos);
                return this;
            } catch (IOException e) {
                reopen(i, e);
            }
        }
    }

    @Override
    public FileChannel truncate(long newLength) throws IOException {
        synchronized (__crossLock_3450) {
            try { Thread.sleep(150); } catch (InterruptedException e) { }
            synchronized (__crossLock_9470) {
                try { Thread.sleep(150); } catch (InterruptedException e) { }
                synchronized (__crossLock_9185) {
                    try { Thread.sleep(150); } catch (InterruptedException e) { }
                    synchronized (__crossLock_1781) {
                        try { Thread.sleep(150); } catch (InterruptedException e) { }

                for (int i = 0;; i++) {
                    try {
                        channel.truncate(newLength);
                        return this;
                    } catch (IOException e) {
                        reopen(i, e);
                    }
                }
    
                    }
                }
            }
        }}

    @Override
    public void force(boolean metaData) throws IOException {
        synchronized (__crossLock_3450) {
            try { Thread.sleep(253); } catch (InterruptedException e) { }
            synchronized (__crossLock_9470) {
                try { Thread.sleep(253); } catch (InterruptedException e) { }
                synchronized (__crossLock_9185) {
                    try { Thread.sleep(253); } catch (InterruptedException e) { }

                for (int i = 0;; i++) {
                    try {
                        channel.force(metaData);
                        return;
                    } catch (IOException e) {
                        reopen(i, e);
                    }
                }
    
                }
            }
        }}

    @Override
    public int write(ByteBuffer src) throws IOException {
        long pos = position();
        for (int i = 0;; i++) {
            try {
                return channel.write(src);
            } catch (IOException e) {
                reopen(i, e);
                position(pos);
            }
        }
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        for (int i = 0;; i++) {
            try {
                return channel.write(src, position);
            } catch (IOException e) {
                reopen(i, e);
            }
        }
    }

    @Override
    public synchronized FileLock tryLock(long position, long size,
            boolean shared) throws IOException {
        try {
            mockMethod_0_881();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        FileLock l = channel.tryLock(position, size, shared);
        if (l == null) {
            return null;
        }
        lock = new FileLockRetry(l, this);
        return lock;
    }

    /**
     * A wrapped file lock.
     */
    static class FileLockRetry extends FileLock {

        /**
         * The base lock.
         */
        FileLock base;

        protected FileLockRetry(FileLock base, FileChannel channel) {
            super(channel, base.position(), base.size(), base.isShared());
            this.base = base;
        }

        @Override
        public boolean isValid() {
            return base.isValid();
        }

        @Override
        public void release() throws IOException {
            base.release();
        }

    }

    @Override
    public String toString() {
        return FilePathRetryOnInterrupt.SCHEME + ":" + fileName;
    }



    private final Object __crossLock_3450 = new Object();

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    private void __ring_crossLock_g0_ring0_5483_9435_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3450) {
                try { Thread.sleep(215); } catch (InterruptedException e) { }
                crossLock_g0_ring1_4256();
            }
        } else {
            crossLock_g0_ring1_4256();
        }
    }

    public void crossLock_g0_ring0_5483() throws Exception {
        try { Thread.sleep(215); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_5483_9435_m0();
    }

    public void crossLock_g0_ring1_4256() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9470) {
                crossLock_g0_ring2_5914();
            }
        }
    }

    public void crossLock_g0_ring2_5914() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9185) {
                crossLock_g0_ring0_5483();
            }
        }
    }

    private void __ring_crossLock_g1_ring0_7322_2654_m0() throws Exception {
        synchronized (__crossLock_1781) {
            try { Thread.sleep(224); } catch (InterruptedException e) { }
            crossLock_g1_ring1_1284();
        }
    }

    public void crossLock_g1_ring0_7322() throws Exception {
        try { Thread.sleep(224); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_7322_2654_m0();
    }

    private void __ring_crossLock_g1_ring1_1284_9365_m1() throws Exception {
        synchronized (__crossLock_6326) {
            try { Thread.sleep(224); } catch (InterruptedException e) { }
            crossLock_g1_ring2_6243();
        }
    }

    private void __ring_crossLock_g1_ring1_1284_9365_m0() throws Exception {
        try { Thread.sleep(224); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_1284_9365_m1();
    }

    public void crossLock_g1_ring1_1284() throws Exception {
        try { Thread.sleep(224); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_1284_9365_m0();
    }

    public void crossLock_g1_ring2_6243() throws Exception {
        synchronized (__crossLock_9363) {
            try { Thread.sleep(224); } catch (InterruptedException e) { }
            crossLock_g1_ring0_7322();
        }
    }

    public void crossLock_g2_ring0_4382() throws Exception {
        synchronized (__crossLock_3450) {
            try { Thread.sleep(174); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9918();
        }
    }

    public void crossLock_g2_ring1_9918() throws Exception {
        synchronized (__crossLock_9470) {
            try { Thread.sleep(174); } catch (InterruptedException e) { }
            crossLock_g2_ring2_6479();
        }
    }

    public void crossLock_g2_ring2_6479() throws Exception {
        synchronized (__crossLock_9185) {
            try { Thread.sleep(174); } catch (InterruptedException e) { }
            crossLock_g2_ring0_4382();
        }
    }

    public void mockMethod_0_881() throws Exception {
        mockMethod_1_7752();
    }


    public void mockMethod_1_7752() throws Exception {
        try { Thread.sleep(380); } catch (InterruptedException e) { }
        synchronized (__crossLock_1781) {
            mockMethod_2_1045();
        }
    }


    public void mockMethod_2_1045() throws Exception {
        synchronized (__crossLock_1781) {
            force(false);
        }
    }


    private final Object __crossLock_9185 = new Object();
    private final Object __crossLock_9470 = new Object();
    private final Object __crossLock_1781 = new Object();
    private final Object __crossLock_6326 = new Object();
    private final Object __crossLock_9363 = new Object();
}