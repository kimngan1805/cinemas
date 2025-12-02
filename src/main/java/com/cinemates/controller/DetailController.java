package com.cinemates.controller;

import com.cinemates.model.Movie;
import com.cinemates.utils.DatabaseHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DetailController implements Initializable {

    // --- 1. CÁC NÚT BẤM (BUTTON) ---
    @FXML private Button watchNowBtn;      // Nút xem một mình
    @FXML private Button watchPartyBtn;    // Nút xem chung
    @FXML private Button backToGalleryBtn; // Nút quay lại Gallery

    // --- 2. CÁC THÀNH PHẦN HIỂN THỊ DỮ LIỆU ---
    // (Lưu ý: Phải đặt fx:id bên detail.fxml y chang mấy cái tên này nha)
    @FXML private Label titleLabel;       // Tên phim to đùng (VD: GOLAM)
    @FXML private Label subTitleLabel;    // Tên phim nhỏ (VD: Golam 2024)
    @FXML private ImageView posterImg;    // Ảnh bìa
    @FXML private Text descText;          // Nội dung phim

    // Mấy cái này nếu bên FXML chưa có thì nó sẽ null (không sao cả)
    @FXML private Label genreLabel;
    @FXML private Label nationLabel;

    private Movie currentMovie; // Biến để lưu phim hiện tại

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // 1. Logic nút "XEM MỘT MÌNH" (Truyền false)
        watchNowBtn.setOnAction(event -> goToPlayer(event, false));

        // 2. Logic nút "TẠO RẠP CHIẾU" (Truyền true)
        watchPartyBtn.setOnAction(event -> goToPlayer(event, true));

        // 3. Logic nút "VỀ TRANG CHỦ"
        backToGalleryBtn.setOnAction(event -> {
            try {
                System.out.println("Quay lại trang chủ...");
                Parent galleryView = FXMLLoader.load(getClass().getResource("/gallery.fxml"));
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(new Scene(galleryView));
                window.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // --- HÀM NHẬN DỮ LIỆU TỪ GALLERY (QUAN TRỌNG) ---
    public void setMovieData(int movieId) {
        // 1. Gọi Database lấy thông tin phim
        this.currentMovie = DatabaseHandler.getMovieById(movieId);

        if (this.currentMovie != null) {
            System.out.println("Đang hiển thị phim: " + currentMovie.getTitle());

            // 2. Điền thông tin vào giao diện
            if (titleLabel != null) titleLabel.setText(currentMovie.getTitle().toUpperCase());
            if (subTitleLabel != null) subTitleLabel.setText(currentMovie.getTitle());
            if (descText != null) descText.setText(currentMovie.getDescription());

            // 3. Load ảnh Poster (Nếu có link)
            if (posterImg != null && currentMovie.getPosterUrl() != null) {
                try {
                    // Load ảnh từ link (true, true để load ngầm cho mượt)
                    Image image = new Image(currentMovie.getPosterUrl(), 300, 450, true, true);
                    posterImg.setImage(image);
                } catch (Exception e) {
                    System.out.println("Lỗi load ảnh detail: " + e.getMessage());
                }
            }
        }
    }

    // --- HÀM CHUYỂN SANG PLAYER ---
    private void goToPlayer(javafx.event.ActionEvent event, boolean isP2P) {
        if (currentMovie == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/player.fxml"));
            Parent playerView = loader.load();

            PlayerController controller = loader.getController();

            // GỌI HÀM MỚI NÈ:
            controller.setMovieToPlay(currentMovie, isP2P);

            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(new Scene(playerView));
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}