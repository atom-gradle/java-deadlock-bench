package org.h2.store.fs.niomem;

public class FileNioMemDataHarness {
    public static void main(String[] args) throws Exception {
        FileNioMemData instance = new FileNioMemData("", false, 0.0f);
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_7517(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_7548(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_8462(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_8776(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_159(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_5570(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_1917(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.mockMethod_0_3755(); } catch (Throwable t) { }
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