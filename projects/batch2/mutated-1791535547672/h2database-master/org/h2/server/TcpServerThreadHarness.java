package org.h2.server;

public class TcpServerThreadHarness {
    public static void main(String[] args) throws Exception {
        TcpServerThread instance = new TcpServerThread((Socket) null, (TcpServer) null, 0);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_2193(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}