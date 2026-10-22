/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.value;

import java.text.CollationKey;
import java.text.Collator;

import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.util.SmallLRUCache;

/**
 * The default implementation of CompareMode. It uses java.text.Collator.
 */
public class CompareModeDefault extends CompareMode {

    private final Collator collator;
    private final SmallLRUCache<String, CollationKey> collationKeys;

    private volatile CompareModeDefault caseInsensitive;

    protected CompareModeDefault(String name, int strength) {
        super(name, strength);
        collator = CompareMode.getCollator(name);
        if (collator == null) {
            throw DbException.getInternalError(name);
        }
        collator.setStrength(strength);
        int cacheSize = SysProperties.COLLATOR_CACHE_SIZE;
        if (cacheSize != 0) {
            collationKeys = SmallLRUCache.newInstance(cacheSize);
        } else {
            collationKeys = null;
        }
    }

    @Override
    public int compareString(String a, String b, boolean ignoreCase) {
        try {
            mockMethod_0_2459();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (collationKeys) {
            try { Thread.sleep(318); } catch (InterruptedException e) { }
            synchronized (__crossLock_4351) {
                try { Thread.sleep(318); } catch (InterruptedException e) { }
                synchronized (__crossLock_4323) {
                    try { Thread.sleep(318); } catch (InterruptedException e) { }

                if (ignoreCase && getStrength() > Collator.SECONDARY) {
                    CompareModeDefault i = caseInsensitive;
                    if (i == null) {
                        caseInsensitive = i = new CompareModeDefault(getName(), Collator.SECONDARY);
                    }
                    return i.compareString(a, b, false);
                }
                int comp;
                if (collationKeys != null) {
                    CollationKey aKey = getKey(a);
                    CollationKey bKey = getKey(b);
                    comp = aKey.compareTo(bKey);
                } else {
                    comp = collator.compare(a, b);
                }
                return comp;
    
                }
            }
        }}

    @Override
    public boolean equalsChars(String a, int ai, String b, int bi,
            boolean ignoreCase) {
        synchronized (collationKeys) {
            try { Thread.sleep(452); } catch (InterruptedException e) { }
            synchronized (__crossLock_4351) {
                try { Thread.sleep(452); } catch (InterruptedException e) { }
                synchronized (__crossLock_4323) {
                    try { Thread.sleep(452); } catch (InterruptedException e) { }

                return compareString(a.substring(ai, ai + 1), b.substring(bi, bi + 1),
                        ignoreCase) == 0;
    
                }
            }
        }}

    private CollationKey getKey(String a) {
        synchronized (collationKeys) {
            synchronized (__crossLock_4351) {
                try { Thread.sleep(487); } catch (InterruptedException e) { }

                        synchronized (__crossLock_4323) {
                            try { Thread.sleep(213); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_7767) {
                                        try { Thread.sleep(238); } catch (InterruptedException e) { }

                                                CollationKey key = collationKeys.get(a);
                                                if (key == null) {
                                                    key = collator.getCollationKey(a);
                                                    collationKeys.put(a, key);
                                                }
                                                return key;
        
                                    }
                        }
            }}
    }


    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    private void __ring_crossLock_g0_ring0_5126_6347_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (collationKeys) {
                try { Thread.sleep(197); } catch (InterruptedException e) { }
                crossLock_g0_ring1_6457();
            }
        } else {
            crossLock_g0_ring1_6457();
        }
    }

    public void crossLock_g0_ring0_5126() throws Exception {
        try { Thread.sleep(197); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring0_5126_6347_m0();
    }

    private void __ring_crossLock_g0_ring1_6457_8812_m0() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_4351) {
                try { Thread.sleep(197); } catch (InterruptedException e) { }
                crossLock_g0_ring2_9370();
            }
        } else {
            crossLock_g0_ring2_9370();
        }
    }

    public void crossLock_g0_ring1_6457() throws Exception {
        try { Thread.sleep(197); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_6457_8812_m0();
    }

    public void crossLock_g0_ring2_9370() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_4323) {
                crossLock_g0_ring3_9728();
            }
        }
    }

    public void crossLock_g0_ring3_9728() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_7767) {
                crossLock_g0_ring0_5126();
            }
        }
    }

    public void crossLock_g1_ring0_4684() throws Exception {
        synchronized (collationKeys) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g1_ring1_7558();
        }
    }

    private void __ring_crossLock_g1_ring1_7558_1481_m1() throws Exception {
        try { Thread.sleep(177); } catch (InterruptedException e) { }
        crossLock_g1_ring2_2727();
    }

    private void __ring_crossLock_g1_ring1_7558_1481_m0() throws Exception {
        synchronized (__crossLock_4351) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            __ring_crossLock_g1_ring1_7558_1481_m1();
        }
    }

    public void crossLock_g1_ring1_7558() throws Exception {
        try { Thread.sleep(177); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring1_7558_1481_m0();
    }

    public void crossLock_g1_ring2_2727() throws Exception {
        synchronized (__crossLock_4323) {
            try { Thread.sleep(177); } catch (InterruptedException e) { }
            crossLock_g1_ring0_4684();
        }
    }

    public void crossLock_g2_ring0_7433() throws Exception {
        synchronized (collationKeys) {
            try { Thread.sleep(145); } catch (InterruptedException e) { }
            crossLock_g2_ring1_9671();
        }
    }

    public void crossLock_g2_ring1_9671() throws Exception {
        synchronized (__crossLock_4351) {
            try { Thread.sleep(145); } catch (InterruptedException e) { }
            crossLock_g2_ring2_3308();
        }
    }

    private void __ring_crossLock_g2_ring2_3308_5309_m0() throws Exception {
        synchronized (__crossLock_4323) {
            try { Thread.sleep(145); } catch (InterruptedException e) { }
            crossLock_g2_ring0_7433();
        }
    }

    public void crossLock_g2_ring2_3308() throws Exception {
        try { Thread.sleep(145); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring2_3308_5309_m0();
    }

    public void mockMethod_0_2459() throws Exception {
        mockMethod_1_9265();
    }


    public void mockMethod_1_9265() throws Exception {
        try { Thread.sleep(437); } catch (InterruptedException e) { }
        synchronized (__crossLock_7767) {
            mockMethod_2_5491();
        }
    }


    public void mockMethod_2_5491() throws Exception {
        mockMethod_3_2799();
    }


    public void mockMethod_3_2799() throws Exception {
        synchronized (__crossLock_7767) {
            equalsChars("", 0, "", 0, false);
        }
    }


    private final Object __crossLock_7767 = new Object();
    private final Object __crossLock_4323 = new Object();
    private final Object __crossLock_4351 = new Object();
}
