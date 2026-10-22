package org.h2.mvstore.db;

public class MVTableHarness {
    public static void main(String[] args) throws Exception {
        MVTable instance = new MVTable(null, (Store) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_138(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}