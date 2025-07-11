package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import org.example.dao.PatientDAO;
import org.example.manager.Scripts;
import org.example.model.Patient;
import org.example.manager.SceneManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserController implements Initializable {
    @FXML private Label welcomeLabel;
    @FXML private Label nameLabel, surnameLabel, cnpLabel, phoneLabel, emailLabel;
    @FXML private ImageView xrayImage;
    @FXML private Label xrayLabel, statusLabel;

    private Patient currentPatient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        xrayImage.setOnMouseClicked(event -> {
            if (xrayImage.getImage() != null && currentPatient != null && currentPatient.getXray() != null) {
                showXrayFullSize();
            }
        });

        xrayImage.setOnMouseEntered(event -> {
            if (currentPatient != null && currentPatient.getXray() != null) {
                xrayImage.setStyle("-fx-cursor: hand;");
            }
        });

        xrayImage.setOnMouseExited(event -> {
            xrayImage.setStyle("-fx-cursor: default;");
        });
    }

    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        if (patient != null) {
            displayPatientInfo();
        } else {
            displayNoPatientInfo();
        }
    }


    private void displayPatientInfo() {
        if (currentPatient == null) return;

        welcomeLabel.setText("Welcome, " + currentPatient.getName() + " " + currentPatient.getSurname());

        nameLabel.setText(currentPatient.getName() != null ? currentPatient.getName() : "N/A");
        surnameLabel.setText(currentPatient.getSurname() != null ? currentPatient.getSurname() : "N/A");
        cnpLabel.setText(currentPatient.getCnp() != null ? currentPatient.getCnp() : "N/A");
        phoneLabel.setText(currentPatient.getPhone() != null ? currentPatient.getPhone() : "N/A");
        emailLabel.setText(currentPatient.getEmail() != null ? currentPatient.getEmail() : "N/A");

        displayXray();

        statusLabel.setText("✅ Information loaded successfully");
        statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 14px;");
    }

    private void displayNoPatientInfo() {
        welcomeLabel.setText("Welcome, Patient");
        nameLabel.setText("N/A");
        surnameLabel.setText("N/A");
        cnpLabel.setText("N/A");
        phoneLabel.setText("N/A");
        emailLabel.setText("N/A");
        xrayLabel.setText("No patient information available");
        xrayLabel.setStyle("-fx-text-fill: #e74c3c;");
        statusLabel.setText("❌ No patient information available");
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
    }


    private void displayXray() {
        if (currentPatient.getXray() != null && currentPatient.getXray().length > 0) {
            try {
                Image image = new Image(new ByteArrayInputStream(currentPatient.getXray()));
                xrayImage.setImage(image);
                xrayLabel.setText("✅ X-ray image available - Click to view full size");
                xrayLabel.setStyle("-fx-text-fill: #27ae60;");
            } catch (Exception e) {
                xrayLabel.setText("❌ Error loading X-ray image");
                xrayLabel.setStyle("-fx-text-fill: #e74c3c;");
                System.err.println("Error loading X-ray image: " + e.getMessage());
            }
        } else {
            xrayImage.setImage(null);
            xrayLabel.setText("ℹ️ No X-ray image available");
            xrayLabel.setStyle("-fx-text-fill: #7f8c8d;");
        }
    }

    private void showXrayFullSize() {
        if (currentPatient == null || currentPatient.getXray() == null || currentPatient.getXray().length == 0) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/xray_viewer.fxml"));
            Stage xrayStage = new Stage();
            xrayStage.setTitle("X-Ray Image - " + currentPatient.getName() + " " + currentPatient.getSurname());
            xrayStage.setScene(new Scene(loader.load()));

            XrayViewerController controller = loader.getController();
            controller.setPatient(currentPatient, false);

            xrayStage.show();

        } catch (Exception e) {
            showErrorAlert("Error", "Cannot display X-ray", "Error displaying X-ray image: " + e.getMessage());
            e.printStackTrace();
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

}