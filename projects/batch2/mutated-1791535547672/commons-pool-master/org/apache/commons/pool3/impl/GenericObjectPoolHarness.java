package org.apache.commons.pool3.impl;

public class GenericObjectPoolHarness {
    public static void main(String[] args) throws Exception {
        GenericObjectPool instance = new GenericObjectPool((PooledObjectFactory) null, (E) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_5772(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}