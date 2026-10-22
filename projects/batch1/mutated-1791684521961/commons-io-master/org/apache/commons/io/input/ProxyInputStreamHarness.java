package org.apache.commons.io.input;

public class ProxyInputStreamHarness {
    public static void main(String[] args) throws Exception {
        ProxyInputStream instance = new ProxyInputStream((AbstractBuilder) null, null);
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_8501(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_4653(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g0_ring2_1292(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g0_ring3_4105(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_7202(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_6809(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_7933(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.crossLock_g2_ring3_7609(); } catch (Throwable t) { }
        });
        Thread t9 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_9449(); } catch (Throwable t) { }
        });
        Thread t10 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_4336(); } catch (Throwable t) { }
        });
        Thread t11 = new Thread(() -> {
            try { instance.crossLock_g1_ring2_3453(); } catch (Throwable t) { }
        });
        Thread t12 = new Thread(() -> {
            try { instance.mockMethod_0_379(); } catch (Throwable t) { }
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