package org.apache.commons.collections4.properties;

public class OrderedPropertiesHarness {
    public static void main(String[] args) throws Exception {
        OrderedProperties instance = new OrderedProperties();
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_4564(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_9406(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g0_ring2_3705(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g0_ring3_7030(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_7242(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_6785(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.crossLock_g2_ring2_4475(); } catch (Throwable t) { }
        });
        Thread t8 = new Thread(() -> {
            try { instance.crossLock_g2_ring3_3531(); } catch (Throwable t) { }
        });
        Thread t9 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_8691(); } catch (Throwable t) { }
        });
        Thread t10 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_3524(); } catch (Throwable t) { }
        });
        Thread t11 = new Thread(() -> {
            try { instance.crossLock_g1_ring2_6163(); } catch (Throwable t) { }
        });
        Thread t12 = new Thread(() -> {
            try { instance.crossLock_g1_ring3_6878(); } catch (Throwable t) { }
        });
        Thread t13 = new Thread(() -> {
            try { instance.mockMethod_0_8725(); } catch (Throwable t) { }
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