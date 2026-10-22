package com.google.common.util.concurrent;

public class RateLimiterHarness {
    public static void main(String[] args) throws Exception {
        RateLimiter instance = new RateLimiter((SleepingStopwatch) null);
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_6065(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_9160(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g0_ring2_9091(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g0_ring3_7925(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_852(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_2633(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g1_ring2_6375(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.crossLock_g1_ring3_86(); } catch (Throwable t) { }
        });
        Thread t9 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_8099(); } catch (Throwable t) { }
        });
        Thread t10 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_7192(); } catch (Throwable t) { }
        });
        Thread t11 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_175(); } catch (Throwable t) { }
        });
        Thread t12 = new Thread(() -> {
            try { instance.mockMethod_0_9160(); } catch (Throwable t) { }
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();
        t8.start();
        t9.start();
        t10.start();
        t11.start();
        t12.start();
        t1.join(5000);
        t2.join(5000);
        t3.join(5000);
        t4.join(5000);
        t5.join(5000);
        t6.join(5000);
        t7.join(5000);
        t8.join(5000);
        t9.join(5000);
        t10.join(5000);
        t11.join(5000);
        t12.join(5000);
    }
}