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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Objects;

public class HomeController implements Initializable {

    // --- KHAI BÁO FXML (GIỮ NGUYÊN & THÊM MỚI) ---
    @FXML private ScrollPane mainScrollPane;
    @FXML private MediaView mainMediaView, previewMediaView;
    @FXML private Label lblMovieTitle, lblPreviewTitle;
    @FXML private HBox dotsContainer;
    @FXML private Button btnVolume, btnPreviewMute;
    @FXML private VBox previewCard, movie1, movie2, movie3;
    @FXML private ImageView previewImage;

    // THÊM MỚI ĐỂ QUẢN LÝ USER
    @FXML private Button btnRegister;
    @FXML private Label lblUserSession;
    @FXML private VBox friendSidebar;        // Khung sidebar chính
    @FXML private VBox vboxFriendList;       // Nơi chứa danh sách bạn bè cụ thể
    @FXML private TextField txtSearchFriend;

    // --- BIẾN QUẢN LÝ ---
    private MediaPlayer mediaPlayer, previewPlayer;
    private Timeline autoSlide, hideTimer;
    private boolean isMuted = true, isPreviewMuted = true;
    private int currentIndex = 0;

    // --- DATA ---
    private String[] videos = {"/videos/video1.mp4", "/videos/video1.mp4"};
    private String[] titles = {"MƠ NGỐ KHEN NGON", "BÍ MẬT NƠI GÓC TỐI"};
    private String sampleVideo = "/videos/video1.mp4";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadHero(currentIndex);
        startAutoSlide();

        setupHoverEffect(movie1, "Phim của Ngân", "/image/background.png");
        setupHoverEffect(movie2, "Ẩn Danh 3", "/image/background.png");
        setupHoverEffect(movie3, "Conan: Cơn Ác Mộng", "/image/background.png");

        previewCard.setOnMouseEntered(e -> { if (hideTimer != null) hideTimer.stop(); });
        previewCard.setOnMouseExited(e -> stopPreview());
    }

    // --- HÀM MỚI: CẬP NHẬT TRẠNG THÁI ĐĂNG NHẬP ---
    public void setLoggedInUser(String username) {
        if (username != null && !username.isEmpty()) {
            // 1. Ẩn nút đăng ký
            btnRegister.setVisible(false);
            btnRegister.setManaged(false);

            // 2. Hiện Username và lời chào
            lblUserSession.setText("Chào, " + username + "!");
            lblUserSession.setVisible(true);
            lblUserSession.setManaged(true);
        }
    }

    /**
     * HÀM HOVER VIDEO PREVIEW
     */
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
            hideTimer = new Timeline(new KeyFrame(Duration.millis(200), ae -> {
                if (!previewCard.isHover()) stopPreview();
            }));
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

    @FXML private void handleTogglePreviewMute() {
        if (previewPlayer != null) {
            isPreviewMuted = !isPreviewMuted;
            previewPlayer.setMute(isPreviewMuted);
            btnPreviewMute.setText(isPreviewMuted ? "🔇" : "🔊");
        }
    }

    @FXML private void handlePrevHero() {
        currentIndex = (currentIndex - 1 + videos.length) % videos.length;
        loadHero(currentIndex);
        if (autoSlide != null) autoSlide.playFromStart();
    }

    @FXML private void handleNextHero() {
        currentIndex = (currentIndex + 1) % videos.length;
        loadHero(currentIndex);
        if (autoSlide != null) autoSlide.playFromStart();
    }

    @FXML private void handleToggleMute() {
        if (mediaPlayer != null) {
            isMuted = !isMuted;
            mediaPlayer.setMute(isMuted);
            btnVolume.setText(isMuted ? "🔇" : "🔊");
        }
    }

    private void loadHero(int index) {
        if (mediaPlayer != null) mediaPlayer.stop();
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

    private void startAutoSlide() {
        autoSlide = new Timeline(new KeyFrame(Duration.seconds(5), e -> handleNextHero()));
        autoSlide.setCycleCount(Timeline.INDEFINITE);
        autoSlide.play();
    }

    private void updateDots(int activeIndex) {
        dotsContainer.getChildren().clear();
        for (int i = 0; i < 8; i++) {
            Region dot = new Region();
            dot.getStyleClass().add(i == activeIndex ? "dot-active" : "dot-normal");
            dotsContainer.getChildren().add(dot);
        }
    }

    @FXML
    private void handleOpenRegister(ActionEvent event) throws IOException {
        stopPreview();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/Register.fxml"));
        stage.setScene(new Scene(root));
        stage.show();
    }
    @FXML
    private void handleToggleFriendSidebar() {
        // Kiểm tra xem nó đang hiện hay ẩn
        boolean isVisible = friendSidebar.isVisible();

        // Đảo ngược trạng thái
        friendSidebar.setVisible(!isVisible);

        //Managed = false để khi ẩn nó không chiếm chỗ trên giao diện
        friendSidebar.setManaged(!isVisible);

        // Nếu hiện ra thì cho nó lên trên cùng
        if (!isVisible) {
            friendSidebar.toFront();
        }
    }
    @FXML
    private void handleAddFriend() {
        String friendName = txtSearchFriend.getText();
        if (friendName != null && !friendName.isEmpty()) {
            System.out.println("Đang gửi lời mời kết bạn tới: " + friendName);
            // Sau này mình viết code SQL INSERT vào bảng friends ở đây
        }
    }
}