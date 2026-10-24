package io.vertx.core.http.impl.http1;

public class Http1ServerRequestHarness {
    public static void main(String[] args) throws Exception {
        Http1ServerRequest instance = new Http1ServerRequest((Http1ServerConnection) null, (HttpRequest) null, (ContextInternal) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_3791(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}