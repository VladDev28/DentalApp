package org.example.Replication;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class PostgreSQLUDPReplica {
    private final DatagramSocket socket;
    private final InetSocketAddress master;
    private final Map<Long, ReplicationMessage> receivedMessages;
    private final Set<Long> expectedSequences;
    private final Connection pgConnection;
    private final ScheduledExecutorService scheduler;

    private long lastReceivedSequence = 0;

    public PostgreSQLUDPReplica(int port, String masterHost, int masterPort,
                                String pgUrl, String pgUser, String pgPassword) throws Exception {
        this.socket = new DatagramSocket(port);
        this.master = new InetSocketAddress(masterHost, masterPort);
        this.receivedMessages = new ConcurrentHashMap<>();
        this.expectedSequences = ConcurrentHashMap.newKeySet();
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Connect to replica PostgreSQL database
        this.pgConnection = DriverManager.getConnection(pgUrl, pgUser, pgPassword);

        startListening();
        startGapDetection();

        System.out.println("UDP Replica started on port " + port +
                ", connected to master " + masterHost + ":" + masterPort);
    }

    void startListening() {
        Thread listener = new Thread(() -> {
            byte[] buffer = new byte[1500];

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    ReplicationMessage msg = deserializeMessage(packet.getData(), packet.getLength());
                    handleMasterMessage(msg);

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

    private void handleMasterMessage(ReplicationMessage msg) {
        // Verify message integrity
        if (!msg.verifyChecksum()) {
            System.err.println("Checksum verification failed for message " + msg.getSequenceNumber());
            return;
        }

        switch (msg.getType()) {
            case WAL_DATA:
                processWALData(msg);
                break;

            case HEARTBEAT:
                // Send acknowledgment for heartbeat
                sendAck(msg.getSequenceNumber());
                break;
        }

        receivedMessages.put(msg.getSequenceNumber(), msg);
        lastReceivedSequence = Math.max(lastReceivedSequence, msg.getSequenceNumber());

        // Send acknowledgment
        sendAck(msg.getSequenceNumber());
    }

    private void processWALData(ReplicationMessage msg) {
        try {
            WALEntry entry = WALEntry.deserialize(msg.getData());
            applyWALEntry(entry);
            System.out.println("Applied WAL entry: " + entry.getOperation() +
                    " on table " + entry.getTableName());
        } catch (Exception e) {
            System.err.println("Error processing WAL data: " + e.getMessage());
        }
    }

    private void applyWALEntry(WALEntry entry) throws SQLException {
        // This is a simplified example - real implementation would be more complex
        switch (entry.getOperation()) {
            case "INSERT":
                performInsert(entry);
                break;
            case "UPDATE":
                performUpdate(entry);
                break;
            case "DELETE":
                performDelete(entry);
                break;
        }
    }

    private void performInsert(WALEntry entry) throws SQLException {
        // Example implementation - adapt to your schema
        StringBuilder sql = new StringBuilder("INSERT INTO " + entry.getTableName() + " (");
        StringBuilder values = new StringBuilder(" VALUES (");

        List<Object> params = new ArrayList<>();
        boolean first = true;

        for (Map.Entry<String, Object> data : entry.getData().entrySet()) {
            if (!first) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append(data.getKey());
            values.append("?");
            params.add(data.getValue());
            first = false;
        }

        sql.append(")").append(values).append(")");

        try (PreparedStatement stmt = pgConnection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();
        }
    }

    private void performUpdate(WALEntry entry) throws SQLException {
        // Simplified update implementation
        System.out.println("UPDATE operation on " + entry.getTableName());
    }

    private void performDelete(WALEntry entry) throws SQLException {
        // Simplified delete implementation
        System.out.println("DELETE operation on " + entry.getTableName());
    }

    private void sendAck(long sequenceNumber) {
        ReplicationMessage ack = new ReplicationMessage(
                sequenceNumber,
                ReplicationMessage.MessageType.ACK,
                new byte[0]
        );

        sendToMaster(ack);
    }

    private void sendResendRequest(long sequenceNumber) {
        ReplicationMessage request = new ReplicationMessage(
                sequenceNumber,
                ReplicationMessage.MessageType.RESEND_REQUEST,
                new byte[0]
        );

        sendToMaster(request);
    }

    private void sendToMaster(ReplicationMessage msg) {
        try {
            byte[] data = serializeMessage(msg);
            DatagramPacket packet = new DatagramPacket(data, data.length, master);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Failed to send message to master: " + e.getMessage());
        }
    }

    private void startGapDetection() {
        scheduler.scheduleAtFixedRate(() -> {
            // Check for gaps in sequence numbers
            for (long i = 1; i < lastReceivedSequence; i++) {
                if (!receivedMessages.containsKey(i)) {
                    sendResendRequest(i);
                }
            }
        }, 5000, 5000, TimeUnit.MILLISECONDS);
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
