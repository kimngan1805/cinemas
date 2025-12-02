package com.cinemates;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Load file giao diện từ thư mục resources
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600); // Kích thước cửa sổ

        stage.setTitle("Cinemates - P2P Watch Party");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}