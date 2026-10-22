package org.h2.engine;

public class SessionLocalHarness {
    public static void main(String[] args) throws Exception {
        SessionLocal instance = new SessionLocal((Database) null, (User) null, 0);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_5927(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}