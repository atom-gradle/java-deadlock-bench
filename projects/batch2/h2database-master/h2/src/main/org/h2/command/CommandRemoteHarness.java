package org.h2.command;

public class CommandRemoteHarness {
    public static void main(String[] args) throws Exception {
        CommandRemote instance = new CommandRemote( null, null, "");
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_1234(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}