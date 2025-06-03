package org.example.Replication;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ReplicationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Operation {
        INSERT, UPDATE, DELETE
    }

    private Operation operation;
    private String tableName;
    private Object data;
    private LocalDateTime timestamp;
    private String sourceNodeId;

    public ReplicationMessage(Operation operation, String tableName, Object data, String sourceNodeId) {
        this.operation = operation;
        this.tableName = tableName;
        this.data = data;
        this.sourceNodeId = sourceNodeId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public Operation getOperation() { return operation; }
    public void setOperation(Operation operation) { this.operation = operation; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
}
