package io.vertx.core.file.impl;

public class FileCacheHarness {
    public static void main(String[] args) throws Exception {
        FileCache instance = new FileCache((File) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_4831(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}