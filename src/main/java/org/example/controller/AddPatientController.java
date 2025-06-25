package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.dao.PatientDAO;
import org.example.dao.UserDAO;
import org.example.model.Patient;
import org.example.model.User;
import org.example.manager.Scripts;

public class AddPatientController {
    @FXML
    private TextField nameField, surnameField, cnpField, phoneField, emailField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveButton;
    @FXML
    private CheckBox createUserAccountCheckbox;


    @FXML
    public void onFieldChanged() {
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
            emailField.requestFocus();
            return false;
        }

        String phone = phoneField.getText().trim();
        if (!phone.matches("^(\\+4|004)?0?(7\\d{8})$")) {
            showErrorAlert("Validation error","Invalid Phone number","Enter a correct phone number");
            phoneField.requestFocus();
            return false;
        }
        return true;
    }

    Scripts script = new Scripts();

    @FXML
    public void handleClearAll(){
        clearAllFields();
    }

    @FXML
    public void handleCancel(){
        nameField.getScene().getWindow().hide();
    }

    @FXML
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
                String successMessage = "Patient has been successfully added to the database.";
                String userAccountInfo = "";
                script.runBatchFile("C:\\postgres_archive\\script.bat");

                if (createUserAccountCheckbox != null && createUserAccountCheckbox.isSelected()) {
                    String userAccountResult = createUserAccountForPatient(patient);
                    userAccountInfo = "\n\n" + userAccountResult;
                }
                showSuccessAlert("Patient Added", successMessage + userAccountInfo);
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

    private String createUserAccountForPatient(Patient patient) {
        try {
            if (UserDAO.userExists(patient.getCnp())) {
                return "⚠️ User account already exists for this patient.";
            }

            String generatedPassword = UserDAO.generateRandomPassword(8);

            User newUser = new User();
            newUser.setUsername(patient.getCnp());
            newUser.setPassword(generatedPassword);
            newUser.setRole("user");

            boolean userSuccess = UserDAO.createUser(newUser);

            if (userSuccess) {
                return "✅ User account created!\n" +
                        "Username: " + patient.getCnp() + "\n" +
                        "Password: " + generatedPassword + "\n" +
                        "Please provide these credentials to the patient.";
            } else {
                return "❌ Failed to create user account. You can create it manually later.";
            }

        } catch (Exception e) {
            return "❌ Error creating user account: " + e.getMessage();
        }
    }

    private int currentPatientId;

    public void setPatientForEditing(Patient patient) {
        if (patient != null) {
            currentPatientId = patient.getId();
            nameField.setText(patient.getName());
            surnameField.setText(patient.getSurname());
            cnpField.setText(patient.getCnp());
            phoneField.setText(patient.getPhone());
            emailField.setText(patient.getEmail());
            updateButtonSaveState();

            if (createUserAccountCheckbox != null) {
                createUserAccountCheckbox.setVisible(false);
            }
        }
    }

    @FXML
    public void handleEditPatient() {
        if (!validateInserts()) {
            return;
        }
        try {
            if (currentPatientId == 0) {
                showErrorAlert("Edit Error", "No patient selected", "Please select a patient to edit.");
                return;
            }

            Patient patient = new Patient();
            patient.setId(currentPatientId);
            patient.setName(nameField.getText().trim());
            patient.setSurname(surnameField.getText().trim());
            patient.setCnp(cnpField.getText().trim());
            patient.setPhone(phoneField.getText().trim());
            patient.setEmail(emailField.getText().trim());

            boolean success = PatientDAO.updatePatient(patient);

            if (success) {
                showSuccessAlert("Patient Updated", "Patient information has been successfully updated.");
                script.runBatchFile("C:\\postgres_archive\\script.bat");
                clearAllFields();
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

    private void clearAllFields() {
        nameField.clear();
        surnameField.clear();
        cnpField.clear();
        phoneField.clear();
        emailField.clear();

        if (createUserAccountCheckbox != null) {
            createUserAccountCheckbox.setSelected(false);
            createUserAccountCheckbox.setVisible(true);
        }

        updateButtonSaveState();
    }

    public void updateButtonSaveState() {
        boolean allFieldsFilled =
                nameField.getText() != null && !nameField.getText().trim().isEmpty() &&
                        surnameField.getText() != null && !surnameField.getText().trim().isEmpty() &&
                        cnpField.getText() != null && !cnpField.getText().trim().isEmpty() &&
                        phoneField.getText() != null && !phoneField.getText().trim().isEmpty() &&
                        emailField.getText() != null && !emailField.getText().trim().isEmpty();

        if (saveButton != null) {
            saveButton.setDisable(!allFieldsFilled);
        }

        if (statusLabel != null) {
            if (allFieldsFilled) {
                statusLabel.setText("✅ Ready to save patient");
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("📝 Fill in all required fields");
                statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: normal;");
            }
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