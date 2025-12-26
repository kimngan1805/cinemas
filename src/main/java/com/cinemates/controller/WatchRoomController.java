package com.cinemates.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.sound.sampled.*;
import java.util.ArrayList;
import java.io.*;
import java.net.*;
import java.util.List;

public class WatchRoomController {
    @FXML private Label lblRoomCode;
    @FXML private MediaView watchMediaView;
    @FXML private VBox vboxChatMessages, vboxParticipants;
    @FXML private TextField txtChatMessage;
    @FXML private Button btnPlayPause, btnMicrophone;

    private MediaPlayer mediaPlayer;
    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean isRunning = true;
    private Timeline syncTimeline;
    private int syncCount = 0;
    @FXML private Slider sliderProgress;
    @FXML private Label lblCurrentTime, lblTotalTime;
    private boolean isDragging = false;
    private List<PrintWriter> allClients = new ArrayList<>();

    // VOICE CHAT
    private TargetDataLine microphone;
    private SourceDataLine speaker;
    private volatile boolean isRecording = false;
    @FXML private Button  btnVolume; // Thêm btnVolume vào đây
    private boolean isMuted = false;

    // KHỞI TẠO PHÒNG
    public void initRoomData(String movieName, List<String> friends, String roomCode, boolean isHost) {
        lblRoomCode.setText(roomCode);

        vboxParticipants.getChildren().clear();

        // ⭐ SỬA: XỬ LÝ CẢ TRƯỜNG HỢP GUEST (chưa đăng nhập)
        String myName;
        if (com.cinemates.App.currentUser != null) {
            myName = com.cinemates.App.currentUser.getUsername();
        } else {
            myName = "Guest-" + (int)(Math.random() * 9999); // Tạo tên tạm cho khách
        }

        if (isHost) {
            Label me = new Label("• " + myName + " (Chủ phòng)");
            me.setStyle("-fx-text-fill: #00ff40; -fx-font-weight: bold;");
            vboxParticipants.getChildren().add(me);

            if (friends != null) {
                friends.forEach(f -> {
                    Label friend = new Label("• " + f);
                    friend.setStyle("-fx-text-fill: white;");
                    vboxParticipants.getChildren().add(friend);
                });
            }
            startServer();
            startSyncHeartbeat();
        } else {
            vboxParticipants.getChildren().add(new Label("• Đang kết nối tới chủ phòng..."));
        }

        // Load video
        try {
            String path = getClass().getResource("/videos/video1.mp4").toExternalForm();
            Media media = new Media(path);
            mediaPlayer = new MediaPlayer(media);
            watchMediaView.setMediaPlayer(mediaPlayer);

            mediaPlayer.setOnReady(() -> {
                sliderProgress.setMax(mediaPlayer.getTotalDuration().toSeconds());
                lblTotalTime.setText(formatTime(mediaPlayer.getTotalDuration()));
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!sliderProgress.isValueChanging()) {
                    sliderProgress.setValue(newTime.toSeconds());
                    lblCurrentTime.setText(formatTime(newTime));
                }
            });

            if (isHost) {
                sliderProgress.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (sliderProgress.isValueChanging()) {
                        mediaPlayer.seek(Duration.seconds(newVal.doubleValue()));
                        broadcast("CMD:SEEK|" + newVal.doubleValue());
                    }
                });
            }

            // CHỈ CHỦ PHÒNG MỚI TỰ PLAY, KHÁCH ĐỢI SYNC
            if (isHost) {
                mediaPlayer.play();
            }

        } catch (Exception e) {
            System.err.println("Lỗi tải video local: " + e.getMessage());
        }
    }

    private String formatTime(Duration elapsed) {
        int minutes = (int) elapsed.toMinutes();
        int seconds = (int) elapsed.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @FXML
    private void handleRewind() {
        if (mediaPlayer != null && serverSocket != null) {
            double newTime = mediaPlayer.getCurrentTime().toSeconds() - 10;
            mediaPlayer.seek(Duration.seconds(Math.max(0, newTime)));
            broadcast("CMD:SEEK|" + Math.max(0, newTime));
        }
    }

    @FXML
    private void handleForward() {
        if (mediaPlayer != null && serverSocket != null) {
            double newTime = mediaPlayer.getCurrentTime().toSeconds() + 10;
            mediaPlayer.seek(Duration.seconds(newTime));
            broadcast("CMD:SEEK|" + newTime);
        }
    }

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

        boolean isHost = (serverSocket != null);

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            btnPlayPause.setText("▶");

            if (isHost) {
                broadcast("CMD:PAUSE");
            }
        } else {
            mediaPlayer.play();
            btnPlayPause.setText("⏸");

            if (isHost) {
                broadcast("CMD:PLAY");
            }
        }
    }

    // ==================== VOICE CHAT ====================

    @FXML
    private void handleToggleMicrophone() {
        if (isRecording) {
            stopVoiceChat();
            btnMicrophone.setText("🎤");
            btnMicrophone.setStyle("-fx-background-color: #444;");
        } else {
            startVoiceChat();
            btnMicrophone.setText("🔴");
            btnMicrophone.setStyle("-fx-background-color: #ff0000;");
        }
    }

    private void startVoiceChat() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
                microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
                microphone.open(format);
                microphone.start();
                isRecording = true;

                System.out.println("🎤 Đã bắt đầu ghi âm...");

                byte[] buffer = new byte[1024];
                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        sendAudioData(buffer, bytesRead);
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi mic: " + e.getMessage());
                Platform.runLater(() -> {
                    btnMicrophone.setText("🎤");
                    btnMicrophone.setStyle("-fx-background-color: #444;");
                });
            }
        }).start();
    }

    private void sendAudioData(byte[] data, int length) {
        try {
            // ⭐ SỬA TẠI ĐÂY
            String myName;
            if (com.cinemates.App.currentUser != null) {
                myName = com.cinemates.App.currentUser.getUsername();
            } else {
                myName = "Guest";
            }

            String encoded = java.util.Base64.getEncoder().encodeToString(
                    java.util.Arrays.copyOf(data, length)
            );

            if (serverSocket != null) {
                broadcast("VOICE:" + myName + "|" + encoded);
            } else if (out != null) {
                out.println("VOICE:" + myName + "|" + encoded);
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi audio: " + e.getMessage());
        }
    }

    private void stopVoiceChat() {
        isRecording = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            System.out.println("🎤 Đã dừng ghi âm.");
        }
    }

    private void playReceivedAudio(byte[] data) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine tempSpeaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                tempSpeaker.open(format);
                tempSpeaker.start();
                tempSpeaker.write(data, 0, data.length);
                tempSpeaker.drain();
                tempSpeaker.close();
            } catch (Exception e) {
                System.err.println("❌ Lỗi phát audio: " + e.getMessage());
            }
        }).start();
    }

    // ==================== END VOICE CHAT ====================

    // MỞ SERVER P2P
    private void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5000);
                System.out.println("🚀 Server chủ phòng đã mở tại port 5000...");
                while (isRunning && !serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("✅ Có thêm một bạn vừa vào phòng!");

                    PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
                    allClients.add(clientOut);

                    String myName;
                    if (com.cinemates.App.currentUser != null) {
                        myName = com.cinemates.App.currentUser.getUsername();
                    } else {
                        myName = "Guest-" + (int)(Math.random() * 9999);
                    }
                    clientOut.println("CMD:UPDATE_USERS|" + myName + " (Chủ phòng)");

                    // ⭐ SỬA: GỬI INITIAL_SYNC NGAY KHI GUEST VÀO
                    if (mediaPlayer != null) {
                        // Đợi 500ms để đảm bảo Guest đã sẵn sàng nhận
                        new Thread(() -> {
                            try {
                                Thread.sleep(500);
                                double currentTime = mediaPlayer.getCurrentTime().toSeconds();
                                String status = (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) ? "PLAYING" : "PAUSED";

                                System.out.println("📤 Host tự động gửi INITIAL_SYNC: " + currentTime + "s, " + status);
                                clientOut.println("CMD:INITIAL_SYNC|" + currentTime + "|" + status);
                                clientOut.flush();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }

                    BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    new Thread(() -> listenToClient(clientIn, clientOut)).start();
                }
            } catch (IOException e) {
                if (isRunning) System.err.println("Lỗi Server: " + e.getMessage());
            }
        }).start();
    }

    private void listenToClient(BufferedReader clientIn, PrintWriter clientOut) {
        try {
            String line;
            while (isRunning && (line = clientIn.readLine()) != null) {
                final String msg = line;
                System.out.println("📩 Host nhận từ Guest: " + msg); // ⭐ THÊM LOG

                Platform.runLater(() -> {
                    // ⭐ XỬ LÝ REQUEST_SYNC
                    if (msg.equals("CMD:REQUEST_SYNC") && mediaPlayer != null) {
                        double currentTime = mediaPlayer.getCurrentTime().toSeconds();
                        String status = (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) ? "PLAYING" : "PAUSED";

                        System.out.println("📤 Host gửi INITIAL_SYNC theo yêu cầu: " + currentTime + "s, " + status);

                        clientOut.println("CMD:INITIAL_SYNC|" + currentTime + "|" + status);
                        clientOut.flush();
                    }
                    else if (msg.startsWith("CHAT:")) {
                        String chatContent = msg.substring(5);
                        updateChatUI(chatContent);
                        broadcast(msg);
                    }
                    else if (msg.startsWith("VOICE:")) {
                        String[] parts = msg.substring(6).split("\\|", 2);
                        String senderName = parts[0];
                        String encoded = parts[1];
                        byte[] audioData = java.util.Base64.getDecoder().decode(encoded);
                        playReceivedAudio(audioData);
                        broadcast(msg);
                        System.out.println("🔊 Đang phát audio từ: " + senderName);
                    }
                    else if (msg.startsWith("CMD:GUEST_LEFT|")) {
                        broadcast(msg);
                        String guestName = msg.split("\\|")[1];
                        String timeNow = java.time.LocalTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                        );
                        updateChatUI("🚪 " + guestName + " đã rời phòng lúc " + timeNow);

                        vboxParticipants.getChildren().removeIf(node ->
                                node instanceof Label && ((Label) node).getText().contains(guestName)
                        );
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("⚠️ Mất kết nối với 1 khách!");
            e.printStackTrace(); // ⭐ THÊM LOG
        }
    }

    // LẮNG NGHE LỆNH TỪ CHỦ PHÒNG (DÀNH CHO KHÁCH)
    private void listenForMessages() {
        new Thread(() -> {
            try {
                String line;
                while (isRunning && (line = in.readLine()) != null) {
                    final String msg = line;
                    System.out.println("📩 Guest nhận: " + msg); // Debug log

                    Platform.runLater(() -> {
                        // 1. CHỦ PHÒNG THOÁT
                        if (msg.equals("CMD:HOST_LEFT")) {
                            isRunning = false;
                            if (mediaPlayer != null) mediaPlayer.stop();

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Thông báo");
                            alert.setHeaderText("Hố hố hố, chủ phòng chạy mất rồi!");
                            alert.setContentText("Phòng phim đã đóng. Nhấn OK để về trang chủ nhé.");

                            alert.showAndWait().ifPresent(response -> {
                                try {
                                    Parent root = FXMLLoader.load(getClass().getResource("/Home.fxml"));
                                    Stage stage = (Stage) watchMediaView.getScene().getWindow();
                                    stage.setScene(new Scene(root));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                        // ⭐ 2. ĐỒNG BỘ LẦN ĐẦU (QUAN TRỌNG NHẤT)
                        else if (msg.startsWith("CMD:INITIAL_SYNC|")) {
                            System.out.println("🎯 Đang xử lý INITIAL_SYNC...");

                            String[] parts = msg.split("\\|");
                            if (parts.length < 3) {
                                System.err.println("⚠️ INITIAL_SYNC thiếu dữ liệu: " + msg);
                                return;
                            }

                            double hostTime = Double.parseDouble(parts[1]);
                            String hostStatus = parts[2];

                            if (mediaPlayer != null) {
                                System.out.println("⏱️ Đồng bộ: " + hostTime + "s, Status: " + hostStatus);

                                // Seek đến vị trí của Host
                                mediaPlayer.seek(Duration.seconds(hostTime));

                                // Đồng bộ trạng thái play/pause
                                if (hostStatus.equals("PLAYING")) {
                                    mediaPlayer.play();
                                    btnPlayPause.setText("⏸");
                                    System.out.println("▶ Video đang phát");
                                } else {
                                    mediaPlayer.pause();
                                    btnPlayPause.setText("▶");
                                    System.out.println("⏸ Video đang dừng");
                                }

                                System.out.println("✅ Đã đồng bộ thành công!");
                            } else {
                                System.err.println("❌ MediaPlayer chưa sẵn sàng!");
                            }
                        }
                        // 3. CƯỠNG CHẾ ĐỒNG BỘ (30s một lần)
                        else if (msg.startsWith("CMD:FORCE_SYNC|")) {
                            double hostTime = Double.parseDouble(msg.split("\\|")[1]);
                            if (mediaPlayer != null) {
                                mediaPlayer.seek(Duration.seconds(hostTime + 0.25));
                                System.out.println("🚀 Cưỡng chế đồng bộ: " + hostTime + "s");
                            }
                        }
                        // 4. ĐỒNG BỘ NHẸ (3s một lần)
                        else if (msg.startsWith("CMD:SYNC|")) {
                            double hostTime = Double.parseDouble(msg.split("\\|")[1]);

                            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                                double myTime = mediaPlayer.getCurrentTime().toSeconds();

                                if (Math.abs(hostTime - myTime) > 2.5) {
                                    System.out.println("🔄 Đuổi kịp Host: " + hostTime + "s");
                                    mediaPlayer.seek(Duration.seconds(hostTime));
                                }
                            }
                        }
                        // 5. PAUSE
                        else if (msg.equals("CMD:PAUSE")) {
                            if (mediaPlayer != null) {
                                mediaPlayer.pause();
                                btnPlayPause.setText("▶");
                                System.out.println("⏸ Host đã pause");
                            }
                        }
                        // 6. PLAY
                        else if (msg.equals("CMD:PLAY")) {
                            if (mediaPlayer != null) {
                                mediaPlayer.play();
                                btnPlayPause.setText("⏸");
                                System.out.println("▶ Host đã play");
                            }
                        }
                        // 7. TUA (SEEK)
                        else if (msg.startsWith("CMD:SEEK|")) {
                            double seekTime = Double.parseDouble(msg.split("\\|")[1]);
                            if (mediaPlayer != null) {
                                mediaPlayer.seek(Duration.seconds(seekTime));
                                System.out.println("⏩ Host tua đến: " + seekTime + "s");
                            }
                        }
                        // 8. CẬP NHẬT DANH SÁCH USER
                        else if (msg.startsWith("CMD:UPDATE_USERS|")) {
                            String[] userList = msg.split("\\|")[1].split(",");
                            vboxParticipants.getChildren().clear();

                            for (String name : userList) {
                                Label lbl = new Label("• " + name.trim());
                                if (name.contains("(Chủ phòng)")) {
                                    lbl.setStyle("-fx-text-fill: #00ff40; -fx-font-weight: bold;");
                                } else {
                                    lbl.setStyle("-fx-text-fill: white;");
                                }
                                vboxParticipants.getChildren().add(lbl);
                            }

                            Label status = new Label("• Đã kết nối P2P thành công!");
                            status.setStyle("-fx-text-fill: #555; -fx-font-size: 10px;");
                            vboxParticipants.getChildren().add(status);
                        }
                        // 9. KHÁCH KHÁC THOÁT
                        else if (msg.startsWith("CMD:GUEST_LEFT|")) {
                            String guestName = msg.split("\\|")[1];
                            String timeNow = java.time.LocalTime.now().format(
                                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                            );
                            updateChatUI("🚪 " + guestName + " đã rời phòng lúc " + timeNow);

                            vboxParticipants.getChildren().removeIf(node ->
                                    node instanceof Label && ((Label) node).getText().contains(guestName)
                            );
                        }
                        // 10. NHẬN CHAT
                        else if (msg.startsWith("CHAT:")) {
                            updateChatUI(msg.substring(5));
                        }
                        // 11. NHẬN AUDIO
                        else if (msg.startsWith("VOICE:")) {
                            String[] parts = msg.substring(6).split("\\|", 2);
                            String senderName = parts[0];
                            String encoded = parts[1];
                            byte[] audioData = java.util.Base64.getDecoder().decode(encoded);
                            playReceivedAudio(audioData);
                            System.out.println("🔊 Phát audio từ: " + senderName);
                        }
                    });
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("⚠️ Mất kết nối P2P hoặc phòng đã đóng!");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @FXML
    private void handleSendMessage() {
        String msg = txtChatMessage.getText();
        if (msg != null && !msg.isEmpty()) {
            // ⭐ SỬA TẠI ĐÂY
            String myName;
            if (com.cinemates.App.currentUser != null) {
                myName = com.cinemates.App.currentUser.getUsername();
            } else {
                myName = "Guest-" + lblRoomCode.getText().hashCode(); // Dùng mã phòng để tạo tên nhất quán
            }

            String fullMessage = myName + ": " + msg;

            if (serverSocket != null) {
                broadcast("CHAT:" + fullMessage);
                updateChatUI(fullMessage);
            } else if (out != null) {
                out.println("CHAT:" + fullMessage);
                updateChatUI(fullMessage);
            }

            txtChatMessage.clear();
        }
    }

    private void updateChatUI(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.1); -fx-padding: 8; -fx-background-radius: 5;");
        vboxChatMessages.getChildren().add(lbl);
    }

    @FXML
    public void connectToHost(String hostIp) {
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Đợi UI load xong
                socket = new Socket(hostIp, 5000);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                System.out.println("✅ Đã kết nối P2P tới: " + hostIp + ":5000");

                Platform.runLater(() -> {
                    vboxParticipants.getChildren().removeIf(node ->
                            node instanceof Label && ((Label) node).getText().contains("Đang kết nối"));

                    Label status = new Label("• Đã kết nối với Chủ phòng!");
                    status.setStyle("-fx-text-fill: #00ff40; -fx-font-style: italic;");
                    vboxParticipants.getChildren().add(status);
                });

                // ⭐ QUAN TRỌNG: GỬI REQUEST SYNC NGAY SAU KHI KẾT NỐI
                System.out.println("📡 Đang yêu cầu đồng bộ từ Host...");
                out.println("CMD:REQUEST_SYNC");
                out.flush();

                // Bắt đầu lắng nghe lệnh từ Host
                listenForMessages();

            } catch (Exception e) {
                System.err.println("❌ Lỗi kết nối P2P: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi kết nối");
                    alert.setHeaderText("Không thể kết nối tới phòng");
                    alert.setContentText("Host có thể đã thoát hoặc IP không đúng: " + hostIp);
                    alert.show();
                });
            }
        }).start();
    }


    private void startSyncHeartbeat() {
        if (syncTimeline != null) syncTimeline.stop();

        syncTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                double hostSeconds = mediaPlayer.getCurrentTime().toSeconds();
                broadcast("CMD:SYNC|" + hostSeconds);

                syncCount++;
                if (syncCount >= 10) {
                    broadcast("CMD:FORCE_SYNC|" + hostSeconds);
                    syncCount = 0;
                }
            }
        }));
        syncTimeline.setCycleCount(Timeline.INDEFINITE);
        syncTimeline.play();
    }

    @FXML
    private void handleLeaveRoom(ActionEvent event) throws IOException {
        if (isRecording) {
            stopVoiceChat();
        }

        if (serverSocket != null) {
            broadcast("CMD:HOST_LEFT");
        } else if (out != null) {
            // ⭐ SỬA TẠI ĐÂY
            String myName;
            if (com.cinemates.App.currentUser != null) {
                myName = com.cinemates.App.currentUser.getUsername();
            } else {
                myName = "Guest";
            }

            out.println("CMD:GUEST_LEFT|" + myName);
            out.flush();
        }

        if (RegisterController.globalOut != null) {
            RegisterController.globalOut.writeObject("CLOSE_ROOM|" + lblRoomCode.getText());
        }

        cleanupAndGoHome(event);
    }

    private void cleanupAndGoHome(ActionEvent event) throws IOException {
        isRunning = false;
        if (mediaPlayer != null) mediaPlayer.stop();
        if (socket != null) socket.close();
        if (serverSocket != null) serverSocket.close();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/Home.fxml"))));
    }

    @FXML
    private void handleToggleVolume() {
        if (mediaPlayer == null) return;

        if (isMuted) {
            // BẬT LẠI TIẾNG
            mediaPlayer.setMute(false);
            btnVolume.setText("🔊");
            btnVolume.setStyle("-fx-background-color: #444;");
            isMuted = false;
            System.out.println("🔊 Đã bật tiếng");
        } else {
            // TẮT TIẾNG
            mediaPlayer.setMute(true);
            btnVolume.setText("🔇");
            btnVolume.setStyle("-fx-background-color: #ff0000;");
            isMuted = true;
            System.out.println("🔇 Đã tắt tiếng");
        }
    }
    private void broadcast(String message) {
        for (PrintWriter clientOut : allClients) {
            try {
                clientOut.println(message);
                clientOut.flush();
            } catch (Exception e) {
                System.err.println("Lỗi gửi tin cho 1 khách, bỏ qua...");
            }
        }
    }

}