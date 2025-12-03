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

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private TextField serverIpField;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;

    // --- CẤU HÌNH SERVER PINGGY ---
    // 1. Địa chỉ Pinggy (Giữ nguyên cái của Ngân)
    private static final String SERVER_HOST = "vumfo-1-52-23-46.a.free.pinggy.link";

    // 2. SỐ PORT MỚI (Xem trên Terminal rồi điền vào đây nha!)
    private static final int SERVER_PORT = 42717;

    @FXML
    public void initialize() {
        loginBtn.setOnAction(e -> handleLogin(e));
    }

    private void handleLogin(javafx.event.ActionEvent event) {
        String name = usernameField.getText();

        if (name.isEmpty()) {
            errorLabel.setText("Vui lòng nhập tên!");
            return;
        }

        ClientSession.myUsername = name;

        // --- GỬI BÁO CÁO "GIẢ LẬP" (SIMULATION) ---
        new Thread(() -> {
            try {
                // TẠO IP GIẢ NGẪU NHIÊN
                // Nó sẽ ra kiểu: 192.168.1.45 hoặc 10.0.0.99 ...
                Random rand = new Random();
                String fakeIp = "192.168." + rand.nextInt(5) + "." + (rand.nextInt(200) + 10);

                System.out.println("⏳ Simulation Mode: Sending IP " + fakeIp + " to Server...");

                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    // Gửi lệnh
                    out.println("REPORT " + name + " " + fakeIp);

                    Thread.sleep(500); // Đợi xíu cho gửi xong
                }
            } catch (Exception ex) {
                System.out.println("❌ Lỗi gửi báo cáo (Check lại Port nha): " + ex.getMessage());
            }
        }).start();

        // --- VÀO APP ---
        try {
            Parent mainView = FXMLLoader.load(getClass().getResource("/view.fxml"));
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(new Scene(mainView));
            window.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}