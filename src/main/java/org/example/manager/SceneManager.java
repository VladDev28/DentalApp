package org.example.manager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.io.IOException;

public class SceneManager {

    /**
     * Switches to a new scene
     * @param event The ActionEvent from the current scene
     * @param fxmlPath Path to the FXML file (e.g., "/fxml/admin.fxml")
     * @param title Window title
     * @return The FXMLLoader used (in case you need to access the controller)
     * @throws IOException if FXML file cannot be loaded
     */
    public static FXMLLoader switchScene(ActionEvent event, String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle(title);
        stage.show();

        return loader;
    }

    /**
     * Switches to a new scene with specific dimensions
     * @param event The ActionEvent from the current scene
     * @param fxmlPath Path to the FXML file
     * @param title Window title
     * @param width Scene width
     * @param height Scene height
     * @return The FXMLLoader used
     * @throws IOException if FXML file cannot be loaded
     */
    public static FXMLLoader switchScene(ActionEvent event, String fxmlPath, String title, double width, double height) throws IOException {
        FXMLLoader loader = switchScene(event, fxmlPath, title);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();
        return loader;
    }

    /**
     * Opens a new window (doesn't close the current one)
     * @param fxmlPath Path to the FXML file
     * @param title Window title
     * @return The FXMLLoader used
     * @throws IOException if FXML file cannot be loaded
     */
    public static FXMLLoader openNewWindow(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        Stage newStage = new Stage();
        newStage.setScene(scene);
        newStage.setTitle(title);
        newStage.show();

        return loader;
    }

    /**
     * Closes the current window
     * @param event The ActionEvent from the current scene
     */
    public static void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
