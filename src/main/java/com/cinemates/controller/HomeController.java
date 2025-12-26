package com.cinemates.controller;

import com.cinemates.model.User;
import com.cinemates.utils.DatabaseHandler;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.*;

public class HomeController implements Initializable {

    @FXML private VBox vboxPublicRooms;
    @FXML private CheckBox chkPublicRoom;
    @FXML private ScrollPane mainScrollPane;
    @FXML private MediaView mainMediaView, previewMediaView;
    @FXML private Label lblMovieTitle, lblPreviewTitle, lblUserSession;
    @FXML private HBox dotsContainer;
    @FXML private Button btnVolume, btnPreviewMute, btnRegister;
    @FXML private VBox previewCard, movie1, movie2, movie3, friendSidebar, vboxMyFriends, vboxOnlineUsers;
    @FXML private ImageView previewImage;
    @FXML private TextField txtSearchFriend;
    @FXML private StackPane createRoomOverlay;
    @FXML private FlowPane flowFriendSelection;
    @FXML private TextField txtInputRoomCode;
    private static ObjectOutputStream serverOut;
    private static ObjectInputStream serverIn;
    private MediaPlayer mediaPlayer, previewPlayer;
    private Timeline autoSlide, hideTimer;
    private boolean isMuted = true, isPreviewMuted = true;
    private int currentIndex = 0;
    private String[] videos = {"/videos/video1.mp4", "/videos/video1.mp4"};
    private String[] titles = {"MƠ NGỐ KHEN NGON", "BÍ MẬT NƠI GÓC TỐI"};
    private User currentUser;
    private static String myGuestName = null;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadHero(currentIndex);
        startAutoSlide();
        setupHoverEffect(movie1, "Phim của Ngân", "/image/background.png");
        setupHoverEffect(movie2, "Ẩn Danh 3", "/image/background.png");
        setupHoverEffect(movie3, "Conan: Cơn Ác Mộng", "/image/background.png");

        movie1.setOnMouseClicked(e -> handleHeroPlayAction());
        movie2.setOnMouseClicked(e -> handleHeroPlayAction());
        movie3.setOnMouseClicked(e -> handleHeroPlayAction());

        previewCard.setOnMouseEntered(e -> { if (hideTimer != null) hideTimer.stop(); });
        previewCard.setOnMouseExited(e -> stopPreview());
        if (RegisterController.globalOut == null) {
            connectToServerAsGuest();
        }
        connectToServerIfNeeded();
        startListeningForPublicRooms();
        // ⭐ TỰ ĐỘNG LẮP NGHE DANH SÁCH PHÒNG CÔNG KHAI
        startListeningForPublicRooms();
    }
    private void connectToServerIfNeeded() {
        if (RegisterController.globalOut != null) {
            System.out.println("✅ Đã có kết nối server từ RegisterController");
            return;
        }

        System.out.println("🔌 Đang kết nối tới Server...");

        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 6789);
                serverOut = new ObjectOutputStream(socket.getOutputStream());
                serverIn = new ObjectInputStream(socket.getInputStream());

                String myIp = java.net.InetAddress.getLocalHost().getHostAddress();

                // ⭐ TẠO TÊN GUEST MỘT LẦN VÀ LƯU LẠI
                if (myGuestName == null) {
                    myGuestName = "Guest-" + (int)(Math.random() * 9999);
                }

                String registerMsg = myGuestName + "|" + myIp + "|5000";

                serverOut.writeObject(registerMsg);
                serverOut.flush();

                System.out.println("📡 Guest đã kết nối Server: " + registerMsg);
                System.out.println("🆔 Tên Guest cố định: " + myGuestName);

                RegisterController.globalOut = serverOut;

                // Lắng nghe...
                while (true) {
                    Object msg = serverIn.readObject();
                    if (msg instanceof String) {
                        String cmd = msg.toString();

                        if (cmd.startsWith("PUBLIC_ROOMS|")) {
                            System.out.println("📺 Nhận danh sách phòng: " + cmd);
                            Platform.runLater(() -> updatePublicRoomsList(cmd));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi kết nối Server: " + e.getMessage());
            }
        }).start();
    }

    // ⭐ THÊM METHOD MỚI:
    public static String getMyGuestName() {
        return myGuestName;
    }

    @FXML private void handleToggleMute() {
        if (mediaPlayer != null) {
            isMuted = !isMuted; mediaPlayer.setMute(isMuted);
            btnVolume.setText(isMuted ? "🔇" : "🔊");
        }
    }

    private void loadHero(int index) {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.getStatus() != MediaPlayer.Status.UNKNOWN) {
                    mediaPlayer.stop();
                }
                mediaPlayer.dispose();
            } catch (Exception e) {
                System.out.println("Lỗi dọn dẹp player cũ: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        try {
            Media media = new Media(getClass().getResource(videos[index]).toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mainMediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setMute(isMuted); mediaPlayer.play();
            lblMovieTitle.setText(titles[index]); updateDots(index);
        } catch (Exception e) {}
    }

    private void setupHoverEffect(VBox movieItem, String title, String imagePath) {
        movieItem.setOnMouseEntered(event -> {
            if (hideTimer != null) hideTimer.stop();
            lblPreviewTitle.setText(title.toUpperCase());
            try { previewImage.setImage(new Image(getClass().getResourceAsStream(imagePath))); } catch (Exception e) {}
            Bounds bounds = movieItem.localToScene(movieItem.getBoundsInLocal());
            previewCard.setTranslateX(bounds.getMinX() - 55);
            previewCard.setTranslateY(bounds.getMinY() - 60);
            previewCard.setVisible(true); previewCard.toFront();
            startPreviewVideo("/videos/video1.mp4");
        });
        movieItem.setOnMouseExited(event -> {
            hideTimer = new Timeline(new KeyFrame(Duration.millis(200), ae -> { if (!previewCard.isHover()) stopPreview(); }));
            hideTimer.play();
        });
    }

    private void startPreviewVideo(String path) {
        if (previewPlayer != null) previewPlayer.dispose();
        try {
            Media media = new Media(getClass().getResource(path).toExternalForm());
            previewPlayer = new MediaPlayer(media);
            previewMediaView.setMediaPlayer(previewPlayer);
            previewPlayer.setMute(isPreviewMuted);
            previewPlayer.setCycleCount(MediaPlayer.INDEFINITE); previewPlayer.play();
        } catch (Exception e) {}
    }

    private void stopPreview() {
        previewCard.setVisible(false);
        if (previewPlayer != null) { previewPlayer.stop(); previewPlayer.dispose(); previewPlayer = null; }
    }

    @FXML private void handleTogglePreviewMute() { if (previewPlayer != null) { isPreviewMuted = !isPreviewMuted; previewPlayer.setMute(isPreviewMuted); btnPreviewMute.setText(isPreviewMuted ? "🔇" : "🔊"); } }
    @FXML private void handlePrevHero() { currentIndex = (currentIndex - 1 + videos.length) % videos.length; loadHero(currentIndex); }
    @FXML private void handleNextHero() { currentIndex = (currentIndex + 1) % videos.length; loadHero(currentIndex); }
    private void startAutoSlide() { autoSlide = new Timeline(new KeyFrame(Duration.seconds(5), e -> handleNextHero())); autoSlide.setCycleCount(Timeline.INDEFINITE); autoSlide.play(); }
    private void updateDots(int activeIndex) { dotsContainer.getChildren().clear(); for (int i = 0; i < 8; i++) { Region dot = new Region(); dot.getStyleClass().add(i == activeIndex ? "dot-active" : "dot-normal"); dotsContainer.getChildren().add(dot); } }

    @FXML
    private void handleHeroPlayAction() {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        stopPreview();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MovieDetail.fxml"));
            Parent root = loader.load();

            // ⭐ TRUYỀN USER QUA MOVIEDETAIL
            MovieDetailController controller = loader.getController();
            controller.setCurrentUser(this.currentUser);

            Stage stage = (Stage) mainScrollPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleOpenRegister(ActionEvent event) throws IOException {
        stopPreview();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/Register.fxml"))));
    }

    @FXML private void handleBackToHome(ActionEvent event) throws IOException {
        handleHeroPlayAction();
    }

    public void setLoggedInUser(User user) {
        this.currentUser = user;
        if (user != null) {
            btnRegister.setVisible(false); btnRegister.setManaged(false);
            lblUserSession.setText("Chào, " + user.getUsername() + " !");
            lblUserSession.setVisible(true); lblUserSession.setManaged(true);

            refreshCommunitySidebar();

            Timeline friendCheck = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
                checkIncomingFriendRequests();
                if (friendSidebar.isVisible()) refreshCommunitySidebar();
            }));
            friendCheck.setCycleCount(Timeline.INDEFINITE);
            friendCheck.play();
        }
    }

    private void checkIncomingFriendRequests() {
        if (currentUser == null) return;
        List<User> requests = DatabaseHandler.getPendingRequests(currentUser.getId());
        for (User sender : requests) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Lời mời kết bạn");
                alert.setHeaderText("Hố hố hố, " + sender.getUsername() + " muốn kết bạn!");
                alert.setContentText("Ngân có đồng ý kết bạn không?");

                ButtonType btnAccept = new ButtonType("Đồng ý");
                ButtonType btnReject = new ButtonType("Từ chối", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnAccept, btnReject);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == btnAccept) {
                    DatabaseHandler.handleFriendRequest(sender.getId(), currentUser.getId(), "ACCEPTED");
                } else {
                    DatabaseHandler.handleFriendRequest(sender.getId(), currentUser.getId(), "REJECTED");
                }
                refreshCommunitySidebar();
            });
        }
    }

    @FXML private void handleAddFriend() {
        String name = txtSearchFriend.getText();
        if (name != null && !name.isEmpty()) {
            System.out.println("Gửi kết bạn tới: " + name);
        }
    }

    @FXML private void handleCloseCreateRoom() {
        if (createRoomOverlay != null) {
            createRoomOverlay.setVisible(false);
            createRoomOverlay.setManaged(false);
        }
    }

    @FXML private void handleToggleCreateRoom() { handleHeroPlayAction(); }

    @FXML private void startMovieRoom(ActionEvent event) throws IOException {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        stopPreview();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/WatchRoom.fxml"))));
    }

    @FXML private void handleToggleFriendSidebar() {
        friendSidebar.setVisible(!friendSidebar.isVisible());
        friendSidebar.setManaged(friendSidebar.isVisible());
        if (friendSidebar.isVisible()) refreshCommunitySidebar();
    }

    private void refreshCommunitySidebar() {
        if (currentUser == null) return;
        Platform.runLater(() -> {
            vboxMyFriends.getChildren().clear();
            vboxOnlineUsers.getChildren().clear();
            List<User> friends = DatabaseHandler.getFriendsList(currentUser.getId());
            for (User f : friends) vboxMyFriends.getChildren().add(createFriendUI(f, true));
            List<User> strangers = DatabaseHandler.getOnlineStrangers(currentUser.getId());
            for (User s : strangers) vboxOnlineUsers.getChildren().add(createFriendUI(s, false));
        });
    }

    private HBox createFriendUI(User user, boolean isFriend) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("friend-item");

        Label avatar = new Label(user.getUsername().substring(0, 1).toUpperCase());

        if (user.isOnline()) {
            avatar.getStyleClass().add("avatar-circle-green");
        } else {
            avatar.getStyleClass().add("avatar-circle-gray");
        }

        VBox info = new VBox(2);
        Label lblName = new Label(user.getUsername());
        lblName.getStyleClass().add("friend-name");

        Label lblStatus = new Label(user.isOnline() ? "Online" : "Offline");
        lblStatus.getStyleClass().add("friend-status");
        if (!user.isOnline()) {
            lblStatus.setStyle("-fx-text-fill: #999999;");
        } else {
            lblStatus.setStyle("-fx-text-fill: #00ff40;");
        }

        info.getChildren().addAll(lblName, lblStatus);

        Button btnAction = new Button(isFriend ? "💬" : "+");
        btnAction.getStyleClass().add(isFriend ? "btn-messenger" : "btn-add-friend-mini");
        btnAction.setOnAction(e -> {
            if (!isFriend) {
                DatabaseHandler.sendFriendRequest(currentUser.getId(), user.getId());
                System.out.println("✅ Đã gửi lời mời tới: " + user.getUsername());
                refreshCommunitySidebar();
            }
        });
        row.getChildren().addAll(avatar, info, btnAction);
        return row;
    }

    // ⭐ PHẦN MỚI: XỬ LÝ PHÒNG CÔNG KHAI

    @FXML
    private void handleRefreshPublicRooms() {
        ObjectOutputStream out = (RegisterController.globalOut != null)
                ? RegisterController.globalOut
                : serverOut;

        if (out != null) {
            try {
                out.writeObject("REQUEST_PUBLIC_ROOMS");
                out.flush();
                System.out.println("🔄 Đã yêu cầu danh sách phòng công khai");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                vboxPublicRooms.getChildren().clear();
                Label lblError = new Label("⚠️ Chưa kết nối với server!");
                lblError.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px; -fx-padding: 20;");
                vboxPublicRooms.getChildren().add(lblError);

                // Thử kết nối lại
                connectToServerIfNeeded();
            });
        }
    }


    private void startListeningForPublicRooms() {
        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(8), e -> {
            if (RegisterController.globalOut != null) {
                handleRefreshPublicRooms();
            }
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();

        // Load ngay lần đầu sau 2s
        new Timeline(new KeyFrame(Duration.seconds(2), e -> handleRefreshPublicRooms())).play();
    }

    public void updatePublicRoomsList(String data) {
        Platform.runLater(() -> {
            vboxPublicRooms.getChildren().clear();

            if (data.equals("PUBLIC_ROOMS|") || data.endsWith("|")) {
                VBox emptyBox = new VBox(10);
                emptyBox.setAlignment(javafx.geometry.Pos.CENTER);
                emptyBox.setStyle("-fx-padding: 40;");

                Label icon = new Label("😴");
                icon.setStyle("-fx-font-size: 48px;");

                Label empty = new Label("Chưa có phòng công khai nào đang mở");
                empty.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 16px;");

                Label hint = new Label("Hãy tạo phòng đầu tiên bằng cách bấm 'Xem ngay' mà không đăng nhập");
                hint.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

                emptyBox.getChildren().addAll(icon, empty, hint);
                vboxPublicRooms.getChildren().add(emptyBox);
                return;
            }

            String roomsData = data.substring(13); // Bỏ "PUBLIC_ROOMS|"
            String[] rooms = roomsData.split(",");

            int count = 0;
            for (String room : rooms) {
                if (room.isEmpty() || room.trim().isEmpty()) continue;

                String[] info = room.split(":");
                if (info.length < 4) {
                    System.err.println("⚠️ Dữ liệu phòng lỗi: " + room);
                    continue;
                }

                String roomCode = info[0].trim();
                String movieName = info[1].trim();
                String users = info[2].trim();
                String hostIP = info[3].trim();

                HBox card = createPublicRoomCard(roomCode, movieName, users, hostIP);
                vboxPublicRooms.getChildren().add(card);
                count++;
            }

            System.out.println("📺 Đã cập nhật: " + count + " phòng công khai");
        });
    }

    private HBox createPublicRoomCard(String roomCode, String movieName, String users, String hostIP) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: linear-gradient(135deg, rgba(102,126,234,0.15) 0%, rgba(118,75,162,0.15) 100%); " +
                "-fx-padding: 18; -fx-background-radius: 10; " +
                "-fx-border-color: rgba(102,126,234,0.3); -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label icon = new Label("🎬");
        icon.setStyle("-fx-font-size: 36px;");

        VBox info = new VBox(8);
        info.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label lblMovie = new Label(movieName);
        lblMovie.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 17px;");

        HBox details = new HBox(18);

        HBox usersBox = new HBox(5);
        Label usersIcon = new Label("👥");
        usersIcon.setStyle("-fx-font-size: 14px;");
        Label lblUsers = new Label(users);
        lblUsers.setStyle("-fx-text-fill: #00ff40; -fx-font-size: 13px; -fx-font-weight: bold;");
        usersBox.getChildren().addAll(usersIcon, lblUsers);

        HBox codeBox = new HBox(5);
        Label codeIcon = new Label("📟");
        codeIcon.setStyle("-fx-font-size: 14px;");
        Label lblCode = new Label(roomCode);
        lblCode.setStyle("-fx-text-fill: #999; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
        codeBox.getChildren().addAll(codeIcon, lblCode);

        details.getChildren().addAll(usersBox, codeBox);

        info.getChildren().addAll(lblMovie, details);

        Button btnJoin = new Button("Tham gia ngay");
        btnJoin.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11 22; " +
                "-fx-background-radius: 7; -fx-cursor: hand; -fx-font-size: 13px;");

        btnJoin.setOnMouseEntered(e -> btnJoin.setStyle(
                "-fx-background-color: linear-gradient(135deg, #7c91f7 0%, #8b5ec0 100%); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11 22; " +
                        "-fx-background-radius: 7; -fx-cursor: hand; -fx-font-size: 13px;"
        ));

        btnJoin.setOnMouseExited(e -> btnJoin.setStyle(
                "-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 11 22; " +
                        "-fx-background-radius: 7; -fx-cursor: hand; -fx-font-size: 13px;"
        ));

        btnJoin.setOnAction(e -> {
            try {
                System.out.println("🚀 Đang tham gia: " + roomCode + ", IP: " + hostIP);

                // ⭐ LẤY CHỈ IP, BỎ PORT NẾU CÓ
                String cleanIP = hostIP;
                if (hostIP.contains(":")) {
                    cleanIP = hostIP.split(":")[0];
                }

                joinPublicRoom(roomCode, cleanIP, movieName);
            } catch (IOException ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Không thể tham gia phòng");
                alert.setContentText("Chi tiết: " + ex.getMessage());
                alert.show();
            }
        });

        card.getChildren().addAll(icon, info, btnJoin);
        return card;
    }

    private void joinPublicRoom(String roomCode, String hostIP, String movieName) throws IOException {
        System.out.println("🚀 Đang tham gia phòng: " + roomCode + " @ " + hostIP);

        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        stopPreview();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/WatchRoom.fxml"));
        Parent root = loader.load();
        WatchRoomController controller = loader.getController();

        controller.initRoomData(movieName, null, roomCode, false);
        controller.connectToHost(hostIP.split(":")[0]); // Lấy IP, bỏ port nếu có

        Stage stage = (Stage) mainScrollPane.getScene().getWindow();
        stage.setScene(new Scene(root));
    }

    public static ObjectOutputStream getServerConnection() {
        return serverOut;
    }
    @FXML
    private void handleJoinRoomByCode() {
        String code = txtInputRoomCode.getText().trim();

        if (code.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng nhập mã phòng!");
            alert.show();
            return;
        }

        ObjectOutputStream out = (RegisterController.globalOut != null)
                ? RegisterController.globalOut
                : serverOut;

        if (out != null) {
            try {
                String username = (currentUser != null) ? currentUser.getUsername() : "Guest";
                out.writeObject("FIND_ROOM|" + code + "|" + username);
                out.flush();
                System.out.println("🔍 Đang tìm phòng: " + code);
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi kết nối: " + e.getMessage());
                alert.show();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Chưa kết nối server!");
            alert.show();
            connectToServerIfNeeded(); // Thử kết nối lại
        }
    }
    private void connectToServerAsGuest() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 6789);
                RegisterController.globalOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                String myIp = java.net.InetAddress.getLocalHost().getHostAddress();
                String guestName = "Guest-" + (int)(Math.random() * 9999);
                String registerMsg = guestName + "|" + myIp + "|5000";
                RegisterController.globalOut.writeObject(registerMsg);
                RegisterController.globalOut.flush();
                System.out.println("📡 Guest đã kết nối server: " + registerMsg);

                // Lắng nghe tin nhắn từ server
                while (true) {
                    Object msg = in.readObject();
                    if (msg instanceof String) {
                        String cmd = msg.toString();

                        if (cmd.startsWith("PUBLIC_ROOMS|")) {
                            Platform.runLater(() -> updatePublicRoomsList(cmd));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi kết nối server: " + e.getMessage());
            }
        }).start();
    }
}