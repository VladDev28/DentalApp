package org.example.Replication;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Simple ReplicationManager for integrating PostgreSQL UDP replication
 * into existing projects. No UI dependencies, just core functionality.
 */
public class ReplicationManager {

    // Simple configuration class
    public static class Config {
        public final String role;           // "master" or "replica"
        public final int port;
        public final String pgUrl;
        public final String pgUser;
        public final String pgPassword;
        public final String masterHost;     // for replica only
        public final Integer masterPort;    // for replica only

        // Master config
        public Config(String role, int port, String pgUrl, String pgUser, String pgPassword) {
            this.role = role;
            this.port = port;
            this.pgUrl = pgUrl;
            this.pgUser = pgUser;
            this.pgPassword = pgPassword;
            this.masterHost = null;
            this.masterPort = null;
        }

        // Replica config
        public Config(String role, int port, String pgUrl, String pgUser, String pgPassword,
                      String masterHost, int masterPort) {
            this.role = role;
            this.port = port;
            this.pgUrl = pgUrl;
            this.pgUser = pgUser;
            this.pgPassword = pgPassword;
            this.masterHost = masterHost;
            this.masterPort = masterPort;
        }
    }

    // Simple status info
    public static class Status {
        public final String state;
        public final long messagesProcessed;
        public final long uptime;
        public final String lastError;

        public Status(String state, long messagesProcessed, long uptime, String lastError) {
            this.state = state;
            this.messagesProcessed = messagesProcessed;
            this.uptime = uptime;
            this.lastError = lastError;
        }

        @Override
        public String toString() {
            return String.format("Status{state='%s', messages=%d, uptime=%ds, error='%s'}",
                    state, messagesProcessed, uptime/1000, lastError);
        }
    }

    // Core components
    private final Config config;
    private PostgreSQLUDPMaster master;
    private PostgreSQLUDPReplica replica;

    // State tracking
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    private final AtomicLong startTime = new AtomicLong(0);
    private volatile String lastError = "";

    // Optional callbacks for your existing code
    private Consumer<String> logCallback;
    private Consumer<String> errorCallback;
    private Consumer<Status> statusCallback;

    public ReplicationManager(Config config) {
        this.config = config;
    }

    /**
     * Start replication system
     */
    public synchronized void start() throws Exception {
        if (running.get()) {
            throw new IllegalStateException("Already running");
        }

        try {
            startTime.set(System.currentTimeMillis());

            if ("master".equalsIgnoreCase(config.role)) {
                master = new PostgreSQLUDPMaster(config.port, config.pgUrl, config.pgUser, config.pgPassword);
                log("Master started on port " + config.port);
            } else if ("replica".equalsIgnoreCase(config.role)) {
                replica = new PostgreSQLUDPReplica(config.port, config.masterHost, config.masterPort,
                        config.pgUrl, config.pgUser, config.pgPassword);
                log("Replica started, connected to " + config.masterHost + ":" + config.masterPort);
            } else {
                throw new IllegalArgumentException("Role must be 'master' or 'replica'");
            }

            running.set(true);
            lastError = "";
            notifyStatus();

        } catch (Exception e) {
            lastError = e.getMessage();
            notifyError("Failed to start: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Stop replication system
     */
    public synchronized void stop() {
        if (!running.get()) return;

        try {
            if (master != null) {
                master.shutdown();
                master = null;
            }
            if (replica != null) {
                replica.shutdown();
                replica = null;
            }

            running.set(false);
            log("Replication stopped");
            notifyStatus();

        } catch (Exception e) {
            lastError = "Shutdown error: " + e.getMessage();
            notifyError(lastError);
        }
    }

    /**
     * Add replica (master only)
     */
    public void addReplica(String host, int port) {
        if (master == null) {
            throw new IllegalStateException("Not running as master");
        }
        master.addReplica(host, port);
        log("Added replica: " + host + ":" + port);
    }

    /**
     * Replicate WAL entry (master only)
     */
    public void replicateWAL(WALEntry entry) {
        if (master == null) {
            throw new IllegalStateException("Not running as master");
        }
        master.replicateWALEntry(entry);
        messagesProcessed.incrementAndGet();
        log("Replicated: " + entry.getOperation() + " on " + entry.getTableName());
        notifyStatus();
    }

    /**
     * Create WAL entry for common operations
     */
    public WALEntry createWALEntry(String operation, String table, Map<String, Object> data) {
        return new WALEntry(operation.toUpperCase(), table, data, "LSN_" + System.currentTimeMillis());
    }

    /**
     * Helper for INSERT operations
     */
    public void replicateInsert(String table, Map<String, Object> data) {
        replicateWAL(createWALEntry("INSERT", table, data));
    }

    /**
     * Helper for UPDATE operations
     */
    public void replicateUpdate(String table, Map<String, Object> data) {
        replicateWAL(createWALEntry("UPDATE", table, data));
    }

    /**
     * Helper for DELETE operations
     */
    public void replicateDelete(String table, Map<String, Object> whereConditions) {
        replicateWAL(createWALEntry("DELETE", table, whereConditions));
    }

    /**
     * Get current status
     */
    public Status getStatus() {
        long uptime = running.get() ? System.currentTimeMillis() - startTime.get() : 0;
        String state = running.get() ? "RUNNING" : "STOPPED";
        return new Status(state, messagesProcessed.get(), uptime, lastError);
    }

    /**
     * Check if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Check if master
     */
    public boolean isMaster() {
        return "master".equalsIgnoreCase(config.role);
    }

    /**
     * Check if replica
     */
    public boolean isReplica() {
        return "replica".equalsIgnoreCase(config.role);
    }

    // Callback setters for integration with your existing code
    public ReplicationManager onLog(Consumer<String> callback) {
        this.logCallback = callback;
        return this;
    }

    public ReplicationManager onError(Consumer<String> callback) {
        this.errorCallback = callback;
        return this;
    }

    public ReplicationManager onStatus(Consumer<Status> callback) {
        this.statusCallback = callback;
        return this;
    }

    // Internal notification methods
    private void log(String message) {
        String logMessage = "[" + new Date() + "] " + message;
        if (logCallback != null) {
            logCallback.accept(logMessage);
        } else {
            System.out.println(logMessage);
        }
    }

    private void notifyError(String error) {
        if (errorCallback != null) {
            errorCallback.accept(error);
        } else {
            System.err.println("ERROR: " + error);
        }
    }

    private void notifyStatus() {
        if (statusCallback != null) {
            statusCallback.accept(getStatus());
        }
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        stop();
    }

    // Static factory methods for easy creation
    public static ReplicationManager createMaster(int port, String pgUrl, String pgUser, String pgPassword) {
        return new ReplicationManager(new Config("master", port, pgUrl, pgUser, pgPassword));
    }

    public static ReplicationManager createReplica(int port, String pgUrl, String pgUser, String pgPassword,
                                                   String masterHost, int masterPort) {
        return new ReplicationManager(new Config("replica", port, pgUrl, pgUser, pgPassword, masterHost, masterPort));
    }
}