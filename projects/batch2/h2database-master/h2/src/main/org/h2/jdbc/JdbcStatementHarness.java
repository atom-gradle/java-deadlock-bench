package org.h2.jdbc;

public class JdbcStatementHarness {
    public static void main(String[] args) throws Exception {
        JdbcStatement instance = new JdbcStatement((JdbcConnection) null, 0, 0, 0);
    }
}