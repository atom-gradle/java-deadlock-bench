package org.h2.jdbc.meta;

public class DatabaseMetaRemoteHarness {
    public static void main(String[] args) throws Exception {
        DatabaseMetaRemote instance = new DatabaseMetaRemote((SessionRemote) null, (ArrayList) null);
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_8655(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_197(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g1_ring2_5916(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g1_ring3_5185(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_8209(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_6806(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_6939(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_7035(); } catch (Throwable t) { }
        });
        Thread t9 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_5152(); } catch (Throwable t) { }
        });
        Thread t10 = new Thread(() -> {
            try { instance.crossLock_g0_ring2_2290(); } catch (Throwable t) { }
        });
        Thread t11 = new Thread(() -> {
            try { instance.crossLock_g0_ring3_4059(); } catch (Throwable t) { }
        });
        Thread t12 = new Thread(() -> {
            try { instance.crossLock_g0_ring4_4537(); } catch (Throwable t) { }
        });
        Thread t13 = new Thread(() -> {
            try { instance.mockMethod_0_7118(); } catch (Throwable t) { }
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
        t13.start();
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
        t13.join(5000);
    }
}