package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.Replication.ReplicationService;
import org.example.dao.PatientDAO;
import org.example.model.Patient;
import java.net.URL;
import java.util.ResourceBundle;

public class AddPatientController {
    @FXML
    private TextField nameField, surnameField, cnpField, phoneField, emailField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveButton;

    public void initialize(URL location, ResourceBundle resources) {
        setupValidation();

    }


    public void setupValidation() {
        nameField.textProperty().addListener((obs, oldText, newText) -> updateButtonSaveState());
        surnameField.textProperty().addListener((obs, oldText, newText) -> updateButtonSaveState());
        cnpField.textProperty().addListener((obs, oldText, newText) -> updateButtonSaveState());
        phoneField.textProperty().addListener((obs, oldText, newText) -> updateButtonSaveState());
        emailField.textProperty().addListener((obs, oldText, newText) -> updateButtonSaveState());

        updateButtonSaveState();
    }

    private boolean validateInserts() {
        String cnp = cnpField.getText().trim();
        if (!cnp.matches("\\d{13}")) {
            showErrorAlert("Validation error","Invalid CNP","CNP should contain 13 digits");
            cnpField.requestFocus();
            return false;
        }
        String email = emailField.getText().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showErrorAlert("Validation error","Invalid email","Email format does not match");
            cnpField.requestFocus();
            return false;
        }
        String phone = phoneField.getText().trim();
        if (!phone.matches("^(\\+4|004)?0?(7\\d{8})$")) {
            showErrorAlert("Validation error","Invalid Phone number","Enter a correct phone number");
            cnpField.requestFocus();
            return false;
        }
        return true;

    }

    public void handleClearAll(){

    }
    public void handleCancel(){

    }


    public void handleSavePatient() {
        if(!validateInserts()){
            return;
        }
        try {
            Patient patient = new Patient();
            patient.setName(nameField.getText().trim());
            patient.setSurname(surnameField.getText().trim());
            patient.setCnp(cnpField.getText().trim());
            patient.setPhone(phoneField.getText().trim());
            patient.setEmail(emailField.getText().trim());

            boolean success = PatientDAO.addPatient(patient);


            if (success) {
                showSuccessAlert("Patient Added", "Patient has been successfully added to the database.");
                clearAllFields();
            } else {
                statusLabel.setText("Failed to save patient");
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                showErrorAlert("Save Error", "Failed to save patient", "Please try again.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void clearAllFields() {

    }

    public void updateButtonSaveState() {
        boolean allFieldsFilled = !nameField.getText().trim().isEmpty() &&
                !surnameField.getText().trim().isEmpty() &&
                !cnpField.getText().trim().isEmpty() &&
                !cnpField.getText().trim().isEmpty() &&
                !cnpField.getText().trim().isEmpty();

        saveButton.setDisable(!allFieldsFilled);

        if (allFieldsFilled) {
            statusLabel.setText("Ready to save patient");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
        } else {
            statusLabel.setText("Fill in all required fields");
            statusLabel.setStyle("-fx-text-fill: #666666;");
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



