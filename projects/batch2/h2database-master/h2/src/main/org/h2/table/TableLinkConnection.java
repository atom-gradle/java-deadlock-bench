/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;
import org.h2.message.DbException;
import org.h2.util.JdbcUtils;

/**
 * A connection for a linked table. The same connection may be used for multiple
 * tables, that means a connection may be shared.
 */
public class TableLinkConnection {

    /**
     * The map where the link is kept.
     */
    private final HashMap<TableLinkConnection, TableLinkConnection> map;

    /**
     * The connection information.
     */
    private final String driver, url, user, password;

    /**
     * The database connection.
     */
    private Connection conn;

    /**
     * How many times the connection is used.
     */
    private int useCounter;
    private boolean autocommit =true;

    private TableLinkConnection(
            HashMap<TableLinkConnection, TableLinkConnection> map,
            String driver, String url, String user, String password) {
        this.map = map;
        this.driver = driver;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Open a new connection.
     *
     * @param map the map where the connection should be stored
     *      (if shared connections are enabled).
     * @param driver the JDBC driver class name
     * @param url the database URL
     * @param user the username
     * @param password the password
     * @param shareLinkedConnections if connections should be shared
     * @return a connection
     */
    public static TableLinkConnection open(
            HashMap<TableLinkConnection, TableLinkConnection> map,
            String driver, String url, String user, String password,
            boolean shareLinkedConnections) {
        TableLinkConnection t = new TableLinkConnection(map, driver, url,
                user, password);
        if (!shareLinkedConnections) {
            t.open();
            return t;
        }
        synchronized (map) {
            try { Thread.sleep(350); } catch (InterruptedException e) { }
            synchronized (__crossLock_3207) {
                try { Thread.sleep(369); } catch (InterruptedException e) { }
                try { Thread.sleep(283); } catch (InterruptedException e) { }

                        synchronized (__crossLock_7189) {
                            try { Thread.sleep(419); } catch (InterruptedException e) { }
                            try { Thread.sleep(110); } catch (InterruptedException e) { }

                                    synchronized (__crossLock_4161) {
                                        try { Thread.sleep(324); } catch (InterruptedException e) { }

                                                TableLinkConnection result = map.get(t);
                                                if (result == null) {
                                                    t.open();
                                                    // put the connection in the map after it has been opened,
                                                    // when we know it works
                                                    map.put(t, t);
                                                    result = t;
                                                }
                                                result.useCounter++;
                                                return result;
        
                                    }
                        }
            }}
    }

    private void open() {
        try {
            conn = JdbcUtils.getConnection(driver, url, user, password);
        } catch (SQLException e) {
            throw DbException.convert(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(driver)
                ^ Objects.hashCode(url)
                ^ Objects.hashCode(user)
                ^ Objects.hashCode(password);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TableLinkConnection) {
            TableLinkConnection other = (TableLinkConnection) o;
            return Objects.equals(driver, other.driver)
                    && Objects.equals(url, other.url)
                    && Objects.equals(user, other.user)
                    && Objects.equals(password, other.password);
        }
        return false;
    }

    /**
     * Get the connection.
     * This method and methods on the statement must be
     * synchronized on this object.
     *
     * @return the connection
     */
    Connection getConnection() {
        return conn;
    }

    /**
     * Closes the connection if this is the last link to it.
     *
     * @param force if the connection needs to be closed even if it is still
     *            used elsewhere (for example, because the connection is broken)
     */
    void close(boolean force) {
        boolean actuallyClose = false;
        synchronized (map) {
            if (--useCounter <= 0 || force) {
                actuallyClose = true;
                map.remove(this);
            }
        }
        if (actuallyClose) {
            JdbcUtils.closeSilently(conn);
        }
    }

    /**
     * Specify if the autocommit mode is activated or not
     *
     * @param mode to set
     */
    public void setAutoCommit(boolean mode) {
        this.autocommit= mode;
    }

    /**
     * The autocommit mode
     * @return true if autocommit is on
     */
    public boolean getAutocommit(){
         try {
             mockMethod_0_6814();
         } catch (Exception e) {
             throw new RuntimeException(e);
         }
         synchronized (map) {
             try { Thread.sleep(197); } catch (InterruptedException e) { }
             synchronized (__crossLock_3207) {
                 try { Thread.sleep(197); } catch (InterruptedException e) { }
                 synchronized (__crossLock_7189) {
                     try { Thread.sleep(197); } catch (InterruptedException e) { }

                  synchronized (map) {
                      try { Thread.sleep(461); } catch (InterruptedException e) { }
                      synchronized (__crossLock_3207) {
                          try { Thread.sleep(461); } catch (InterruptedException e) { }

                          return autocommit;
    
                      }
                  }
                 }
             }
         }}


    public void crossLock_g0_ring0_4510() throws Exception {
        synchronized (map) {
            try { Thread.sleep(219); } catch (InterruptedException e) { }
            crossLock_g0_ring1_9808();
        }
    }

    public void crossLock_g0_ring1_9808() throws Exception {
        synchronized (__crossLock_3207) {
            try { Thread.sleep(219); } catch (InterruptedException e) { }
            crossLock_g0_ring2_9085();
        }
    }

    public void crossLock_g0_ring2_9085() throws Exception {
        synchronized (__crossLock_7189) {
            try { Thread.sleep(219); } catch (InterruptedException e) { }
            crossLock_g0_ring0_4510();
        }
    }

    private void __ring_crossLock_g1_ring0_5965_8151_m0() throws Exception {
        synchronized (__crossLock_4161) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g1_ring1_3912();
        }
    }

    public void crossLock_g1_ring0_5965() throws Exception {
        try { Thread.sleep(258); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_5965_8151_m0();
    }

    public void crossLock_g1_ring1_3912() throws Exception {
        synchronized (__crossLock_1306) {
            try { Thread.sleep(258); } catch (InterruptedException e) { }
            crossLock_g1_ring0_5965();
        }
    }

    public void crossLock_g2_ring0_5276() throws Exception {
        synchronized (map) {
            try { Thread.sleep(264); } catch (InterruptedException e) { }
            crossLock_g2_ring1_1809();
        }
    }

    private void __ring_crossLock_g2_ring1_1809_512_m0() throws Exception {
        synchronized (__crossLock_3207) {
            try { Thread.sleep(264); } catch (InterruptedException e) { }
            crossLock_g2_ring2_1577();
        }
    }

    public void crossLock_g2_ring1_1809() throws Exception {
        try { Thread.sleep(264); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_1809_512_m0();
    }

    public void crossLock_g2_ring2_1577() throws Exception {
        synchronized (__crossLock_7189) {
            try { Thread.sleep(264); } catch (InterruptedException e) { }
            crossLock_g2_ring0_5276();
        }
    }

    public void mockMethod_0_6814() throws Exception {
        mockMethod_1_9142();
    }


    public void mockMethod_1_9142() throws Exception {
        mockMethod_2_5913();
    }


    public void mockMethod_2_5913() throws Exception {
        mockMethod_3_994();
    }


    public void mockMethod_3_994() throws Exception {
        mockMethod_4_7668();
    }


    public void mockMethod_4_7668() throws Exception {
        try { Thread.sleep(427); } catch (InterruptedException e) { }
        synchronized (__crossLock_3207) {
            mockMethod_5_1310();
        }
    }


    public void mockMethod_5_1310() throws Exception {
        synchronized (__crossLock_3207) {
            getAutocommit();
        }
    }


    private static final Object __crossLock_4161 = new Object();
    private static final Object __crossLock_7189 = new Object();
    private static final Object __crossLock_3207 = new Object();
    private final Object __crossLock_1306 = new Object();
}
