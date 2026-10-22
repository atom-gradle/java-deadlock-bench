package org.h2.constraint;

public class ConstraintDomainHarness {
    public static void main(String[] args) throws Exception {
        ConstraintDomain instance = new ConstraintDomain((Schema) null, 0, "", (Domain) null);
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_3694(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}