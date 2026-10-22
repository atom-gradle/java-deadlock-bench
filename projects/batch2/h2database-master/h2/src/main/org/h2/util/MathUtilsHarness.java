package org.h2.util;

import java.lang.reflect.Constructor;

public class MathUtilsHarness {
    public static void main(String[] args) throws Exception {
        Constructor<MathUtils> __ctor = MathUtils.class.getDeclaredConstructor();
        __ctor.setAccessible(true);
        MathUtils instance = __ctor.newInstance();
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_7905(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_2832(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_7115(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_6824(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g1_ring2_1974(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_6436(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_7300(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.mockMethod_0_9360(); } catch (Throwable t) { }
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();
        t8.start();
        t1.join(5000);
        t2.join(5000);
        t3.join(5000);
        t4.join(5000);
        t5.join(5000);
        t6.join(5000);
        t7.join(5000);
        t8.join(5000);
    }
}