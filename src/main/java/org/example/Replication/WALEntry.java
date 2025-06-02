package org.example.Replication;

import java.io.*;
import java.util.Map;

public class WALEntry {
    private final String operation; // INSERT, UPDATE, DELETE
    private final String tableName;
    private final Map<String, Object> data;
    private final String lsn; // Log Sequence Number

    public WALEntry(String operation, String tableName, Map<String, Object> data, String lsn) {
        this.operation = operation;
        this.tableName = tableName;
        this.data = data;
        this.lsn = lsn;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        return baos.toByteArray();
    }

    public static WALEntry deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (WALEntry) ois.readObject();
    }

    // Getters
    public String getOperation() { return operation; }
    public String getTableName() { return tableName; }
    public Map<String, Object> getData() { return data; }
    public String getLsn() { return lsn; }
}
