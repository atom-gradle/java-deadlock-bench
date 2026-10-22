package org.h2.mvstore.tx;

public class TransactionHarness {
    public static void main(String[] args) throws Exception {
        Transaction instance = new Transaction((TransactionStore) null, 0, 0L, 0, "", 0L, 0, 0, null, (TransactionStore.RollbackListener) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_4888(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}