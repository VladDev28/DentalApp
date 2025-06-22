package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import org.example.dao.PatientDAO;
import org.example.dao.UserDAO;
import org.example.model.Patient;
import org.example.model.User;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ManageUsersController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TableView<UserTableRow> usersTable;
    @FXML private TableColumn<UserTableRow, String> usernameColumn, roleColumn, patientNameColumn, statusColumn;
    @FXML private Button resetPasswordButton, deleteUserButton;
    @FXML private Label statusLabel, userCountLabel;

    // Inner class to represent user data in the table
    public static class UserTableRow {
        private final SimpleStringProperty username;
        private final SimpleStringProperty role;
        private final SimpleStringProperty patientName;
        private final SimpleStringProperty status;

        public UserTableRow(String username, String role, String patientName, String status) {
            this.username = new SimpleStringProperty(username);
            this.role = new SimpleStringProperty(role);
            this.patientName = new SimpleStringProperty(patientName);
            this.status = new SimpleStringProperty(status);
        }

        public String getUsername() { return username.get(); }
        public String getRole() { return role.get(); }
        public String getPatientName() { return patientName.get(); }
        public String getStatus() { return status.get(); }

        public SimpleStringProperty usernameProperty() { return username; }
        public SimpleStringProperty roleProperty() { return role; }
        public SimpleStringProperty patientNameProperty() { return patientName; }
        public SimpleStringProperty statusProperty() { return status; }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadAllUsers();
    }

    private void setupTable() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        patientNameColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            resetPasswordButton.setDisable(!hasSelection);
            deleteUserButton.setDisable(!hasSelection || "admin".equals(newSelection.getRole())); // Can't delete admin users
        });
    }

    @FXML
    private void loadAllUsers() {
        try {
            List<User> users = UserDAO.getAllUsers();
            List<UserTableRow> tableRows = new ArrayList<>();

            for (User user : users) {
                String patientName = "N/A";
                String status = "Active";

                // Try to find associated patient
                if ("user".equals(user.getRole())) {
                    Patient patient = PatientDAO.findByCnp(user.getUsername());
                    if (patient != null) {
                        patientName = patient.getName() + " " + patient.getSurname();
                        status = "Patient Account";
                    }
                } else if ("admin".equals(user.getRole())) {
                    status = "Administrator";
                }

                tableRows.add(new UserTableRow(user.getUsername(), user.getRole(), patientName, status));
            }

            usersTable.setItems(FXCollections.observableArrayList(tableRows));
            updateUserCount(tableRows.size());
            statusLabel.setText("Users loaded successfully");
            statusLabel.setStyle("-fx-text-fill: #27ae60;");

        } catch (Exception e) {
            statusLabel.setText("Error loading users: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            showErrorAlert("Database Error", "Failed to load users", e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadAllUsers();
            return;
        }

        try {
            // Search in users
            List<User> allUsers = UserDAO.searchUsers(query);
            List<UserTableRow> filteredRows = new ArrayList<>();

            for (User user : allUsers) {
                String patientName = "N/A";
                String status = "Active";
                boolean includeRow = false;

                // Check if query matches username or role
                if (user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                        user.getRole().toLowerCase().contains(query.toLowerCase())) {
                    includeRow = true;
                }

                if ("user".equals(user.getRole())) {
                    Patient patient = PatientDAO.findByCnp(user.getUsername());
                    if (patient != null) {
                        patientName = patient.getName() + " " + patient.getSurname();
                        status = "Patient Account";


                        if (patientName.toLowerCase().contains(query.toLowerCase())) {
                            includeRow = true;
                        }
                    }

                } else if ("admin".equals(user.getRole())) {
                    status = "Administrator";
                }

                if (includeRow) {
                    filteredRows.add(new UserTableRow(user.getUsername(), user.getRole(), patientName, status));
                }
            }

            usersTable.setItems(FXCollections.observableArrayList(filteredRows));
            updateUserCount(filteredRows.size());
            statusLabel.setText("Found " + filteredRows.size() + " users matching: " + query);
            statusLabel.setStyle("-fx-text-fill: #27ae60;");

        } catch (Exception e) {
            statusLabel.setText("Search error: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    private void updateUserCount(int count) {
        userCountLabel.setText("Total Users: " + count);
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

}