package org.example.Replication;

import java.security.MessageDigest;
import java.util.Base64;

public class ReplicationMessage {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        WAL_DATA, HEARTBEAT, ACK, RESEND_REQUEST, HANDSHAKE
    }

    private long sequenceNumber;
    private MessageType type;
    private byte[] data;
    private String checksum;
    private long timestamp;

    public ReplicationMessage(long seqNum, MessageType type, byte[] data) {
        this.sequenceNumber = seqNum;
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.checksum = calculateChecksum(data);
    }

    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data != null ? data : new byte[0]);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }

    public boolean verifyChecksum() {
        return checksum.equals(calculateChecksum(data));
    }

    // Getters and setters
    public long getSequenceNumber() { return sequenceNumber; }
    public MessageType getType() { return type; }
    public byte[] getData() { return data; }
    public String getChecksum() { return checksum; }
    public long getTimestamp() { return timestamp; }
}
