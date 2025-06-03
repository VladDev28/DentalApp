package org.example;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.Replication.ReplicationService;

public class App extends Application
{
    @Override
    public void start(Stage stage) throws Exception {

        ReplicationService.initialize();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("Dental App Login");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> {
            ReplicationService.shutdown();
        });
    }

    public static void main( String[] args )
    {
        launch();
    }

}
