package io.vertx.core.impl;

public class VertxImplHarness {
    public static void main(String[] args) throws Exception {
        VertxImpl instance = new VertxImpl((VertxOptions) null, (ClusterManager) null, (NodeSelector) null, (VertxMetrics) null, (VertxTracer) null, null, (Transport) null, (Throwable) null, (FileResolver) null, (VertxThreadFactory) null, (ExecutorServiceFactory) null, (EventExecutorProvider) null, false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_4944(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}