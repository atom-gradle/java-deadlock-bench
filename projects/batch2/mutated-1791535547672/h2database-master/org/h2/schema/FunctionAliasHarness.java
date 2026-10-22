package org.h2.schema;

public class FunctionAliasHarness {
    public static void main(String[] args) throws Exception {
        FunctionAlias instance = new FunctionAlias((Schema) null, 0, "");
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_472(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}