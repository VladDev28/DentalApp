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

        try (Connection conn = Database.getMasterConnection()) { // Always authenticate against master
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

                    SceneManager.switchScene(event, "/fxml/admin.fxml", "Hospital Management - Admin Dashboard");

                } else {
                    errorLabel.setText("Invalid credentials.");
                }
            } else {
                errorLabel.setText("Invalid credentials.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            SceneManager.switchScene(event, "/fxml/login.fxml", "Dentist Office");
        } catch (Exception e) {
            errorLabel.setText("Error loading registration page.");
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