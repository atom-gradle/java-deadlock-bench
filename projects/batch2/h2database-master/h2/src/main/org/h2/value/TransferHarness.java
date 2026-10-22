package org.h2.value;

public class TransferHarness {
    public static void main(String[] args) throws Exception {
        Transfer instance = new Transfer(null, null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_4903(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}