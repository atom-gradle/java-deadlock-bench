package org.h2.mvstore;

public class AppendOnlyMultiFileStoreHarness {
    public static void main(String[] args) throws Exception {
        AppendOnlyMultiFileStore instance = new AppendOnlyMultiFileStore((Map) null, (Object) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_8450(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}