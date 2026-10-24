package io.vertx.core.internal.threadchecker;

public class BlockedThreadCheckerHarness {
    public static void main(String[] args) throws Exception {
        BlockedThreadChecker instance = new BlockedThreadChecker(0L, (TimeUnit) null, 0L, (TimeUnit) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_1433(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}