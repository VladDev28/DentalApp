package org.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Database {
    private static final String MASTER_URL = "jdbc:postgresql://localhost:5432/dentist";
    private static final String REPLICA_URL = "jdbc:postgresql://localhost:5433/dentist";
    private static final String USER = "postgres";
    private static final String PASSWORD = "root";

    private static boolean isReplicaMode = false;

    public static Connection getConnection() throws SQLException {
        if (isReplicaMode) {
            return getReplicaConnection();
        }
        return getMasterConnection();
    }

    public static Connection getMasterConnection() throws SQLException {
        return DriverManager.getConnection(MASTER_URL, USER, PASSWORD);
    }

    public static Connection getReplicaConnection() throws SQLException {
        return DriverManager.getConnection(REPLICA_URL, USER, PASSWORD);
    }

    public static Connection getUserReadConnection() throws SQLException {
        return getReplicaConnection();
    }

    public static Connection getAdminConnection() throws SQLException {
        return getMasterConnection();
    }
}