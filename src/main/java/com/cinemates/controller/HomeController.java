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
import java.net.URL;
import java.util.*;

public class HomeController implements Initializable {

    // --- CÁC BIẾN FXML (GIỮ NGUYÊN) ---
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

    // --- BIẾN QUẢN LÝ ---
    private MediaPlayer mediaPlayer, previewPlayer;
    private Timeline autoSlide, hideTimer;
    private boolean isMuted = true, isPreviewMuted = true;
    private int currentIndex = 0;
    private String[] videos = {"/videos/video1.mp4", "/videos/video1.mp4"};
    private String[] titles = {"MƠ NGỐ KHEN NGON", "BÍ MẬT NƠI GÓC TỐI"};
    private User currentUser;

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
    }

    // --- PHẦN 1: LOGIC VIDEO & UI (ĐÃ FIX LỖI CRASH) ---

    @FXML private void handleToggleMute() {
        if (mediaPlayer != null) {
            isMuted = !isMuted; mediaPlayer.setMute(isMuted);
            btnVolume.setText(isMuted ? "🔇" : "🔊");
        }
    }

    private void loadHero(int index) {
        // SỬA LỖI: Kiểm tra null trước khi stop để không bị crash
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
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

    @FXML private void handleHeroPlayAction() {
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
        stopPreview();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MovieDetail.fxml"));
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

    // --- PHẦN 2: LOGIC CỘNG ĐỒNG (KẾT BẠN & POPUP) ---

    public void setLoggedInUser(User user) {
        this.currentUser = user;
        if (user != null) {
            btnRegister.setVisible(false); btnRegister.setManaged(false);
            lblUserSession.setText("Chào, " + user.getUsername() + " !");
            lblUserSession.setVisible(true); lblUserSession.setManaged(true);

            refreshCommunitySidebar();

            // BỘ CANH CỬA: 5 giây quét Database tìm lời mời kết bạn một lần
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

    @FXML private void handleAddFriend() { // Đã thêm lại hàm này
        String name = txtSearchFriend.getText();
        if (name != null && !name.isEmpty()) {
            System.out.println("Gửi kết bạn tới: " + name);
        }
    }

    @FXML private void handleCloseCreateRoom() { // Đã thêm lại hàm này
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
        HBox row = new HBox(12); row.setAlignment(javafx.geometry.Pos.CENTER_LEFT); row.getStyleClass().add("friend-item");
        Label avatar = new Label(user.getUsername().substring(0, 1).toUpperCase());
        avatar.getStyleClass().add(isFriend ? "avatar-circle-green" : "avatar-circle-gray");
        VBox info = new VBox(2);
        Label lblName = new Label(user.getUsername()); lblName.getStyleClass().add("friend-name");
        Label lblStatus = new Label(user.isOnline() ? "Online" : "Offline"); lblStatus.getStyleClass().add("friend-status");
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
}