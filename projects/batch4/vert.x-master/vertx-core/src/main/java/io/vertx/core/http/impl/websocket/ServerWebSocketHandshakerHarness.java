package io.vertx.core.http.impl.websocket;

public class ServerWebSocketHandshakerHarness {
    public static void main(String[] args) throws Exception {
        ServerWebSocketHandshaker instance = new ServerWebSocketHandshaker(null, null, null, null, false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_1237(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}
