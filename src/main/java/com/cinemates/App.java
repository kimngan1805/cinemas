package com.cinemates;

import com.cinemates.model.User; // Nhớ import model User
import com.cinemates.utils.DatabaseHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class App extends Application {

    // THÊM DÒNG NÀY: Biến tĩnh để lưu User đang dùng máy
    public static User currentUser;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/Home.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 800);

        stage.setTitle("Cinemates - P2P Watch Party");
        stage.setScene(scene);
        stage.show();

        // Xử lý khi nhấn nút X tắt app
        stage.setOnCloseRequest(event -> {
            if (currentUser != null) {
                // 1. Cập nhật DB về Offline
                DatabaseHandler.updateOnlineStatus(currentUser.getId(), false);
                System.out.println("👋 Đã set Offline cho " + currentUser.getUsername());
            }
            System.exit(0); // Tắt sạch các luồng chạy ngầm
        });
    }

    public static void main(String[] args) {
        launch();
    }
}