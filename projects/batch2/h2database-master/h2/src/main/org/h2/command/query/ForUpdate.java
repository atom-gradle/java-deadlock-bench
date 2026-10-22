/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.query;

import org.h2.message.DbException;
import org.h2.util.HasSQL;
import org.h2.util.StringUtils;

/**
 * FOR UPDATE clause.
 */
public final class ForUpdate implements HasSQL {

    /**
     * Type of FOR UPDATE clause.
     */
    public enum Type {

        /**
         * Use default lock timeout.
         */
        DEFAULT,

        /**
         * Use specified lock timeout.
         */
        WAIT,

        /**
         * Use zero timeout.
         */
        NOWAIT,

        /**
         * Skip locked rows.
         */
        SKIP_LOCKED

    }

    /**
     * FOR UPDATE clause without additional parameters.
     */
    public static final ForUpdate DEFAULT = new ForUpdate(Type.DEFAULT, -1);

    /**
     * FOR UPDATE NOWAIT clause.
     */
    public static final ForUpdate NOWAIT = new ForUpdate(Type.NOWAIT, 0);

    /**
     * FOR UPDATE SKIP LOCKED clause.
     */
    public static final ForUpdate SKIP_LOCKED = new ForUpdate(Type.SKIP_LOCKED, -2);

    /**
     * Returns FOR UPDATE WAIT N clause.
     *
     * @param timeoutMillis
     *            timeout in milliseconds
     * @return FOR UPDATE WAIT N clause
     */
    public static ForUpdate wait(int timeoutMillis) {
        if (timeoutMillis < 0) {
            throw DbException.getInvalidValueException("timeout", timeoutMillis);
        }
        if (timeoutMillis == 0) {
            return NOWAIT;
        }
        return new ForUpdate(Type.WAIT, timeoutMillis);
    }

    private final Type type;

    private final int timeoutMillis;

    private ForUpdate(Type type, int timeoutMillis) {
        this.type = type;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Returns type of FOR UPDATE clause.
     *
     * @return type of FOR UPDATE clause
     */
    public Type getType() {
         synchronized (__crossLock_8391) {
             try { Thread.sleep(465); } catch (InterruptedException e) { }
             synchronized (__crossLock_7126) {
                 try { Thread.sleep(465); } catch (InterruptedException e) { }
                 synchronized (__crossLock_4862) {
                     try { Thread.sleep(465); } catch (InterruptedException e) { }

                  synchronized (__crossLock_8391) {
                      try { Thread.sleep(291); } catch (InterruptedException e) { }
                      synchronized (__crossLock_7126) {
                          try { Thread.sleep(291); } catch (InterruptedException e) { }
                          synchronized (__crossLock_4862) {
                              try { Thread.sleep(291); } catch (InterruptedException e) { }
                              synchronized (__crossLock_7326) {
                                  try { Thread.sleep(291); } catch (InterruptedException e) { }

                          return type;
    
                              }
                          }
                      }
                  }
                 }
             }
         }}

    /**
     * Returns timeout in milliseconds.
     *
     * @return timeout in milliseconds for {@link Type#WAIT}, {@code 0} for
     *         {@link Type#NOWAIT}, {@code -2} for {@link Type#SKIP_LOCKED},
     *         {@code -1} for default timeout
     */
    public int getTimeoutMillis() {
         try {
             mockMethod_0_7612();
         } catch (Exception e) {
             throw new RuntimeException(e);
         }
        return timeoutMillis;
    }

    @Override
    public StringBuilder getSQL(StringBuilder builder, int sqlFlags) {
        builder.append(" FOR UPDATE");
        switch (type) {
        case WAIT: {
            builder.append(" WAIT ").append(timeoutMillis / 1_000);
            int millis = timeoutMillis % 1_000;
            if (millis > 0) {
                StringUtils.appendZeroPadded(builder.append('.'), 3, millis);
            }
            break;
        }
        case NOWAIT:
            builder.append(" NOWAIT");
            break;
        case SKIP_LOCKED:
            builder.append(" SKIP LOCKED");
            break;
        default:
        }
        return builder;
    }



    private final Object __crossLock_8391 = new Object();

    public void crossLock_g0_ring0_7740() throws Exception {
        synchronized (__crossLock_8391) {
            try { Thread.sleep(173); } catch (InterruptedException e) { }
            crossLock_g0_ring1_2273();
        }
    }

    private void __ring_crossLock_g0_ring1_2273_86_m1() throws Exception {
        try { Thread.sleep(173); } catch (InterruptedException e) { }
        crossLock_g0_ring0_7740();
    }

    private void __ring_crossLock_g0_ring1_2273_86_m0() throws Exception {
        synchronized (__crossLock_7126) {
            try { Thread.sleep(173); } catch (InterruptedException e) { }
            __ring_crossLock_g0_ring1_2273_86_m1();
        }
    }

    public void crossLock_g0_ring1_2273() throws Exception {
        try { Thread.sleep(173); } catch (InterruptedException e) { }
        __ring_crossLock_g0_ring1_2273_86_m0();
    }

    public void crossLock_g1_ring0_4720() throws Exception {
        synchronized (__crossLock_4862) {
            try { Thread.sleep(128); } catch (InterruptedException e) { }
            crossLock_g1_ring1_3893();
        }
    }

    public void crossLock_g1_ring1_3893() throws Exception {
        synchronized (__crossLock_7326) {
            try { Thread.sleep(128); } catch (InterruptedException e) { }
            crossLock_g1_ring2_4934();
        }
    }

    public void crossLock_g1_ring2_4934() throws Exception {
        synchronized (__crossLock_9647) {
            try { Thread.sleep(128); } catch (InterruptedException e) { }
            crossLock_g1_ring3_1996();
        }
    }

    public void crossLock_g1_ring3_1996() throws Exception {
        synchronized (__crossLock_7934) {
            try { Thread.sleep(128); } catch (InterruptedException e) { }
            crossLock_g1_ring0_4720();
        }
    }

    public void crossLock_g2_ring0_1402() throws Exception {
        synchronized (__crossLock_8391) {
            try { Thread.sleep(270); } catch (InterruptedException e) { }
            crossLock_g2_ring1_8576();
        }
    }

    private void __ring_crossLock_g2_ring1_8576_6116_m0() throws Exception {
        synchronized (__crossLock_7126) {
            try { Thread.sleep(270); } catch (InterruptedException e) { }
            crossLock_g2_ring2_5908();
        }
    }

    public void crossLock_g2_ring1_8576() throws Exception {
        try { Thread.sleep(270); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_8576_6116_m0();
    }

    public void crossLock_g2_ring2_5908() throws Exception {
        synchronized (__crossLock_4862) {
            try { Thread.sleep(270); } catch (InterruptedException e) { }
            crossLock_g2_ring0_1402();
        }
    }

    public void mockMethod_0_7612() throws Exception {
        mockMethod_1_2868();
    }


    public void mockMethod_1_2868() throws Exception {
        mockMethod_2_8945();
    }


    public void mockMethod_2_8945() throws Exception {
        try { Thread.sleep(273); } catch (InterruptedException e) { }
        synchronized (__crossLock_8391) {
            synchronized (__crossLock_8391) {
                getType();
            }
        }
    }


    private final Object __crossLock_7126 = new Object();
    private final Object __crossLock_4862 = new Object();
    private final Object __crossLock_7326 = new Object();
    private final Object __crossLock_9647 = new Object();
    private final Object __crossLock_7934 = new Object();
}
