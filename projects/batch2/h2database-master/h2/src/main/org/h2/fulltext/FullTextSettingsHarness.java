package org.h2.fulltext;

import java.lang.reflect.Constructor;

public class FullTextSettingsHarness {
    public static void main(String[] args) throws Exception {
        Constructor<FullTextSettings> __ctor = FullTextSettings.class.getDeclaredConstructor();
        __ctor.setAccessible(true);
        FullTextSettings instance = __ctor.newInstance();
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_919(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}