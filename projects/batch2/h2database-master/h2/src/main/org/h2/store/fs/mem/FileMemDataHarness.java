package org.h2.store.fs.mem;

public class FileMemDataHarness {
    public static void main(String[] args) throws Exception {
        FileMemData instance = new FileMemData("", false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_3365(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}