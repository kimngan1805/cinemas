package com.cinemates.controller;

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

public class MovieDetailController implements Initializable {
    @FXML private FlowPane flowEpisodes, flowRecommended, flowFriendSelection;
    @FXML private VBox containerEpisodes, containerRecommended, containerReviews, vboxReviewList;
    @FXML private Button btnTabEpisodes, btnTabRecommended, btnTabReviews;
    @FXML private StackPane createRoomOverlay;
    @FXML private Label lblTitle, lblSelectedMovie;

    private List<String> selectedFriends = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEpisodes(25);
        loadRecommended();
        loadSampleReviews();
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
        loadFriendsToSelection();
    }

    private void loadFriendsToSelection() {
        flowFriendSelection.getChildren().clear();
        selectedFriends.clear();
        String[] myFriends = {"Kim Ngân", "Bé Hằng", "Anh Tài", "Mỹ Linh", "Gia Bảo"};

        for (String name : myFriends) {
            VBox card = new VBox(10);
            card.getStyleClass().add("friend-select-item");
            card.setAlignment(javafx.geometry.Pos.CENTER);
            Label avatar = new Label(name.substring(0, 1).toUpperCase());
            avatar.getStyleClass().add("avatar-circle-green");
            Label lblName = new Label(name);
            lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            card.getChildren().addAll(avatar, lblName);

            card.setOnMouseClicked(e -> {
                if (card.getStyleClass().contains("friend-select-active")) {
                    card.getStyleClass().remove("friend-select-active");
                    selectedFriends.remove(name);
                } else {
                    card.getStyleClass().add("friend-select-active");
                    selectedFriends.add(name);
                }
            });
            flowFriendSelection.getChildren().add(card);
        }
    }

    @FXML private void handleCloseCreateRoom() { createRoomOverlay.setVisible(false); createRoomOverlay.setManaged(false); }

    @FXML
    private void handleStartWatchingWithFriends(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/WatchRoom.fxml"));
        Parent root = loader.load();

        // TRUYỀN DỮ LIỆU SANG WATCH ROOM
        WatchRoomController controller = loader.getController();
        controller.initRoomData(lblTitle.getText(), selectedFriends);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void loadEpisodes(int total) { flowEpisodes.getChildren().clear(); for (int i = 1; i <= total; i++) { Button btn = new Button("▶ Tập " + i); btn.getStyleClass().add("btn-episode"); flowEpisodes.getChildren().add(btn); } }
    private void loadRecommended() { flowRecommended.getChildren().clear(); String[] movies = {"Trường Nguyệt Tẫn Minh", "Dữ Phượng Hành"}; for (String m : movies) { VBox card = new VBox(10); Region img = new Region(); img.setPrefSize(180, 260); img.setStyle("-fx-background-color: #2d2f34;"); card.getChildren().addAll(img, new Label(m)); flowRecommended.getChildren().add(card); } }
    private void loadSampleReviews() { vboxReviewList.getChildren().addAll(new Label("Kim Ngân: Hay quá!"), new Label("Hằng: Giao diện xịn!")); }
    @FXML private void handleBackToHome(ActionEvent event) throws IOException { Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow(); stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/Home.fxml")))); }
}