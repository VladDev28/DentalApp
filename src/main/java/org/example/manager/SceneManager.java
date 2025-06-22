package org.example.manager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.io.IOException;

public class SceneManager {

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

    public static FXMLLoader switchScene(ActionEvent event, String fxmlPath, String title, double width, double height) throws IOException {
        FXMLLoader loader = switchScene(event, fxmlPath, title);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();
        return loader;
    }

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
}
