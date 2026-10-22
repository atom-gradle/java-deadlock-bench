package org.apache.commons.collections4;

import java.lang.reflect.Constructor;

public class SetUtilsHarness {
    public static void main(String[] args) throws Exception {
        Constructor<SetUtils> __ctor = SetUtils.class.getDeclaredConstructor();
        __ctor.setAccessible(true);
        SetUtils instance = __ctor.newInstance();
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_5733(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_3673(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_6043(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_2749(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g1_ring2_9926(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g1_ring3_9949(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_8728(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_7018(); } catch (Throwable t) { }
        });
        Thread t9 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_6071(); } catch (Throwable t) { }
        });
        Thread t10 = new Thread(() -> {
            try { instance.mockMethod_0_3995(); } catch (Throwable t) { }
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
    }
}