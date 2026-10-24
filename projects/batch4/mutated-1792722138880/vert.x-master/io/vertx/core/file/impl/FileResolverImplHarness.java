package io.vertx.core.file.impl;

public class FileResolverImplHarness {
    public static void main(String[] args) throws Exception {
        FileResolverImpl instance = new FileResolverImpl();
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_6947(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}