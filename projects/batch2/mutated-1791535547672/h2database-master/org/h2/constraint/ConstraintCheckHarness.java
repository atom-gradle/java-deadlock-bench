package org.h2.constraint;

public class ConstraintCheckHarness {
    public static void main(String[] args) throws Exception {
        ConstraintCheck instance = new ConstraintCheck((Schema) null, 0, "", (Table) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_530(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}