package com.cinemates.controller;

import com.cinemates.P2PNetwork;
import com.cinemates.model.Episode;
import com.cinemates.model.Movie;
import com.cinemates.utils.DatabaseHandler;
import com.cinemates.utils.DriveUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {

    // --- FXML VARS ---
    @FXML private MediaView mediaView;
    @FXML private Button playBtn;
    @FXML private Button backBtn;
    @FXML private Label titleLabel;
    @FXML private Slider volumeSlider;
    @FXML private Slider timeSlider;
    @FXML private Label timeLabel;
    @FXML private Button muteBtn;

    @FXML private VBox offlineSidebar;
    @FXML private VBox p2pSidebar;
    @FXML private VBox connectionMenu;
    @FXML private Button btnCreateRoom;
    @FXML private Button btnJoinRoom;
    @FXML private VBox hostPanel;
    @FXML private Label lblRoomId;
    @FXML private Button btnCancelHost;
    @FXML private VBox guestPanel;
    @FXML private TextField ipField;
    @FXML private Button connectBtn;
    @FXML private Button btnCancelGuest;
    @FXML private ListView<String> chatListView;
    @FXML private TextField chatInput;
    @FXML private Button sendBtn;

    // --- DATA VARS ---
    private MediaPlayer mediaPlayer;
    private Movie currentMovie;
    private Duration totalDuration;
    private P2PNetwork p2p;

    private boolean isP2PMode = false;
    private boolean isHost = false;
    private boolean isMuted = false;

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        p2p = new P2PNetwork();
        p2p.setOnMessageReceived(msg -> Platform.runLater(() -> handleIncomingMessage(msg)));

        // FIX LỖI 3: Thoát phòng sạch sẽ
        backBtn.setOnAction(event -> {
            cleanupAndExit(); // Ngắt kết nối mạng
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/detail.fxml"));
                Parent root = loader.load();
                if (currentMovie != null) {
                    DetailController detailController = loader.getController();
                    detailController.setMovieData(currentMovie.getId());
                }
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(new Scene(root));
                window.show();
            } catch (IOException e) { e.printStackTrace(); }
        });

        playBtn.setOnAction(e -> togglePlay());

        // Slider Time (Tua)
        timeSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                double newTime = timeSlider.getValue();
                mediaPlayer.seek(Duration.seconds(newTime));
                if (isP2PMode && isHost) {
                    p2p.send("CMD:SEEK:" + newTime);
                }
            }
        });

        // FIX LỖI 1: Mute đồng bộ
        if (muteBtn != null) {
            muteBtn.setOnAction(e -> {
                if (mediaPlayer == null) return;
                toggleMute(!isMuted); // Đảo ngược trạng thái hiện tại

                // Gửi lệnh cho người khác
                if (isP2PMode && isHost) {
                    p2p.send("CMD:MUTE:" + isMuted);
                }
            });
        }

        setupConnectionLogic();

        sendBtn.setOnAction(e -> {
            String msg = chatInput.getText();
            if (!msg.isEmpty()) {
                p2p.send(msg);
                chatListView.getItems().add("Me: " + msg);
                chatInput.clear();
            }
        });
    }

    // --- HÀM XỬ LÝ TIN NHẮN TỚI ---
    private void handleIncomingMessage(String msg) {
        if (msg.startsWith("CMD:")) {
            if (isP2PMode && !isHost) { // Chỉ Khách mới nghe lệnh Host
                processCommand(msg);
            }
        } else {
            chatListView.getItems().add("Friend: " + msg);
        }
    }

    // --- XỬ LÝ LỆNH ĐỒNG BỘ (NÂNG CẤP) ---
    private void processCommand(String cmd) {
        if (mediaPlayer == null) return;

        // 1. HOST NHẬN ĐƯỢC YÊU CẦU INFO
        if (isHost && cmd.equals("CMD:REQUEST_INFO")) {
            double t = mediaPlayer.getCurrentTime().toSeconds();
            String status = (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) ? "PLAY" : "PAUSE";
            p2p.send("CMD:SYNC_ALL:" + t + ":" + status);
            return;
        }

        // 2. KHÁCH NHẬN LỆNH
        try {
            if (cmd.startsWith("CMD:SYNC_ALL:")) {
                String[] parts = cmd.split(":");
                double seconds = Double.parseDouble(parts[2]);
                String status = parts[3];

                // FIX LỖI 2: Bù thời gian trễ mạng (Latency Compensation)
                // Cộng thêm 0.5s để bù cho thời gian gửi tin
                double compensatedTime = seconds + 0.5;

                mediaPlayer.seek(Duration.seconds(compensatedTime));
                timeSlider.setValue(compensatedTime);

                if (status.equals("PLAY")) {
                    mediaPlayer.play();
                    playBtn.setText("⏸");
                } else {
                    mediaPlayer.pause();
                    playBtn.setText("▶");
                }
            }
            else if (cmd.equals("CMD:PAUSE")) {
                mediaPlayer.pause();
                playBtn.setText("▶");
            }
            else if (cmd.equals("CMD:PLAY")) {
                mediaPlayer.play();
                playBtn.setText("⏸");
            }
            else if (cmd.startsWith("CMD:SEEK:")) {
                double s = Double.parseDouble(cmd.split(":")[2]);
                mediaPlayer.seek(Duration.seconds(s));
            }
            // FIX LỖI 1: Nhận lệnh Mute
            else if (cmd.startsWith("CMD:MUTE:")) {
                boolean muteStatus = Boolean.parseBoolean(cmd.split(":")[2]);
                toggleMute(muteStatus);
            }
        } catch (Exception e) {
            System.out.println("Lỗi xử lý lệnh: " + e.getMessage());
        }
    }

    // --- CÁC HÀM ĐIỀU KHIỂN ---

    // Hàm bật/tắt tiếng (Dùng chung cho cả bấm nút và nhận lệnh mạng)
    private void toggleMute(boolean mute) {
        isMuted = mute;
        mediaPlayer.setMute(isMuted);
        muteBtn.setText(isMuted ? "🔇" : "🔊");
    }

    @FXML public void togglePlay() {
        if (mediaPlayer == null) return;

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playBtn.setText("▶");
            if (isP2PMode && isHost) p2p.send("CMD:PAUSE");
        } else {
            mediaPlayer.play();
            playBtn.setText("⏸");
            if (isP2PMode && isHost) p2p.send("CMD:PLAY");

            // Khách bấm Play -> Xin đồng bộ lại ngay
            if (isP2PMode && !isHost) p2p.send("CMD:REQUEST_INFO");
        }
    }

    // --- CÁC HÀM KHÁC (GIỮ NGUYÊN) ---

    // Hàm dọn dẹp quan trọng (FIX LỖI 3)
    private void cleanupAndExit() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        // Đóng Socket P2P để lần sau vào lại không bị lỗi port
        // (Lưu ý: Cần thêm hàm close() trong P2PNetwork nếu chưa có)
        // p2p.close();

        isP2PMode = false;
        isHost = false;
    }

    // ... (Copy lại các hàm setupConnectionLogic, setMovieToPlay, skipBack, skipForward từ bản cũ) ...
    // Nhớ giữ nguyên logic lấy IP, kết nối Server nha!

    // Code nút TẠO PHÒNG
    private void setupConnectionLogic() {
        btnCreateRoom.setOnAction(e -> {
            connectionMenu.setVisible(false);
            hostPanel.setVisible(true);
            guestPanel.setVisible(false);
            lblRoomId.setText("...");
            isHost = true;

            new Thread(() -> {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("CREATE");
                    String response = in.readLine();
                    if (response != null && response.startsWith("CREATED")) {
                        String code = response.split(" ")[1];
                        Platform.runLater(() -> {
                            lblRoomId.setText(code);
                            chatListView.getItems().add("System: Phòng " + code + " sẵn sàng!");
                        });
                        p2p.startHost();
                    }
                } catch (Exception ex) { Platform.runLater(() -> lblRoomId.setText("Lỗi")); }
            }).start();
        });

        // Code nút KẾT NỐI (Khách)
        connectBtn.setOnAction(e -> {
            String code = ipField.getText().trim();
            if (code.isEmpty()) return;
            isHost = false;

            chatListView.getItems().add("System: Đang kết nối...");
            new Thread(() -> {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("JOIN " + code);
                    String response = in.readLine();
                    if (response != null && response.startsWith("FOUND")) {
                        String hostIp = response.split(" ")[1];
                        Platform.runLater(() -> chatListView.getItems().add("System: Kết nối tới Host..."));

                        p2p.connect(hostIp);

                        // FIX LỖI 2: Chờ 1s cho kết nối ổn định rồi mới xin data
                        try { Thread.sleep(1000); } catch (Exception ex) {}
                        p2p.send("CMD:REQUEST_INFO");

                    } else { Platform.runLater(() -> chatListView.getItems().add("System: Không tìm thấy phòng!")); }
                } catch (Exception ex) { }
            }).start();
        });

        btnJoinRoom.setOnAction(e -> {
            connectionMenu.setVisible(false);
            hostPanel.setVisible(false);
            guestPanel.setVisible(true);
        });

        btnCancelHost.setOnAction(e -> resetP2PInterface());
        btnCancelGuest.setOnAction(e -> resetP2PInterface());
    }

    // ... (Các hàm updateTimeLabel, formatTime, setMode giữ nguyên) ...
    public void setMovieToPlay(Movie movie, boolean isP2P) {
        this.currentMovie = movie;
        this.isP2PMode = isP2P;
        setMode(isP2P);
        if (titleLabel != null) titleLabel.setText(movie.getTitle());

        Episode ep = DatabaseHandler.getFirstEpisode(movie.getId());
        if (ep != null) {
            String videoUrl = DriveUtils.getGoogleVideoLink(ep.getVideoId());
            try {
                Media media = new Media(videoUrl);
                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);

                mediaPlayer.setOnReady(() -> {
                    totalDuration = media.getDuration();
                    timeSlider.setMax(totalDuration.toSeconds());
                    playBtn.setText("▶");
                });

                mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    if (!timeSlider.isPressed()) timeSlider.setValue(newTime.toSeconds());
                    updateTimeLabel(newTime);
                });

                mediaPlayer.setAutoPlay(false);

            } catch (Exception e) { System.out.println("Lỗi Video"); }
        }
    }

    private void updateTimeLabel(Duration currentTime) {
        if (timeLabel != null && totalDuration != null) {
            String current = formatTime(currentTime);
            String total = formatTime(totalDuration);
            Platform.runLater(() -> timeLabel.setText(current + " / " + total));
        }
    }

    private String formatTime(Duration duration) {
        if (duration == null || duration.lessThan(Duration.ZERO) || duration.isUnknown()) return "00:00";
        int seconds = (int) duration.toSeconds();
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        if (hours > 0) return String.format("%02d:%02d:%02d", hours, minutes, secs);
        else return String.format("%02d:%02d", minutes, secs);
    }

    public void setMode(boolean isP2P) {
        if (isP2P) {
            p2pSidebar.setVisible(true); p2pSidebar.setManaged(true);
            offlineSidebar.setVisible(false); offlineSidebar.setManaged(false);
        } else {
            p2pSidebar.setVisible(false); p2pSidebar.setManaged(false);
            offlineSidebar.setVisible(true); offlineSidebar.setManaged(true);
        }
    }

    private void resetP2PInterface() {
        connectionMenu.setVisible(true);
        hostPanel.setVisible(false);
        guestPanel.setVisible(false);
        ipField.clear();
        isHost = false;
    }

    @FXML public void skipBack() {
        if (mediaPlayer == null) return;
        double t = mediaPlayer.getCurrentTime().toSeconds() - 10;
        mediaPlayer.seek(Duration.seconds(t));
        if (isP2PMode && isHost) p2p.send("CMD:SEEK:" + t);
    }

    @FXML public void skipForward() {
        if (mediaPlayer == null) return;
        double t = mediaPlayer.getCurrentTime().toSeconds() + 10;
        mediaPlayer.seek(Duration.seconds(t));
        if (isP2PMode && isHost) p2p.send("CMD:SEEK:" + t);
    }
}