package io.vertx.core.http.impl.http2.codec;

public class Http2ClientConnectionImplHarness {
    public static void main(String[] args) throws Exception {
        Http2ClientConnectionImpl instance = new Http2ClientConnectionImpl((ContextInternal) null, (HostAndPort) null, (VertxHttp2ConnectionHandler) null, (TransportMetrics) null, (ClientMetrics) null, null, null, (Duration) null, 0L, (TracingPolicy) null, false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_6375(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}