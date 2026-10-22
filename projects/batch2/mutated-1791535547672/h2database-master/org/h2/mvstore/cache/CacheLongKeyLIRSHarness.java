package org.h2.mvstore.cache;

public class CacheLongKeyLIRSHarness {
    public static void main(String[] args) throws Exception {
        CacheLongKeyLIRS instance = new CacheLongKeyLIRS((Config) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_6581(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}