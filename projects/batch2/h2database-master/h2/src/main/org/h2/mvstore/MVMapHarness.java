package org.h2.mvstore;

public class MVMapHarness {
    public static void main(String[] args) throws Exception {
        MVMap instance = new MVMap(null, null, null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_4262(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}