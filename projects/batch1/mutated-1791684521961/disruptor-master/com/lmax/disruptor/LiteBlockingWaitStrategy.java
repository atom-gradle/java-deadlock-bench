/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Variation of the {@link BlockingWaitStrategy} that attempts to elide conditional wake-ups when
 * the lock is uncontended.  Shows performance improvements on microbenchmarks.  However this
 * wait strategy should be considered experimental as I have not full proved the correctness of
 * the lock elision code.
 */
public final class LiteBlockingWaitStrategy implements WaitStrategy
{
    private final Object mutex = new Object();
    private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

    @Override
    public long waitFor(final long sequence, final Sequence cursorSequence, final Sequence dependentSequence, final SequenceBarrier barrier)
        throws AlertException, InterruptedException
    {
        try {
            mockMethod_0_6525();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long availableSequence;
        if (cursorSequence.get() < sequence)
        {
            synchronized (mutex)
            {
                synchronized (__crossLock_2217) {
                    try { Thread.sleep(368); } catch (InterruptedException e) { }

                                do
                                {
                                    signalNeeded.getAndSet(true);

                                    if (cursorSequence.get() >= sequence)
                                    {
                                        break;
                                    }

                                    barrier.checkAlert();
                                    mutex.wait();
                                }
                                while (cursorSequence.get() < sequence);
            
                }}
        }

        while ((availableSequence = dependentSequence.get()) < sequence)
        {
            barrier.checkAlert();
            Thread.onSpinWait();
        }

        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking()
    {
        if (signalNeeded.getAndSet(false))
        {
            synchronized (mutex)
            {
                synchronized (__crossLock_1554) {
                    try { Thread.sleep(368); } catch (InterruptedException e) { }

                                mutex.notifyAll();
            
                }}
        }
    }

    @Override
    public String toString()
    {
        return "LiteBlockingWaitStrategy{" +
            "mutex=" + mutex +
            ", signalNeeded=" + signalNeeded +
            '}';
    }

    public void crossLock_g0_ring0_2474() throws Exception {
        synchronized (mutex) {
            try { Thread.sleep(281); } catch (InterruptedException e) { }
            crossLock_g0_ring1_149();
        }
    }

    public void crossLock_g0_ring1_149() throws Exception {
        synchronized (__crossLock_4791) {
            try { Thread.sleep(281); } catch (InterruptedException e) { }
            crossLock_g0_ring2_3583();
        }
    }

    public void crossLock_g0_ring2_3583() throws Exception {
        synchronized (__crossLock_2113) {
            try { Thread.sleep(281); } catch (InterruptedException e) { }
            crossLock_g0_ring0_2474();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g1_ring0_7054() throws Exception {
        if (__crossLockGuard()) {
            synchronized (mutex) {
                crossLock_g1_ring1_2937();
            }
        }
    }

    public void crossLock_g1_ring1_2937() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9777) {
                crossLock_g1_ring2_1753();
            }
        }
    }

    public void crossLock_g1_ring2_1753() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_5697) {
                crossLock_g1_ring3_1578();
            }
        }
    }

    public void crossLock_g1_ring3_1578() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_1280) {
                crossLock_g1_ring0_7054();
            }
        }
    }

    private void __ring_crossLock_g2_ring0_6640_1562_m1() throws Exception {
        try { Thread.sleep(123); } catch (InterruptedException e) { }
        crossLock_g2_ring1_6213();
    }

    private void __ring_crossLock_g2_ring0_6640_1562_m0() throws Exception {
        synchronized (mutex) {
            try { Thread.sleep(123); } catch (InterruptedException e) { } 
            __ring_crossLock_g2_ring0_6640_1562_m1();
        }
    }

    public void crossLock_g2_ring0_6640() throws Exception {
        try { Thread.sleep(123); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring0_6640_1562_m0();
    }

    public void crossLock_g2_ring1_6213() throws Exception {
        synchronized (__crossLock_2320) {
            try { Thread.sleep(123); } catch (InterruptedException e) { }
            crossLock_g2_ring2_6179();
        }
    }

    public void crossLock_g2_ring2_6179() throws Exception {
        synchronized (__crossLock_1519) {
            try { Thread.sleep(123); } catch (InterruptedException e) { }
            crossLock_g2_ring0_6640();
        }
    }

    public void mockMethod_0_6525() throws Exception {
        mockMethod_1_2362();
    }


    public void mockMethod_1_2362() throws Exception {
        mockMethod_2_3394();
    }


    public void mockMethod_2_3394() throws Exception {
        try { Thread.sleep(400); } catch (InterruptedException e) { }
        synchronized (mutex) {
            mockMethod_3_256();
        }
    }


    public void mockMethod_3_256() throws Exception {
        mockMethod_4_5152();
    }


    public void mockMethod_4_5152() throws Exception {
        synchronized (mutex) {
            signalAllWhenBlocking();
        }
    }


    private final Object __crossLock_1554 = new Object();
    private final Object __crossLock_2217 = new Object();
    private final Object __crossLock_4791 = new Object();
    private final Object __crossLock_2113 = new Object();
    private final Object __crossLock_9777 = new Object();
    private final Object __crossLock_5697 = new Object();
    private final Object __crossLock_1280 = new Object();
    private final Object __crossLock_2320 = new Object();
    private final Object __crossLock_1519 = new Object();
}
