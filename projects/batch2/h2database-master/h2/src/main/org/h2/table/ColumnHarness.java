package org.h2.table;

public class ColumnHarness {
    public static void main(String[] args) throws Exception {
        Column instance = new Column("", null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_4622(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}