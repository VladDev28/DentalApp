package org.example.Replication;

import org.example.dao.PatientDAO;
import org.example.model.Patient;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class UDPReplicationReceiver {
    private static final Logger logger = Logger.getLogger(UDPReplicationReceiver.class.getName());
    private static UDPReplicationReceiver instance;
    private DatagramSocket socket;
    private ExecutorService executor;
    private volatile boolean running = false;

    private UDPReplicationReceiver() {
        try {
            socket = new DatagramSocket(ReplicationConfig.REPLICATION_PORT);
            executor = Executors.newSingleThreadExecutor();
        } catch (SocketException e) {
            logger.severe("Failed to create UDP receiver socket: " + e.getMessage());
        }
    }

    public static synchronized UDPReplicationReceiver getInstance() {
        if (instance == null) {
            instance = new UDPReplicationReceiver();
        }
        return instance;
    }

    public void startListening() {
        if (running) return;

        running = true;
        executor.submit(() -> {
            byte[] buffer = new byte[8192];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Deserialize the message
                    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    ReplicationMessage message = (ReplicationMessage) ois.readObject();

                    // Process the replication message
                    processReplicationMessage(message);

                    ois.close();
                    bais.close();

                } catch (Exception e) {
                    if (running) {
                        logger.warning("Error receiving replication message: " + e.getMessage());
                    }
                }
            }
        });

        logger.info("UDP Replication Receiver started on port " + ReplicationConfig.REPLICATION_PORT);
    }

    private void processReplicationMessage(ReplicationMessage message) {
        try {
            logger.info("Received replication message: " + message.getOperation() +
                    " on " + message.getTableName() + " from " + message.getSourceNodeId());

            if ("patients".equals(message.getTableName())) {
                Patient patient = (Patient) message.getData();

                switch (message.getOperation()) {
                    case INSERT:
                        PatientDAO.addPatientNoReplication(patient);
                        break;
                    case UPDATE:
                        PatientDAO.updatePatientNoReplication(patient);
                        break;
                    case DELETE:
                        PatientDAO.deletePatientNoReplication(patient.getId());
                        break;
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to process replication message: " + e.getMessage());
        }
    }

    public void stopListening() {
        running = false;
        if (socket != null) {
            socket.close();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}