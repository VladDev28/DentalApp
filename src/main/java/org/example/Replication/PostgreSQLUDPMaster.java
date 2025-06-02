package org.example.Replication;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PostgreSQLUDPMaster {
    private final DatagramSocket socket;
    private final List<InetSocketAddress> replicas;
    private final AtomicLong sequenceNumber;
    private final Map<Long, ReplicationMessage> sentMessages;
    private final ScheduledExecutorService scheduler;
    private final Connection pgConnection;

    // Configuration
    private static final int MAX_PACKET_SIZE = 1400; // Stay under MTU
    private static final int HEARTBEAT_INTERVAL = 5000; // 5 seconds
    private static final int RESEND_TIMEOUT = 3000; // 3 seconds

    public PostgreSQLUDPMaster(int port, String pgUrl, String pgUser, String pgPassword) throws Exception {
        this.socket = new DatagramSocket(port);
        this.replicas = new CopyOnWriteArrayList<>();
        this.sequenceNumber = new AtomicLong(0);
        this.sentMessages = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);

        // Connect to PostgreSQL
        this.pgConnection = DriverManager.getConnection(pgUrl, pgUser, pgPassword);

        // Start background tasks
        startHeartbeat();
        startResendMonitor();
        startListening();

        System.out.println("UDP Master started on port " + port);
    }

    public void addReplica(String host, int port) {
        replicas.add(new InetSocketAddress(host, port));
        System.out.println("Added replica: " + host + ":" + port);
    }

    // Simulate WAL reading and replication
    public void replicateWALEntry(WALEntry entry) {
        try {
            byte[] data = entry.serialize();

            // Split large messages if needed
            List<byte[]> chunks = splitMessage(data);

            for (byte[] chunk : chunks) {
                ReplicationMessage msg = new ReplicationMessage(
                        sequenceNumber.incrementAndGet(),
                        ReplicationMessage.MessageType.WAL_DATA,
                        chunk
                );

                sendToAllReplicas(msg);
                sentMessages.put(msg.getSequenceNumber(), msg);
            }
        } catch (Exception e) {
            System.err.println("Error replicating WAL entry: " + e.getMessage());
        }
    }

    private List<byte[]> splitMessage(byte[] data) {
        List<byte[]> chunks = new ArrayList<>();
        int offset = 0;

        while (offset < data.length) {
            int chunkSize = Math.min(MAX_PACKET_SIZE, data.length - offset);
            byte[] chunk = new byte[chunkSize];
            System.arraycopy(data, offset, chunk, 0, chunkSize);
            chunks.add(chunk);
            offset += chunkSize;
        }

        return chunks;
    }

    private void sendToAllReplicas(ReplicationMessage msg) {
        byte[] serialized = serializeMessage(msg);

        for (InetSocketAddress replica : replicas) {
            try {
                DatagramPacket packet = new DatagramPacket(
                        serialized, serialized.length, replica
                );
                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Failed to send to replica " + replica + ": " + e.getMessage());
            }
        }
    }

    private void startListening() {
        Thread listener = new Thread(() -> {
            byte[] buffer = new byte[MAX_PACKET_SIZE];

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    ReplicationMessage msg = deserializeMessage(packet.getData(), packet.getLength());
                    handleReplicaMessage(msg, packet.getAddress(), packet.getPort());

                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        System.err.println("Error receiving message: " + e.getMessage());
                    }
                }
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    private void handleReplicaMessage(ReplicationMessage msg, InetAddress address, int port) {
        switch (msg.getType()) {
            case ACK:
                // Remove acknowledged message from resend queue
                sentMessages.remove(msg.getSequenceNumber());
                break;

            case RESEND_REQUEST:
                // Resend requested message
                ReplicationMessage toResend = sentMessages.get(msg.getSequenceNumber());
                if (toResend != null) {
                    try {
                        byte[] data = serializeMessage(toResend);
                        DatagramPacket packet = new DatagramPacket(
                                data, data.length, address, port
                        );
                        socket.send(packet);
                    } catch (IOException e) {
                        System.err.println("Failed to resend message: " + e.getMessage());
                    }
                }
                break;
        }
    }

    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            ReplicationMessage heartbeat = new ReplicationMessage(
                    sequenceNumber.incrementAndGet(),
                    ReplicationMessage.MessageType.HEARTBEAT,
                    new byte[0]
            );
            sendToAllReplicas(heartbeat);
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void startResendMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();

            for (ReplicationMessage msg : sentMessages.values()) {
                if (currentTime - msg.getTimestamp() > RESEND_TIMEOUT) {
                    sendToAllReplicas(msg);
                    // Update timestamp to avoid immediate resend
                    sentMessages.put(msg.getSequenceNumber(), new ReplicationMessage(
                            msg.getSequenceNumber(), msg.getType(), msg.getData()
                    ));
                }
            }
        }, RESEND_TIMEOUT, RESEND_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private byte[] serializeMessage(ReplicationMessage msg) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(msg);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    private ReplicationMessage deserializeMessage(byte[] data, int length) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data, 0, length);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (ReplicationMessage) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize message", e);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        socket.close();
        try {
            pgConnection.close();
        } catch (SQLException e) {
            System.err.println("Error closing PostgreSQL connection: " + e.getMessage());
        }
    }
}

