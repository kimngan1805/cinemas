package com.cinemates.controller;

import com.cinemates.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;

public class RegisterController {

    @FXML private VBox vboxRegister, vboxLogin;
    @FXML private Label lblTitle, lblSubtitle;
    // Khai báo đúng ID như trong FXML
    @FXML private TextField txtRegUser, txtRegEmail, txtLogUser;
    @FXML private PasswordField txtRegPass, txtRegConfirm, txtLogPass;
    // HIỆN FORM ĐĂNG NHẬP
    @FXML
    private void showLoginForm() {
        vboxRegister.setVisible(false);
        vboxRegister.setManaged(false);
        vboxLogin.setVisible(true);
        vboxLogin.setManaged(true);
        lblTitle.setText("Chào mừng quay lại");
        lblSubtitle.setText("Đăng nhập để xem phim cùng bạn bè");
    }

    // HIỆN FORM ĐĂNG KÝ
    @FXML
    private void showRegisterForm() {
        vboxLogin.setVisible(false);
        vboxLogin.setManaged(false);
        vboxRegister.setVisible(true);
        vboxRegister.setManaged(true);
        lblTitle.setText("Gia nhập Cinemates");
        lblSubtitle.setText("Tạo tài khoản để trải nghiệm phim cùng bạn bè");
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        String user = txtRegUser.getText(); // Lấy tên Ngân vừa nhập
        String email = txtRegEmail.getText();
        String pass = txtRegPass.getText();

        // 1. Lưu vào Database (Sử dụng Handler Ngân đã có)
        if (DatabaseHandler.registerUser(user, email, pass)) {
            System.out.println("✅ Đăng ký thành công!");

            try {
                // 2. Dùng FXMLLoader để nạp trang Home
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Home.fxml"));
                Parent root = loader.load();

                // 3. LẤY CONTROLLER CỦA TRANG HOME
                // Đây là bước quan trọng nhất để truyền tin
                HomeController homeController = loader.getController();

                // 4. Gửi tên người dùng sang trang Home
                homeController.setLoggedInUser(user);

                // 5. Hiển thị trang Home đã được cập nhật
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();

            } catch (IOException e) {
                System.err.println("❌ Lỗi chuyển trang: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("❌ Đăng ký thất bại rồi!");
        }
    }

    @FXML
    private void handleLoginAction(ActionEvent event) throws IOException {
        String user = txtLogUser.getText(); // Lấy tên từ ô đăng nhập
        String pass = txtLogPass.getText();

        // 1. Kiểm tra tài khoản trong Database
        if (DatabaseHandler.checkLogin(user, pass)) {
            System.out.println("✅ Đăng nhập ngon lành!");

            try {
                // 2. Nạp file Home.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Home.fxml"));
                Parent root = loader.load();

                // 3. LẤY CONTROLLER CỦA TRANG HOME để truyền dữ liệu
                HomeController homeController = loader.getController();

                // 4. Gọi hàm hiện tên người dùng lên Navbar
                homeController.setLoggedInUser(user);

                // 5. Hiển thị trang Home
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("❌ Sai tên hoặc mật khẩu rồi vợ ơi!");
            // Ngân có thể thêm Label báo lỗi màu đỏ ở đây cho xinh
        }
    }

    @FXML
    private void handleBackToHome(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/Home.fxml"));
        stage.setScene(new Scene(root));
        stage.show();
    }
}