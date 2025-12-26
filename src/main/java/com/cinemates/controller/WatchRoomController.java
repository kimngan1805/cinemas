package com.cinemates.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
import java.util.List;

public class WatchRoomController {
    @FXML private Label lblRoomCode;
    @FXML private MediaView watchMediaView;
    @FXML private VBox vboxChatMessages, vboxParticipants;
    @FXML private TextField txtChatMessage;
    @FXML private Button btnPlayPause;

    private MediaPlayer mediaPlayer;
    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean isRunning = true; // Cờ kiểm soát vòng lặp server

    // KHỞI TẠO PHÒNG
    public void initRoomData(String movieName, List<String> friends) {
        // 1. Tạo mã phòng ngẫu nhiên
        lblRoomCode.setText("CIN-" + (int)(Math.random() * 899 + 100) + "-MATES");
        vboxParticipants.getChildren().clear();
        Label me = new Label("• Bạn (Chủ phòng)");
        me.setStyle("-fx-text-fill: #00ff40; -fx-font-weight: bold;");
        vboxParticipants.getChildren().add(me);
        if (friends != null) {
            for (String friendName : friends) {
                Label lbl = new Label("• " + friendName);
                lbl.setStyle("-fx-text-fill: white;");
                vboxParticipants.getChildren().add(lbl);
            }
        }
        // 2. Load Video mẫu (Dùng link web để test)
        try {
            Media media = new Media("https://www.w3schools.com/html/mov_bbb.mp4");
            mediaPlayer = new MediaPlayer(media);
            watchMediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.play();
        } catch (Exception e) { System.err.println("Lỗi tải Media: " + e.getMessage()); }

        // 3. Hiển thị danh sách bạn bè
        vboxParticipants.getChildren().clear();
        vboxParticipants.getChildren().add(new Label("Bạn (Chủ phòng)"));
        if (friends != null) {
            friends.forEach(f -> {
                Label friend = new Label("• " + f); friend.setStyle("-fx-text-fill: #00ff40;");
                vboxParticipants.getChildren().add(friend);
            });
        }

        startServer(); // Mở cổng 5000 đợi bạn bè kết nối
    }

    // CHỨC NĂNG COPY MÃ PHÒNG
    @FXML
    private void handleCopyCode() {
        String code = lblRoomCode.getText();
        if (code != null && !code.isEmpty()) {
            try {
                ClipboardContent content = new ClipboardContent();
                content.putString(code);
                Clipboard.getSystemClipboard().setContent(content);
                System.out.println("✅ Đã copy vào bộ nhớ: " + code);
                if (txtChatMessage != null) {
                    txtChatMessage.setPromptText("Đã copy: " + code);
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi copy: " + e.getMessage());
            }
        }
    }

    // ĐỒNG BỘ PLAY/PAUSE
    @FXML
    private void handleTogglePlay() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause(); btnPlayPause.setText("▶");
            if (out != null) out.println("CMD:PAUSE");
        } else {
            mediaPlayer.play(); btnPlayPause.setText("⏸");
            if (out != null) out.println("CMD:PLAY");
        }
    }

    // MỞ SERVER P2P
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5000);
                System.out.println("Server đã khởi động tại port 5000, đang chờ kết nối...");
                while (isRunning && !serverSocket.isClosed()) {
                    try {
                        socket = serverSocket.accept();
                        System.out.println("✅ Có bạn vừa vào phòng!");
                        setupStreams();
                        listenForMessages();
                    } catch (SocketException se) {
                        if (!isRunning || serverSocket.isClosed()) {
                            System.out.println("Server socket đã đóng.");
                            break;
                        }
                        se.printStackTrace();
                    }
                }
            } catch (IOException e) { System.err.println("Lỗi Server: " + e.getMessage()); }
        }).start();
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // LẮNG NGHE LỆNH TỪ MÁY BẠN
    private void listenForMessages() {
        new Thread(() -> {
            try {
                String line;
                while (isRunning && (line = in.readLine()) != null) {
                    final String msg = line;
                    Platform.runLater(() -> {
                        if (msg.startsWith("CHAT:")) updateChatUI("Bạn: " + msg.substring(5));
                        else if (msg.equals("CMD:PAUSE")) { if(mediaPlayer!=null) mediaPlayer.pause(); btnPlayPause.setText("▶"); }
                        else if (msg.equals("CMD:PLAY")) { if(mediaPlayer!=null) mediaPlayer.play(); btnPlayPause.setText("⏸"); }
                    });
                }
            } catch (IOException e) { System.err.println("Mất kết nối với bạn!"); }
        }).start();
    }

    @FXML
    private void handleSendMessage() {
        String msg = txtChatMessage.getText();
        if (msg != null && !msg.isEmpty() && out != null) {
            out.println("CHAT:" + msg);
            updateChatUI("Tôi: " + msg);
            txtChatMessage.clear();
        }
    }

    private void updateChatUI(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.1); -fx-padding: 8; -fx-background-radius: 5;");
        vboxChatMessages.getChildren().add(lbl);
    }

    @FXML
    private void handleLeaveRoom(ActionEvent event) throws IOException {
        isRunning = false; // Dừng các vòng lặp
        if (mediaPlayer != null) mediaPlayer.stop();
        if (socket != null && !socket.isClosed()) socket.close();
        if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/Home.fxml"))));
    }
}