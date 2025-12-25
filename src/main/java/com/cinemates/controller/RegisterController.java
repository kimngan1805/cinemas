package com.cinemates.controller;

import com.cinemates.App;
import com.cinemates.model.User;
import com.cinemates.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;

public class RegisterController {
    @FXML private VBox vboxRegister, vboxLogin;
    @FXML private Label lblTitle, lblSubtitle;
    @FXML private TextField txtRegUser, txtRegEmail, txtLogUser;
    @FXML private PasswordField txtRegPass, txtRegConfirm, txtLogPass;

    @FXML private void showLoginForm() { vboxRegister.setVisible(false); vboxRegister.setManaged(false); vboxLogin.setVisible(true); vboxLogin.setManaged(true); }
    @FXML private void showRegisterForm() { vboxLogin.setVisible(false); vboxLogin.setManaged(false); vboxRegister.setVisible(true); vboxRegister.setManaged(true); }

    @FXML
    private void handleLoginAction(ActionEvent event) {
        String userStr = txtLogUser.getText();
        String pass = txtLogPass.getText();

        int userId = DatabaseHandler.checkLogin(userStr, pass); // Lấy ID thay vì boolean
        if (userId != -1) {
            // Tạo đối tượng User hoàn chỉnh
            User loggedInUser = new User(userId, userStr, true);
            goToHome(event, loggedInUser);
        } else {
            System.out.println("❌ Sai tên hoặc mật khẩu rồi vợ ơi!");
        }
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        String user = txtRegUser.getText();
        String email = txtRegEmail.getText();
        String pass = txtRegPass.getText();

        if (DatabaseHandler.registerUser(user, email, pass)) {
            int userId = DatabaseHandler.checkLogin(user, pass);
            goToHome(event, new User(userId, user, true));
        }
    }

    private void goToHome(ActionEvent event, User user) {
        try {
            App.currentUser = user;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Home.fxml"));
            Parent root = loader.load();
            HomeController homeController = loader.getController();
            homeController.setLoggedInUser(user); // Truyền nguyên con User qua Home

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleBackToHome(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/Home.fxml"))));
    }
}