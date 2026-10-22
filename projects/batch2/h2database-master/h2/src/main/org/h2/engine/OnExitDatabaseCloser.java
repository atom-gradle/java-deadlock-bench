/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.engine;

import java.util.WeakHashMap;

import org.h2.message.Trace;

/**
 * This class is responsible to close a database on JVM shutdown.
 */
class OnExitDatabaseCloser extends Thread {

    private static final WeakHashMap<Database, Void> DATABASES = new WeakHashMap<>();

    private static final Thread INSTANCE = new OnExitDatabaseCloser();

    private static boolean registered;

    private static boolean terminated;

    /**
     * Register database instance to close one on the JVM process shutdown.
     *
     * @param db Database instance.
     */
    static synchronized void register(Database db) {
        if (terminated) {
            // Shutdown in progress
            return;
        }
        DATABASES.put(db, null);
        if (!registered) {
            // Mark as registered unconditionally to avoid further attempts to register a
            // shutdown hook in case of exception.
            registered = true;
            try {
                Runtime.getRuntime().addShutdownHook(INSTANCE);
            } catch (IllegalStateException e) {
                // shutdown in progress - just don't register the handler
                // (maybe an application wants to write something into a
                // database at shutdown time)
            } catch (SecurityException e) {
                // applets may not do that - ignore
                // Google App Engine doesn't allow
                // to instantiate classes that extend Thread
            }
        }
    }

    /**
     * Unregister database instance.
     *
     * @param db Database instance.
     */
    static synchronized void unregister(Database db) {
        if (terminated) {
            // Shutdown in progress, do nothing
            // This method can be called from the onShutdown()
            return;
        }
        DATABASES.remove(db);
        if (DATABASES.isEmpty() && registered) {
            try {
                Runtime.getRuntime().removeShutdownHook(INSTANCE);
            } catch (IllegalStateException e) {
                // ignore
            } catch (SecurityException e) {
                // applets may not do that - ignore
            }
            registered = false;
        }
    }

    private static void onShutdown() {
        synchronized(OnExitDatabaseCloser.class) {
            synchronized (__crossLock_1310) {
                try { Thread.sleep(197); } catch (InterruptedException e) { }

                        synchronized (__crossLock_2800) {
                            try { Thread.sleep(393); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_3421) {
                                        try { Thread.sleep(116); } catch (InterruptedException e) { }

                                                terminated = true;
        
                                    }
                        }
            }}
        RuntimeException root = null;
        for (Database database : DATABASES.keySet()) {
            try {
                database.onShutdown();
            } catch (RuntimeException e) {
                // this can happen when stopping a web application,
                // if loading classes is no longer allowed
                // it would throw an IllegalStateException
                try {
                    database.getTrace(Trace.DATABASE).error(e, "could not close the database");
                    // if this was successful, we ignore the exception
                    // otherwise not
                } catch (Throwable e2) {
                    e.addSuppressed(e2);
                    if (root == null) {
                        root = e;
                    } else {
                        root.addSuppressed(e);
                    }
                }
            }
        }
        if (root != null) {
            throw root;
        }
    }

    private OnExitDatabaseCloser() {
    }

    @Override
    public void run() {
        try {
            mockMethod_0_9406();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (__crossLock_3421) {
            try { Thread.sleep(476); } catch (InterruptedException e) { }
            synchronized (__crossLock_1310) {
                try { Thread.sleep(476); } catch (InterruptedException e) { }
                synchronized (__crossLock_2800) {
                    try { Thread.sleep(476); } catch (InterruptedException e) { }

                synchronized (__crossLock_3421) {
                    try { Thread.sleep(311); } catch (InterruptedException e) { }
                    synchronized (__crossLock_1310) {
                        try { Thread.sleep(311); } catch (InterruptedException e) { }
                        synchronized (__crossLock_2800) {
                            try { Thread.sleep(311); } catch (InterruptedException e) { }

                        onShutdown();
    
                        }
                    }
                }
                }
            }
        }}



    private static final Object __crossLock_3421 = new Object();

    public void crossLock_g0_ring0_7837() throws Exception {
        synchronized (__crossLock_3421) {
            try { Thread.sleep(269); } catch (InterruptedException e) { }
            crossLock_g0_ring1_8609();
        }
    }

    public void crossLock_g0_ring1_8609() throws Exception {
        synchronized (__crossLock_1310) {
            try { Thread.sleep(269); } catch (InterruptedException e) { }
            crossLock_g0_ring2_6173();
        }
    }

    public void crossLock_g0_ring2_6173() throws Exception {
        synchronized (__crossLock_2800) {
            try { Thread.sleep(269); } catch (InterruptedException e) { }
            crossLock_g0_ring3_5140();
        }
    }

    private void __ring_crossLock_g0_ring3_5140_5608_m1() throws Exception {
        synchronized (__crossLock_6325) {
            try { Thread.sleep(269); } catch (InterruptedException e) { }
            crossLock_g0_ring0_7837();
        }
    }

    private void __ring_crossLock_g0_ring3_5140_5608_m0() throws Exception {
        try { Thread.sleep(269); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring3_5140_5608_m1();
    }

    public void crossLock_g0_ring3_5140() throws Exception {
        try { Thread.sleep(269); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring3_5140_5608_m0();
    }

    public void crossLock_g1_ring0_7861() throws Exception {
        synchronized (__crossLock_3421) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring1_5714();
        }
    }

    private void __ring_crossLock_g1_ring1_5714_7247_m0() throws Exception {
        synchronized (__crossLock_1310) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring2_9183();
        }
    }

    public void crossLock_g1_ring1_5714() throws Exception {
        try { Thread.sleep(181); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_5714_7247_m0();
    }

    private void __ring_crossLock_g1_ring2_9183_689_m0() throws Exception {
        synchronized (__crossLock_2800) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring3_4480();
        }
    }

    public void crossLock_g1_ring2_9183() throws Exception {
        try { Thread.sleep(181); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring2_9183_689_m0();
    }

    public void crossLock_g1_ring3_4480() throws Exception {
        synchronized (__crossLock_9011) {
            try { Thread.sleep(181); } catch (InterruptedException e) { }
            crossLock_g1_ring0_7861();
        }
    }

    public void crossLock_g2_ring0_4450() throws Exception {
        synchronized (__crossLock_3421) {
            try { Thread.sleep(243); } catch (InterruptedException e) { }
            crossLock_g2_ring1_2249();
        }
    }

    public void crossLock_g2_ring1_2249() throws Exception {
        synchronized (__crossLock_1310) {
            try { Thread.sleep(243); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1789();
        }
    }

    public void crossLock_g2_ring2_1789() throws Exception {
        synchronized (__crossLock_2800) {
            try { Thread.sleep(243); } catch (InterruptedException e) { }
            crossLock_g2_ring0_4450();
        }
    }

    public void syncBlock___crossLock_2800_2587() {
        synchronized (__crossLock_2800) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_9406() throws Exception {
        mockMethod_1_1465();
    }


    public void mockMethod_1_1465() throws Exception {
        mockMethod_2_969();
    }


    public void mockMethod_2_969() throws Exception {
        mockMethod_3_4676();
    }


    public void mockMethod_3_4676() throws Exception {
        try { Thread.sleep(430); } catch (InterruptedException e) { }
        synchronized (__crossLock_1310) {
            mockMethod_4_5262();
            syncBlock___crossLock_2800_2587();
        }
    }


    public void mockMethod_4_5262() throws Exception {
        synchronized (__crossLock_1310) {
            run();
        }
    }


    private static final Object __crossLock_2800 = new Object();
    private static final Object __crossLock_1310 = new Object();
    private final Object __crossLock_6325 = new Object();
    private final Object __crossLock_9011 = new Object();
}
