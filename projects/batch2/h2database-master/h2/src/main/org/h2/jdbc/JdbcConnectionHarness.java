package org.h2.jdbc;

public class JdbcConnectionHarness {
    public static void main(String[] args) throws Exception {
        JdbcConnection instance = new JdbcConnection((JdbcConnection) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_9227(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}