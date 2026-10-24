package io.vertx.core.impl;

public class ResourceHookHarness {
    public static void main(String[] args) throws Exception {
        ResourceHook instance = new ResourceHook((CloseFuture) null, (CloseableResource) null);
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_157(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_5488(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_4584(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_1524(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_2187(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_6461(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.mockMethod_0_4455(); } catch (Throwable t) { }
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();
        t1.join(5000);
        t2.join(5000);
        t3.join(5000);
        t4.join(5000);
        t5.join(5000);
        t6.join(5000);
        t7.join(5000);
    }
}