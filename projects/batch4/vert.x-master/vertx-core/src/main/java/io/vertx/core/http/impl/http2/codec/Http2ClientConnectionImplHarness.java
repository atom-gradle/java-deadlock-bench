package io.vertx.core.http.impl.http2.codec;

public class Http2ClientConnectionImplHarness {
    public static void main(String[] args) throws Exception {
        Http2ClientConnectionImpl instance = new Http2ClientConnectionImpl(null, null, null, null, null, null, 1L, null, false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_6375(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}
