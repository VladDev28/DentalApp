package org.example.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.example.database.Database;
import org.example.dao.PatientDAO;
import javafx.collections.FXCollections;
import org.example.model.Patient;
import org.example.manager.SceneManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminController implements Initializable {
    @FXML private TextField searchField;
    @FXML private TableView<Patient> patientsTable;
    @FXML private TableColumn<Patient, String> nameCol, surnameCol, cnpCol, phoneCol, emailCol;
    @FXML private Button viewButton, editButton, deleteButton, uploadButton;
    @FXML private Label statusLabel, patientCountLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up table columns

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        surnameCol.setCellValueFactory(new PropertyValueFactory<>("surname"));
        cnpCol.setCellValueFactory(new PropertyValueFactory<>("cnp"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Set up selection listener to enable/disable buttons
        patientsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            if (viewButton != null) viewButton.setDisable(!hasSelection);
            if (editButton != null) editButton.setDisable(!hasSelection);
            if (deleteButton != null) deleteButton.setDisable(!hasSelection);
            if (uploadButton != null) uploadButton.setDisable(!hasSelection);
        });

        loadAllPatients();
    }

    private void loadAllPatients() {
        try {
            List<Patient> patients = PatientDAO.getAll();
            patientsTable.setItems(FXCollections.observableArrayList(patients));
            updatePatientCount(patients.size());
            if (statusLabel != null) {
                statusLabel.setText("Patients loaded successfully");
            }
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Error loading patients: " + e.getMessage());
            }
            showErrorAlert("Database Error", "Failed to load patients", e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        try {
            List<Patient> filtered;
            if (query.isEmpty()) {
                filtered = PatientDAO.getAll();
                if (statusLabel != null) {
                    statusLabel.setText("Showing all patients");
                }
            } else {
                filtered = PatientDAO.searchByNameOrPhone(query);
                if (statusLabel != null) {
                    statusLabel.setText("Search completed - " + filtered.size() + " results");
                }
            }
            patientsTable.setItems(FXCollections.observableArrayList(filtered));
            updatePatientCount(filtered.size());
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Search error: " + e.getMessage());
            }
            showErrorAlert("Search Error", "Failed to search patients", e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        searchField.clear();
        loadAllPatients();
    }

    @FXML
    private void handleAddPatient(ActionEvent event) {
        try {
            SceneManager.openNewWindow("/fxml/add_patient.fxml", "Add New Patient");
            if (statusLabel != null) {
                statusLabel.setText("Add patient window opened");
            }
        } catch (IOException e) {
            showErrorAlert("Error", "Cannot open add patient window", e.getMessage());
        }
    }

    @FXML
    private void handleEditPatient(ActionEvent event) {
        Patient selected = patientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                // Open edit patient dialog/scene
                SceneManager.openNewWindow("/fxml/edit_patient.fxml", "Edit Patient");
                if (statusLabel != null) {
                    statusLabel.setText("Edit patient window opened");
                }
            } catch (IOException e) {
                showErrorAlert("Error", "Cannot open edit patient window", e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeletePatient() {
        Patient selected = patientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Delete");
            confirmAlert.setHeaderText("Delete Patient");
            confirmAlert.setContentText("Are you sure you want to delete patient: " +
                    selected.getName() + " " + selected.getSurname() + "?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    boolean success = PatientDAO.deletePatient(selected.getId());
                    if (success) {
                        if (statusLabel != null) {
                            statusLabel.setText("Patient deleted successfully");
                        }
                        refreshTable();
                    } else {
                        if (statusLabel != null) {
                            statusLabel.setText("Failed to delete patient");
                        }
                    }
                } catch (Exception e) {
                    showErrorAlert("Delete Error", "Failed to delete patient", e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleUploadXray() {
        Patient selected = patientsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select X-Ray Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );

            File selectedFile = fileChooser.showOpenDialog(patientsTable.getScene().getWindow());
            if (selectedFile != null) {
                try {
                    byte[] imageData = Files.readAllBytes(selectedFile.toPath());
                    boolean success = PatientDAO.updatePatientXray(selected.getId(), imageData);

                    if (success) {
                        if (statusLabel != null) {
                            statusLabel.setText("X-Ray uploaded successfully");
                        }
                        showInfoAlert("Success", "X-Ray uploaded successfully for " +
                                selected.getName() + " " + selected.getSurname());
                    } else {
                        if (statusLabel != null) {
                            statusLabel.setText("Failed to upload X-Ray");
                        }
                    }
                } catch (IOException e) {
                    showErrorAlert("Upload Error", "Failed to read image file", e.getMessage());
                } catch (Exception e) {
                    showErrorAlert("Database Error", "Failed to save X-Ray", e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Logout");
        confirmAlert.setHeaderText("Logout");
        confirmAlert.setContentText("Are you sure you want to logout?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                SceneManager.switchScene(event, "/fxml/login.fxml", "Hospital Management - Login");
            } catch (IOException e) {
                showErrorAlert("Error", "Cannot return to login screen", e.getMessage());
            }
        }
    }

    private void updatePatientCount(int count) {
        if (patientCountLabel != null) {
            patientCountLabel.setText("Total Patients: " + count);
        }
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}