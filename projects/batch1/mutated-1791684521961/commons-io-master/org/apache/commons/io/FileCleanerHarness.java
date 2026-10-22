package org.apache.commons.io;

public class FileCleanerHarness {
    public static void main(String[] args) throws Exception {
        FileCleaner instance = new FileCleaner();
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_6347(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_6697(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g0_ring2_5543(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g0_ring3_9795(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_9157(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_6171(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g1_ring2_5381(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.crossLock_g1_ring3_2093(); } catch (Throwable t) { }
        });
        Thread t9 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_8843(); } catch (Throwable t) { }
        });
        Thread t10 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_8626(); } catch (Throwable t) { }
        });
        Thread t11 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_618(); } catch (Throwable t) { }
        });
        Thread t12 = new Thread(() -> {
            try { instance.mockMethod_0_7218(); } catch (Throwable t) { }
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