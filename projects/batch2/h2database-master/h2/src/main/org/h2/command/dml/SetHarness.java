package org.h2.command.dml;

public class SetHarness {
    public static void main(String[] args) throws Exception {
        Set instance = new Set(null, 0);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_7167(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}