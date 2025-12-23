package com.cinemates.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
import java.util.List;

public class WatchRoomController {
    @FXML private Label lblRoomCode, lblRoomInfo;
    @FXML private VBox vboxChatMessages, vboxParticipants;
    @FXML private TextField txtChatMessage;

    private String myTailscaleIP = "100.x.y.z"; // Ngân lấy từ app Tailscale nha
    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // HÀM NHẬN DỮ LIỆU VÀ KÍCH HOẠT MẠNG
    public void initRoomData(String movieName, List<String> friends) {
        String code = "CIN-" + (int)(Math.random() * 900 + 100) + "-MATES";
        if (lblRoomCode != null) lblRoomCode.setText(code);

        // 1. Nếu Ngân là CHỦ PHÒNG: Mở cổng chờ bạn bè
        startServer();

        // 2. Đăng ký phòng lên Server trung tâm (Render)
        // registerToSignalingServer(code, myTailscaleIP);
    }

    // LUỒNG DÀNH CHO CHỦ PHÒNG (SERVER)
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5000); // Mở port 5000
                System.out.println("📡 Đang đợi bạn bè kết nối qua IP Tailscale...");

                while (true) {
                    socket = serverSocket.accept();
                    setupStreams();
                    listenForMessages();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    // LUỒNG DÀNH CHO BẠN BÈ (CLIENT)
    public void connectToHost(String hostTailscaleIP) {
        new Thread(() -> {
            try {
                socket = new Socket(hostTailscaleIP, 5000); // Kết nối tới IP Tailscale của Ngân
                setupStreams();
                listenForMessages();
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void listenForMessages() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String finalLine = line;
                    // Dùng Platform.runLater để cập nhật UI từ luồng mạng
                    Platform.runLater(() -> {
                        if (finalLine.startsWith("CHAT:")) {
                            updateChatUI("Bạn: " + finalLine.substring(5));
                        }
                    });
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void handleSendMessage() {
        String msg = txtChatMessage.getText();
        if (msg != null && out != null) {
            out.println("CHAT:" + msg); // Gửi qua P2P
            updateChatUI("Tôi: " + msg);
            txtChatMessage.clear();
        }
    }

    private void updateChatUI(String text) {
        vboxChatMessages.getChildren().add(new Label(text));
    }

    @FXML
    private void handleLeaveRoom(ActionEvent event) throws IOException {
        // Đóng kết nối trước khi đi
        if (socket != null) socket.close();
        if (serverSocket != null) serverSocket.close();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/Home.fxml"))));
    }
}