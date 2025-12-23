package com.cinemates.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    // --- KHAI BÁO FXML ---
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
    private List<String> selectedFriends = new ArrayList<>();
    private String[] videos = {"/videos/video1.mp4", "/videos/video1.mp4"};
    private String[] titles = {"MƠ NGỐ KHEN NGON", "BÍ MẬT NƠI GÓC TỐI"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadHero(currentIndex);
        startAutoSlide();

        // Cài đặt hiệu ứng Hover và click cho Poster
        setupHoverEffect(movie1, "Phim của Ngân", "/image/background.png");
        setupHoverEffect(movie2, "Ẩn Danh 3", "/image/background.png");
        setupHoverEffect(movie3, "Conan: Cơn Ác Mộng", "/image/background.png");

        movie1.setOnMouseClicked(e -> handleHeroPlayAction());
        movie2.setOnMouseClicked(e -> handleHeroPlayAction());
        movie3.setOnMouseClicked(e -> handleHeroPlayAction());

        previewCard.setOnMouseEntered(e -> { if (hideTimer != null) hideTimer.stop(); });
        previewCard.setOnMouseExited(e -> stopPreview());
        addSampleFriends();
    }

    // --- LUỒNG CHÍNH: CHUYỂN SANG TRANG DETAIL ---
    @FXML
    private void handleHeroPlayAction() {
        // 1. Dọn dẹp tài nguyên triệt để
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        stopPreview();

        try {
            // 2. Bay sang trang MovieDetail.fxml
            Parent root = FXMLLoader.load(getClass().getResource("/MovieDetail.fxml"));
            Stage stage = (Stage) mainScrollPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Lỗi: Không tìm thấy file MovieDetail.fxml!");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggleCreateRoom() {
        // Thay vì hiện Overlay, ta bay thẳng sang Detail để chọn bạn bè ở đó
        handleHeroPlayAction();
    }

    private void loadHero(int index) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        try {
            Media media = new Media(getClass().getResource(videos[index]).toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mainMediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setMute(isMuted);
            mediaPlayer.play();
            lblMovieTitle.setText(titles[index]);
            updateDots(index);
        } catch (Exception e) {}
    }

    // --- GIỮ NGUYÊN TOÀN BỘ LOGIC CÒN LẠI CỦA NGÂN ---
    private void setupHoverEffect(VBox movieItem, String title, String imagePath) {
        movieItem.setOnMouseEntered(event -> {
            if (hideTimer != null) hideTimer.stop();
            lblPreviewTitle.setText(title.toUpperCase());
            try { previewImage.setImage(new Image(getClass().getResourceAsStream(imagePath))); } catch (Exception e) {}
            Bounds bounds = movieItem.localToScene(movieItem.getBoundsInLocal());
            previewCard.setTranslateX(bounds.getMinX() - 55);
            previewCard.setTranslateY(bounds.getMinY() - 60);
            previewCard.setVisible(true);
            previewCard.toFront();
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
            previewPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            previewPlayer.play();
        } catch (Exception e) {}
    }

    private void stopPreview() {
        previewCard.setVisible(false);
        if (previewPlayer != null) {
            previewPlayer.stop();
            previewPlayer.dispose();
            previewPlayer = null;
        }
    }

    @FXML private void handleTogglePreviewMute() { if (previewPlayer != null) { isPreviewMuted = !isPreviewMuted; previewPlayer.setMute(isPreviewMuted); btnPreviewMute.setText(isPreviewMuted ? "🔇" : "🔊"); } }
    @FXML private void handlePrevHero() { currentIndex = (currentIndex - 1 + videos.length) % videos.length; loadHero(currentIndex); }
    @FXML private void handleNextHero() { currentIndex = (currentIndex + 1) % videos.length; loadHero(currentIndex); }
    @FXML private void handleToggleMute() { if (mediaPlayer != null) { isMuted = !isMuted; mediaPlayer.setMute(isMuted); btnVolume.setText(isMuted ? "🔇" : "🔊"); } }
    private void startAutoSlide() { autoSlide = new Timeline(new KeyFrame(Duration.seconds(5), e -> handleNextHero())); autoSlide.setCycleCount(Timeline.INDEFINITE); autoSlide.play(); }
    private void updateDots(int activeIndex) { dotsContainer.getChildren().clear(); for (int i = 0; i < 8; i++) { Region dot = new Region(); dot.getStyleClass().add(i == activeIndex ? "dot-active" : "dot-normal"); dotsContainer.getChildren().add(dot); } }

    @FXML
    private void handleOpenRegister(ActionEvent event) throws IOException {
        stopPreview();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/Register.fxml"));
        stage.setScene(new Scene(root));
        stage.show();
    }

    public void setLoggedInUser(String username) {
        if (username != null && !username.isEmpty()) {
            btnRegister.setVisible(false);
            btnRegister.setManaged(false);
            lblUserSession.setText("Chào, " + username + " !");
            lblUserSession.setVisible(true);
            lblUserSession.setManaged(true);
        }
    }

    @FXML private void handleToggleFriendSidebar() { boolean isVisible = friendSidebar.isVisible(); friendSidebar.setVisible(!isVisible); friendSidebar.setManaged(!isVisible); if (!isVisible) friendSidebar.toFront(); }
    @FXML private void handleAddFriend() { String friendName = txtSearchFriend.getText(); if (friendName != null && !friendName.isEmpty()) System.out.println("Gửi mời: " + friendName); }
    @FXML private void handleCloseCreateRoom() { createRoomOverlay.setVisible(false); createRoomOverlay.setManaged(false); }
    @FXML private void startMovieRoom(ActionEvent event) throws IOException { if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); } stopPreview(); Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); Parent root = FXMLLoader.load(getClass().getResource("/WatchRoom.fxml")); stage.setScene(new Scene(root)); stage.show(); }

    private void addSampleFriends() {
        vboxMyFriends.getChildren().clear(); vboxOnlineUsers.getChildren().clear();
        vboxMyFriends.getChildren().add(createFriendUI("Kim Ngân", "Đang xem: Phim Anime", true));
        vboxMyFriends.getChildren().add(createFriendUI("Chồng của Ngân", "Online", true));
        vboxOnlineUsers.getChildren().add(createFriendUI("Anh Người Lạ 1", "Đang trực tuyến", false));
        vboxOnlineUsers.getChildren().add(createFriendUI("Bé Hàng Xóm", "Đang xem phim ma", false));
    }

    private HBox createFriendUI(String name, String status, boolean isFriend) {
        HBox row = new HBox(12); row.setAlignment(javafx.geometry.Pos.CENTER_LEFT); row.getStyleClass().add("friend-item");
        Label avatar = new Label(name.substring(0, 1).toUpperCase()); avatar.getStyleClass().add(isFriend ? "avatar-circle-green" : "avatar-circle-gray");
        VBox info = new VBox(2); Label lblName = new Label(name); lblName.getStyleClass().add("friend-name");
        Label lblStatus = new Label(status); lblStatus.getStyleClass().add("friend-status");
        info.getChildren().addAll(lblName, lblStatus); HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
        Button btnAction = new Button(isFriend ? "💬" : "+"); btnAction.getStyleClass().add(isFriend ? "btn-messenger" : "btn-add-friend-mini");
        row.getChildren().addAll(avatar, info, btnAction); return row;
    }
}