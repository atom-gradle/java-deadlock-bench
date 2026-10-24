package io.vertx.core.eventbus.impl.clustered;

public class OutboundConnectionHarness {
    public static void main(String[] args) throws Exception {
        OutboundConnection instance = new OutboundConnection((ClusteredEventBus) null, "");
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_3115(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}