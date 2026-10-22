package org.h2.schema;

public class SequenceHarness {
    public static void main(String[] args) throws Exception {
        Sequence instance = new Sequence((SessionLocal) null, (Schema) null, 0, "", (SequenceOptions) null, false);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_6349(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}