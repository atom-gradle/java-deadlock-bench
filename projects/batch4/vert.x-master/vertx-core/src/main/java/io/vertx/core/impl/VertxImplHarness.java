package io.vertx.core.impl;

public class VertxImplHarness {
    public static void main(String[] args) throws Exception {
        VertxImpl instance = new VertxImpl(null, null, null, null, null, null, null, null, null, null, null, false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_4944(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}
