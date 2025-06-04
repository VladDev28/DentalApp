package org.example.Replication;

import java.util.Arrays;
import java.util.List;

public class ReplicationConfig {
    public static final int REPLICATION_PORT = 8080;
    public static final String MULTICAST_GROUP = "230.0.0.1";

    public static final List<String> CLUSTER_NODES = Arrays.asList(
            "192.168.0.118",
            "192.168.1.119",
            "192.168.1.186"
    );

    public static String getCurrentNodeId() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown-" + System.currentTimeMillis();
        }
    }
}