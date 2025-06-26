package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.dao.PatientDAO;
import org.example.model.Patient;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class XrayViewerController implements Initializable {
    @FXML private ImageView xrayImageView;
    @FXML private ScrollPane scrollPane;
    @FXML private Label patientNameLabel;
    @FXML private Label cnpLabel;
    @FXML private Label imageSizeLabel;
    @FXML private Label dimensionsLabel;
    @FXML private Label zoomLabel;
    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Button fitButton;
    @FXML private Button actualSizeButton;
    @FXML private Button deleteButton;
    @FXML private StackPane imageContainer;

    private Patient patient;
    private Image xrayImage;
    private double currentZoom = 1.0;
    private AdminController adminController;
    private boolean isAdminView = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Enable panning in scroll pane
        scrollPane.setPannable(true);

        // Add mouse wheel zoom support
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                handleMouseWheelZoom(event);
                event.consume();
            }
        });

        // Configure delete button visibility (hidden by default, shown for admin)
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);
    }

    public void setPatient(Patient patient, boolean isAdmin) {
        this.patient = patient;
        this.isAdminView = isAdmin;

        if (patient != null) {
            loadPatientInfo();
            loadXrayImage();

            // Show delete button only for admin view
            deleteButton.setVisible(isAdmin);
            deleteButton.setManaged(isAdmin);
        }
    }

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    private void loadPatientInfo() {
        patientNameLabel.setText(patient.getName() + " " + patient.getSurname());
        cnpLabel.setText("CNP: " + patient.getCnp());
    }

    private void loadXrayImage() {
        if (patient.getXray() != null && patient.getXray().length > 0) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(patient.getXray());
                xrayImage = new Image(bis);

                if (xrayImage.isError()) {
                    showError("Failed to load X-ray image");
                    return;
                }

                xrayImageView.setImage(xrayImage);
                xrayImageView.setPreserveRatio(true);
                xrayImageView.setSmooth(true);

                // Update image info
                double sizeInKB = patient.getXray().length / 1024.0;
                imageSizeLabel.setText(String.format("Size: %.1f KB", sizeInKB));
                dimensionsLabel.setText(String.format("Dimensions: %.0f x %.0f px",
                        xrayImage.getWidth(), xrayImage.getHeight()));

                // Initially fit to window
                handleFitToWindow();

            } catch (Exception e) {
                showError("Error loading X-ray: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleZoomIn() {
        setZoom(currentZoom * 1.2);
    }

    @FXML
    private void handleZoomOut() {
        setZoom(currentZoom * 0.8);
    }

    @FXML
    private void handleFitToWindow() {
        if (xrayImage != null) {
            double scrollPaneWidth = scrollPane.getWidth() - 20;
            double scrollPaneHeight = scrollPane.getHeight() - 20;

            double widthRatio = scrollPaneWidth / xrayImage.getWidth();
            double heightRatio = scrollPaneHeight / xrayImage.getHeight();
            double fitRatio = Math.min(widthRatio, heightRatio);

            xrayImageView.setFitWidth(xrayImage.getWidth() * fitRatio);
            xrayImageView.setFitHeight(xrayImage.getHeight() * fitRatio);

            currentZoom = fitRatio;
            updateZoomLabel();
        }
    }

    @FXML
    private void handleActualSize() {
        if (xrayImage != null) {
            xrayImageView.setFitWidth(xrayImage.getWidth());
            xrayImageView.setFitHeight(xrayImage.getHeight());
            currentZoom = 1.0;
            updateZoomLabel();
        }
    }

    @FXML
    private void handleDeleteXray() {
        if (!isAdminView || patient == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete X-Ray Image");
        confirm.setContentText("Are you sure you want to delete the X-ray image for " +
                patient.getName() + " " + patient.getSurname() + "?\n\nThis action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = PatientDAO.updatePatientXray(patient.getId(), null);
                if (success) {
                    patient.setXray(null);

                    // Refresh admin table if reference exists
                    if (adminController != null) {
                        adminController.refreshTable();
                    }

                    // Close the viewer
                    handleClose();

                    showInfo("X-ray image deleted successfully.");
                } else {
                    showError("Failed to delete X-ray image. Please try again.");
                }
            } catch (Exception e) {
                showError("Error deleting X-ray: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) scrollPane.getScene().getWindow();
        stage.close();
    }

    private void handleMouseWheelZoom(ScrollEvent event) {
        double zoomFactor = 1.05;
        double deltaY = event.getDeltaY();

        if (deltaY < 0) {
            setZoom(currentZoom / zoomFactor);
        } else {
            setZoom(currentZoom * zoomFactor);
        }
    }

    private void setZoom(double zoom) {
        // Limit zoom between 10% and 500%
        zoom = Math.max(0.1, Math.min(5.0, zoom));
        currentZoom = zoom;

        if (xrayImage != null) {
            xrayImageView.setFitWidth(xrayImage.getWidth() * zoom);
            xrayImageView.setFitHeight(xrayImage.getHeight() * zoom);
            updateZoomLabel();
        }
    }

    private void updateZoomLabel() {
        zoomLabel.setText(String.format("%.0f%%", currentZoom * 100));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}