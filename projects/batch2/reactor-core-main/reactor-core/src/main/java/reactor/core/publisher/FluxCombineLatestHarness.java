package reactor.core.publisher;

import org.reactivestreams.Publisher;

public class FluxCombineLatestHarness {
    public static void main(String[] args) throws Exception {
        FluxCombineLatest instance = new FluxCombineLatest(new Publisher[0], null, null, 0);
        Thread t1 = new Thread(() -> {
            try { instance.crossLock_g0_ring0_918(); } catch (Throwable t) { }
        });
        Thread t2 = new Thread(() -> {
            try { instance.crossLock_g0_ring1_5533(); } catch (Throwable t) { }
        });
        Thread t3 = new Thread(() -> {
            try { instance.crossLock_g1_ring0_8010(); } catch (Throwable t) { }
        });
        Thread t4 = new Thread(() -> {
            try { instance.crossLock_g1_ring1_312(); } catch (Throwable t) { }
        });
        Thread t5 = new Thread(() -> {
            try { instance.crossLock_g2_ring0_1594(); } catch (Throwable t) { }
        });
        Thread t6 = new Thread(() -> {
            try { instance.crossLock_g2_ring1_7474(); } catch (Throwable t) { }
        });
        Thread t7 = new Thread(() -> {
            try { instance.mockMethod_0_5496(); } catch (Throwable t) { }
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