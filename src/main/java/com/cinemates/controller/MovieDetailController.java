package com.cinemates.controller;

import com.cinemates.App;
import com.cinemates.model.User;
import com.cinemates.utils.DatabaseHandler;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.control.CheckBox;

public class MovieDetailController implements Initializable {
    @FXML private FlowPane flowEpisodes, flowRecommended, flowFriendSelection;
    @FXML private VBox containerEpisodes, containerRecommended, containerReviews, vboxReviewList;
    @FXML private Button btnTabEpisodes, btnTabRecommended, btnTabReviews;
    @FXML private StackPane createRoomOverlay;
    @FXML private Label lblTitle, lblSelectedMovie,lblRoomTypeInfo;
    private Timeline syncTimeline;
    @FXML private CheckBox chkPublicRoom;

    private User currentUser;
    private List<String> selectedFriends = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEpisodes(25);
        loadRecommended();
        loadSampleReviews();
    }
    public void setCurrentUser(User user) {
        this.currentUser = user;
        App.currentUser = user; // Đảm bảo App.currentUser cũng được set
    }


    @FXML
    private void handleTabSwitch(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        containerEpisodes.setVisible(false); containerEpisodes.setManaged(false);
        containerRecommended.setVisible(false); containerRecommended.setManaged(false);
        containerReviews.setVisible(false); containerReviews.setManaged(false);
        btnTabEpisodes.getStyleClass().remove("tab-active");
        btnTabRecommended.getStyleClass().remove("tab-active");
        btnTabReviews.getStyleClass().remove("tab-active");

        if (clickedBtn == btnTabEpisodes) { containerEpisodes.setVisible(true); containerEpisodes.setManaged(true); }
        else if (clickedBtn == btnTabRecommended) { containerRecommended.setVisible(true); containerRecommended.setManaged(true); }
        else if (clickedBtn == btnTabReviews) { containerReviews.setVisible(true); containerReviews.setManaged(true); }
        clickedBtn.getStyleClass().add("tab-active");
    }


    @FXML
    private void handleOpenCreateRoom() {
        lblSelectedMovie.setText(lblTitle.getText().toUpperCase());
        createRoomOverlay.setVisible(true);
        createRoomOverlay.setManaged(true);
        createRoomOverlay.toFront();

        // ⭐ LOGIC QUAN TRỌNG: CHECK ĐĂNG NHẬP
        if (currentUser == null) {
            // ❌ CHƯA ĐĂNG NHẬP → CHỈ TẠO PUBLIC ROOM
            chkPublicRoom.setSelected(true);
            chkPublicRoom.setDisable(true); // Khóa checkbox

            // Ẩn phần chọn bạn bè
            flowFriendSelection.setVisible(false);
            flowFriendSelection.setManaged(false);

            // Hiện thông báo
            if (lblRoomTypeInfo != null) {
                lblRoomTypeInfo.setText("⚠️ Bạn chưa đăng nhập → Chỉ tạo được Phòng Công Khai");
                lblRoomTypeInfo.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 12px;");
            }

            System.out.println("🌍 Chế độ: PUBLIC ONLY (chưa đăng nhập)");
        } else {
            // ✅ ĐÃ ĐĂNG NHẬP → TẠO ĐƯỢC CẢ PUBLIC VÀ PRIVATE
            chkPublicRoom.setDisable(false); // Cho phép chọn
            loadFriendsToSelection(); // Load danh sách bạn bè

            if (lblRoomTypeInfo != null) {
                lblRoomTypeInfo.setText("💡 Bạn có thể tạo Phòng Công Khai hoặc Phòng Riêng Tư");
                lblRoomTypeInfo.setStyle("-fx-text-fill: #00ff40; -fx-font-size: 12px;");
            }

            System.out.println("🔐 Chế độ: PUBLIC/PRIVATE (đã đăng nhập)");
        }
    }

    // MovieDetailController.java
    private void loadFriendsToSelection() {
        flowFriendSelection.getChildren().clear();
        selectedFriends.clear(); // Danh sách lưu tên những người được tick chọn
        if (currentUser == null) {
            Label lblEmpty = new Label("Bạn chưa đăng nhập!");
            lblEmpty.setStyle("-fx-text-fill: #ff0000; -fx-padding: 20;");
            flowFriendSelection.getChildren().add(lblEmpty);
            return;
        }
        // 1. Kiểm tra xem Ngân đã đăng nhập chưa
        if (com.cinemates.App.currentUser == null) return;

        // 2. Lấy danh sách bạn bè THẬT từ Database (đã có trạng thái online/offline)
        List<User> myFriends = DatabaseHandler.getFriendsList(com.cinemates.App.currentUser.getId());

        boolean hasOnlineFriend = false;

        for (User friend : myFriends) {
            // --- CHỈ HIỂN THỊ NẾU BẠN ĐANG ONLINE ---
            if (friend.isOnline()) {
                hasOnlineFriend = true;

                // Tạo giao diện card cho từng người bạn
                VBox card = new VBox(10);
                card.getStyleClass().add("friend-select-item");
                card.setAlignment(javafx.geometry.Pos.CENTER);

                // Avatar màu xanh cho người đang online
                Label avatar = new Label(friend.getUsername().substring(0, 1).toUpperCase());
                avatar.getStyleClass().add("avatar-circle-green");

                Label lblName = new Label(friend.getUsername());
                lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                card.getChildren().addAll(avatar, lblName);

                // Logic khi nhấn vào để chọn (Toggle Selection)
                card.setOnMouseClicked(e -> {
                    if (card.getStyleClass().contains("friend-select-active")) {
                        card.getStyleClass().remove("friend-select-active");
                        selectedFriends.remove(friend.getUsername());
                    } else {
                        card.getStyleClass().add("friend-select-active");
                        selectedFriends.add(friend.getUsername());
                    }
                    System.out.println("👥 Đã chọn: " + selectedFriends);
                });

                flowFriendSelection.getChildren().add(card);
            }
        }

        // 3. Nếu không có ai online, hiện một dòng thông báo cho đỡ trống
        if (!hasOnlineFriend) {
            Label lblEmpty = new Label("Hố hố hố, hiện tại không có bạn nào online hết vợ ơi!");
            lblEmpty.setStyle("-fx-text-fill: #999999; -fx-font-style: italic; -fx-padding: 20;");
            flowFriendSelection.getChildren().add(lblEmpty);
        }
    }

    @FXML private void handleCloseCreateRoom() { createRoomOverlay.setVisible(false); createRoomOverlay.setManaged(false); }

    // MovieDetailController.java
    @FXML
    private void handleStartWatchingWithFriends(ActionEvent event) throws IOException {
        String movieName = lblTitle.getText();
        String roomCode = "CIN-" + (int)(Math.random() * 899 + 100) + "-MATES";

        // ⭐ SỬA: Dùng tên Guest cố định từ HomeController
        String myName;
        if (currentUser != null) {
            myName = currentUser.getUsername().trim();
        } else {
            // LẤY TÊN GUEST ĐÃ ĐĂNG KÝ SERVER
            myName = HomeController.getMyGuestName();
            if (myName == null) {
                myName = "Guest-" + (int)(Math.random() * 9999);
                System.err.println("⚠️ Chưa kết nối server, tạo tên tạm: " + myName);
            } else {
                System.out.println("✅ Dùng tên Guest đã đăng ký: " + myName);
            }
        }

        boolean isPublic = chkPublicRoom.isSelected();

        java.io.ObjectOutputStream serverOut = null;

        if (RegisterController.globalOut != null) {
            serverOut = RegisterController.globalOut;
            System.out.println("✅ Dùng connection từ RegisterController");
        } else {
            try {
                serverOut = HomeController.getServerConnection();
                System.out.println("✅ Dùng connection từ HomeController");
            } catch (Exception e) {
                System.err.println("⚠️ Không thể lấy connection từ HomeController");
            }
        }

        if (serverOut != null) {
            try {
                String registerCmd = "REGISTER_ROOM|" + roomCode + "|" + myName + "|" + movieName + "|" + isPublic;
                serverOut.writeObject(registerCmd);
                serverOut.flush();

                System.out.println("📡 Đã đăng ký phòng với Server:");
                System.out.println("   Mã phòng: " + roomCode);
                System.out.println("   Host: " + myName);
                System.out.println("   Phim: " + movieName);
                System.out.println("   Loại: " + (isPublic ? "PUBLIC ✅" : "PRIVATE 🔒"));

                if (!isPublic && currentUser != null) {
                    String myIp = java.net.InetAddress.getLocalHost().getHostAddress();
                    for (String friendName : selectedFriends) {
                        String inviteCmd = "INVITE|" + myName + "|" + friendName.trim() + "|" + roomCode + "|" + movieName + "|" + myIp;
                        serverOut.writeObject(inviteCmd);
                        serverOut.flush();
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi đăng ký phòng: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("⚠️ CẢNH BÁO: Không có kết nối Server!");
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/WatchRoom.fxml"));
        Parent root = loader.load();
        WatchRoomController controller = loader.getController();
        controller.initRoomData(movieName, selectedFriends, roomCode, true);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
    }


    private void loadEpisodes(int total) { flowEpisodes.getChildren().clear(); for (int i = 1; i <= total; i++) { Button btn = new Button("▶ Tập " + i); btn.getStyleClass().add("btn-episode"); flowEpisodes.getChildren().add(btn); } }
    private void loadRecommended() { flowRecommended.getChildren().clear(); String[] movies = {"Trường Nguyệt Tẫn Minh", "Dữ Phượng Hành"}; for (String m : movies) { VBox card = new VBox(10); Region img = new Region(); img.setPrefSize(180, 260); img.setStyle("-fx-background-color: #2d2f34;"); card.getChildren().addAll(img, new Label(m)); flowRecommended.getChildren().add(card); } }
    private void loadSampleReviews() { vboxReviewList.getChildren().addAll(new Label("Kim Ngân: Hay quá!"), new Label("Hằng: Giao diện xịn!")); }
    @FXML private void handleBackToHome(ActionEvent event) throws IOException { Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/Home.fxml")))); }
}