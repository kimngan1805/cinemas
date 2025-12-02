package com.cinemates.controller;

import com.cinemates.ClientSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private TextField serverIpField;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        loginBtn.setOnAction(e -> handleLogin(e));
    }

    private void handleLogin(javafx.event.ActionEvent event) {
        String name = usernameField.getText();
        String ip = serverIpField.getText();

        if (name.isEmpty() || ip.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đủ thông tin!");
            return;
        }

        // Kết nối thử tới Server
        boolean success = ClientSession.connect(ip, 5000, name);

        if (success) {
            System.out.println("Đăng nhập thành công!");
            try {
                // Chuyển sang màn hình chính (View.fxml)
                Parent mainView = FXMLLoader.load(getClass().getResource("/view.fxml"));
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(new Scene(mainView));
                window.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            errorLabel.setText("Không thể kết nối Server! Kiểm tra lại IP.");
        }
    }
}