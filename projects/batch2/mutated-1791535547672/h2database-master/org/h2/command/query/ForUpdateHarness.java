package org.h2.command.query;

public class ForUpdateHarness {
    public static void main(String[] args) throws Exception {
        ForUpdate instance = new ForUpdate((Type) null, 0);
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_7740(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_2273(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_4720(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_3893(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g1_ring2_4934(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g1_ring3_1996(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_1402(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_8576(); } catch (Throwable t) { }
        });
        Thread t9 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_5908(); } catch (Throwable t) { }
        });
        Thread t10 = new Thread(() -> {
            try { instance.mockMethod_0_7612(); } catch (Throwable t) { }
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