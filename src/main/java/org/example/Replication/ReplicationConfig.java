package org.example.Replication;

import java.util.Arrays;
import java.util.List;

public class ReplicationConfig {
    public static final int REPLICATION_PORT = 9999;
    public static final String MULTICAST_GROUP = "230.0.0.1";

    public static final List<String> CLUSTER_NODES = Arrays.asList(
            "192.168.1.100",
            "192.168.1.101",
            "192.168.1.102"
    );

    // Current node configuration
    public static String getCurrentNodeId() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown-" + System.currentTimeMillis();
        }
    }
}