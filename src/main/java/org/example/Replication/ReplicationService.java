package org.example.Replication;

import org.example.Replication.ReplicationConfig;
import org.example.Replication.ReplicationMessage;
import org.example.Replication.UDPReplicationReceiver;
import org.example.Replication.UDPReplicationSender;
import org.example.model.Patient;

public class ReplicationService {
    private static final UDPReplicationSender sender = UDPReplicationSender.getInstance();
    private static final UDPReplicationReceiver receiver = UDPReplicationReceiver.getInstance();

    public static void initialize() {
        receiver.startListening();
    }

    public static void shutdown() {
        sender.shutdown();
        receiver.stopListening();
    }

    public static void replicatePatientInsert(Patient patient) {
        ReplicationMessage message = new ReplicationMessage(
                ReplicationMessage.Operation.INSERT,
                "patients",
                patient,
                ReplicationConfig.getCurrentNodeId()
        );
        sender.sendReplicationMessage(message);
    }

    public static void replicatePatientUpdate(Patient patient) {
        ReplicationMessage message = new ReplicationMessage(
                ReplicationMessage.Operation.UPDATE,
                "patients",
                patient,
                ReplicationConfig.getCurrentNodeId()
        );
        sender.sendReplicationMessage(message);
    }

    public static void replicatePatientDelete(long patientId) {
        Patient patient = new Patient();
        patient.setId((int) patientId);

        ReplicationMessage message = new ReplicationMessage(
                ReplicationMessage.Operation.DELETE,
                "patients",
                patient,
                ReplicationConfig.getCurrentNodeId()
        );
        sender.sendReplicationMessage(message);
    }
}