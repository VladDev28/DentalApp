package org.example.Replication;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class UDPReplicationSender {
    private static final Logger logger = Logger.getLogger(UDPReplicationSender.class.getName());
    private static UDPReplicationSender instance;
    private ExecutorService executor;
    private DatagramSocket socket;

    private UDPReplicationSender() {
        try {
            socket = new DatagramSocket();
            executor = Executors.newFixedThreadPool(2);
        } catch (SocketException e) {
            logger.severe("Failed to create UDP socket: " + e.getMessage());
        }
    }

    public static synchronized UDPReplicationSender getInstance() {
        if (instance == null) {
            instance = new UDPReplicationSender();
        }
        return instance;
    }

    public void sendReplicationMessage(ReplicationMessage message) {
        executor.submit(() -> {
            try {
                // Serialize the message
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(message);
                byte[] data = baos.toByteArray();

                // Send to all cluster nodes
                for (String nodeIp : ReplicationConfig.CLUSTER_NODES) {
                    if (!nodeIp.equals(ReplicationConfig.getCurrentNodeId())) {
                        try {
                            InetAddress address = InetAddress.getByName(nodeIp);
                            DatagramPacket packet = new DatagramPacket(
                                    data, data.length, address, ReplicationConfig.REPLICATION_PORT
                            );
                            socket.send(packet);
                            logger.info("Sent replication message to " + nodeIp);
                        } catch (Exception e) {
                            logger.warning("Failed to send to " + nodeIp + ": " + e.getMessage());
                        }
                    }
                }

                oos.close();
                baos.close();
            } catch (IOException e) {
                logger.severe("Failed to serialize replication message: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        if (socket != null) {
            socket.close();
        }
    }
}