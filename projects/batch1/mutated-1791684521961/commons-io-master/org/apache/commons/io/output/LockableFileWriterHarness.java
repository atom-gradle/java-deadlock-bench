package org.apache.commons.io.output;

public class LockableFileWriterHarness {
    public static void main(String[] args) throws Exception {
        LockableFileWriter instance = new LockableFileWriter((Builder) null);
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_550(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_7314(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_924(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_2295(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_8540(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_3621(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_6775(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.mockMethod_0_6761(); } catch (Throwable t) { }
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