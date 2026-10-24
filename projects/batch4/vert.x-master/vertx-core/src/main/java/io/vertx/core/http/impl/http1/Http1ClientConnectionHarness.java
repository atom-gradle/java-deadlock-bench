package io.vertx.core.http.impl.http1;

public class Http1ClientConnectionHarness {
    public static void main(String[] args) throws Exception {
        Http1ClientConnection instance = new Http1ClientConnection(null, null, null, null, null, false, null, false, null, null, null, null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_8775(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}
