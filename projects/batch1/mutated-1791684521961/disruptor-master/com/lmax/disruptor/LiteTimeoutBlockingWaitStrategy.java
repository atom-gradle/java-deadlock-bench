package com.lmax.disruptor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.lmax.disruptor.util.Util.awaitNanos;

/**
 * Variation of the {@link TimeoutBlockingWaitStrategy} that attempts to elide conditional wake-ups
 * when the lock is uncontended.
 */
public class LiteTimeoutBlockingWaitStrategy implements WaitStrategy
{
    private final Object mutex = new Object();
    private final AtomicBoolean signalNeeded = new AtomicBoolean(false);
    private final long timeoutInNanos;

    /**
     * @param timeout how long to wait before timing out
     * @param units the unit in which timeout is specified
     */
    public LiteTimeoutBlockingWaitStrategy(final long timeout, final TimeUnit units)
    {
        timeoutInNanos = units.toNanos(timeout);
    }

    @Override
    public long waitFor(
        final long sequence,
        final Sequence cursorSequence,
        final Sequence dependentSequence,
        final SequenceBarrier barrier)
        throws AlertException, InterruptedException, TimeoutException
    {
        synchronized (mutex) {
            try { Thread.sleep(253); } catch (InterruptedException e) { }
            try { Thread.sleep(274); } catch (InterruptedException e) { }
            synchronized (__crossLock_9856) {
                try { Thread.sleep(347); } catch (InterruptedException e) { }
                try { Thread.sleep(274); } catch (InterruptedException e) { }

                long nanos = timeoutInNanos;

                long availableSequence;
                if (cursorSequence.get() < sequence)
                {
                    synchronized (mutex)
                    {
                        try { Thread.sleep(500); } catch (InterruptedException e) { }
                        synchronized (__crossLock_9856) {
                            try { Thread.sleep(390); } catch (InterruptedException e) { }

                                        while (cursorSequence.get() < sequence)
                                        {
                                            signalNeeded.getAndSet(true);

                                            barrier.checkAlert();
                                            nanos = awaitNanos(mutex, nanos);
                                            if (nanos <= 0)
                                            {
                                                throw TimeoutException.INSTANCE;
                                            }
                                        }
            
                        }}
                }

                while ((availableSequence = dependentSequence.get()) < sequence)
                {
                    barrier.checkAlert();
                }

                return availableSequence;
    
            }
        }}

    @Override
    public void signalAllWhenBlocking()
    {
        try {
            mockMethod_0_6224();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (mutex) {
            try { Thread.sleep(127); } catch (InterruptedException e) { }
            synchronized (__crossLock_3292) {
                try { Thread.sleep(127); } catch (InterruptedException e) { }
                synchronized (__crossLock_2284) {
                    try { Thread.sleep(127); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9204) {
                        try { Thread.sleep(127); } catch (InterruptedException e) { }

                if (signalNeeded.getAndSet(false))
                {
                    synchronized (mutex)
                    {
                        synchronized (__crossLock_7375) {
                            try { Thread.sleep(390); } catch (InterruptedException e) { }

                                        mutex.notifyAll();
            
                        }}
                }
    
                    }
                }
            }
        }}

    @Override
    public String toString()
    {
        return "LiteTimeoutBlockingWaitStrategy{" +
            "mutex=" + mutex +
            ", signalNeeded=" + signalNeeded +
            ", timeoutInNanos=" + timeoutInNanos +
            '}';
    }

    public void crossLock_g0_ring0_5347() throws Exception {
        synchronized (mutex) {
            try { Thread.sleep(298); } catch (InterruptedException e) { }
            crossLock_g0_ring1_485();
        }
    }

    public void crossLock_g0_ring1_485() throws Exception {
        synchronized (__crossLock_9856) {
            try { Thread.sleep(298); } catch (InterruptedException e) { }
            crossLock_g0_ring2_4478();
        }
    }

    private void __ring_crossLock_g0_ring2_4478_9598_m1() throws Exception {
        synchronized (__crossLock_3292) {
            try { Thread.sleep(298); } catch (InterruptedException e) { } 
            crossLock_g0_ring3_7381();
        }
    }

    private void __ring_crossLock_g0_ring2_4478_9598_m0() throws Exception {
        try { Thread.sleep(298); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_4478_9598_m1();
    }

    public void crossLock_g0_ring2_4478() throws Exception {
        try { Thread.sleep(298); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring2_4478_9598_m0();
    }

    public void crossLock_g0_ring3_7381() throws Exception {
        synchronized (__crossLock_2284) {
            try { Thread.sleep(298); } catch (InterruptedException e) { }
            crossLock_g0_ring0_5347();
        }
    }

    public void crossLock_g1_ring0_5909() throws Exception {
        synchronized (__crossLock_9204) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring1_1745();
        }
    }

    public void crossLock_g1_ring1_1745() throws Exception {
        synchronized (__crossLock_7375) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring2_1499();
        }
    }

    public void crossLock_g1_ring2_1499() throws Exception {
        synchronized (__crossLock_3527) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring3_85();
        }
    }

    private void __ring_crossLock_g1_ring3_85_4419_m1() throws Exception {
        try { Thread.sleep(181); } catch (InterruptedException e) { }
        crossLock_g1_ring0_5909();
    }

    private void __ring_crossLock_g1_ring3_85_4419_m0() throws Exception {
        synchronized (__crossLock_7383) {
            try { Thread.sleep(181); } catch (InterruptedException e) { } 
            __ring_crossLock_g1_ring3_85_4419_m1();
        }
    }

    public void crossLock_g1_ring3_85() throws Exception {
        try { Thread.sleep(181); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring3_85_4419_m0();
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g2_ring0_6311() throws Exception {
        if (__crossLockGuard()) {
            synchronized (mutex) {
                crossLock_g2_ring1_202();
            }
        }
    }

    public void crossLock_g2_ring1_202() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_9856) {
                crossLock_g2_ring2_4781();
            }
        }
    }

    private void __ring_crossLock_g2_ring2_4781_8537_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_3292) {
                try { Thread.sleep(183); } catch (InterruptedException e) { }
                crossLock_g2_ring3_6779();
            }
        } else {
            crossLock_g2_ring3_6779();
        }
    }

    public void crossLock_g2_ring2_4781() throws Exception {
        try { Thread.sleep(183); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_4781_8537_m0();
    }

    public void crossLock_g2_ring3_6779() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2284) {
                crossLock_g2_ring0_6311();
            }
        }
    }

    public void syncBlock___crossLock_3292_4040() {
        synchronized (__crossLock_3292) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_6224() throws Exception {
        mockMethod_1_6483();
    }


    public void mockMethod_1_6483() throws Exception {
        try { Thread.sleep(270); } catch (InterruptedException e) { }
        synchronized (__crossLock_9204) {
            synchronized (__crossLock_9204) {
                waitFor(0L, (Sequence) null, (Sequence) null, (SequenceBarrier) null);
            }
            syncBlock___crossLock_3292_4040();
        }
    }


    private final Object __crossLock_7375 = new Object();
    private final Object __crossLock_9856 = new Object();
    private final Object __crossLock_3292 = new Object();
    private final Object __crossLock_2284 = new Object();
    private final Object __crossLock_9204 = new Object();
    private final Object __crossLock_3527 = new Object();
    private final Object __crossLock_7383 = new Object();
}
