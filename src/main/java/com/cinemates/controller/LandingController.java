package com.cinemates.controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
public class LandingController {

    @FXML
    public void handleStartBtn(ActionEvent event) throws IOException {
        System.out.println("Chuyển cảnh sang Gallery...");

        // Dấu / ở đầu nghĩa là tìm ngay trong thư mục resources
        Parent galleryView = FXMLLoader.load(getClass().getResource("/gallery.fxml"));

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene newScene = new Scene(galleryView);
        window.setScene(newScene);
        window.show();
    }
}
