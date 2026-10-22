package org.h2.engine;

import java.lang.reflect.Constructor;

public class EngineHarness {
    public static void main(String[] args) throws Exception {
        Constructor<Engine> __ctor = Engine.class.getDeclaredConstructor();
        __ctor.setAccessible(true);
        Engine instance = __ctor.newInstance();
        Thread t1 = new Thread(() -> {
            try { instance.mockMethod_0_7871(); } catch (Throwable t) { }
        });
        t1.start();
        t1.join(5000);
    }
}