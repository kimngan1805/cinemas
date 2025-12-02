package com.cinemates.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignalingServer {
    private static final int PORT = 5000;

    // Lưu danh sách phòng đang hoạt động
    private static final Map<String, String> activeRooms = new HashMap<>();

    public static void main(String[] args) {
        System.out.println(">>> SERVER BÀ MỐI ĐANG CHẠY TẠI PORT " + PORT + " <<<");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String myRoomId = null; // Lưu mã phòng mà người này đang làm chủ

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("📩 " + socket.getInetAddress().getHostAddress() + ": " + request);

                    String[] parts = request.split(" ");
                    String command = parts[0];

                    // 1. TẠO PHÒNG
                    if (command.equals("CREATE")) {
                        String roomId = String.valueOf(1000 + new Random().nextInt(9000));
                        String hostIp = socket.getInetAddress().getHostAddress();

                        activeRooms.put(roomId, hostIp);
                        this.myRoomId = roomId; // Đánh dấu người này là chủ phòng này

                        out.println("CREATED " + roomId);
                        System.out.println("✅ Phòng " + roomId + " được tạo bởi " + hostIp);
                    }

                    // 2. VÀO PHÒNG
                    else if (command.equals("JOIN")) {
                        if (parts.length < 2) {
                            out.println("ERROR"); continue;
                        }
                        String roomId = parts[1];

                        if (activeRooms.containsKey(roomId)) {
                            String hostIp = activeRooms.get(roomId);
                            out.println("FOUND " + hostIp);
                            System.out.println("🔗 Chỉ đường tới " + hostIp);
                        } else {
                            out.println("NOT_FOUND");
                        }
                    }
                }
            } catch (Exception e) {
                // Khi client ngắt kết nối đột ngột
            } finally {
                // Dọn dẹp: Nếu chủ phòng thoát, xóa phòng đó đi
//                if (myRoomId != null) {
//                    activeRooms.remove(myRoomId);
//                    System.out.println("❌ Chủ phòng đã thoát. Đã xóa phòng: " + myRoomId);
//                }
                try { socket.close(); } catch (Exception e) {}
            }
        }
    }
}