package org.example.controller;

import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import org.example.database.Database;
import org.example.model.Patient;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.awt.*;
import java.io.ByteArrayInputStream;

public class UserController {
    @FXML private Label nameLabel, cnpLabel, phoneLabel, emailLabel;
    @FXML private ImageView xrayImage;

    private Patient currentPatient;

    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        nameLabel.setText(patient.getName() + " " + patient.getSurname());
        cnpLabel.setText("CNP: " + patient.getCnp());
        phoneLabel.setText("Phone: " + patient.getPhone());
        emailLabel.setText("Email: " + patient.getEmail());

        if (patient.getXray() != null) {
            Image image = new Image(new ByteArrayInputStream(patient.getXray()));
            xrayImage.setImage(image);
        }
    }
}
