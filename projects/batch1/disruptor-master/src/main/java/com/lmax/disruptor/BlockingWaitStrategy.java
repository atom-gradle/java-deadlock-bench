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

/**
 * Blocking strategy that uses a lock and condition variable for {@link EventProcessor}s waiting on a barrier.

 *
 * <p>This strategy can be used when throughput and low-latency are not as important as CPU resource.
 */
public final class BlockingWaitStrategy implements WaitStrategy
{
    private final Object mutex = new Object();

    @Override
    public long waitFor(final long sequence, final Sequence cursorSequence, final Sequence dependentSequence, final SequenceBarrier barrier)
        throws AlertException, InterruptedException
    {
        synchronized (mutex) {
            try { Thread.sleep(225); } catch (InterruptedException e) { }
            try { Thread.sleep(237); } catch (InterruptedException e) { }
            synchronized (__crossLock_2295) {
                try { Thread.sleep(355); } catch (InterruptedException e) { }
                try { Thread.sleep(237); } catch (InterruptedException e) { }

                synchronized (mutex) {
                    try { Thread.sleep(381); } catch (InterruptedException e) { }
                    try { Thread.sleep(374); } catch (InterruptedException e) { }
                    synchronized (__crossLock_2295) {
                        try { Thread.sleep(374); } catch (InterruptedException e) { }
                        synchronized (__crossLock_8369) {
                            try { Thread.sleep(374); } catch (InterruptedException e) { }
                            synchronized (__crossLock_7079) {
                                try { Thread.sleep(374); } catch (InterruptedException e) { }

                        long availableSequence;
                        if (cursorSequence.get() < sequence)
                        {
                            synchronized (mutex)
                            {
                                synchronized (__crossLock_5177) {
                                    try { Thread.sleep(298); } catch (InterruptedException e) { }

                                                while (cursorSequence.get() < sequence)
                                                {
                                                    barrier.checkAlert();
                                                    mutex.wait();
                                                }
            
                                }}
                        }

                        while ((availableSequence = dependentSequence.get()) < sequence)
                        {
                            barrier.checkAlert();
                            Thread.onSpinWait();
                        }

                        return availableSequence;
    
                            }
                        }
                    }
                }
            }
        }}

    @Override
    public void signalAllWhenBlocking()
    {
        try {
            mockMethod_0_8954();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (mutex)
        {
            synchronized (__crossLock_4978) {
                try { Thread.sleep(298); } catch (InterruptedException e) { }

                        mutex.notifyAll();
        
            }}
    }

    @Override
    public String toString()
    {
        return "BlockingWaitStrategy{" +
            "mutex=" + mutex +
            '}';
    }

    public void crossLock_g0_ring0_3082() throws Exception {
        synchronized (mutex) {
            try { Thread.sleep(118); } catch (InterruptedException e) { }
            crossLock_g0_ring1_9626();
        }
    }

    public void crossLock_g0_ring1_9626() throws Exception {
        synchronized (__crossLock_2295) {
            try { Thread.sleep(118); } catch (InterruptedException e) { }
            crossLock_g0_ring0_3082();
        }
    }

    private void __ring_crossLock_g1_ring0_1922_8743_m1() throws Exception {
        synchronized (__crossLock_8369) {
            try { Thread.sleep(148); } catch (InterruptedException e) { } 
            crossLock_g1_ring1_7846();
        }
    }

    private void __ring_crossLock_g1_ring0_1922_8743_m0() throws Exception {
        try { Thread.sleep(148); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_1922_8743_m1();
    }

    public void crossLock_g1_ring0_1922() throws Exception {
        try { Thread.sleep(148); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_1922_8743_m0();
    }

    private void __ring_crossLock_g1_ring1_7846_3921_m0() throws Exception {
        synchronized (__crossLock_7079) {
            try { Thread.sleep(148); } catch (InterruptedException e) { } 
            crossLock_g1_ring0_1922();
        }
    }

    public void crossLock_g1_ring1_7846() throws Exception {
        try { Thread.sleep(148); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_7846_3921_m0();
    }

    public void crossLock_g2_ring0_1615() throws Exception {
        synchronized (__crossLock_5177) {
            try { Thread.sleep(219); } catch (InterruptedException e) { }
            crossLock_g2_ring1_7692();
        }
    }

    public void crossLock_g2_ring1_7692() throws Exception {
        synchronized (__crossLock_4978) {
            try { Thread.sleep(219); } catch (InterruptedException e) { }
            crossLock_g2_ring2_5246();
        }
    }

    private void __ring_crossLock_g2_ring2_5246_6781_m1() throws Exception {
        try { Thread.sleep(219); } catch (InterruptedException e) { }
        crossLock_g2_ring0_1615();
    }

    private void __ring_crossLock_g2_ring2_5246_6781_m0() throws Exception {
        synchronized (__crossLock_9147) {
            try { Thread.sleep(219); } catch (InterruptedException e) { } 
            __ring_crossLock_g2_ring2_5246_6781_m1();
        }
    }

    public void crossLock_g2_ring2_5246() throws Exception {
        try { Thread.sleep(219); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_5246_6781_m0();
    }

    public void mockMethod_0_8954() throws Exception {
        mockMethod_1_2293();
    }


    public void mockMethod_1_2293() throws Exception {
        mockMethod_2_2773();
    }


    public void mockMethod_2_2773() throws Exception {
        mockMethod_3_5770();
    }


    public void mockMethod_3_5770() throws Exception {
        mockMethod_4_2756();
    }


    public void mockMethod_4_2756() throws Exception {
        try { Thread.sleep(475); } catch (InterruptedException e) { }
        synchronized (__crossLock_7079) {
            synchronized (__crossLock_7079) {
                waitFor(0L, (Sequence) null, (Sequence) null, (SequenceBarrier) null);
            }
        }
    }


    private final Object __crossLock_4978 = new Object();
    private final Object __crossLock_5177 = new Object();
    private final Object __crossLock_2295 = new Object();
    private final Object __crossLock_8369 = new Object();
    private final Object __crossLock_7079 = new Object();
    private final Object __crossLock_9147 = new Object();
}
