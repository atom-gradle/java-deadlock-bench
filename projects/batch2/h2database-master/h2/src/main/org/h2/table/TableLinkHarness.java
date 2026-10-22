package org.h2.table;

public class TableLinkHarness {
    public static void main(String[] args) throws Exception {
        TableLink instance = new TableLink(null, 0, "", "", "", "", "", "", "", false, false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_7992(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}