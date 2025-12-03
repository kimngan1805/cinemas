package com.cinemates.controller;

import com.cinemates.ClientSession;
import com.cinemates.P2PNetwork;
import com.cinemates.model.Episode;
import com.cinemates.model.Movie;
import com.cinemates.utils.DatabaseHandler;
import com.cinemates.utils.DriveUtils;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class PlayerController implements Initializable {

    @FXML private MediaView mediaView;
    @FXML private Button playBtn;
    @FXML private Button backBtn;
    @FXML private Label titleLabel;
    @FXML private Text descText;
    @FXML private Slider volumeSlider;
    @FXML private Slider timeSlider;
    @FXML private Label timeLabel;
    @FXML private Button muteBtn;

    @FXML private VBox offlineSidebar;
    @FXML private VBox p2pSidebar;
    @FXML private Label lblMemberCount;
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

    private MediaPlayer mediaPlayer;
    private Movie currentMovie;
    private Duration totalDuration;
    private P2PNetwork p2p;
    private Timeline syncTimer;

    private boolean isP2PMode = false;
    private boolean isHost = false;
    private boolean isMuted = false;
    private boolean isSyncing = false;

    private static final String SERVER_HOST = "vumfo-1-52-23-46.a.free.pinggy.link";
    private static final int SERVER_PORT = 42717;

    // --- CẤU HÌNH GIẢM TẢI CHO MÁY ---SYNC_TOLERANCE
    // Tăng độ lệch cho phép lên 2.0s để máy đỡ phải sửa liên tục
    private static final double SYNC_TOLERANCE = 3.0;
    private static final double CATCHUP_BUFFER = 0.5;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        p2p = new P2PNetwork();

        p2p.setOnMessageReceived(msg -> {
            Platform.runLater(() -> {
                if (msg.startsWith("CMD:COUNT:")) {
                    String count = msg.split(":")[2];
                    if (lblMemberCount != null) lblMemberCount.setText("Online: " + count);
                    return;
                }
                if (msg.equals("CMD:CLOSE_ROOM")) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thông báo");
                    alert.setHeaderText(null);
                    alert.setContentText("Chủ phòng đã thoát. Bạn sẽ quay về trang chi tiết.");
                    alert.show();
                    backToDetailScene(null);
                    return;
                }
                if (msg.startsWith("CHAT:")) {
                    String[] parts = msg.split(":", 3);
                    if (parts.length >= 3) {
                        String sender = parts[1];
                        String content = parts[2];
                        if (!sender.equals(ClientSession.myUsername)) {
                            chatListView.getItems().add(sender + ": " + content);
                        }
                    }
                    return;
                }
                if (msg.startsWith("CMD:")) {
                    handleSyncCommand(msg);
                } else if (msg.startsWith("SYSTEM: Connected")) {
                    if (!isHost) requestSyncImmediate();
                }
            });
        });

        backBtn.setOnAction(event -> backToDetailScene(event));
        playBtn.setOnAction(e -> togglePlay());

        if (muteBtn != null) {
            muteBtn.setOnAction(e -> {
                if (mediaPlayer == null) return;
                isMuted = !isMuted;
                mediaPlayer.setMute(isMuted);
                muteBtn.setText(isMuted ? "🔇" : "🔊");
            });
        }

        if (volumeSlider != null) {
            volumeSlider.setValue(100);
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (mediaPlayer != null) {
                    if (isMuted) {
                        isMuted = false;
                        mediaPlayer.setMute(false);
                        muteBtn.setText("🔊");
                    }
                    mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
                }
            });
        }

        timeSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                double newTime = timeSlider.getValue();
                mediaPlayer.seek(Duration.seconds(newTime));
                if (isP2PMode && isHost) {
                    p2p.send("CMD:SEEK:" + newTime);
                }
            }
        });

        setupConnectionLogic();

        sendBtn.setOnAction(e -> {
            String msg = chatInput.getText();
            if (!msg.isEmpty()) {
                String fullMsg = "CHAT:" + ClientSession.myUsername + ":" + msg;
                p2p.send(fullMsg);
                chatListView.getItems().add("Me: " + msg);
                chatInput.clear();
            }
        });
    }

    private void startSyncTimer() {
        if (syncTimer != null) syncTimer.stop();
        // --- GIẢM TẢI: Kiểm tra mỗi 4 giây thay vì 2 giây ---
        syncTimer = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            if (isP2PMode && isHost && mediaPlayer != null) {
                double currentT = mediaPlayer.getCurrentTime().toSeconds();
                String status = (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) ? "PLAY" : "PAUSE";
                p2p.send("CMD:AUTO_CHECK:" + currentT + ":" + status);
            }
        }));
        syncTimer.setCycleCount(Timeline.INDEFINITE);
        syncTimer.play();
    }

    private void requestSyncImmediate() {
        p2p.send("CMD:REQUEST_INFO");
    }

    private void handleSyncCommand(String cmd) {
        if (mediaPlayer == null) return;

        if (isHost && cmd.equals("CMD:REQUEST_INFO")) {
            double t = mediaPlayer.getCurrentTime().toSeconds();
            String status = (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) ? "PLAY" : "PAUSE";
            p2p.send("CMD:FORCE_SYNC:" + t + ":" + status);
            return;
        }

        if (isHost) return;

        try {
            if (cmd.startsWith("CMD:FORCE_SYNC:") || cmd.startsWith("CMD:SYNC_ALL:")) {
                String[] parts = cmd.split(":");
                double hostTime = Double.parseDouble(parts[2]);
                String status = parts[3];
                performSafeSeek(hostTime, status, true);
            }
            else if (cmd.startsWith("CMD:AUTO_CHECK:")) {
                if (isSyncing) return; // Đang bận thì thôi

                String[] parts = cmd.split(":");
                double hostTime = Double.parseDouble(parts[2]);
                String hostStatus = parts[3];

                double myTime = mediaPlayer.getCurrentTime().toSeconds();
                double diff = hostTime - myTime;

                // Chỉ sửa nếu lệch nhiều (trên 2.0s)
                if (Math.abs(diff) > SYNC_TOLERANCE) {
                    System.out.println("!!! Lệch " + diff + "s. Đang sửa...");
                    performSafeSeek(hostTime, hostStatus, false);
                }
                else {
                    boolean amIPlaying = (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING);
                    boolean isHostPlaying = hostStatus.equals("PLAY");
                    if (amIPlaying != isHostPlaying) {
                        if (isHostPlaying) { mediaPlayer.play(); playBtn.setText("⏸"); }
                        else { mediaPlayer.pause(); playBtn.setText("▶"); }
                    }
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
                performSafeSeek(s, null, true);
            }
        } catch (Exception e) {
            System.out.println("Lỗi xử lý lệnh: " + e.getMessage());
        }
    }

    private void performSafeSeek(double targetTime, String status, boolean isForce) {
        isSyncing = true;
        double finalTarget = targetTime;
        if ("PLAY".equals(status)) {
            finalTarget += CATCHUP_BUFFER;
        }
        mediaPlayer.seek(Duration.seconds(finalTarget));
        timeSlider.setValue(finalTarget);

        if ("PLAY".equals(status)) {
            mediaPlayer.play();
            playBtn.setText("⏸");
        } else if ("PAUSE".equals(status)) {
            mediaPlayer.pause();
            playBtn.setText("▶");
        }

        PauseTransition cooldown = new PauseTransition(Duration.seconds(2));
        cooldown.setOnFinished(e -> isSyncing = false);
        cooldown.play();
    }

    @FXML public void togglePlay() {
        if (mediaPlayer == null) return;

        if (isP2PMode && !isHost) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playBtn.setText("▶");
            } else {
                p2p.send("CMD:REQUEST_INFO");
            }
            return;
        }

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playBtn.setText("▶");
            if (isP2PMode && isHost) p2p.send("CMD:PAUSE");
        } else {
            mediaPlayer.play();
            playBtn.setText("⏸");
            if (isP2PMode && isHost) {
                double currentT = mediaPlayer.getCurrentTime().toSeconds();
                p2p.send("CMD:FORCE_SYNC:" + currentT + ":PLAY");
            }
        }
    }

    private void cleanupAndExit() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        if (p2p != null) p2p.close();
        if (syncTimer != null) syncTimer.stop();
        isP2PMode = false;
        isHost = false;
        isMuted = false;
        isSyncing = false;
    }

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
                     java.util.Scanner in = new java.util.Scanner(socket.getInputStream())) {

                    out.println("CREATE");
                    if (in.hasNextLine()) {
                        String response = in.nextLine();
                        if (response.startsWith("CREATED")) {
                            String code = response.split(" ")[1];
                            Platform.runLater(() -> {
                                lblRoomId.setText(code);
                                if (lblMemberCount != null) lblMemberCount.setText("Online: 1");
                                startSyncTimer();
                            });
                            p2p.startHost();
                        }
                    }
                } catch (Exception ex) { Platform.runLater(() -> lblRoomId.setText("Lỗi Server")); }
            }).start();
        });

        connectBtn.setOnAction(e -> {
            String code = ipField.getText().trim();
            if (code.isEmpty()) return;
            isHost = false;
            new Thread(() -> {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     java.util.Scanner in = new java.util.Scanner(socket.getInputStream())) {

                    out.println("JOIN " + code);
                    if (in.hasNextLine()) {
                        String response = in.nextLine();
                        if (response.startsWith("FOUND")) {
                            String hostIp = response.split(" ")[1];
                            p2p.connect(hostIp, ClientSession.myUsername);
                        } else {
                            Platform.runLater(() -> chatListView.getItems().add("System: Không tìm thấy phòng!"));
                        }
                    }
                } catch (Exception ex) { }
            }).start();
        });

        btnJoinRoom.setOnAction(e -> {
            connectionMenu.setVisible(false);
            hostPanel.setVisible(false);
            guestPanel.setVisible(true);
        });
        btnCancelHost.setOnAction(e -> backToDetailScene(e));
        btnCancelGuest.setOnAction(e -> backToDetailScene(e));
    }

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
        connectionMenu.setVisible(true); hostPanel.setVisible(false); guestPanel.setVisible(false);
        ipField.clear(); isHost = false;
        if (p2p != null) p2p.close();
    }

    private void stopVideo() {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
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

    // --- HÀM CHUNG: QUAY VỀ TRANG DETAIL (ĐÃ NÂNG CẤP) ---
    private void backToDetailScene(javafx.event.Event event) {

        // 1. XỬ LÝ LỜI CHÀO TẠM BIỆT TRƯỚC KHI NGẮT KẾT NỐI
        if (isP2PMode && p2p != null) {

            if (isHost) {
                // TRƯỜNG HỢP HOST: Đuổi tất cả
                System.out.println("Host đang giải tán phòng...");
                p2p.send("CMD:CLOSE_ROOM");
            }
            else {
                // TRƯỜNG HỢP GUEST: Báo cáo là em out nha
                System.out.println("Khách đang rời phòng...");

                // Gửi tin nhắn dạng System để hiện lên khung chat của mọi người
                // Cấu trúc: CHAT:System:[Tên] đã rời phòng.
                p2p.send("CHAT:" + ClientSession.myUsername + " đã rời phòng.");

                // MẸO QUAN TRỌNG: Ngủ 0.2s để tin nhắn kịp bay sang máy Host
                // Nếu không có dòng này, mạng bị cắt nhanh quá tin nhắn chưa kịp đi
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) { }
            }
        }

        // 2. Dọn dẹp sạch sẽ (Cắt mạng, tắt video)
        cleanupAndExit();

        // 3. Chuyển cảnh về Detail (Code cũ giữ nguyên)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/detail.fxml"));
            Parent root = loader.load();

            if (currentMovie != null) {
                DetailController detailController = loader.getController();
                detailController.setMovieData(currentMovie.getId());
            }

            Stage window = (Stage) backBtn.getScene().getWindow();
            window.setScene(new Scene(root));
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}