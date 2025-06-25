package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import org.example.dao.PatientDAO;
import org.example.model.Patient;
import org.example.manager.Scripts;

import java.net.URL;
import java.util.ResourceBundle;

public class EditPatientController implements Initializable {
    @FXML private TextField nameField, surnameField, cnpField, phoneField, emailField;
    @FXML private Label statusLabel, titleLabel;
    @FXML private Button saveButton;

    private Patient currentPatient;
    private AdminController adminController;
    private Scripts scripts = new Scripts();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupValidation();
        updateButtonState();
    }

    private void setupValidation() {
        nameField.textProperty().addListener((obs, oldText, newText) -> updateButtonState());
        surnameField.textProperty().addListener((obs, oldText, newText) -> updateButtonState());
        cnpField.textProperty().addListener((obs, oldText, newText) -> updateButtonState());
        phoneField.textProperty().addListener((obs, oldText, newText) -> updateButtonState());
        emailField.textProperty().addListener((obs, oldText, newText) -> updateButtonState());
    }

    public void setPatientForEditing(Patient patient) {
        this.currentPatient = patient;
        if (patient != null) {
            nameField.setText(patient.getName());
            surnameField.setText(patient.getSurname());
            cnpField.setText(patient.getCnp());
            phoneField.setText(patient.getPhone());
            emailField.setText(patient.getEmail());

            titleLabel.setText("Edit Patient - " + patient.getName() + " " + patient.getSurname());
            updateButtonState();
        }
    }

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    @FXML
    private void handleSaveChanges(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }

        try {
            currentPatient.setName(nameField.getText().trim());
            currentPatient.setSurname(surnameField.getText().trim());
            currentPatient.setCnp(cnpField.getText().trim());
            currentPatient.setPhone(phoneField.getText().trim());
            currentPatient.setEmail(emailField.getText().trim());

            boolean success = PatientDAO.updatePatient(currentPatient);

            if (success) {
                showSuccessAlert("Patient Updated", "Patient information has been successfully updated.");

                if (adminController != null) {
                    adminController.refreshTable();
                }

                scripts.runBatchFile("C:\\postgres_archive\\script.bat");

                handleCancel(event);
            } else {
                statusLabel.setText("Failed to update patient");
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                showErrorAlert("Update Error", "Failed to update patient", "Please try again.");
            }
        } catch (Exception e) {
            statusLabel.setText("Error updating patient");
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            showErrorAlert("Update Error", "An error occurred", "Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        Button source = (Button) event.getSource();
        source.getScene().getWindow().hide();
    }

    @FXML
    private void handleReset(ActionEvent event) {
        if (currentPatient != null) {
            setPatientForEditing(currentPatient);
            statusLabel.setText("Fields reset to original values");
            statusLabel.setStyle("-fx-text-fill: #f39c12;");
        }
    }

    private boolean validateInputs() {
        String cnp = cnpField.getText().trim();
        if (!cnp.matches("\\d{13}")) {
            showErrorAlert("Validation Error", "Invalid CNP", "CNP must contain exactly 13 digits");
            cnpField.requestFocus();
            return false;
        }

        String email = emailField.getText().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showErrorAlert("Validation Error", "Invalid Email", "Please enter a valid email address");
            emailField.requestFocus();
            return false;
        }

        String phone = phoneField.getText().trim();
        if (!phone.matches("^(\\+4|004)?0?(7\\d{8})$")) {
            showErrorAlert("Validation Error", "Invalid Phone Number", "Please enter a valid Romanian phone number");
            phoneField.requestFocus();
            return false;
        }

        return true;
    }

    private void updateButtonState() {
        boolean allFieldsFilled =
                !nameField.getText().trim().isEmpty() &&
                        !surnameField.getText().trim().isEmpty() &&
                        !cnpField.getText().trim().isEmpty() &&
                        !phoneField.getText().trim().isEmpty() &&
                        !emailField.getText().trim().isEmpty();

        saveButton.setDisable(!allFieldsFilled);

        if (allFieldsFilled) {
            statusLabel.setText("✅ Ready to save changes");
            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else {
            statusLabel.setText("📝 Fill in all required fields");
            statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: normal;");
        }
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}