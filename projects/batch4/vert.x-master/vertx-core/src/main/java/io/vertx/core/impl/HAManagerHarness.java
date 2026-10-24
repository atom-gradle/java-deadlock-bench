package io.vertx.core.impl;

public class HAManagerHarness {
    public static void main(String[] args) throws Exception {
        HAManager instance = new HAManager(null, null, null, null, null,  0, "");
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_9364(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}
