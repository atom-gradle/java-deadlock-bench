package org.apache.commons.pool3.impl;

public class GenericKeyedObjectPoolHarness {
    public static void main(String[] args) throws Exception {
        GenericKeyedObjectPool instance = new GenericKeyedObjectPool((KeyedPooledObjectFactory) null, (T) null, (E) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_1480(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}