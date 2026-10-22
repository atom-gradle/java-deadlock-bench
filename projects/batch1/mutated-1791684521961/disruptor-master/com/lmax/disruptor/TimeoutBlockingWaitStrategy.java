package com.lmax.disruptor;

import java.util.concurrent.TimeUnit;

import static com.lmax.disruptor.util.Util.awaitNanos;

/**
 * Blocking strategy that uses a lock and condition variable for {@link EventProcessor}s waiting on a barrier.
 * However it will periodically wake up if it has been idle for specified period by throwing a
 * {@link TimeoutException}.  To make use of this, the event handler class should override
 * {@link EventHandler#onTimeout(long)}, which the {@link BatchEventProcessor} will call if the timeout occurs.
 *
 * <p>This strategy can be used when throughput and low-latency are not as important as CPU resource.
 */
public class TimeoutBlockingWaitStrategy implements WaitStrategy
{
    private final Object mutex = new Object();
    private final long timeoutInNanos;

    /**
     * @param timeout how long to wait before waking up
     * @param units the unit in which timeout is specified
     */
    public TimeoutBlockingWaitStrategy(final long timeout, final TimeUnit units)
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
        try {
            mockMethod_0_5134();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (mutex) {
            try { Thread.sleep(276); } catch (InterruptedException e) { }
            synchronized (__crossLock_9366) {
                try { Thread.sleep(276); } catch (InterruptedException e) { }
                synchronized (__crossLock_1172) {
                    try { Thread.sleep(276); } catch (InterruptedException e) { }

                synchronized (mutex) {
                    try { Thread.sleep(328); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9366) {
                        try { Thread.sleep(328); } catch (InterruptedException e) { }

                        long timeoutNanos = timeoutInNanos;

                        long availableSequence;
                        if (cursorSequence.get() < sequence)
                        {
                            synchronized (mutex)
                            {
                                synchronized (__crossLock_1172) {
                                    try { Thread.sleep(320); } catch (InterruptedException e) { }

                                                while (cursorSequence.get() < sequence)
                                                {
                                                    barrier.checkAlert();
                                                    timeoutNanos = awaitNanos(mutex, timeoutNanos);
                                                    if (timeoutNanos <= 0)
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
                }
                }
            }
        }}

    @Override
    public void signalAllWhenBlocking()
    {
        synchronized (mutex)
        {
            synchronized (__crossLock_2735) {
                try { Thread.sleep(320); } catch (InterruptedException e) { }

                        mutex.notifyAll();
        
            }}
    }

    @Override
    public String toString()
    {
        return "TimeoutBlockingWaitStrategy{" +
            "mutex=" + mutex +
            ", timeoutInNanos=" + timeoutInNanos +
            '}';
    }

    public void crossLock_g0_ring0_1901() throws Exception {
        synchronized (mutex) {
            try { Thread.sleep(121); } catch (InterruptedException e) { }
            crossLock_g0_ring1_2321();
        }
    }

    public void crossLock_g0_ring1_2321() throws Exception {
        synchronized (__crossLock_9366) {
            try { Thread.sleep(121); } catch (InterruptedException e) { }
            crossLock_g0_ring2_5707();
        }
    }

    public void crossLock_g0_ring2_5707() throws Exception {
        synchronized (__crossLock_1172) {
            try { Thread.sleep(121); } catch (InterruptedException e) { }
            crossLock_g0_ring0_1901();
        }
    }

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g1_ring0_5620() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2735) {
                crossLock_g1_ring1_1791();
            }
        }
    }

    public void crossLock_g1_ring1_1791() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2885) {
                crossLock_g1_ring0_5620();
            }
        }
    }

    public void crossLock_g2_ring0_6636() throws Exception {
        synchronized (mutex) {
            try { Thread.sleep(159); } catch (InterruptedException e) { }
            crossLock_g2_ring1_3022();
        }
    }

    public void crossLock_g2_ring1_3022() throws Exception {
        synchronized (__crossLock_9366) {
            try { Thread.sleep(159); } catch (InterruptedException e) { }
            crossLock_g2_ring2_3053();
        }
    }

    public void crossLock_g2_ring2_3053() throws Exception {
        synchronized (__crossLock_1172) {
            try { Thread.sleep(159); } catch (InterruptedException e) { }
            crossLock_g2_ring0_6636();
        }
    }

    public void mockMethod_0_5134() throws Exception {
        mockMethod_1_8019();
    }


    public void mockMethod_1_8019() throws Exception {
        try { Thread.sleep(370); } catch (InterruptedException e) { }
        synchronized (__crossLock_9366) {
            synchronized (__crossLock_9366) {
                waitFor(0L, (Sequence) null, (Sequence) null, (SequenceBarrier) null);
            }
        }
    }


    private final Object __crossLock_2735 = new Object();
    private final Object __crossLock_1172 = new Object();
    private final Object __crossLock_9366 = new Object();
    private final Object __crossLock_2885 = new Object();
}
