package com.cinemates.controller;

import com.cinemates.model.Movie;
import com.cinemates.utils.DatabaseHandler;
import com.cinemates.utils.DriveUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GalleryController implements Initializable {

    // Liên kết với cái ScrollPane trong file FXML qua cái ID
    @FXML
    private ScrollPane trendingScrollPane;
    @FXML private HBox newMoviesContainer; // Cái hộp mình mới đặt tên
    @FXML
    public void goToDetail(javafx.scene.input.MouseEvent event) throws java.io.IOException {
        System.out.println("Đã chọn phim! Đang vào trang chi tiết...");

        // Load file detail.fxml
        javafx.scene.Parent detailView = javafx.fxml.FXMLLoader.load(getClass().getResource("/detail.fxml"));

        // Lấy cửa sổ hiện tại và đổi cảnh
        javafx.stage.Stage window = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        window.setScene(new javafx.scene.Scene(detailView));
        window.show();
    }
    private void loadMoviesFromDB() {
        // Lấy danh sách phim
        List<Movie> movies = DatabaseHandler.getRecentlyAddedMovies();

        if (newMoviesContainer != null) {
            newMoviesContainer.getChildren().clear(); // Xóa sạch dữ liệu cũ

            for (Movie movie : movies) {
                try {
                    // --- TẠO GIAO DIỆN CHO 1 PHIM ---

                    // 1. Tạo cái khung (VBox)
                    VBox card = new VBox();
                    card.setSpacing(10);
                    card.setStyle("-fx-cursor: hand; -fx-alignment: TOP_CENTER;");
                    card.setPrefWidth(150);

                    // 2. Tạo Poster (ImageView)
                    String linkAnh = movie.getPosterUrl();
                    ImageView poster = new ImageView();
                    // Load ảnh (true, true để load ngầm cho mượt)
                    Image image = new Image(linkAnh, 200, 300, true, true);
                    poster.setImage(image);
                    poster.setFitWidth(150);
                    poster.setFitHeight(220);
                    poster.setPreserveRatio(true);

                    // 3. Tạo Tên phim (Label)
                    Label title = new Label(movie.getTitle());
                    title.setTextFill(Color.WHITE);
                    title.setWrapText(true);
                    title.setFont(Font.font("System", FontWeight.BOLD, 14));
                    title.setMaxWidth(150);

                    // 4. Nhét vào khung
                    card.getChildren().addAll(poster, title);

                    // 5. Sự kiện Click (in ra console chơi thôi, chưa chuyển trang)
                    card.setOnMouseClicked(e -> {
                        try {
                            // 1. Load file Detail
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/detail.fxml"));
                            Parent root = loader.load();

                            // 2. Lấy controller của trang Detail
                            DetailController detailController = loader.getController();

                            // 3. Truyền ID phim qua (QUAN TRỌNG NHẤT)
                            detailController.setMovieData(movie.getId());

                            // 4. Chuyển cảnh
                            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
                            stage.setScene(new Scene(root));
                            stage.show();

                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });;

                    // 6. Bỏ khung vào danh sách hiển thị
                    newMoviesContainer.getChildren().add(card);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Tạo một dòng thời gian, cứ 50 mili-giây (0.05s) thì chạy một lần
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(50), event -> {
            // Lấy vị trí hiện tại của thanh trượt (từ 0.0 đến 1.0)
            double currentValue = trendingScrollPane.getHvalue();

            // Nếu đã chạy gần tới cuối (ví dụ > 0.99)
            if (currentValue >= trendingScrollPane.getHmax() - 0.01) {
                // Thì quay đầu về lại vị trí 0
                trendingScrollPane.setHvalue(0);
            } else {
                // Nếu chưa tới cuối, thì nhích thêm một chút xíu (0.002)
                // Số này càng nhỏ thì chạy càng chậm và mượt
                trendingScrollPane.setHvalue(currentValue + 0.002);
            }
        }));
        loadMoviesFromDB();

        // Cho nó chạy lặp đi lặp lại vô tận
        timeline.setCycleCount(Timeline.INDEFINITE);
        // Bắt đầu chạy
        timeline.play();
    }
}