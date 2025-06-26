package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.dao.PatientDAO;
import org.example.dao.UserDAO;
import javafx.collections.FXCollections;
import org.example.model.Patient;
import org.example.model.User;
import org.example.manager.SceneManager;
import org.example.manager.Scripts;

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

    @FXML private MenuBar menuBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        surnameCol.setCellValueFactory(new PropertyValueFactory<>("surname"));
        cnpCol.setCellValueFactory(new PropertyValueFactory<>("cnp"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        patientsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            if (viewButton != null) viewButton.setDisable(!hasSelection);
            if (editButton != null) editButton.setDisable(!hasSelection);
            if (deleteButton != null) deleteButton.setDisable(!hasSelection);
            if (uploadButton != null) uploadButton.setDisable(!hasSelection);
        });

        loadAllPatients();
    }

    Scripts script = new Scripts();

    private void loadAllPatients() {
        try {
            List<Patient> patients = PatientDAO.getAll();
            patientsTable.setItems(FXCollections.observableArrayList(patients));
            updatePatientCount(patients.size());
            if (statusLabel != null) {
                statusLabel.setText("Loaded " + patients.size() + " patients");
            }
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Error loading patients: " + e.getMessage());
            }
            showErrorAlert("Database Error", "Failed to load patients", e.getMessage());
        }
    }

    private void updatePatientCount(int count) {
        if (patientCountLabel != null) {
            patientCountLabel.setText("Total Patients: " + count);
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        try {
            List<Patient> patients;
            if (query.isEmpty()) {
                patients = PatientDAO.getAll();
                if (statusLabel != null) {
                    statusLabel.setText("Showing all patients");
                }
            } else {
                patients = PatientDAO.searchByNameOrPhone(query);
                if (statusLabel != null) {
                    statusLabel.setText("Found " + patients.size() + " patients matching: " + query);
                }
            }
            patientsTable.setItems(FXCollections.observableArrayList(patients));
            updatePatientCount(patients.size());
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("Search error: " + e.getMessage());
            }
            showErrorAlert("Search Error", "Failed to search patients", e.getMessage());
        }
    }

    @FXML
    void refreshTable() {
        searchField.clear();
        loadAllPatients();
        if (statusLabel != null) {
            statusLabel.setText("Patient list refreshed");
        }
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
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showErrorAlert("No Patient Selected", "Please select a patient", "Select a patient from the table first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit_patient.fxml"));
            Stage editStage = new Stage();
            editStage.setTitle("Edit Patient - " + selectedPatient.getName() + " " + selectedPatient.getSurname());
            editStage.setScene(new Scene(loader.load()));

            EditPatientController controller = loader.getController();
            controller.setPatientForEditing(selectedPatient);
            controller.setAdminController(this);

            editStage.show();

            if (statusLabel != null) {
                statusLabel.setText("Edit patient window opened for: " + selectedPatient.getName());
            }
        } catch (IOException e) {
            showErrorAlert("Error", "Cannot open edit patient window", e.getMessage());
        }
    }

    @FXML
    private void handleViewXray(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showErrorAlert("No Patient Selected", "Please select a patient", "Select a patient from the table first.");
            return;
        }

        if (selectedPatient.getXray() == null || selectedPatient.getXray().length == 0) {
            showInfoAlert("No X-Ray Available",
                    "No X-ray image found for patient: " + selectedPatient.getName() + " " + selectedPatient.getSurname() + "\n\n" +
                            "Use the 'Upload X-Ray' button to add an X-ray image.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/xray_viewer.fxml"));
            Stage xrayStage = new Stage();
            xrayStage.setTitle("X-Ray Viewer - Admin View");
            xrayStage.setScene(new Scene(loader.load()));

            XrayViewerController controller = loader.getController();
            controller.setPatient(selectedPatient, true);
            controller.setAdminController(this);

            xrayStage.show();

            if (statusLabel != null) {
                statusLabel.setText("Viewing X-ray for: " + selectedPatient.getName() + " " + selectedPatient.getSurname());
            }

        } catch (IOException e) {
            showErrorAlert("Error", "Cannot open X-ray viewer", e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void handleDeletePatient(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showErrorAlert("No Patient Selected", "Please select a patient", "Select a patient from the table first.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Patient");
        confirmAlert.setContentText("Are you sure you want to delete patient: " +
                selectedPatient.getName() + " " + selectedPatient.getSurname() + "?\n\n" +
                "This action cannot be undone!");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = PatientDAO.deletePatient(selectedPatient.getId());
                if (success) {
                    loadAllPatients();
                    if (statusLabel != null) {
                        statusLabel.setText("Patient deleted successfully");
                    }
                    showInfoAlert("Success", "Patient deleted successfully.");
                } else {
                    if (statusLabel != null) {
                        statusLabel.setText("Failed to delete patient");
                    }
                    showErrorAlert("Delete Failed", "Failed to delete patient", "Please try again.");
                }
            } catch (Exception e) {
                if (statusLabel != null) {
                    statusLabel.setText("Error deleting patient: " + e.getMessage());
                }
                showErrorAlert("Delete Error", "An error occurred", "Error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUploadXray(ActionEvent event) {
        Patient selectedPatient = patientsTable.getSelectionModel().getSelectedItem();
        if (selectedPatient == null) {
            showErrorAlert("No Patient Selected", "Please select a patient", "Select a patient from the table first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select X-Ray Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                long fileSize = selectedFile.length();
                long maxSize = 10 * 1024 * 1024;

                if (fileSize > maxSize) {
                    showErrorAlert("File Too Large", "X-Ray image is too large",
                            "Please select an image smaller than 10MB. Current size: " + (fileSize / 1024 / 1024) + "MB");
                    return;
                }
                byte[] imageData = Files.readAllBytes(selectedFile.toPath());

                boolean success = PatientDAO.updatePatientXray(selectedPatient.getId(), imageData);

                if (success) {

                    selectedPatient.setXray(imageData);

                    if (statusLabel != null) {
                        statusLabel.setText("✅ X-Ray uploaded successfully for " + selectedPatient.getName());
                        statusLabel.setStyle("-fx-text-fill: #27ae60;");
                    }

                    showInfoAlert("Success",
                            "X-Ray image uploaded successfully for patient: " +
                                    selectedPatient.getName() + " " + selectedPatient.getSurname() + "\n\n" +
                                    "File: " + selectedFile.getName() + "\n" +
                                    "Size: " + String.format("%.2f MB", fileSize / 1024.0 / 1024.0));

                    loadAllPatients();

                } else {
                    if (statusLabel != null) {
                        statusLabel.setText("❌ Failed to upload X-Ray");
                        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    }
                    showErrorAlert("Upload Failed", "Failed to upload X-Ray", "Please try again.");
                }
            } catch (IOException e) {
                if (statusLabel != null) {
                    statusLabel.setText("❌ Error reading X-Ray file: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
                showErrorAlert("File Error", "Cannot read the image file", "Error: " + e.getMessage());
            } catch (Exception e) {
                if (statusLabel != null) {
                    statusLabel.setText("❌ Error uploading X-Ray: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
                showErrorAlert("Upload Error", "An error occurred", "Error: " + e.getMessage());
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