package org.example.dao;

import org.example.database.Database;
import org.example.manager.Scripts;
import org.example.model.Patient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public static List<Patient> getAll() {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Patient patient = createPatientFromResultSet(rs);
                patients.add(patient);
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving all patients: " + e.getMessage());
            e.printStackTrace();
        }

        return patients;
    }

    Scripts script = new Scripts();

    public static List<Patient> searchByNameOrPhone(String query) {
        List<Patient> patients = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return getAll();
        }

        String sql = "SELECT * FROM patients WHERE " +
                "LOWER(name) LIKE LOWER(?) OR " +
                "LOWER(surname) LIKE LOWER(?) OR " +
                "phone LIKE ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + query.trim() + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Patient patient = createPatientFromResultSet(rs);
                    patients.add(patient);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error searching patients: " + e.getMessage());
            e.printStackTrace();
        }

        return patients;
    }

    private static Patient createPatientFromResultSet(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getInt("id"));
        patient.setName(rs.getString("name"));
        patient.setSurname(rs.getString("surname"));
        patient.setCnp(rs.getString("cnp"));
        patient.setPhone(rs.getString("phone"));
        patient.setEmail(rs.getString("email"));

        byte[] xrayData = rs.getBytes("xray");
        if (xrayData != null) {
            patient.setXray(xrayData);
        }

        return patient;
    }

    public static Patient findByCnp(String cnp) {
        String sql = "SELECT * FROM patients WHERE cnp = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cnp);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return createPatientFromResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding patient by CNP: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public static boolean updatePatientXray(int patientId, byte[] xrayData) {
        String sql = "UPDATE patients SET xray = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (xrayData != null) {
                stmt.setBytes(1, xrayData);
            } else {
                stmt.setNull(1, Types.BLOB);
            }
            stmt.setInt(2, patientId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating patient X-ray: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addPatient(Patient patient) {
        String sql = "INSERT INTO patients (name, surname, cnp, phone, email) VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, patient.getName());
            stmt.setString(2, patient.getSurname());
            stmt.setString(3, patient.getCnp());
            stmt.setString(4, patient.getPhone());
            stmt.setString(5, patient.getEmail());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    patient.setId(rs.getInt("id"));
                    return true;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error adding patient: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public static boolean updatePatient(Patient patient) {
        String sql = "UPDATE patients SET name = ?, surname = ?, cnp = ?, phone = ?, email = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, patient.getName());
            stmt.setString(2, patient.getSurname());
            stmt.setString(3, patient.getCnp());
            stmt.setString(4, patient.getPhone());
            stmt.setString(5, patient.getEmail());
            stmt.setLong(6, patient.getId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;


        } catch (SQLException e) {
            System.err.println("Error updating patient: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public static boolean deletePatient(long patientId) {
        String sql = "DELETE FROM patients WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, patientId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting patient: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}