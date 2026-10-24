package io.vertx.core.http.impl.http1;

public class Http1ClientConnectionHarness {
    public static void main(String[] args) throws Exception {
        Http1ClientConnection instance = new Http1ClientConnection((HttpVersion) null, (WebSocketMetrics) null, (TransportMetrics) null, (Http1ClientConfig) null, (TracingPolicy) null, false, (ChannelHandlerContext) null, false, (SocketAddress) null, (HostAndPort) null, (ContextInternal) null, (ClientMetrics) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_8775(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}