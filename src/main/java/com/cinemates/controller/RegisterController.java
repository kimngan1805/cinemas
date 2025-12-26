package com.cinemates.controller;

import com.cinemates.App;
import com.cinemates.model.User;
import com.cinemates.utils.DatabaseHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Optional;

public class RegisterController {
    @FXML private VBox vboxRegister, vboxLogin;
    @FXML private Label lblTitle, lblSubtitle;
    @FXML private TextField txtRegUser, txtRegEmail, txtLogUser;
    @FXML private PasswordField txtRegPass, txtRegConfirm, txtLogPass;

    public static ObjectOutputStream globalOut;
    private static HomeController activeHomeController; // ⭐ THÊM BIẾN NÀY

    @FXML private void showLoginForm() { vboxRegister.setVisible(false); vboxRegister.setManaged(false); vboxLogin.setVisible(true); vboxLogin.setManaged(true); }
    @FXML private void showRegisterForm() { vboxLogin.setVisible(false); vboxLogin.setManaged(false); vboxRegister.setVisible(true); vboxRegister.setManaged(true); }

    @FXML
    private void handleLoginAction(ActionEvent event) {
        String userStr = txtLogUser.getText();
        String pass = txtLogPass.getText();

        int userId = DatabaseHandler.checkLogin(userStr, pass);
        if (userId != -1) {
            User loggedInUser = new User(userId, userStr, true);
            notifyCentralServer(loggedInUser);
            goToHome(event, loggedInUser);
        } else {
            System.out.println("❌ Sai tên hoặc mật khẩu rồi vợ ơi!");
        }
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        String user = txtRegUser.getText();
        String email = txtRegEmail.getText();
        String pass = txtRegPass.getText();

        if (DatabaseHandler.registerUser(user, email, pass)) {
            int userId = DatabaseHandler.checkLogin(user, pass);
            User newUser = new User(userId, user, true);
            notifyCentralServer(newUser);
            goToHome(event, newUser);
        }
    }

    private void goToHome(ActionEvent event, User user) {
        try {
            App.currentUser = user;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Home.fxml"));
            Parent root = loader.load();
            HomeController homeController = loader.getController();
            homeController.setLoggedInUser(user);

            // ⭐ LƯU REFERENCE ĐỂ DÙNG SAU
            activeHomeController = homeController;

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void notifyCentralServer(User user) {
        new Thread(() -> {
            int discoveryPort = 6789;
            try {
                Socket socket = new Socket("localhost", discoveryPort);
                globalOut = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                String myIp = InetAddress.getLocalHost().getHostAddress();
                String registerMsg = user.getUsername() + "|" + myIp + "|5000";
                globalOut.writeObject(registerMsg);
                globalOut.flush();
                System.out.println("📡 Đã báo danh: " + registerMsg);

                while (true) {
                    Object msg = in.readObject();
                    if (msg instanceof String) {
                        String cmd = msg.toString();
                        Platform.runLater(() -> handleServerCommand(cmd));
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Mất kết nối với Server trung tâm!");
            }
        }).start();
    }

    // ⭐ SỬA HÀM NÀY
    private void handleServerCommand(String cmd) {
        String[] parts = cmd.split("\\|");

        // ⭐ XỬ LÝ PUBLIC_ROOMS
        if (cmd.startsWith("PUBLIC_ROOMS|")) {
            System.out.println("📺 Nhận danh sách phòng: " + cmd);

            // Forward cho HomeController nếu có
            if (activeHomeController != null) {
                Platform.runLater(() -> {
                    activeHomeController.updatePublicRoomsList(cmd);
                });
            } else {
                // Nếu chưa có HomeController, thử tìm trong Scene hiện tại
                Platform.runLater(() -> {
                    try {
                        javafx.stage.Window window = javafx.stage.Window.getWindows().stream()
                                .filter(w -> w instanceof Stage && w.isShowing())
                                .findFirst().orElse(null);

                        if (window != null) {
                            Scene scene = ((Stage) window).getScene();
                            Parent root = scene.getRoot();

                            // Tìm controller trong root
                            Object userData = root.getUserData();
                            if (userData instanceof HomeController) {
                                HomeController controller = (HomeController) userData;
                                controller.updatePublicRoomsList(cmd);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Không tìm thấy HomeController: " + e.getMessage());
                    }
                });
            }
        }

        else if (cmd.startsWith("ROOM_FOUND|")) {
            String roomCode = parts[1];
            String[] address = parts[2].split(":");
            String hostIp = address[0];

            Platform.runLater(() -> {
                loadWatchRoom("Phim xem chung", roomCode, hostIp, false);
            });
        }
        else if (cmd.startsWith("ROOM_NOT_FOUND")) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Mã phòng không tồn tại!");
                alert.show();
            });
        }
        else if (cmd.startsWith("INVITE|")) {
            showInvitePopup(parts[1], parts[3], parts[4], parts[5]);
        }
    }

    private void showInvitePopup(String sender, String roomCode, String movieName, String hostIp) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(sender + " mời vợ xem phim nè!");
        alert.setContentText("Phim: " + movieName);

        ButtonType btnAccept = new ButtonType("Vào luôn");
        ButtonType btnDecline = new ButtonType("Bận rồi", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnAccept, btnDecline);

        alert.showAndWait().ifPresent(type -> {
            if (type == btnAccept) {
                loadWatchRoom(movieName, roomCode, hostIp, false);
            }
        });
    }

    private void loadWatchRoom(String movie, String code, String hostIp, boolean isHost) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/WatchRoom.fxml"));
                Parent root = loader.load();
                WatchRoomController controller = loader.getController();

                controller.initRoomData(movie, null, code, isHost);

                if (!isHost) {
                    controller.connectToHost(hostIp);
                }

                Stage stage = (Stage) javafx.stage.Window.getWindows().stream()
                        .filter(w -> w instanceof Stage && w.isShowing()).findFirst().orElse(null);
                if (stage != null) stage.setScene(new Scene(root));
            } catch (IOException e) { e.printStackTrace(); }
        });
    }

    @FXML
    private void handleBackToHome(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/Home.fxml"))));
    }
}