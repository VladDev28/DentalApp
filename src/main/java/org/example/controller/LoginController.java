package org.example.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import org.example.database.Database;
import org.example.dao.PatientDAO;
import org.example.model.Patient;
import org.example.manager.SceneManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        try (Connection conn = Database.getUserReadConnection()) {
            String sql = "SELECT password, role FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                String role = rs.getString("role");

                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword);
                if (result.verified) {
                    errorLabel.setText("");

                    if ("admin".equalsIgnoreCase(role)) {
                        SceneManager.switchScene(event, "/fxml/admin.fxml", "Hospital Management - Admin Dashboard");
                    } else if ("user".equalsIgnoreCase(role)) {
                        FXMLLoader loader = SceneManager.switchScene(event, "/fxml/user.fxml", "Hospital Management - Patient Portal");

                        UserController userController = loader.getController();

                        Patient patient = PatientDAO.findByCnpForUser(username);
                        if (patient != null) {
                            userController.setPatient(patient);
                        }

                    } else {
                        errorLabel.setText("Unknown user role. Please contact administration.");
                    }

                } else {
                    errorLabel.setText("Invalid credentials.");
                }
            } else {
                errorLabel.setText("Invalid credentials.");
            }
        } catch (SQLException e) {
            errorLabel.setText("Database error. Please try again later.");
            e.printStackTrace();
        } catch (IOException e) {
            errorLabel.setText("Error loading interface. Please contact support.");
            e.printStackTrace();
        }
    }

    @FXML
    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        errorLabel.setText("");
    }
}